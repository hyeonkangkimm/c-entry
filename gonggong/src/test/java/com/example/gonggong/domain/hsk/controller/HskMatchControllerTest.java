package com.example.gonggong.domain.hsk.controller;

import com.example.gonggong.domain.hsk.dto.HskCandidateResponse;
import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import com.example.gonggong.domain.hsk.dto.HskMatchResponse;
import com.example.gonggong.domain.hsk.service.HskMatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HskMatchControllerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.standaloneSetup(new HskMatchController(new FakeHskMatchService()))
			.build();
	}

	@Test
	void returnsHskCandidates() throws Exception {
		mockMvc.perform(post("/api/seller/hsk/match")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "productName": "baby plastic bowl",
					  "description": "plastic tableware for children",
					  "imageUrl": null
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.matched").value(true))
			.andExpect(jsonPath("$.candidates[0].hskCode").value("3924100000"))
			.andExpect(jsonPath("$.candidates[0].displayName").value("플라스틱제 식탁용품 > 플라스틱으로 만든 식탁용품과 주방용품"));
	}

	private static class FakeHskMatchService extends HskMatchService {

		private FakeHskMatchService() {
			super(null, null);
		}

		@Override
		public HskMatchResponse match(HskMatchRequest request) {
			return new HskMatchResponse(
				true,
				List.of(new HskCandidateResponse(
					"3924100000",
					"플라스틱으로 만든 식탁용품과 주방용품",
					"Tableware and kitchenware, of plastics",
					"플라스틱제 식탁용품 > 플라스틱으로 만든 식탁용품과 주방용품",
					0.91,
					"공식 HSK 품목명과 상품 특징이 일치합니다."
				)),
				"공식 관세청 HSK 품목 데이터에서 후보를 찾았습니다."
			);
		}
	}
}
