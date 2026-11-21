package com.autonomousenterprise.api.logging;

import com.autonomousenterprise.api.correlation.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
	private static final String API_PATH = "/api";
	private static final String FALLBACK_ROUTE = "/api/**";
	private static final String EVENT_NAME = "api_request_completed";

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!isApiRequest(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		long startTime = System.nanoTime();
		boolean requestFailed = false;
		try {
			filterChain.doFilter(request, response);
		}
		catch (IOException | ServletException | RuntimeException | Error exception) {
			requestFailed = true;
			throw exception;
		}
		finally {
			long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
			int responseStatus = response.getStatus();
			if (requestFailed && responseStatus < HttpServletResponse.SC_BAD_REQUEST) {
				responseStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			}
			logCompletion(request, responseStatus, durationMs);
		}
	}

	private boolean isApiRequest(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		String requestPath = request.getRequestURI().substring(contextPath.length());
		return requestPath.equals(API_PATH) || requestPath.startsWith(API_PATH + "/");
	}

	private void logCompletion(HttpServletRequest request, int status, long durationMs) {
		var event = switch (status / 100) {
			case 4 -> logger.atWarn();
			case 5 -> logger.atError();
			default -> logger.atInfo();
		};
		event.addKeyValue("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY))
				.addKeyValue("method", request.getMethod())
				.addKeyValue("route", routePattern(request))
				.addKeyValue("status", status)
				.addKeyValue("durationMs", durationMs)
				.log(EVENT_NAME);
	}

	private String routePattern(HttpServletRequest request) {
		Object routePattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		return routePattern == null ? FALLBACK_ROUTE : routePattern.toString();
	}
}
