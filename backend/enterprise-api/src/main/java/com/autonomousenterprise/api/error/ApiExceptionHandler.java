package com.autonomousenterprise.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);
	private static final String CORRELATION_ID_MDC_KEY = "correlationId";
	private static final String TRACE_ID_MDC_KEY = "traceId";
	private static final int MAX_CORRELATION_ID_LENGTH = 128;

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<ApiFieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldError)
				.toList();
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
				"Request validation failed.", request, fieldErrors);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidation(
			HandlerMethodValidationException exception, HttpServletRequest request) {
		List<ApiFieldError> fieldErrors = new ArrayList<>();
		for (ParameterValidationResult result : exception.getParameterValidationResults()) {
			String parameterName = result.getMethodParameter().getParameterName();
			for (var error : result.getResolvableErrors()) {
				String field = error instanceof FieldError fieldError
						? fieldError.getField()
						: safeFieldName(parameterName);
				fieldErrors.add(new ApiFieldError(field, safeValidationMessage(error.getDefaultMessage())));
			}
		}
		exception.getCrossParameterValidationResults().forEach(error ->
				fieldErrors.add(new ApiFieldError("parameter", safeValidationMessage(error.getDefaultMessage()))));
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
				"Request validation failed.", request, fieldErrors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
			ConstraintViolationException exception, HttpServletRequest request) {
		List<ApiFieldError> fieldErrors = exception.getConstraintViolations().stream()
				.map(this::toFieldError)
				.toList();
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
				"Request validation failed.", request, fieldErrors);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiErrorResponse> handleMissingRequestParameter(
			MissingServletRequestParameterException exception, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "MISSING_REQUEST_PARAMETER",
				"A required request parameter is missing.", request, List.of());
	}

	@ExceptionHandler(TypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
			TypeMismatchException exception, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_PARAMETER",
				"A request parameter has an invalid value.", request, List.of());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
			HttpMessageNotReadableException exception, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
				"Request body is missing or malformed.", request, List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
			NoResourceFoundException exception, HttpServletRequest request) {
		return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
				"The requested resource was not found.", request, List.of());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
			ResponseStatusException exception, HttpServletRequest request) {
		HttpStatusCode status = exception.getStatusCode();
		boolean clientError = status.is4xxClientError();
		if (!clientError && !status.is5xxServerError()) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		String errorCode = clientError ? "REQUEST_REJECTED" : "INTERNAL_SERVER_ERROR";
		String message = clientError
				? "The request could not be completed."
				: "An unexpected error occurred.";
		return response(status, errorCode, message, request, List.of());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
			HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
		Set<org.springframework.http.HttpMethod> supportedMethods = exception.getSupportedHttpMethods();
		if (supportedMethods != null) {
			headers.setAllow(supportedMethods);
		}
		return response(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
				"HTTP method is not supported for this resource.", request, List.of(), headers);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
			HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
		List<MediaType> supportedMediaTypes = exception.getSupportedMediaTypes();
		if (supportedMediaTypes != null && !supportedMediaTypes.isEmpty()) {
			headers.setAccept(supportedMediaTypes);
		}
		return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
				"Content type is not supported for this resource.", request, List.of(), headers);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
			Exception exception, HttpServletRequest request) {
		String correlationId = correlationId();
		logger.error("Unhandled API exception; type={}, correlationId={}",
				exception.getClass().getSimpleName(), correlationId);
		return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
				"An unexpected error occurred.", request, List.of(), correlationId, HttpHeaders.EMPTY);
	}

	private ApiFieldError toFieldError(FieldError error) {
		return new ApiFieldError(error.getField(), safeValidationMessage(error.getDefaultMessage()));
	}

	private ApiFieldError toFieldError(ConstraintViolation<?> violation) {
		String field = "parameter";
		for (var node : violation.getPropertyPath()) {
			if (node.getName() != null && !node.getName().isBlank()) {
				field = node.getName();
			}
		}
		return new ApiFieldError(safeFieldName(field), safeValidationMessage(violation.getMessage()));
	}

	private String safeFieldName(String field) {
		return field == null || field.isBlank() ? "parameter" : field;
	}

	private String safeValidationMessage(String message) {
		return message == null || message.isBlank() ? "Invalid value." : message;
	}

	private ResponseEntity<ApiErrorResponse> response(
			HttpStatusCode status, String errorCode, String message, HttpServletRequest request,
			List<ApiFieldError> fieldErrors) {
		return response(status, errorCode, message, request, fieldErrors, correlationId(), HttpHeaders.EMPTY);
	}

	private ResponseEntity<ApiErrorResponse> response(
			HttpStatusCode status, String errorCode, String message, HttpServletRequest request,
			List<ApiFieldError> fieldErrors, HttpHeaders headers) {
		return response(status, errorCode, message, request, fieldErrors, correlationId(), headers);
	}

	private ResponseEntity<ApiErrorResponse> response(
			HttpStatusCode status, String errorCode, String message, HttpServletRequest request,
			List<ApiFieldError> fieldErrors, String correlationId, HttpHeaders headers) {
		List<ApiFieldError> sortedFieldErrors = fieldErrors.stream()
				.sorted(Comparator.comparing(ApiFieldError::field).thenComparing(ApiFieldError::message))
				.toList();
		ApiErrorResponse body = new ApiErrorResponse(
				Instant.now(),
				status.value(),
				errorCode,
				message,
				request.getRequestURI(),
				correlationId,
				sortedFieldErrors);
		return new ResponseEntity<>(body, headers, status);
	}

	private String correlationId() {
		String value = MDC.get(CORRELATION_ID_MDC_KEY);
		if (value == null || value.isBlank()) {
			value = MDC.get(TRACE_ID_MDC_KEY);
		}
		if (value == null || value.isBlank()) {
			return null;
		}

		StringBuilder sanitized = new StringBuilder(Math.min(value.length(), MAX_CORRELATION_ID_LENGTH));
		for (int index = 0; index < value.length() && sanitized.length() < MAX_CORRELATION_ID_LENGTH; index++) {
			char character = value.charAt(index);
			if (Character.isLetterOrDigit(character) || character == '-' || character == '_' || character == '.' || character == ':') {
				sanitized.append(character);
			}
		}
		return sanitized.isEmpty() ? null : sanitized.toString();
	}
}
