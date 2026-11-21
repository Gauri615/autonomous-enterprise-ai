package com.autonomousenterprise.api.persistence.identity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public record OidcIssuer(String value) {

	private static final int MAX_BYTES = 480;

	public OidcIssuer {
		Objects.requireNonNull(value, "OIDC issuer is required.");
		byte[] bytes = BinaryIdentifierCodec.encodeUtf8(value);
		if (bytes.length == 0 || bytes.length > MAX_BYTES || containsWhitespace(value)) {
			throw new IllegalArgumentException("OIDC issuer must be a non-empty URL of at most 480 UTF-8 bytes.");
		}
		validateIssuerUrl(value);
	}

	public byte[] toBytes() {
		return BinaryIdentifierCodec.encodeUtf8(value);
	}

	public static OidcIssuer fromBytes(byte[] bytes) {
		Objects.requireNonNull(bytes, "OIDC issuer bytes are required.");
		return new OidcIssuer(BinaryIdentifierCodec.decodeUtf8(bytes));
	}

	private static boolean containsWhitespace(String value) {
		return value.codePoints()
				.anyMatch(codePoint -> Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint));
	}

	private static void validateIssuerUrl(String value) {
		try {
			URI uri = new URI(value);
			if (!uri.isAbsolute()
					|| !"https".equalsIgnoreCase(uri.getScheme())
					|| uri.getHost() == null
					|| uri.getRawQuery() != null
					|| uri.getRawFragment() != null
					|| uri.getRawUserInfo() != null) {
				throw new IllegalArgumentException("OIDC issuer must be an absolute HTTPS issuer URL.");
			}
		} catch (URISyntaxException exception) {
			throw new IllegalArgumentException("OIDC issuer URL is malformed.", exception);
		}
	}
}
