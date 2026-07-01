package com.example.gonggong.domain.demand.service;

import com.example.gonggong.domain.demand.dto.DemandPriorityItemResponse;
import com.example.gonggong.domain.demand.dto.DemandPriorityTop10Response;
import com.example.gonggong.domain.demand.entity.ImportTrend;
import com.example.gonggong.domain.demand.exception.DemandErrorCode;
import com.example.gonggong.domain.demand.exception.DemandException;
import com.example.gonggong.domain.demand.repository.EssentialIndustryItemReader;
import com.example.gonggong.domain.demand.repository.ImportTrendReader;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemandPriorityServiceTest {

	@Test
	void returnsTop10ByGrowthAndEssentialPartWeight() {
		DemandPriorityService service = new DemandPriorityService(
			new FakeImportTrendRepository(),
			new FakeEssentialIndustryItemRepository()
		);

		DemandPriorityTop10Response response = service.getTop10PriorityItems();

		assertThat(response.items()).hasSize(3);
		assertThat(response.items())
			.extracting(DemandPriorityItemResponse::hskCode)
			.containsExactly("3924", "8504", "9503");

		DemandPriorityItemResponse first = response.items().get(0);
		assertThat(first.itemName()).isEqualTo("플라스틱제 식탁용품");
		assertThat(first.importAmountGrowthRate()).isEqualByComparingTo("100.00");
		assertThat(first.importWeightGrowthRate()).isEqualByComparingTo("100.00");
		assertThat(first.essentialPart()).isTrue();
		assertThat(first.recallCount()).isZero();
		assertThat(first.priorityScore()).isEqualTo(85);
		assertThat(first.priorityLevel()).isEqualTo("HIGH");
		assertThat(first.reason()).contains("수입액 증가율 100.00%");
		assertThat(first.reason()).contains("수입중량 증가율 100.00%");
		assertThat(first.reason()).contains("필수 산업 품목");
	}

	@Test
	void throwsCustomExceptionWhenImportTrendDataIsEmpty() {
		DemandPriorityService service = new DemandPriorityService(
			new EmptyImportTrendRepository(),
			hskCode -> false
		);

		assertThatThrownBy(service::getTop10PriorityItems)
			.isInstanceOf(DemandException.class)
			.extracting("baseCode")
			.isEqualTo(DemandErrorCode.IMPORT_TREND_DATA_EMPTY);
	}

	private static class FakeImportTrendRepository implements ImportTrendReader {

		@Override
		public Optional<String> findLatestYearMonth() {
			return Optional.of("2026-05");
		}

		@Override
		public List<ImportTrend> findByYearMonth(String yearMonth) {
			return List.of(
				new ImportTrend("3924", "플라스틱제 식탁용품", "2026-05", new BigDecimal("2000"), new BigDecimal("400")),
				new ImportTrend("8504", "전기 변압기", "2026-05", new BigDecimal("1500"), new BigDecimal("150")),
				new ImportTrend("9503", "완구류", "2026-05", new BigDecimal("1200"), new BigDecimal("180"))
			);
		}

		@Override
		public Optional<ImportTrend> findByHskCodeAndYearMonth(String hskCode, String yearMonth) {
			return switch (hskCode) {
				case "3924" -> Optional.of(new ImportTrend("3924", "플라스틱제 식탁용품", "2025-05", new BigDecimal("1000"), new BigDecimal("200")));
				case "8504" -> Optional.of(new ImportTrend("8504", "전기 변압기", "2025-05", new BigDecimal("1000"), new BigDecimal("100")));
				case "9503" -> Optional.of(new ImportTrend("9503", "완구류", "2025-05", new BigDecimal("1000"), new BigDecimal("150")));
				default -> Optional.empty();
			};
		}
	}

	private static class FakeEssentialIndustryItemRepository implements EssentialIndustryItemReader {

		@Override
		public boolean existsByHskCode(String hskCode) {
			return "3924".equals(hskCode);
		}
	}

	private static class EmptyImportTrendRepository implements ImportTrendReader {

		@Override
		public Optional<String> findLatestYearMonth() {
			return Optional.empty();
		}

		@Override
		public List<ImportTrend> findByYearMonth(String yearMonth) {
			return List.of();
		}

		@Override
		public Optional<ImportTrend> findByHskCodeAndYearMonth(String hskCode, String yearMonth) {
			return Optional.empty();
		}
	}
}
