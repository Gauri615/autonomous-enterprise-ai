package com.autonomousenterprise.api.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Correlation-ID";
	public static final String MDC_KEY = "correlationId";
	private static final int MAX_ID_LENGTH = 128;
	private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String correlationId = validatedIncomingId(request.getHeader(HEADER_NAME));
		if (correlationId == null) {
			correlationId = UUID.randomUUID().toString();
		}

		MDC.put(MDC_KEY, correlationId);
		response.setHeader(HEADER_NAME, correlationId);
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			if (!response.isCommitted()) {
				response.setHeader(HEADER_NAME, correlationId);
			}
			MDC.remove(MDC_KEY);
		}
	}

	private String validatedIncomingId(String candidate) {
		if (candidate == null || candidate.length() > MAX_ID_LENGTH || !VALID_ID.matcher(candidate).matches()) {
			return null;
		}
		return candidate;
	}
}
