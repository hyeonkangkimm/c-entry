package com.example.gonggong.domain.risk.provider;

import com.example.gonggong.domain.risk.domain.TariffRate;
import com.example.gonggong.domain.risk.domain.TariffType;
import com.example.gonggong.domain.risk.repository.TariffRateRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaTariffRateProviderTest {

	@Test
	void prefersExactOriginRateOverCommonRate() {
		TariffRateRepository repository = mock(TariffRateRepository.class);
		LocalDate date = LocalDate.of(2026, 6, 25);
		when(repository.findEffectiveRates("3924100000", date)).thenReturn(List.of(
			new TariffRate(
				"3924100000",
				null,
				TariffType.WTO,
				new BigDecimal("8.0"),
				BigDecimal.ZERO,
				LocalDate.of(2026, 1, 1),
				null,
				null,
				true
			),
			new TariffRate(
				"3924100000",
				"CN",
				TariffType.ANTI_DUMPING,
				new BigDecimal("8.0"),
				new BigDecimal("12.0"),
				LocalDate.of(2026, 1, 1),
				null,
				"덤핑방지관세 고시",
				true
			)
		));

		TariffRateResult result = new JpaTariffRateProvider(repository)
			.findTariffRate("3924100000", "cn", date)
			.orElseThrow();

		assertThat(result.tariffType()).isEqualTo(TariffType.ANTI_DUMPING);
		assertThat(result.finalRate()).isEqualByComparingTo("20.0");
	}

	@Test
	void doesNotApplyOfficialFtaRateWithoutOriginProofCondition() {
		TariffRateRepository repository = mock(TariffRateRepository.class);
		LocalDate date = LocalDate.of(2026, 6, 25);
		when(repository.findEffectiveRates("3924100000", date)).thenReturn(List.of(
			new TariffRate(
				"3924100000",
				null,
				"FRCCN1",
				TariffType.FTA,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				null,
				null,
				"2",
				null,
				LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 12, 31),
				"한중 FTA",
				true
			),
			new TariffRate(
				"3924100000",
				null,
				"C",
				TariffType.WTO,
				new BigDecimal("6.5"),
				BigDecimal.ZERO,
				null,
				null,
				"1",
				null,
				LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 12, 31),
				"WTO",
				true
			)
		));

		TariffRateResult result = new JpaTariffRateProvider(repository)
			.findTariffRate("3924100000", "CN", date)
			.orElseThrow();

		assertThat(result.tariffType()).isEqualTo(TariffType.WTO);
		assertThat(result.baseRate()).isEqualByComparingTo("6.5");
	}
}
