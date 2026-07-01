package com.example.gonggong.domain.risk.provider;

import com.example.gonggong.domain.risk.domain.TariffRate;
import com.example.gonggong.domain.risk.repository.TariffRateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class JpaTariffRateProvider implements TariffRateProvider {

	private final TariffRateRepository repository;

	public JpaTariffRateProvider(TariffRateRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<TariffRateResult> findTariffRate(
		String hskCode,
		String originCountry,
		LocalDate effectiveDate
	) {
		String normalizedOrigin = normalizeCountry(originCountry);
		List<TariffRate> rates = repository.findEffectiveRates(hskCode, effectiveDate);

		return rates.stream()
			.filter(rate -> appliesToOrigin(rate, normalizedOrigin))
			.filter(this::isDirectlyApplicable)
			.sorted(Comparator
				.comparingInt((TariffRate rate) -> originPriority(rate, normalizedOrigin))
				.reversed()
				.thenComparing(
					Comparator.comparingInt((TariffRate rate) -> tariffPriority(rate))
						.reversed()
				)
				.thenComparing(
					TariffRate::getEffectiveFrom,
					Comparator.nullsLast(Comparator.reverseOrder())
				))
			.findFirst()
			.map(rate -> new TariffRateResult(
				rate.getTariffType(),
				rate.getBaseRate(),
				rate.getAdditionalRate(),
				rate.getLegalNotice()
			));
	}

	private boolean appliesToOrigin(TariffRate rate, String originCountry) {
		String rateOrigin = normalizeCountry(rate.getOriginCountry());
		return rateOrigin == null || rateOrigin.equals(originCountry);
	}

	private boolean isDirectlyApplicable(TariffRate rate) {
		if (rate.getBaseRate() == null) {
			return false;
		}
		if (rate.getUsageRateCode() != null && !rate.getUsageRateCode().isBlank()) {
			return false;
		}
		if (rate.getCountryScope() != null && !"1".equals(rate.getCountryScope())) {
			return false;
		}
		String tariffCode = rate.getTariffCode();
		if (tariffCode == null || tariffCode.isBlank()) {
			return true;
		}
		return "A".equals(tariffCode) || "C".equals(tariffCode);
	}

	private int originPriority(TariffRate rate, String originCountry) {
		String rateOrigin = normalizeCountry(rate.getOriginCountry());
		return rateOrigin != null && rateOrigin.equals(originCountry) ? 1 : 0;
	}

	private int tariffPriority(TariffRate rate) {
		return switch (rate.getTariffType()) {
			case ANTI_DUMPING, COUNTERVAILING, SAFEGUARD, SPECIAL -> 4;
			case FTA -> 3;
			case WTO -> 2;
			case BASIC -> 1;
			case UNKNOWN -> 0;
		};
	}

	private String normalizeCountry(String value) {
		return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
	}
}
