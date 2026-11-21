package com.autonomousenterprise.api.metadata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/${enterprise.api.version}")
public class ApiMetadataController {

	private final String applicationName;
	private final ApiMetadataProperties properties;

	public ApiMetadataController(
			@Value("${spring.application.name}") String applicationName,
			ApiMetadataProperties properties) {
		this.applicationName = applicationName;
		this.properties = properties;
	}

	@GetMapping("/meta")
	public ApiMetadataResponse metadata() {
		return new ApiMetadataResponse(applicationName, properties.version(), "UP");
	}
}
