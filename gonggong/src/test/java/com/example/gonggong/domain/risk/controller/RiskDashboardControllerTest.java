package com.example.gonggong.domain.risk.controller;

import com.example.gonggong.domain.risk.domain.OverallRiskLevel;
import com.example.gonggong.domain.risk.domain.RiskStatus;
import com.example.gonggong.domain.risk.dto.response.ChemicalRiskResponse;
import com.example.gonggong.domain.risk.dto.response.CustomsRiskResponse;
import com.example.gonggong.domain.risk.dto.response.KcRiskResponse;
import com.example.gonggong.domain.risk.dto.response.RecallRiskResponse;
import com.example.gonggong.domain.risk.dto.response.RiskDashboardResponse;
import com.example.gonggong.domain.risk.service.RiskDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RiskDashboardControllerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.standaloneSetup(new RiskDashboardController(new FakeRiskDashboardService()))
			.build();
	}

	@Test
	void analyzesRiskDashboard() throws Exception {
		String body = """
			{
			  "hskCode": "3924100000",
			  "productName": "유아용 플라스틱 식기",
			  "productDescription": "어린이용 플라스틱 그릇",
			  "ingredients": ["PVC", "프탈레이트"],
			  "originCountry": "CN",
			  "declaredValue": 100000,
			  "currency": "KRW",
			  "quantity": 10,
			  "shippingCost": 10000,
			  "insuranceCost": 0,
			  "kcCertificationNumber": null
			}
			""";

		mockMvc.perform(post("/api/v1/risk-dashboard/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hskCode").value("3924100000"))
			.andExpect(jsonPath("$.productName").value("유아용 플라스틱 식기"))
			.andExpect(jsonPath("$.overallRiskLevel").value("HIGH"))
			.andExpect(jsonPath("$.recallRisk.status").value("WARNING"))
			.andExpect(jsonPath("$.customsRisk.status").value("UNKNOWN"))
			.andExpect(jsonPath("$.kcRisk.status").value("UNKNOWN"))
			.andExpect(jsonPath("$.chemicalRisk.status").value("UNKNOWN"));
	}

	private static class FakeRiskDashboardService extends RiskDashboardService {

		private FakeRiskDashboardService() {
			super(null, null, null, null, null);
		}

		@Override
		public RiskDashboardResponse analyze(com.example.gonggong.domain.risk.dto.request.RiskDashboardAnalyzeRequest request) {
			return new RiskDashboardResponse(
				null,
				request.hskCode(),
				request.productName(),
				OverallRiskLevel.HIGH,
				78,
				LocalDateTime.of(2026, 6, 23, 15, 30),
				new RecallRiskResponse(RiskStatus.WARNING, 70, 0, null, "리콜 정보 조회 완료", List.of()),
				new CustomsRiskResponse(RiskStatus.UNKNOWN, 50, null, null, null, null, null, "미등록 코드입니다. 관세청 정식 법령 지침을 확인해 주세요.", "https://unipass.customs.go.kr/"),
				new KcRiskResponse(RiskStatus.UNKNOWN, 50, null, false, null, null, null, "정보가 없습니다.", "https://www.safetykorea.kr/", "제품안전정보센터에서 실시간 검증하기"),
		new ChemicalRiskResponse(RiskStatus.UNKNOWN, 50, "성분 정보를 현재 조회할 수 없습니다.", List.of(), "https://icis.mcee.go.kr/"),
				List.of()
			);
		}
	}
}
