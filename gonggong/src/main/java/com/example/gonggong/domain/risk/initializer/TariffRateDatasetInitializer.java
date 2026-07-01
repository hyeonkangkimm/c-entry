package com.example.gonggong.domain.risk.initializer;

import com.example.gonggong.domain.risk.dataset.TariffDatasetReader;
import com.example.gonggong.domain.risk.repository.TariffRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
@Component
public class TariffRateDatasetInitializer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(TariffRateDatasetInitializer.class);
	private static final int BATCH_SIZE = 500;
	private static final int EXPECTED_ITEM_COUNT = 380_216;
	private static final String INSERT_SQL = """
		insert into tariff_rate (
			hsk_code, origin_country, tariff_code, tariff_type,
			base_rate, additional_rate, unit_amount, base_price,
			country_scope, usage_rate_code, effective_from, effective_to,
			legal_notice, active
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	private final TariffRateRepository repository;
	private final TariffDatasetReader datasetReader;
	private final JdbcTemplate jdbcTemplate;
	private final Resource datasetResource;
	private final boolean initialize;

	public TariffRateDatasetInitializer(
		TariffRateRepository repository,
		TariffDatasetReader datasetReader,
		JdbcTemplate jdbcTemplate,
		@Value("${tariff.dataset.resource:classpath:data/customs-tariff-rates-20260211.xlsx}")
		Resource datasetResource,
		@Value("${tariff.dataset.initialize:true}") boolean initialize
	) {
		this.repository = repository;
		this.datasetReader = datasetReader;
		this.jdbcTemplate = jdbcTemplate;
		this.datasetResource = datasetResource;
		this.initialize = initialize;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (!initialize) {
			return;
		}

		long existingCount = repository.count();
		if (existingCount == EXPECTED_ITEM_COUNT) {
			return;
		}
		if (existingCount > 0) {
			jdbcTemplate.update("delete from tariff_rate");
		}

		int itemCount = datasetReader.read(datasetResource, BATCH_SIZE, rows -> {
			List<Object[]> arguments = new ArrayList<>(rows.size());
			rows.forEach(row -> arguments.add(new Object[] {
				row.hskCode(),
				null,
				row.tariffCode(),
				row.tariffType().name(),
				row.rate(),
				BigDecimal.ZERO,
				row.unitAmount(),
				row.basePrice(),
				row.countryScope(),
				row.usageRateCode(),
				row.effectiveFrom() == null ? null : Date.valueOf(row.effectiveFrom()),
				row.effectiveTo() == null ? null : Date.valueOf(row.effectiveTo()),
				row.legalNotice(),
				true
			}));
			jdbcTemplate.batchUpdate(INSERT_SQL, arguments);
		});
		if (itemCount != EXPECTED_ITEM_COUNT) {
			throw new IllegalStateException(
				"관세율 데이터 건수가 예상과 다릅니다. expected="
					+ EXPECTED_ITEM_COUNT + ", actual=" + itemCount
			);
		}
		log.info("Official customs tariff dataset initialized itemCount={}", itemCount);
	}
}
