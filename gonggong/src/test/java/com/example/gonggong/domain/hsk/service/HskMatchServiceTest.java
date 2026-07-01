package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.domain.HskItem;
import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import com.example.gonggong.domain.hsk.dto.HskMatchResponse;
import com.example.gonggong.domain.hsk.repository.HskItemReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HskMatchServiceTest {

	@Test
	void returnsOnlyCodesFoundInOfficialDataset() {
		HskFeatureExtractor extractor = request -> new HskFeatures(
			"plastic tableware",
			List.of("plastic tableware", "tableware", "플라스틱 식기")
		);
		HskItem officialItem = new HskItem(
			"3924100000",
			"플라스틱으로 만든 식탁용품과 주방용품",
			"Tableware and kitchenware, of plastics"
		);
		HskItemReader reader = keyword -> keyword.contains("tableware") || keyword.contains("식기")
			? List.of(officialItem)
			: List.of();
		HskMatchService service = new HskMatchService(extractor, reader);

		HskMatchResponse response = service.match(new HskMatchRequest(
			"baby plastic bowl",
			"plastic tableware for children",
			null
		));

		assertThat(response.matched()).isTrue();
		assertThat(response.candidates()).isNotEmpty();
		assertThat(response.candidates().get(0).hskCode()).isEqualTo("3924100000");
		assertThat(response.candidates().get(0).koreanName()).isEqualTo("플라스틱으로 만든 식탁용품과 주방용품");
	}

	@Test
	void returnsNoCandidateInsteadOfGeneratingCode() {
		HskFeatureExtractor extractor = request -> new HskFeatures(
			"unknown product",
			List.of("unknown")
		);
		HskItemReader reader = keyword -> List.of();
		HskMatchService service = new HskMatchService(extractor, reader);

		HskMatchResponse response = service.match(new HskMatchRequest(
			"unknown product",
			"",
			null
		));

		assertThat(response.matched()).isFalse();
		assertThat(response.candidates()).isEmpty();
	}

	@Test
	void ranksSpecificProductTypeAboveGenericTargetUserMatch() {
		HskFeatureExtractor extractor = request -> new HskFeatures(
			"어린이용 플라스틱 그릇",
			List.of(
				"어린이용", "baby", "children", "플라스틱", "plastic",
				"생활용품", "유아용품", "주방용품", "그릇", "식기류",
				"가정용", "식사용", "급식용", "소형", "경량",
				"재사용", "식품용", "용기", "접시", "보울",
				"키즈", "유아", "아동",
				"tableware", "kitchenware"
			)
		);
		HskItem babyCosmetics = new HskItem(
			"3304993000",
			"어린이용 제품류",
			"Baby cosmetics"
		);
		HskItem plasticTableware = new HskItem(
			"3924100000",
			"플라스틱으로 만든 식탁용품과 주방용품",
			"Tableware and kitchenware, of plastics"
		);
		HskItemReader reader = keyword -> {
			if (keyword.contains("tableware") || keyword.contains("kitchenware")) {
				return List.of(plasticTableware);
			}
			if (keyword.contains("baby") || keyword.contains("어린이용")) {
				return List.of(babyCosmetics);
			}
			return List.of();
		};
		HskMatchService service = new HskMatchService(extractor, reader);

		HskMatchResponse response = service.match(new HskMatchRequest(
			"baby plastic bowl",
			"plastic tableware for children",
			null
		));

		assertThat(response.candidates().get(0).hskCode()).isEqualTo("3924100000");
	}

	@Test
	void expandsWatchCompoundTermsToClockAndWatchCandidates() {
		HskFeatureExtractor extractor = request -> new HskFeatures(
			"손목시계",
			List.of("손목시계")
		);
		HskItem watch = new HskItem(
			"9102190000",
			"그 밖의 손목시계",
			"Other wrist-watches",
			"손목시계/휴대용 시계 > 전기구동식 > 기타"
		);
		HskItemReader reader = keyword -> keyword.equals("시계") || keyword.equals("watch") || keyword.equals("clock")
			? List.of(watch)
			: List.of();
		HskMatchService service = new HskMatchService(extractor, reader);

		HskMatchResponse response = service.match(new HskMatchRequest(
			"남성용 방수 손목시계",
			"quartz wrist watch",
			null
		));

		assertThat(response.matched()).isTrue();
		assertThat(response.candidates()).isNotEmpty();
		assertThat(response.candidates().get(0).hskCode()).isEqualTo("9102190000");
		assertThat(response.candidates().get(0).displayName()).contains("손목시계/휴대용 시계");
	}

	@Test
	void mapsLaptopTermsToOfficialPortableComputerClassification() {
		HskFeatureExtractor extractor = request -> new HskFeatures(
			"초슬림 노트북 컴퓨터",
			List.of("노트북", "컴퓨터", "디스플레이", "사무용", "학습용")
		);
		HskItem adhesive = new HskItem(
			"3506911000",
			"광학용 투명 점착 필름 접착제ㆍ광학용 투명 경화성 액상 접착제",
			"Optically clear adhesives for the manufacture of flat panel displays"
		);
		HskItem laptop = new HskItem(
			"8471300000",
			"휴대용 자동자료처리기계(중량이 10킬로그램 이하인 것으로서 적어도 중앙처리장치, 키보드, 디스플레이를 갖추고 있는 것으로 한정한다)",
			"Portable automatic data processing machines, weighing not more than 10 kg, consisting of at least a central processing unit, a keyboard and a display"
		);
		HskItemReader reader = keyword -> {
			if (keyword.contains("휴대용 자동자료처리기계") || keyword.contains("portable automatic data processing")) {
				return List.of(laptop);
			}
			if (keyword.contains("디스플레이")) {
				return List.of(adhesive);
			}
			return List.of();
		};
		HskMatchService service = new HskMatchService(extractor, reader);

		HskMatchResponse response = service.match(new HskMatchRequest(
			"새로운 초슬림 노트북 14.1인치 16GB 램 2TB SSD 인텔 4405U 게이밍 PC",
			"1920*1080 디스플레이 사무용 학습용 컴퓨터 PC 윈도우 11 프로",
			null
		));

		assertThat(response.candidates().get(0).hskCode()).isEqualTo("8471300000");
	}
}
