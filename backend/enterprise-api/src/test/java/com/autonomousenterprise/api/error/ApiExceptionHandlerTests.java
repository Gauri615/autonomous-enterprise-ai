package com.autonomousenterprise.api.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.autonomousenterprise.api.correlation.CorrelationIdFilter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiExceptionHandlerTests.TestControllerConfiguration.class)
class ApiExceptionHandlerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Validator validator;

	@Test
	void validationFailureUsesStandardErrorResponseAndFieldDetails() throws Exception {
		mockMvc.perform(post("/_test/errors/validated")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.message").value("Request validation failed."))
				.andExpect(jsonPath("$.path").value("/_test/errors/validated"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("must not be blank"));
	}

	@Test
	void constraintValidationFailureUsesStandardResponse() throws Exception {
		mockMvc.perform(get("/_test/errors/minimum").param("value", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("value"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("must be greater than or equal to 1"));
	}

	@Test
	void constraintViolationExceptionUsesStandardResponse() {
		var violations = validator.validate(new ValidatedRequest(""));
		var exception = new ConstraintViolationException(violations);
		var request = new MockHttpServletRequest("GET", "/_test/errors/constraint");

		ResponseEntity<ApiErrorResponse> response =
				new ApiExceptionHandler().handleConstraintViolation(exception, request);

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_FAILED");
		assertThat(response.getBody().fieldErrors())
				.contains(new ApiFieldError("name", "must not be blank"));
	}

	@Test
	void correlationIdIsIncludedWhenValidIncomingIdIsAvailable() throws Exception {
		mockMvc.perform(get("/_test/errors/required")
						.header(CorrelationIdFilter.HEADER_NAME, "request-123"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.correlationId").value("request-123"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string(CorrelationIdFilter.HEADER_NAME, "request-123"));
	}

	@Test
	void missingIncomingIdGeneratesUniqueIdForResponseAndErrorBody() throws Exception {
		var first = mockMvc.perform(get("/_test/errors/required"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.correlationId").isNotEmpty())
				.andReturn();
		var second = mockMvc.perform(get("/_test/errors/required"))
				.andExpect(status().isBadRequest())
				.andReturn();

		String firstId = first.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);
		String secondId = second.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);
		assertThat(firstId).isNotBlank().matches("[0-9a-f-]{36}");
		assertThat(first.getResponse().getContentAsString()).contains("\"correlationId\":\"" + firstId + "\"");
		assertThat(secondId).isNotBlank().isNotEqualTo(firstId);
		assertThat(second.getResponse().getContentAsString()).contains("\"correlationId\":\"" + secondId + "\"");
	}

	@Test
	void invalidIncomingIdIsReplacedWithGeneratedId() throws Exception {
		mockMvc.perform(get("/_test/errors/required")
						.header(CorrelationIdFilter.HEADER_NAME, "invalid id"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.correlationId").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string(CorrelationIdFilter.HEADER_NAME, org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")));
	}

	@Test
	void excessivelyLongIncomingIdIsReplacedWithGeneratedId() throws Exception {
		String tooLongId = "a".repeat(129);
		mockMvc.perform(get("/_test/errors/required")
						.header(CorrelationIdFilter.HEADER_NAME, tooLongId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.correlationId").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")));
	}

	@Test
	void correlationIdMdcIsRemovedAfterRequest() throws Exception {
		var result = mockMvc.perform(get("/_test/errors/mdc"))
				.andExpect(status().isOk())
				.andReturn();
		String correlationId = result.getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);

		assertThat(result.getResponse().getContentAsString()).isEqualTo(correlationId);
		assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
	}

	@Test
	void malformedJsonReturnsGenericBadRequest() throws Exception {
		mockMvc.perform(post("/_test/errors/validated")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
				.andExpect(jsonPath("$.message").value("Request body is missing or malformed."));
	}

	@Test
	void missingRequestParameterReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/_test/errors/required"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("MISSING_REQUEST_PARAMETER"));
	}

	@Test
	void invalidRequestParameterReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/_test/errors/minimum").param("value", "not-a-number"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_PARAMETER"));
	}

	@Test
	void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
		mockMvc.perform(get("/_test/errors/post-only"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void unsupportedMediaTypeReturnsUnsupportedMediaType() throws Exception {
		mockMvc.perform(post("/_test/errors/json-only")
						.contentType(MediaType.TEXT_PLAIN)
						.content("plain text"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));
	}

	@Test
	void unexpectedExceptionDoesNotExposeInternalDetails() throws Exception {
		String response = mockMvc.perform(get("/_test/errors/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.message").value("An unexpected error occurred."))
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(response)
				.doesNotContain("sensitive exception message")
				.doesNotContain("IllegalStateException")
				.doesNotContain("at com.autonomousenterprise");
	}

	@Test
	void actuatorHealthEndpointRemainsAvailable() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void unknownResourceReturnsNotFound() throws Exception {
		mockMvc.perform(get("/_test/errors/unknown"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestControllerConfiguration {
		@Bean
		ErrorTestController errorTestController() {
			return new ErrorTestController();
		}
	}

	@RestController
	static class ErrorTestController {

		@PostMapping("/_test/errors/validated")
		void validated(@RequestBody @Valid ValidatedRequest request) {
		}

		@GetMapping("/_test/errors/minimum")
		int minimum(@RequestParam @Min(1) int value) {
			return value;
		}

		@GetMapping("/_test/errors/required")
		String required(@RequestParam String value) {
			return value;
		}

		@PostMapping("/_test/errors/post-only")
		void postOnly() {
		}

		@PostMapping(path = "/_test/errors/json-only", consumes = MediaType.APPLICATION_JSON_VALUE)
		void jsonOnly(@RequestBody String body) {
		}

		@GetMapping("/_test/errors/unexpected")
		void unexpected() {
			throw new IllegalStateException("sensitive exception message");
		}

		@GetMapping("/_test/errors/mdc")
		String mdc() {
			return MDC.get(CorrelationIdFilter.MDC_KEY);
		}
	}

	record ValidatedRequest(@NotBlank String name) {
	}
}
