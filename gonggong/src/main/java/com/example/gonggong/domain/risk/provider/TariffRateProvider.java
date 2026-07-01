package com.example.gonggong.domain.risk.provider;

import java.time.LocalDate;
import java.util.Optional;

public interface TariffRateProvider {

	Optional<TariffRateResult> findTariffRate(
		String hskCode,
		String originCountry,
		LocalDate effectiveDate
	);
}
