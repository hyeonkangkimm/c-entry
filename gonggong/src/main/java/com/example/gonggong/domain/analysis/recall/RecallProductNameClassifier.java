package com.example.gonggong.domain.analysis.recall;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RecallProductNameClassifier {

	public String classify(String recallProductName) {
		String normalized = normalize(recallProductName);

		if (containsAny(normalized, "완구", "장난감")) {
			return "어린이용품>완구";
		}
		if (containsAny(normalized, "책가방", "가방", "섬유제품", "의류", "신발")) {
			return "생활용품>가방/섬유제품";
		}
		if (containsAny(normalized, "유아", "어린이", "아동")) {
			return "어린이용품>기타 어린이제품";
		}
		if (containsAny(normalized, "식기", "냄비", "프라이팬", "주방")) {
			return "생활용품>주방용품";
		}
		if (containsAny(normalized, "조명", "램프", "전등")) {
			return "전기용품>조명기기";
		}
		if (containsAny(normalized, "찜질", "전기장판", "온열", "난로", "히터")) {
			return "전기용품>온열기기";
		}
		if (containsAny(normalized, "충전", "배터리", "어댑터")) {
			return "전기용품>충전기/배터리";
		}
		if (containsAny(normalized, "화학", "세정", "방향", "접착제")) {
			return "화학제품>생활화학제품";
		}
		if (containsAny(normalized, "레저", "스포츠", "운동")) {
			return "생활용품>레저용품";
		}
		return "기타";
	}

	private String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
	}

	private boolean containsAny(String value, String... keywords) {
		for (String keyword : keywords) {
			if (value.contains(keyword)) {
				return true;
			}
		}
		return false;
	}
}
