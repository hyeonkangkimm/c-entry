package com.example.gonggong.domain.analysis.recall;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HarmfulIngredientExtractorTest {

	private final HarmfulIngredientExtractor extractor = new HarmfulIngredientExtractor();

	@Test
	void extractsKnownHazardousIngredientsFromRecallDescriptions() {
		assertThat(extractor.extract("""
			ㅇ 프탈레이트계 가소제 기준치 초과
			- 측정값: DEHP 41.101 %, DBP 0.010 %, DINP 0.060 %
			ㅇ 총 납 기준치 초과
			ㅇ 카드뮴 기준치 초과
			"""))
			.containsExactly("프탈레이트계 가소제", "DEHP", "DBP", "DINP", "납", "카드뮴");
	}

	@Test
	void returnsEmptyListWhenDescriptionIsBlank() {
		assertThat(extractor.extract(null)).isEmpty();
		assertThat(extractor.extract("   ")).isEmpty();
	}
}
