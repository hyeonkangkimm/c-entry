package com.example.gonggong.domain.analysis.recall;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecallProductNameClassifierTest {

	private final RecallProductNameClassifier classifier = new RecallProductNameClassifier();

	@Test
	void mapsBackpackRecallProductNameToBagAndTextileCategory() {
		assertThat(classifier.classify("가정용섬유제품(책가방)"))
			.isEqualTo("생활용품>가방/섬유제품");
	}

	@Test
	void mapsToyRecallProductNamesToToyCategory() {
		assertThat(classifier.classify("운동완구(완구)"))
			.isEqualTo("어린이용품>완구");
		assertThat(classifier.classify("미술공예 완구(완구)"))
			.isEqualTo("어린이용품>완구");
	}
}
