package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HskVectorQueryBuilderTest {

	@Test
	void addsFanContextToFanDocumentsWithGenericLeafNames() {
		HskVectorQueryBuilder builder = new HskVectorQueryBuilder();

		String documentText = builder.buildDocumentText(new com.example.gonggong.domain.hsk.domain.HskItem(
			"8414519000",
			"기타",
			"Other",
			"팬>테이블용ㆍ바닥용ㆍ벽용ㆍ창용ㆍ천장용ㆍ지붕용 팬>기타"
		));

		assertThat(documentText).contains("선풍기");
		assertThat(documentText).contains("전기선풍기");
		assertThat(documentText).contains("table fan");
		assertThat(documentText).contains("electric fan");
	}

	@Test
	void expandsFanTermsForVectorSearch() {
		HskVectorQueryBuilder builder = new HskVectorQueryBuilder();
		HskMatchRequest request = new HskMatchRequest(
			"클립형 충전식 탁상용 선풍기",
			"portable rechargeable clip desk fan",
			null
		);
		HskFeatures features = new HskFeatures(
			"클립형 충전식 탁상용 선풍기",
			List.of("탁상용 선풍기", "충전식 선풍기", "선풍기"),
			"선풍기",
			"FINISHED_PRODUCT",
			List.of("탁상용 선풍기", "선풍기"),
			List.of("배터리", "모터"),
			List.of("클립형", "충전식")
		);

		String query = builder.buildQuery(request, features);

		assertThat(query).contains("선풍기");
		assertThat(query).contains("팬");
		assertThat(query).contains("electric fan");
		assertThat(query).contains("table fan");
	}

	@Test
	void addsChapterContextToWatchDocumentsWithGenericLeafNames() {
		HskVectorQueryBuilder builder = new HskVectorQueryBuilder();

		String documentText = builder.buildDocumentText(new com.example.gonggong.domain.hsk.domain.HskItem(
			"9102191000",
			"배터리ㆍ축전지 구동식",
			"Battery or accumulator operated"
		));

		assertThat(documentText).contains("손목시계");
		assertThat(documentText).contains("시계");
		assertThat(documentText).contains("wrist watch");
		assertThat(documentText).contains("watch");
	}

	@Test
	void expandsWatchTermsForVectorSearch() {
		HskVectorQueryBuilder builder = new HskVectorQueryBuilder();
		HskMatchRequest request = new HskMatchRequest(
			"남성용 방수 손목시계",
			"quartz wrist watch",
			null
		);
		HskFeatures features = new HskFeatures(
			"손목시계",
			List.of("손목시계"),
			"손목시계",
			"FINISHED_PRODUCT",
			List.of("손목시계"),
			List.of(),
			List.of("방수", "쿼츠")
		);

		String query = builder.buildQuery(request, features);

		assertThat(query).contains("손목시계");
		assertThat(query).contains("시계");
		assertThat(query).contains("watch");
		assertThat(query).contains("clock");
	}

	@Test
	void buildsQueryFromPrimaryProductAndHskCandidateKeywords() {
		HskVectorQueryBuilder builder = new HskVectorQueryBuilder();
		HskMatchRequest request = new HskMatchRequest(
			"Redragon M908 RGB 게이밍 마우스",
			"USB 유선 12400 DPI",
			null,
			"유선 게이밍 마우스",
			List.of("컴퓨터 마우스", "입력장치"),
			"컴퓨터용 마우스",
			"FINISHED_PRODUCT",
			List.of("유선 마우스", "게이밍 마우스"),
			List.of("버튼", "LED"),
			List.of("USB", "12400 DPI")
		);
		HskFeatures features = new HskFeatures(
			"유선 게이밍 마우스",
			List.of("유선 마우스"),
			"컴퓨터용 마우스",
			"FINISHED_PRODUCT",
			List.of("유선 마우스", "게이밍 마우스"),
			List.of("버튼", "LED"),
			List.of("USB", "12400 DPI")
		);

		String query = builder.buildQuery(request, features);

		assertThat(query).contains("컴퓨터용 마우스");
		assertThat(query).contains("유선 마우스");
		assertThat(query).contains("컴퓨터 마우스");
		assertThat(query).contains("입력장치");
		assertThat(query).doesNotContain("Redragon");
	}
}
