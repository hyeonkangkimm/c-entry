package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.domain.HskItem;
import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import com.example.gonggong.domain.hsk.dto.HskMatchResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HskCandidateRerankerServiceTest {

	@Test
	void putsAiSelectedHskCandidateFirst() {
		HskFeatureExtractor extractor = request -> new HskFeatures(
			"computer keyboard",
			List.of("keyboard"),
			"computer keyboard",
			"FINISHED_PRODUCT",
			List.of("keyboard", "computer keyboard"),
			List.of(),
			List.of()
		);
		HskVectorCandidateSearcher searcher = (request, features, limit) -> List.of(
			new HskVectorCandidate(new HskItem("9503003910", "keyboard shaped toy", "Keyboard shaped toy"), 0.95),
			new HskVectorCandidate(new HskItem("8471601020", "keyboard", "Keyboard"), 0.90)
		);
		HskCandidateReranker reranker = (request, features, candidates) -> new HskRerankResult(
			"8471601020",
			0.96,
			"The sold product is a computer input keyboard, not a toy."
		);
		HskMatchService service = new HskMatchService(extractor, searcher, reranker);

		HskMatchResponse response = service.match(new HskMatchRequest(
			"wired keyboard",
			"104 key USB computer keyboard",
			null
		));

		assertThat(response.matched()).isTrue();
		assertThat(response.candidates().get(0).hskCode()).isEqualTo("8471601020");
		assertThat(response.candidates().get(0).confidence()).isEqualTo(0.96);
		assertThat(response.candidates().get(0).reason()).contains("computer input keyboard");
	}

	@Test
	void returnsUnmatchedWhenAiRejectsEveryVectorCandidate() {
		HskFeatureExtractor extractor = request -> new HskFeatures("unknown product", List.of("unknown"));
		HskVectorCandidateSearcher searcher = (request, features, limit) -> List.of(
			new HskVectorCandidate(new HskItem("1111111111", "first unrelated candidate", "First"), 0.80),
			new HskVectorCandidate(new HskItem("2222222222", "second unrelated candidate", "Second"), 0.70)
		);
		HskCandidateReranker reranker = (request, features, candidates) -> HskRerankResult.notSelected();
		HskMatchService service = new HskMatchService(extractor, searcher, reranker);

		HskMatchResponse response = service.match(new HskMatchRequest("unknown product", "", null));

		assertThat(response.matched()).isFalse();
		assertThat(response.candidates()).hasSize(2);
		assertThat(response.message()).contains("HSK");
	}
}
