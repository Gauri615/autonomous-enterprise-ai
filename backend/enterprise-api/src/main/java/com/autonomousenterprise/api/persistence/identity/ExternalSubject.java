package com.autonomousenterprise.api.persistence.identity;

import java.util.Objects;

public record ExternalSubject(String value) {

	private static final int MAX_BYTES = 255;

	public ExternalSubject {
		Objects.requireNonNull(value, "OIDC subject is required.");
		if (value.isEmpty() || value.length() > MAX_BYTES) {
			throw new IllegalArgumentException("OIDC subject must contain 1-255 ASCII bytes.");
		}
		BinaryIdentifierCodec.encodeAscii(value);
	}

	public byte[] toBytes() {
		return BinaryIdentifierCodec.encodeAscii(value);
	}

	public static ExternalSubject fromBytes(byte[] bytes) {
		Objects.requireNonNull(bytes, "OIDC subject bytes are required.");
		return new ExternalSubject(BinaryIdentifierCodec.decodeAscii(bytes));
	}
}
