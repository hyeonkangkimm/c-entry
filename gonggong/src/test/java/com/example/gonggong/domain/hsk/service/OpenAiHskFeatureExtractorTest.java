package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.analysis.openai.ProductNormalizeResult;
import com.example.gonggong.domain.analysis.service.ProductNormalizer;
import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiHskFeatureExtractorTest {

	@Test
	void usesProvidedNormalizedFeaturesWithoutCallingOpenAiAgain() {
		CountingProductNormalizer normalizer = new CountingProductNormalizer();
		OpenAiHskFeatureExtractor extractor = new OpenAiHskFeatureExtractor(normalizer);

		HskFeatures result = extractor.extract(new HskMatchRequest(
			"kids backpack",
			"school bag",
			null,
			"아동용 책가방",
			List.of("책가방", "가방"),
			"아동용 책가방",
			"FINISHED_PRODUCT",
			List.of("책가방", "가방", "backpack"),
			List.of("섬유"),
			List.of("방수")
		));

		assertThat(normalizer.callCount).isZero();
		assertThat(result.standardProductName()).isEqualTo("아동용 책가방");
		assertThat(result.primaryProductName()).isEqualTo("아동용 책가방");
		assertThat(result.productForm()).isEqualTo("FINISHED_PRODUCT");
		assertThat(result.primarySearchKeywords()).containsExactly("책가방", "가방", "backpack");
		assertThat(result.componentKeywords()).containsExactly("섬유");
		assertThat(result.featureKeywords()).containsExactly("방수");
	}

	@Test
	void keepsPrimaryKeywordsSeparateFromComponentsAndFeatures() {
		ProductNormalizer normalizer = request -> new ProductNormalizeResult(
			"초슬림 노트북 컴퓨터",
			List.of("노트북", "디스플레이"),
			null,
			"전기전자>컴퓨터",
			null,
			null,
			null,
			null,
			List.of("금속", "플라스틱"),
			"일반",
			List.of(),
			List.of("컴퓨터", "디스플레이"),
			"노트북 컴퓨터",
			"FINISHED_PRODUCT",
			List.of("노트북 컴퓨터", "휴대용 컴퓨터", "휴대용 자동자료처리기계"),
			List.of("디스플레이", "SSD", "RAM"),
			List.of("초슬림", "14.1인치", "게이밍"),
			0.92
		);
		OpenAiHskFeatureExtractor extractor = new OpenAiHskFeatureExtractor(normalizer);

		HskFeatures result = extractor.extract(new HskMatchRequest(
			"초슬림 노트북 14.1인치 16GB RAM 2TB SSD",
			"고화질 디스플레이 게이밍 PC",
			null
		));

		assertThat(result.primaryProductName()).isEqualTo("노트북 컴퓨터");
		assertThat(result.productForm()).isEqualTo("FINISHED_PRODUCT");
		assertThat(result.primarySearchKeywords())
			.containsExactly("노트북 컴퓨터", "휴대용 컴퓨터", "휴대용 자동자료처리기계");
		assertThat(result.componentKeywords()).containsExactly("디스플레이", "SSD", "RAM");
		assertThat(result.featureKeywords()).containsExactly("초슬림", "14.1인치", "게이밍");
	}

	private static class CountingProductNormalizer implements ProductNormalizer {

		private int callCount;

		@Override
		public ProductNormalizeResult normalize(com.example.gonggong.domain.analysis.dto.ProductAnalyzeRequest request) {
			callCount++;
			throw new AssertionError("ProductNormalizer should not be called when HSK normalized fields are provided");
		}
	}
}
