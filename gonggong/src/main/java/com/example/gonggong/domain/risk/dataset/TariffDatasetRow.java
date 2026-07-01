package com.example.gonggong.domain.risk.dataset;

import com.example.gonggong.domain.risk.domain.TariffType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TariffDatasetRow(
	String hskCode,
	String tariffCode,
	TariffType tariffType,
	BigDecimal rate,
	String unitAmount,
	String basePrice,
	String countryScope,
	String usageRateCode,
	LocalDate effectiveFrom,
	LocalDate effectiveTo,
	String legalNotice
) {
}
