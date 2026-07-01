package com.example.gonggong.domain.risk.calculator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CustomsDutyCalculatorTest {

	private final CustomsDutyCalculator calculator = new CustomsDutyCalculator();

	@Test
	void calculatesTaxableValueAndDutyWithBigDecimal() {
		BigDecimal taxableValue = calculator.taxableValue(
			new BigDecimal("100000"),
			new BigDecimal("10000"),
			BigDecimal.ZERO
		);

		BigDecimal duty = calculator.customsDuty(taxableValue, new BigDecimal("20.0"));

		assertThat(taxableValue).isEqualByComparingTo("110000");
		assertThat(duty).isEqualByComparingTo("22000");
	}
}
