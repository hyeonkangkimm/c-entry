package com.example.gonggong.global.logging;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomPayloadLogFormatterTest {

	@Test
	void masksCertificationNumber() {
		assertThat(DomPayloadLogFormatter.maskCertificationNumber("KC-ABC-123456"))
			.isEqualTo("KC****56");
	}

	@Test
	void clipsLongTextAndNormalizesWhitespace() {
		assertThat(DomPayloadLogFormatter.clip("abc   def ghi", 7))
			.isEqualTo("abc def...");
	}

	@Test
	void clipsListSizeAndText() {
		assertThat(DomPayloadLogFormatter.clipList(List.of("first value", "second value", "third value"), 2, 6))
			.containsExactly("first ...", "second...");
	}
}
