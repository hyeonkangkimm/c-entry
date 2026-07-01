package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.domain.HskItem;
import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import com.example.gonggong.domain.hsk.dto.HskMatchResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HskVectorMatchServiceTest {

	@Test
	void returnsOfficialCodesFromVectorCandidatesOnly() {
		HskFeatureExtractor extractor = request -> new HskFeatures(
			"게이밍 노트북",
			List.of("노트북", "노트북 컴퓨터"),
			"노트북 컴퓨터",
			"FINISHED_PRODUCT",
			List.of("노트북 컴퓨터", "노트북"),
			List.of("디스플레이", "SSD"),
			List.of("14.1인치")
		);
		HskVectorCandidateSearcher searcher = (request, features, limit) -> List.of(new HskVectorCandidate(
			new HskItem(
				"8471300000",
				"휴대용 자동자료처리기계",
				"Portable automatic data processing machines"
			),
			0.93
		));
		HskMatchService service = new HskMatchService(extractor, searcher);

		HskMatchResponse response = service.match(new HskMatchRequest(
			"14.1인치 게이밍 노트북",
			"16GB RAM 2TB SSD laptop computer",
			null
		));

		assertThat(response.matched()).isTrue();
		assertThat(response.candidates()).hasSize(1);
		assertThat(response.candidates().get(0).hskCode()).isEqualTo("8471300000");
		assertThat(response.candidates().get(0).confidence()).isGreaterThanOrEqualTo(0.90);
	}

	@Test
	void returnsNoCandidateWhenVectorSearchReturnsEmpty() {
		HskFeatureExtractor extractor = request -> new HskFeatures("알 수 없는 상품", List.of("알 수 없음"));
		HskVectorCandidateSearcher searcher = (request, features, limit) -> List.of();
		HskMatchService service = new HskMatchService(extractor, searcher);

		HskMatchResponse response = service.match(new HskMatchRequest(
			"알 수 없는 상품",
			"",
			null
		));

		assertThat(response.matched()).isFalse();
		assertThat(response.candidates()).isEmpty();
	}
}
