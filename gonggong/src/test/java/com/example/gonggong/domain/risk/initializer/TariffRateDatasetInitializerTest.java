package com.example.gonggong.domain.risk.initializer;

import com.example.gonggong.domain.risk.dataset.TariffDatasetReader;
import com.example.gonggong.domain.risk.dataset.TariffDatasetRow;
import com.example.gonggong.domain.risk.domain.TariffType;
import com.example.gonggong.domain.risk.repository.TariffRateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TariffRateDatasetInitializerTest {

	@Test
	void reloadsOfficialRatesWhenStoredCountDiffers() {
		TariffRateRepository repository = mock(TariffRateRepository.class);
		TariffDatasetReader reader = mock(TariffDatasetReader.class);
		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		Resource resource = new ByteArrayResource(new byte[0]);
		when(repository.count()).thenReturn(10L);
		when(reader.read(eq(resource), anyInt(), any())).thenAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			java.util.function.Consumer<List<TariffDatasetRow>> consumer = invocation.getArgument(2);
			consumer.accept(List.of(new TariffDatasetRow(
				"3924100000",
				"C",
				TariffType.WTO,
				new BigDecimal("6.5"),
				null,
				null,
				"1",
				null,
				LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 12, 31),
				"official source"
			)));
			return 380_216;
		});

		new TariffRateDatasetInitializer(repository, reader, jdbcTemplate, resource, true).run();

		verify(jdbcTemplate).update("delete from tariff_rate");
		verify(jdbcTemplate).batchUpdate(anyString(), anyList());
	}
}
