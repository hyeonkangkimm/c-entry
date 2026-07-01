package com.example.gonggong.domain.risk.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CustomsDutyCalculator {

	private static final BigDecimal PERCENT_DIVISOR = new BigDecimal("100");

	public BigDecimal taxableValue(
		BigDecimal declaredValue,
		BigDecimal shippingCost,
		BigDecimal insuranceCost
	) {
		return safe(declaredValue)
			.add(safe(shippingCost))
			.add(safe(insuranceCost));
	}

	public BigDecimal customsDuty(BigDecimal taxableValue, BigDecimal finalTariffRate) {
		return safe(taxableValue)
			.multiply(safe(finalTariffRate))
			.divide(PERCENT_DIVISOR, 0, RoundingMode.HALF_UP);
	}

	private BigDecimal safe(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
