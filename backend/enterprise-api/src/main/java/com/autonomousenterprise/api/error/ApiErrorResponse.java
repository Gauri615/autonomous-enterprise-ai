package com.autonomousenterprise.api.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String errorCode,
		String message,
		String path,
		String correlationId,
		List<ApiFieldError> fieldErrors) {
}
