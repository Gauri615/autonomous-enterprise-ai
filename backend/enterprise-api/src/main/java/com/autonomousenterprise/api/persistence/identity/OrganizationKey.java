package com.autonomousenterprise.api.persistence.identity;

import java.util.Objects;
import java.util.regex.Pattern;

public record OrganizationKey(String value) {

	private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
	private static final int MAX_BYTES = 100;

	public OrganizationKey {
		Objects.requireNonNull(value, "Organization key is required.");
		if (value.length() > MAX_BYTES || !SLUG.matcher(value).matches()) {
			throw new IllegalArgumentException("Organization key must be a 1-100 character lowercase ASCII slug.");
		}
	}

	public byte[] toBytes() {
		return BinaryIdentifierCodec.encodeAscii(value);
	}

	public static OrganizationKey fromBytes(byte[] bytes) {
		Objects.requireNonNull(bytes, "Organization key bytes are required.");
		return new OrganizationKey(BinaryIdentifierCodec.decodeAscii(bytes));
	}
}
