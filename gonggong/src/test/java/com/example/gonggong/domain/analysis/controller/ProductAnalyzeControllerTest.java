package com.example.gonggong.domain.analysis.controller;

import com.example.gonggong.domain.analysis.service.ProductAnalyzeService;
import com.example.gonggong.domain.analysis.dto.ProductAnalyzeRequest;
import com.example.gonggong.domain.analysis.openai.ProductNormalizeResult;
import com.example.gonggong.domain.analysis.recall.HarmfulIngredientExtractor;
import com.example.gonggong.domain.analysis.recall.RecallProductNameClassifier;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallClient;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallItem;
import com.example.gonggong.domain.analysis.service.ProductNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductAnalyzeControllerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ProductAnalyzeService productAnalyzeService = new ProductAnalyzeService(
			new FakeProductNormalizer(),
			new FakeSafetyKoreaRecallClient(),
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);
		ProductAnalyzeController controller = new ProductAnalyzeController(productAnalyzeService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void analyzeProductReturnsRecallAnalysisResponse() throws Exception {
		String requestBody = """
			{
			  "productName": "baby plastic bowl",
			  "description": "children plastic tableware, cute baby feeding bowl",
			  "imageUrl": "https://example.com/product.jpg",
			  "pageUrl": "https://www.aliexpress.com/item/123.html",
			  "site": "aliexpress"
			}
			""";

		mockMvc.perform(post("/api/products/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.riskLevel").value("WARNING"))
			.andExpect(jsonPath("$.category").value("어린이용품>완구"))
			.andExpect(jsonPath("$.recallReason").value("프탈레이트계 가소제 기준치 초과 - DEHP 9.0%"))
			.andExpect(jsonPath("$.harmfulIngredients[0]").value("프탈레이트계 가소제"))
			.andExpect(jsonPath("$.harmfulIngredients[1]").value("DEHP"))
			.andExpect(jsonPath("$.matchedRecalls[0].recallProductName").value("운동완구(완구)"))
			.andExpect(jsonPath("$.matchedRecalls[0].modelName").value("TOY-1"))
			.andExpect(jsonPath("$.matchedRecalls[0].manufacturer").value("Example Factory"))
			.andExpect(jsonPath("$.matchedRecalls[0].reason").value("프탈레이트계 가소제 기준치 초과 - DEHP 9.0%"))
			.andExpect(jsonPath("$.message").value("유사 리콜 이력이 있는 상품입니다. 구매 전 상세 정보를 확인하세요."));
	}

	private static class FakeProductNormalizer implements ProductNormalizer {

		@Override
		public ProductNormalizeResult normalize(ProductAnalyzeRequest request) {
			return new ProductNormalizeResult(
				"어린이 완구",
				List.of("완구", "운동완구"),
				null,
				"어린이용품>완구",
				"운동완구(완구)",
				List.of("플라스틱"),
				"어린이",
				List.of(),
				List.of("완구"),
				0.88
			);
		}
	}

	private static class FakeSafetyKoreaRecallClient implements SafetyKoreaRecallClient {

		@Override
		public List<SafetyKoreaRecallItem> searchByProductName(String productName) {
			return List.of(new SafetyKoreaRecallItem(
				"100",
				"운동완구(완구)",
				null,
				"TOY-1",
				"Example Factory",
				"20260101",
				"프탈레이트계 가소제 기준치 초과 - DEHP 9.0%",
				null,
				"교환 및 환불",
				List.of()
			));
		}

		@Override
		public List<SafetyKoreaRecallItem> searchByBrandName(String brandName) {
			return List.of();
		}

		@Override
		public List<SafetyKoreaRecallItem> searchForeignByProductName(String productName) {
			return List.of();
		}

		@Override
		public List<SafetyKoreaRecallItem> searchForeignByBrandName(String brandName) {
			return List.of();
		}

		@Override
		public SafetyKoreaRecallItem findDetail(String recallUid) {
			return null;
		}
	}
}
