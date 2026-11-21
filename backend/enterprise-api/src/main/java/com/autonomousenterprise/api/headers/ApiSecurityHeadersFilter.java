package com.autonomousenterprise.api.headers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiSecurityHeadersFilter extends OncePerRequestFilter {

	private static final String API_PATH = "/api";
	private static final String CONTENT_TYPE_OPTIONS_HEADER = "X-Content-Type-Options";
	private static final String FRAME_OPTIONS_HEADER = "X-Frame-Options";
	private static final String REFERRER_POLICY_HEADER = "Referrer-Policy";
	private static final String PERMISSIONS_POLICY_HEADER = "Permissions-Policy";
	private static final String CACHE_CONTROL_HEADER = "Cache-Control";

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!isApiRequest(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		setSecurityHeaders(response);
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			if (!response.isCommitted()) {
				setSecurityHeaders(response);
			}
		}
	}

	private boolean isApiRequest(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		String requestPath = request.getRequestURI().substring(contextPath.length());
		return requestPath.equals(API_PATH) || requestPath.startsWith(API_PATH + "/");
	}

	private void setSecurityHeaders(HttpServletResponse response) {
		response.setHeader(CONTENT_TYPE_OPTIONS_HEADER, "nosniff");
		response.setHeader(FRAME_OPTIONS_HEADER, "DENY");
		response.setHeader(REFERRER_POLICY_HEADER, "strict-origin-when-cross-origin");
		response.setHeader(PERMISSIONS_POLICY_HEADER,
				"camera=(), microphone=(), geolocation=(), payment=(), usb=()");
		response.setHeader(CACHE_CONTROL_HEADER, "no-store");
	}
}
