package com.example.gonggong.domain.risk.calculator;

import com.example.gonggong.domain.risk.domain.OverallRiskLevel;
import com.example.gonggong.domain.risk.domain.RiskStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

@Component
public class RiskScoreCalculator {

	private static final BigDecimal RECALL_WEIGHT = new BigDecimal("0.25");
	private static final BigDecimal CUSTOMS_WEIGHT = new BigDecimal("0.20");
	private static final BigDecimal KC_WEIGHT = new BigDecimal("0.25");
	private static final BigDecimal CHEMICAL_WEIGHT = new BigDecimal("0.30");

	public int calculateOverallScore(int recallScore, int customsScore, int kcScore, int chemicalScore) {
		BigDecimal score = BigDecimal.valueOf(clamp(recallScore)).multiply(RECALL_WEIGHT)
			.add(BigDecimal.valueOf(clamp(customsScore)).multiply(CUSTOMS_WEIGHT))
			.add(BigDecimal.valueOf(clamp(kcScore)).multiply(KC_WEIGHT))
			.add(BigDecimal.valueOf(clamp(chemicalScore)).multiply(CHEMICAL_WEIGHT));

		return clamp(score.setScale(0, RoundingMode.HALF_UP).intValue());
	}

	public OverallRiskLevel toOverallRiskLevel(int score, int unavailableCount, RiskStatus... statuses) {
		int actualUnavailableCount = unavailableCount + (int)Arrays.stream(statuses)
			.filter(status -> status == RiskStatus.UNAVAILABLE)
			.count();
		if (actualUnavailableCount >= 2) {
			return OverallRiskLevel.UNKNOWN;
		}
		return toOverallRiskLevel(score, unavailableCount);
	}

	public OverallRiskLevel toOverallRiskLevel(int score, int unavailableCount) {
		if (unavailableCount >= 2) {
			return OverallRiskLevel.UNKNOWN;
		}
		int clamped = clamp(score);
		if (clamped >= 80) {
			return OverallRiskLevel.CRITICAL;
		}
		if (clamped >= 60) {
			return OverallRiskLevel.HIGH;
		}
		if (clamped >= 30) {
			return OverallRiskLevel.MEDIUM;
		}
		return OverallRiskLevel.LOW;
	}

	private int clamp(int value) {
		return Math.max(0, Math.min(100, value));
	}
}
