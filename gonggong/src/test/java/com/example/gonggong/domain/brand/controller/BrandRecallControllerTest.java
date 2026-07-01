package com.example.gonggong.domain.brand.controller;

import com.example.gonggong.domain.brand.service.BrandRecallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrandRecallControllerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		BrandRecallService brandRecallService = new BrandRecallService();
		BrandRecallController controller = new BrandRecallController(brandRecallService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void returnsRecallHistoryWhenBrandHasDummyRecallRecords() throws Exception {
		mockMvc.perform(get("/api/brands/{brandName}/recalls", "Example Brand"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.brandName").value("Example Brand"))
			.andExpect(jsonPath("$.exists").value(true))
			.andExpect(jsonPath("$.message").value("이 브랜드의 다른 제품 리콜 이력이 있습니다."))
			.andExpect(jsonPath("$.items[0].productName").value("Example Brand 전기 온열기"))
			.andExpect(jsonPath("$.items[0].defectReason").value("온도 상승 시험 기준 초과"))
			.andExpect(jsonPath("$.items[0].publishedAt").value("2025-11-18"))
			.andExpect(jsonPath("$.items[1].productName").value("Example Brand 무선 마우스"))
			.andExpect(jsonPath("$.items[1].defectReason").value("전자파 적합성 기준 부적합"))
			.andExpect(jsonPath("$.items[1].publishedAt").value("2025-09-02"));
	}

	@Test
	void returnsEmptyMessageWhenBrandHasNoRecallHistory() throws Exception {
		mockMvc.perform(get("/api/brands/{brandName}/recalls", "Safe Brand"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.brandName").value("Safe Brand"))
			.andExpect(jsonPath("$.exists").value(false))
			.andExpect(jsonPath("$.message").value("리콜 이력이 존재하지 않습니다."))
			.andExpect(jsonPath("$.items").isArray())
			.andExpect(jsonPath("$.items").isEmpty());
	}
}
