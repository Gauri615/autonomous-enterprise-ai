package com.autonomousenterprise.api.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.autonomousenterprise.api.correlation.CorrelationIdFilter;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.slf4j.LoggerFactory;
import com.autonomousenterprise.api.logging.ApiRequestLoggingFilter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiMetadataControllerTests {

	@Autowired
	private MockMvc mockMvc;

	private Logger requestLogger;
	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void captureRequestLogs() {
		requestLogger = (Logger) LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
		logAppender = new ListAppender<>();
		logAppender.setContext(requestLogger.getLoggerContext());
		logAppender.start();
		requestLogger.addAppender(logAppender);
		requestLogger.setLevel(Level.INFO);
	}

	@AfterEach
	void stopCapturingRequestLogs() {
		requestLogger.detachAppender(logAppender);
		logAppender.stop();
	}

	@Test
	void metadataReturnsPublicApplicationInformationAndCorrelationId() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/meta?access_token=query-secret")
						.header(CorrelationIdFilter.HEADER_NAME, "request-log-123")
						.content("request-body-secret"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.applicationName").value("enterprise-api"))
				.andExpect(jsonPath("$.apiVersion").value("v1"))
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
				.andExpect(header().string("Permissions-Policy",
						"camera=(), microphone=(), geolocation=(), payment=(), usb=()"))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andReturn();

		assertThat(result.getResponse().getHeaders("X-Content-Type-Options")).containsExactly("nosniff");
		assertThat(result.getResponse().getHeaders("X-Frame-Options")).containsExactly("DENY");
		assertThat(result.getResponse().getHeaders("Referrer-Policy"))
				.containsExactly("strict-origin-when-cross-origin");
		assertThat(result.getResponse().getHeaders("Permissions-Policy"))
				.containsExactly("camera=(), microphone=(), geolocation=(), payment=(), usb=()");
		assertThat(result.getResponse().getHeaders("Cache-Control")).containsExactly("no-store");
		assertThat(result.getResponse().getHeaders(CorrelationIdFilter.HEADER_NAME)).hasSize(1);

		ILoggingEvent event = onlyCompletionEvent();
		assertThat(event.getLevel()).isEqualTo(Level.INFO);
		assertThat(event.getMDCPropertyMap()).containsEntry("correlationId", "request-log-123");
		assertThat(keyValues(event))
				.containsEntry("correlationId", "request-log-123")
				.containsEntry("method", "GET")
				.containsEntry("route", "/api/v1/meta")
				.containsEntry("status", 200);
		assertThat((Long) keyValues(event).get("durationMs")).isGreaterThanOrEqualTo(0);
		assertThat(event.getFormattedMessage())
				.doesNotContain("access_token")
				.doesNotContain("query-secret")
				.doesNotContain("request-body-secret");
		assertThat(keyValues(event).toString())
				.doesNotContain("access_token")
				.doesNotContain("query-secret")
				.doesNotContain("request-body-secret");
	}

	@Test
	void apiErrorResponseIncludesSecurityAndCorrelationHeaders() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/meta?token=query-secret")
						.content("password=body-secret"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"))
				.andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
				.andExpect(header().string("Permissions-Policy",
						"camera=(), microphone=(), geolocation=(), payment=(), usb=()"))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andReturn();

		assertThat(result.getResponse().getHeaders("X-Content-Type-Options")).containsExactly("nosniff");
		assertThat(result.getResponse().getHeaders("X-Frame-Options")).containsExactly("DENY");
		assertThat(result.getResponse().getHeaders("Referrer-Policy"))
				.containsExactly("strict-origin-when-cross-origin");
		assertThat(result.getResponse().getHeaders("Permissions-Policy"))
				.containsExactly("camera=(), microphone=(), geolocation=(), payment=(), usb=()");
		assertThat(result.getResponse().getHeaders("Cache-Control")).containsExactly("no-store");
		assertThat(result.getResponse().getHeaders(CorrelationIdFilter.HEADER_NAME)).hasSize(1);
		ILoggingEvent event = onlyCompletionEvent();
		assertThat(event.getLevel()).isEqualTo(Level.WARN);
		assertThat(keyValues(event))
				.containsEntry("method", "POST")
				.containsEntry("status", 405);
		assertThat(keyValues(event).toString())
				.doesNotContain("query-secret")
				.doesNotContain("body-secret");
	}

	@Test
	void actuatorHealthRemainsOutsideApiSecurityHeaderFilter() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(header().doesNotExist("X-Content-Type-Options"))
				.andExpect(header().doesNotExist("X-Frame-Options"))
				.andExpect(header().doesNotExist("Referrer-Policy"))
				.andExpect(header().doesNotExist("Permissions-Policy"));
		assertThat(logAppender.list).isEmpty();
	}

	private ILoggingEvent onlyCompletionEvent() {
		assertThat(logAppender.list).hasSize(1);
		ILoggingEvent event = logAppender.list.getFirst();
		assertThat(event.getMessage()).isEqualTo("api_request_completed");
		return event;
	}

	private Map<String, Object> keyValues(ILoggingEvent event) {
		return event.getKeyValuePairs().stream()
				.collect(Collectors.toMap(pair -> pair.key, pair -> pair.value));
	}
}
