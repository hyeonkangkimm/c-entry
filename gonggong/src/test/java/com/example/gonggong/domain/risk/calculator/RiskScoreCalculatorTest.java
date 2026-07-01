package com.example.gonggong.domain.risk.calculator;

import com.example.gonggong.domain.risk.domain.OverallRiskLevel;
import com.example.gonggong.domain.risk.domain.RiskStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskScoreCalculatorTest {

	private final RiskScoreCalculator calculator = new RiskScoreCalculator();

	@Test
	void calculatesWeightedScore() {
		int score = calculator.calculateOverallScore(70, 60, 50, 90);

		assertThat(score).isEqualTo(69);
	}

	@Test
	void clampsScoreToZeroAndOneHundred() {
		assertThat(calculator.calculateOverallScore(-50, 0, 0, 0)).isZero();
		assertThat(calculator.calculateOverallScore(200, 200, 200, 200)).isEqualTo(100);
	}

	@Test
	void mapsOverallRiskLevelBoundaries() {
		assertThat(calculator.toOverallRiskLevel(29, 0)).isEqualTo(OverallRiskLevel.LOW);
		assertThat(calculator.toOverallRiskLevel(30, 0)).isEqualTo(OverallRiskLevel.MEDIUM);
		assertThat(calculator.toOverallRiskLevel(60, 0)).isEqualTo(OverallRiskLevel.HIGH);
		assertThat(calculator.toOverallRiskLevel(80, 0)).isEqualTo(OverallRiskLevel.CRITICAL);
	}

	@Test
	void returnsUnknownWhenTwoOrMoreRiskAreasAreUnavailable() {
		OverallRiskLevel level = calculator.toOverallRiskLevel(
			78,
			2,
			RiskStatus.UNAVAILABLE,
			RiskStatus.UNKNOWN,
			RiskStatus.WARNING,
			RiskStatus.DANGER
		);

		assertThat(level).isEqualTo(OverallRiskLevel.UNKNOWN);
	}
}
