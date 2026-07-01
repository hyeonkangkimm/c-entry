package com.example.gonggong.domain.risk.dataset;

import com.example.gonggong.domain.risk.domain.TariffType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TariffDatasetReaderTest {

	@Test
	void streamsAllRowsFromLatestOfficialSheet() {
		AtomicInteger consumed = new AtomicInteger();
		AtomicBoolean containsBasic = new AtomicBoolean();
		AtomicBoolean containsWto = new AtomicBoolean();
		AtomicBoolean containsFta = new AtomicBoolean();
		AtomicBoolean containsFirstCode = new AtomicBoolean();

		int rowCount = new TariffDatasetReader().read(
			new ClassPathResource("data/customs-tariff-rates-20260211.xlsx"),
			500,
			rows -> {
				consumed.addAndGet(rows.size());
				rows.forEach(row -> {
					containsBasic.compareAndSet(false, row.tariffType() == TariffType.BASIC);
					containsWto.compareAndSet(false, row.tariffType() == TariffType.WTO);
					containsFta.compareAndSet(false, row.tariffType() == TariffType.FTA);
					containsFirstCode.compareAndSet(false,
						row.hskCode().equals("0101211000")
							&& "A".equals(row.tariffCode())
							&& row.rate().signum() == 0);
				});
			}
		);

		assertThat(rowCount).isEqualTo(380_216);
		assertThat(consumed).hasValue(380_216);
		assertThat(containsBasic).isTrue();
		assertThat(containsWto).isTrue();
		assertThat(containsFta).isTrue();
		assertThat(containsFirstCode).isTrue();
	}
}
