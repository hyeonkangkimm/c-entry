package com.example.gonggong.domain.hsk.dataset;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HskDatasetReaderTest {

	@Test
	void readsOfficialTenDigitHskSheet() {
		HskDatasetReader reader = new HskDatasetReader();

		List<HskDatasetRow> rows = reader.read(
			new ClassPathResource("data/customs-hsk-items-20260101.xlsx")
		);

		assertThat(rows).hasSize(11327);
		assertThat(rows.get(0).hskCode()).isEqualTo("0101211000");
		assertThat(rows).anyMatch(row -> row.hskCode().equals("3924100000"));
		assertThat(rows).allMatch(row -> row.hskCode().matches("\\d{10}"));
		assertThat(rows)
			.filteredOn(row -> row.hskCode().equals("9102119090"))
			.singleElement()
			.satisfies(row -> {
				assertThat(row.displayName()).contains("손목시계");
				assertThat(row.displayName()).contains("기계식 표시부만을 갖춘 것");
				assertThat(row.displayName()).endsWith("기타");
			});
	}
}
