package com.autonomousenterprise.api.persistence.identity;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

final class BinaryIdentifierCodec {

	private BinaryIdentifierCodec() {
	}

	static byte[] encodeUtf8(String value) {
		try {
			ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.encode(CharBuffer.wrap(value));
			byte[] bytes = new byte[encoded.remaining()];
			encoded.get(bytes);
			return bytes;
		} catch (CharacterCodingException exception) {
			throw new IllegalArgumentException("Identifier contains malformed Unicode.", exception);
		}
	}

	static String decodeUtf8(byte[] bytes) {
		try {
			return StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(bytes))
					.toString();
		} catch (CharacterCodingException exception) {
			throw new IllegalArgumentException("Stored identifier contains malformed UTF-8.", exception);
		}
	}

	static byte[] encodeAscii(String value) {
		byte[] bytes = new byte[value.length()];
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (character > 0x7f) {
				throw new IllegalArgumentException("Identifier must contain ASCII characters only.");
			}
			bytes[index] = (byte) character;
		}
		return bytes;
	}

	static String decodeAscii(byte[] bytes) {
		char[] characters = new char[bytes.length];
		for (int index = 0; index < bytes.length; index++) {
			int value = bytes[index] & 0xff;
			if (value > 0x7f) {
				throw new IllegalArgumentException("Stored identifier contains non-ASCII bytes.");
			}
			characters[index] = (char) value;
		}
		return new String(characters);
	}
}
