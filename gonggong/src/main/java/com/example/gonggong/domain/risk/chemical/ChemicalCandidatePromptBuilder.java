package com.example.gonggong.domain.risk.chemical;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChemicalCandidatePromptBuilder {

	public String build(List<String> ingredients) {
		String joinedIngredients = ingredients == null ? "" : String.join(" | ", ingredients);
		return """
			너는 KC/리콜/HSK 분석에서 추출된 위해성분을 한국환경공단 화학물질 정보 조회 서비스 검색용 후보로 정제하는 AI다.

			입력은 성분명 목록이다. 성분명은 한국어, 영어, 약어, CAS 번호일 수 있다.
			출력은 반드시 JSON 하나만 반환한다.

			[출력 규칙]
			- chemicalCandidates 배열을 반환한다.
			- 최대 5개까지만 반환한다.
			- 각 항목은 name, casNumber, englishName을 가진다.
			- name은 입력에 가까운 원문 성분명을 유지한다.
			- casNumber는 확실할 때만 넣고, 아니면 null로 둔다.
			- englishName은 한국환경공단 검색에 사용할 영어 성분명이다.
			- 한국어 성분명이 들어오면 반드시 흔히 쓰는 영어명으로 번역해서 넣는다.
			- 영어명이 불확실하면 그 후보는 생략한다.
			- 추측하지 말고, 확실한 후보만 남긴다.
			- 중복 후보는 제거한다.

			[예시]
			- 납 -> { "name": "납", "casNumber": null, "englishName": "lead" }
			- 수은 -> { "name": "수은", "casNumber": null, "englishName": "mercury" }
			- 카드뮴 -> { "name": "카드뮴", "casNumber": null, "englishName": "cadmium" }

			입력 성분:
			%s
			""".formatted(joinedIngredients);
	}
}
