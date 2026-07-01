package com.example.gonggong.domain.risk.provider;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

public class RuleBasedKcCertificationCandidateDecider implements KcCertificationCandidateDecider {

	@Override
	public KcCertificationCandidateDecision decide(
		String productName,
		String modelName,
		String brandName,
		Map<String, SafetyKoreaCertificationItem> candidateById
	) {
		if (candidateById == null || candidateById.isEmpty()) {
			return KcCertificationCandidateDecision.none("KC 인증 후보가 없습니다.");
		}
		return candidateById.entrySet().stream()
			.max(Comparator.comparingInt(entry -> score(entry.getValue(), productName, modelName, brandName)))
			.map(entry -> {
				int score = score(entry.getValue(), productName, modelName, brandName);
				if (score >= 45) {
					return new KcCertificationCandidateDecision(entry.getKey(), KcCertificationMatchType.EXACT, "규칙 기반으로 동일 후보에 가깝다고 판단했습니다.");
				}
				if (score >= 30) {
					return new KcCertificationCandidateDecision(entry.getKey(), KcCertificationMatchType.SIMILAR, "규칙 기반으로 유사 후보라고 판단했습니다.");
				}
				return KcCertificationCandidateDecision.none("KC 인증 DB 후보는 있었지만 현재 상품과 동일 모델로 판단할 근거가 부족합니다.");
			})
			.orElseGet(() -> KcCertificationCandidateDecision.none("KC 인증 후보가 없습니다."));
	}

	private int score(SafetyKoreaCertificationItem item, String productName, String modelName, String brandName) {
		int score = 0;
		if (equalsNormalized(item.modelName(), modelName)) {
			score += 50;
		}
		else if (containsEither(item.modelName(), modelName)) {
			score += 32;
		}
		if (containsEither(item.productName(), productName)) {
			score += 25;
		}
		if (containsEither(item.brandName(), brandName)) {
			score += 15;
		}
		if (isValidStatus(item.statusName())) {
			score += 10;
		}
		return score;
	}

	private boolean isValidStatus(String statusName) {
		String normalized = normalize(statusName);
		return !normalized.contains("취소")
			&& !normalized.contains("만료")
			&& !normalized.contains("폐기")
			&& !normalized.contains("파기")
			&& !normalized.contains("정지")
			&& !normalized.contains("invalid")
			&& !normalized.contains("expired")
			&& !normalized.contains("cancel");
	}

	private boolean containsEither(String left, String right) {
		String normalizedLeft = normalize(left);
		String normalizedRight = normalize(right);
		if (normalizedLeft.isBlank() || normalizedRight.isBlank()) {
			return false;
		}
		return normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft);
	}

	private boolean equalsNormalized(String left, String right) {
		String normalizedLeft = normalize(left);
		String normalizedRight = normalize(right);
		return !normalizedLeft.isBlank() && normalizedLeft.equals(normalizedRight);
	}

	private String normalize(String value) {
		return value == null ? "" : value.replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT);
	}
}
