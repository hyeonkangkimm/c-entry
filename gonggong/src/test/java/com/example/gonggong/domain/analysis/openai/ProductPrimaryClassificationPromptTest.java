package com.example.gonggong.domain.analysis.openai;

import com.example.gonggong.domain.analysis.dto.ProductAnalyzeRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPrimaryClassificationPromptTest {

	@Test
	void instructsAiToSeparatePrimaryProductFromComponentsAndFeatures() {
		String prompt = new ProductNormalizePromptBuilder().build(new ProductAnalyzeRequest(
			"고화질 디스플레이 초슬림 노트북",
			"16GB RAM 2TB SSD",
			null,
			null,
			"hsk-match"
		));

		assertThat(prompt).contains("주된 판매 물품");
		assertThat(prompt).contains("주된 물품은 디스플레이가 아니라 노트북");
		assertThat(prompt).contains("가방");
		assertThat(prompt).contains("가방을 방제기, 분사기, 살포기 또는 산업용 장치로 해석하지 마라");
		assertThat(prompt).contains("기계식 스토커");
		assertThat(prompt).contains("FINISHED_PRODUCT");
		assertThat(prompt).contains("primaryProductName");
		assertThat(prompt).contains("primarySearchKeywords");
		assertThat(prompt).contains("componentKeywords");
		assertThat(prompt).contains("featureKeywords");
	}
}
