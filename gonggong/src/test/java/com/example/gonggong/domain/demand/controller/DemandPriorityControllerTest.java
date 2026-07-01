package com.example.gonggong.domain.demand.controller;

import com.example.gonggong.domain.demand.entity.ImportTrend;
import com.example.gonggong.domain.demand.repository.EssentialIndustryItemReader;
import com.example.gonggong.domain.demand.repository.ImportTrendReader;
import com.example.gonggong.domain.demand.service.DemandPriorityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DemandPriorityControllerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		DemandPriorityService service = new DemandPriorityService(
			new FakeImportTrendRepository(),
			hskCode -> true
		);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new DemandPriorityController(service))
			.build();
	}

	@Test
	void returnsDemandPriorityTop10Items() throws Exception {
		mockMvc.perform(get("/api/demand/priority-items/top10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].hskCode").value("3924"))
			.andExpect(jsonPath("$.items[0].itemName").value("플라스틱제 식탁용품"))
			.andExpect(jsonPath("$.items[0].priorityScore").value(85))
			.andExpect(jsonPath("$.items[0].priorityLevel").value("HIGH"));
	}

	private static class FakeImportTrendRepository implements ImportTrendReader {

		@Override
		public Optional<String> findLatestYearMonth() {
			return Optional.of("2026-05");
		}

		@Override
		public List<ImportTrend> findByYearMonth(String yearMonth) {
			return List.of(
				new ImportTrend("3924", "플라스틱제 식탁용품", "2026-05", new BigDecimal("2000"), new BigDecimal("400"))
			);
		}

		@Override
		public Optional<ImportTrend> findByHskCodeAndYearMonth(String hskCode, String yearMonth) {
			return Optional.of(new ImportTrend("3924", "플라스틱제 식탁용품", "2025-05", new BigDecimal("1000"), new BigDecimal("200")));
		}
	}
}
