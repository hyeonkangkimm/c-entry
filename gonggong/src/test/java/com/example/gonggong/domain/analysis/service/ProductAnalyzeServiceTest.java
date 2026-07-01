package com.example.gonggong.domain.analysis.service;

import com.example.gonggong.domain.analysis.RiskLevel;
import com.example.gonggong.domain.analysis.dto.ProductAnalyzeRequest;
import com.example.gonggong.domain.analysis.dto.ProductAnalyzeResponse;
import com.example.gonggong.domain.analysis.exception.AnalysisErrorCode;
import com.example.gonggong.domain.analysis.exception.AnalysisException;
import com.example.gonggong.domain.analysis.openai.ProductNormalizeResult;
import com.example.gonggong.domain.analysis.recall.HarmfulIngredientExtractor;
import com.example.gonggong.domain.analysis.recall.RecallProductNameClassifier;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallClient;
import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductAnalyzeServiceTest {

	@Test
	void analyzesProductBySearchingSafetyKoreaWithOpenAiKeywords() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"아동용 책가방",
			List.of("책가방", "아동 책가방", "가정용섬유제품"),
			"JJ",
			"생활용품>가방/섬유제품",
			"가정용섬유제품(책가방)",
			List.of("섬유"),
			"어린이",
			List.of(),
			List.of("가방"),
			0.91
		));
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient(List.of(
			new SafetyKoreaRecallItem(
				"3802",
				"가정용섬유제품(책가방)",
				"JJ",
				"HKAK31101S-00",
				"(주)이랜드월드패션사업부",
				"20130418",
				"프탈레이트계 가소제 기준치 초과 - DEHP 5.6%, DBP 0.5%",
				null,
				"수선 및 교환, 환불",
				null
			),
			new SafetyKoreaRecallItem(
				"3802",
				"가정용섬유제품(책가방)",
				"JJ",
				"HKAK31101S-00",
				"(주)이랜드월드패션사업부",
				"20130418",
				"프탈레이트계 가소제 기준치 초과 - DEHP 5.6%, DBP 0.5%",
				null,
				"수선 및 교환, 환불",
				null
			)
		));
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			recallClient,
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"kids school backpack waterproof lightweight",
			"",
			"",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(recallClient.productNameQueries)
			.containsExactly("책가방", "아동 책가방", "가정용섬유제품");
		assertThat(recallClient.brandNameQueries).containsExactly("JJ");
		assertThat(response.riskLevel()).isEqualTo(RiskLevel.DANGER);
		assertThat(response.category()).isEqualTo("생활용품>가방/섬유제품");
		assertThat(response.brandName()).isEqualTo("JJ");
		assertThat(response.standardProductName()).isEqualTo("아동용 책가방");
		assertThat(response.primaryProductName()).isEqualTo("아동용 책가방");
		assertThat(response.primarySearchKeywords()).containsExactly("가방");
		assertThat(response.recallReason()).isEqualTo("프탈레이트계 가소제 기준치 초과 - DEHP 5.6%, DBP 0.5%");
		assertThat(response.harmfulIngredients()).containsExactly("프탈레이트계 가소제", "DEHP", "DBP");
		assertThat(response.matchedRecalls()).hasSize(1);
		assertThat(response.matchedRecalls().get(0).recallProductName()).isEqualTo("가정용섬유제품(책가방)");
		assertThat(response.matchedRecalls().get(0).announcementDate()).isEqualTo("2013-04-18");
		assertThat(response.matchedRecalls().get(0).similarity()).isGreaterThanOrEqualTo(0.8);
	}

	@Test
	void includesForeignRecallSearchResultsWithSource() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"kids backpack",
			List.of("backpack"),
			"BrandA",
			"생활용품>가방/섬유제품",
			"가정용섬유제품(책가방)",
			List.of("fabric"),
			"children",
			List.of(),
			List.of("bag"),
			0.84
		));
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient(List.of(new SafetyKoreaRecallItem(
			"domestic-1",
			"가정용섬유제품(책가방)",
			"BrandA",
			"MODEL-D",
			"Domestic Company",
			"20260101",
			"DEHP 기준치 초과",
			null,
			"교환 및 환불",
			List.of()
		)));
		recallClient.foreignItems = List.of(new SafetyKoreaRecallItem(
			"foreign-1",
			"School backpack",
			"BrandA",
			"MODEL-F",
			"Foreign Maker",
			"20260201",
			"Lead content exceeds the limit",
			null,
			"Recall and refund",
			List.of()
		));
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			recallClient,
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"BrandA kids backpack",
			"",
			"",
			"https://www.temu.com/item/123.html",
			"temu"
		));

		assertThat(recallClient.productNameQueries).containsExactly("backpack");
		assertThat(recallClient.foreignProductNameQueries).containsExactly("backpack");
		assertThat(recallClient.brandNameQueries).containsExactly("BrandA");
		assertThat(recallClient.foreignBrandNameQueries).containsExactly("BrandA");
		assertThat(response.matchedRecalls())
			.extracting("source")
			.containsExactlyInAnyOrder("DOMESTIC", "FOREIGN");
		assertThat(response.matchedRecalls())
			.extracting("recallProductName")
			.contains("가정용섬유제품(책가방)", "School backpack");
	}

	@Test
	void continuesAnalysisWhenForeignRecallSearchFails() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"kids backpack",
			List.of("backpack"),
			null,
			"생활용품>가방/섬유제품",
			"가정용섬유제품(책가방)",
			List.of("fabric"),
			"children",
			List.of(),
			List.of("bag"),
			0.84
		));
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient(List.of(new SafetyKoreaRecallItem(
			"domestic-1",
			"가정용섬유제품(책가방)",
			"BrandA",
			"MODEL-D",
			"Domestic Company",
			"20260101",
			"DEHP 기준치 초과",
			null,
			"교환 및 환불",
			List.of()
		)));
		recallClient.failForeignProductSearch = true;
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			recallClient,
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"BrandA kids backpack",
			"",
			"",
			"https://www.temu.com/item/123.html",
			"temu"
		));

		assertThat(response.matchedRecalls()).hasSize(1);
		assertThat(response.matchedRecalls().get(0).source()).isEqualTo("DOMESTIC");
	}

	@Test
	void enrichesRecallListItemsWithDetailWhenReasonIsMissing() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"아동용 책가방",
			List.of("책가방"),
			null,
			"생활용품>가방/섬유제품",
			"가정용섬유제품(책가방)",
			List.of(),
			"어린이",
			List.of(),
			List.of(),
			0.9
		));
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient(List.of(
			new SafetyKoreaRecallItem(
				"3802",
				"가정용섬유제품(책가방)",
				"JJ",
				"HKAK31101S-00",
				"(주)이랜드월드패션사업부",
				"20130418",
				null,
				null,
				"수선 및 교환, 환불",
				null
			)
		));
		recallClient.detailItem = new SafetyKoreaRecallItem(
			"3802",
			"가정용섬유제품(책가방)",
			"JJ",
			"HKAK31101S-00",
			"(주)이랜드월드패션사업부",
			"20130418",
			"납 기준치 초과",
			"납에 노출될 경우 위해 가능",
			"수선 및 교환, 환불",
			List.of()
		);
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			recallClient,
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"kids school backpack",
			"",
			"",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(recallClient.detailQueries).containsExactly("3802");
		assertThat(response.recallReason()).isEqualTo("납 기준치 초과");
		assertThat(response.harmfulIngredients()).containsExactly("납");
	}

	@Test
	void returnsDangerOnlyWhenSameModelRecallHasHazardousIngredient() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"아동용 책가방 HKAK31101S-00",
			List.of("책가방"),
			"JJ",
			"생활용품>가방/섬유제품",
			"가정용섬유제품(책가방)",
			"HKAK31101S-00",
			null,
			null,
			List.of("섬유"),
			"어린이",
			List.of(),
			List.of(),
			0.82
		));
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			new FakeSafetyKoreaRecallClient(List.of(new SafetyKoreaRecallItem(
				"3802",
				"가정용섬유제품(책가방)",
				"JJ",
				"HKAK31101S-00",
				"(주)이랜드월드패션사업부",
				"20130418",
				null,
				null,
				null,
				null,
				"프탈레이트계 가소제 기준치 초과 - DEHP 5.6%",
				null,
				"수선 및 교환, 환불",
				List.of()
			))),
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"JJ HKAK31101S-00 kids backpack",
			"",
			"",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(response.riskLevel()).isEqualTo(RiskLevel.DANGER);
		assertThat(response.riskScore()).isGreaterThanOrEqualTo(90);
	}

	@Test
	void returnsWarningForCategoryOnlyHazardousRecall() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"아동용 책가방",
			List.of("책가방"),
			null,
			"생활용품>가방/섬유제품",
			"가정용섬유제품(책가방)",
			null,
			null,
			null,
			List.of("섬유"),
			"어린이",
			List.of(),
			List.of(),
			0.62
		));
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			new FakeSafetyKoreaRecallClient(List.of(new SafetyKoreaRecallItem(
				"3802",
				"가정용섬유제품(책가방)",
				"JJ",
				"HKAK31101S-00",
				"(주)이랜드월드패션사업부",
				"20130418",
				null,
				null,
				null,
				null,
				"프탈레이트계 가소제 기준치 초과 - DEHP 5.6%",
				null,
				"수선 및 교환, 환불",
				List.of()
			))),
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"kids backpack",
			"",
			"",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(response.riskLevel()).isEqualTo(RiskLevel.WARNING);
		assertThat(response.riskScore()).isBetween(60, 79);
	}

	@Test
	void returnsDangerForConcreteProductKeywordWithSevereHazardEvenWithoutExactIdentifiers() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"키즈 안전 축구공",
			List.of("키즈 안전 축구공"),
			null,
			"어린이용품>완구",
			"운동완구(완구)",
			null,
			null,
			null,
			List.of("플라스틱"),
			"어린이",
			List.of(),
			List.of(),
			0.87
		));
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			new FakeSafetyKoreaRecallClient(List.of(new SafetyKoreaRecallItem(
				"10022224",
				"운동완구(완구)",
				"YAYA CORPORATION",
				null,
				"주식회사 야야 부산지점",
				"20260226",
				null,
				null,
				null,
				null,
				"프탈레이트계 가소제 기준치 411.1배 초과 - DEHP 41.101%",
				null,
				"즉시 사용 중지 및 교환, 환불",
				List.of()
			))),
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"YAYA kids safety soccer ball",
			"",
			"",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(response.riskLevel()).isEqualTo(RiskLevel.DANGER);
		assertThat(response.riskScore()).isGreaterThanOrEqualTo(90);
	}

	@Test
	void returnsReviewForCategoryOnlyCompletedRecallWithoutHazardousIngredient() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"아동용 책가방",
			List.of("책가방"),
			null,
			"생활용품>가방/섬유제품",
			"가정용섬유제품(책가방)",
			null,
			null,
			null,
			List.of("섬유"),
			"어린이",
			List.of(),
			List.of(),
			0.58
		));
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			new FakeSafetyKoreaRecallClient(List.of(new SafetyKoreaRecallItem(
				"3802",
				"가정용섬유제품(책가방)",
				"JJ",
				"HKAK31101S-00",
				"(주)이랜드월드패션사업부",
				"20130418",
				"20130501",
				"20130530",
				null,
				null,
				null,
				null,
				"수선 및 교환, 환불 완료",
				List.of()
			))),
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"kids backpack",
			"",
			"",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(response.riskLevel()).isEqualTo(RiskLevel.REVIEW);
		assertThat(response.riskScore()).isBetween(30, 59);
	}

	@Test
	void excludesRecallWhenWatchKeywordOnlyMatchesHandWarmerShape() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"손목시계",
			List.of("손목시계", "시계"),
			null,
			"생활용품>시계",
			null,
			null,
			null,
			null,
			List.of("금속"),
			"일반",
			List.of(),
			List.of("손목시계", "시계", "watch"),
			"손목시계",
			"FINISHED_PRODUCT",
			List.of("손목시계", "시계", "watch"),
			List.of(),
			List.of("액세서리"),
			0.91
		));
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient(List.of());
		recallClient.foreignItems = List.of(new SafetyKoreaRecallItem(
			"foreign-warmer-1",
			"회중시계 모양 전기 손난로",
			null,
			null,
			"Foreign Maker",
			"20260201",
			"해당 제품에서 니켈의 용출량이 기준치를 초과하였으며 니켈은 피부 알레르기 반응을 유발할 수 있음.",
			null,
			"Recall and refund",
			List.of()
		));
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			recallClient,
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"men wrist watch",
			"",
			"",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(recallClient.foreignProductNameQueries).containsExactly("손목시계", "시계");
		assertThat(response.riskLevel()).isEqualTo(RiskLevel.NORMAL);
		assertThat(response.matchedRecalls()).isEmpty();
		assertThat(response.harmfulIngredients()).isEmpty();
		assertThat(response.recallReason()).isEqualTo("매칭된 리콜 이력이 없습니다.");
	}

	@Test
	void usesAiRefinedRiskIngredientsWhenNoRecallMatchExists() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"테스트 상품",
			List.of("테스트 상품"),
			null,
			"기타",
			null,
			null,
			null,
			null,
			List.of(),
			"일반",
			List.of("납"),
			List.of("테스트"),
			"테스트 상품",
			"UNKNOWN",
			List.of("테스트"),
			List.of(),
			List.of(),
			0.72
		));
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			new FakeSafetyKoreaRecallClient(List.of()),
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"test product",
			"",
			"",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(response.matchedRecalls()).isEmpty();
		assertThat(response.harmfulIngredients()).containsExactly("납");
		assertThat(response.recallReason()).isEqualTo("매칭된 리콜 이력이 없습니다.");
	}

	@Test
	void exposesStructuredChemicalCandidatesFromNormalizedAnalysis() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"테스트 상품",
			List.of("테스트"),
			null,
			"기타",
			null,
			null,
			null,
			null,
			List.of(),
			"일반",
			List.of("납"),
			List.of("테스트"),
			"테스트 상품",
			"UNKNOWN",
			List.of("테스트"),
			List.of(),
			List.of(),
			List.of(),
			List.of(new com.example.gonggong.domain.risk.chemical.ChemicalIngredientCandidate("납", null, "lead")),
			0.72
		));
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			new FakeSafetyKoreaRecallClient(List.of()),
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"test product",
			"",
			"",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(response.chemicalCandidates()).singleElement().satisfies(candidate -> {
			assertThat(candidate.name()).isEqualTo("납");
			assertThat(candidate.englishName()).isEqualTo("lead");
		});
	}

	@Test
	void excludesRecallWhenKeyboardKeywordOnlyMatchesStuffedToyShape() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"게이밍 키보드",
			List.of("키보드"),
			null,
			"전기용품>컴퓨터 입력장치",
			null,
			null,
			null,
			null,
			List.of("플라스틱"),
			"일반",
			List.of(),
			List.of("키보드", "keyboard"),
			"게이밍 키보드",
			"FINISHED_PRODUCT",
			List.of("키보드", "keyboard"),
			List.of(),
			List.of("유선", "RGB"),
			0.91
		));
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient(List.of(new SafetyKoreaRecallItem(
			"toy-keyboard-shape-1",
			"키보드 모양 봉제인형",
			null,
			null,
			"Toy Maker",
			"20260201",
			"프탈레이트계 가소제 기준치 초과",
			null,
			"Recall and refund",
			List.of()
		)));
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			recallClient,
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor()
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"gaming keyboard",
			"",
			"",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(recallClient.productNameQueries).containsExactly("키보드");
		assertThat(response.riskLevel()).isEqualTo(RiskLevel.NORMAL);
		assertThat(response.matchedRecalls()).isEmpty();
		assertThat(response.harmfulIngredients()).isEmpty();
	}

	@Test
	void returnsNormalWhenAiRelevanceDecisionRejectsSimilarRecallCandidates() {
		FakeProductNormalizer normalizer = new FakeProductNormalizer(new ProductNormalizeResult(
			"유선 키보드",
			List.of("키보드"),
			null,
			"전기용품>컴퓨터 입력장치",
			null,
			null,
			null,
			null,
			List.of("플라스틱"),
			"일반",
			List.of(),
			List.of("키보드", "keyboard"),
			"유선 키보드",
			"FINISHED_PRODUCT",
			List.of("키보드", "keyboard"),
			List.of(),
			List.of("유선", "104키"),
			0.92
		));
		FakeSafetyKoreaRecallClient recallClient = new FakeSafetyKoreaRecallClient(List.of(new SafetyKoreaRecallItem(
			"foreign-keyring-1",
			"LED 내장된 팽이, 키보드 키캡 모양 키링",
			null,
			null,
			"Foreign Maker",
			"20260201",
			"프탈레이트계 가소제 기준치 초과",
			null,
			"Recall and refund",
			List.of()
		)));
		FakeRecallRelevanceDecider relevanceDecider = new FakeRecallRelevanceDecider(List.of());
		ProductAnalyzeService service = new ProductAnalyzeService(
			normalizer,
			recallClient,
			new RecallProductNameClassifier(),
			new HarmfulIngredientExtractor(),
			relevanceDecider
		);

		ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
			"g-clicker wired keyboard silence M RGB",
			"",
			"",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(relevanceDecider.lastCandidates)
			.extracting(SafetyKoreaRecallItem::recallProductName)
			.containsExactly("LED 내장된 팽이, 키보드 키캡 모양 키링");
		assertThat(response.riskLevel()).isEqualTo(RiskLevel.NORMAL);
		assertThat(response.matchedRecalls()).isEmpty();
		assertThat(response.harmfulIngredients()).isEmpty();
		assertThat(response.recallReason()).isEqualTo("매칭된 리콜 이력이 없습니다.");
	}

	private static class FakeProductNormalizer implements ProductNormalizer {

		private final ProductNormalizeResult result;

		private FakeProductNormalizer(ProductNormalizeResult result) {
			this.result = result;
		}

		@Override
		public ProductNormalizeResult normalize(ProductAnalyzeRequest request) {
			return result;
		}
	}

	private static class FakeSafetyKoreaRecallClient implements SafetyKoreaRecallClient {

		private final List<SafetyKoreaRecallItem> items;
		private List<SafetyKoreaRecallItem> foreignItems = List.of();
		private final List<String> productNameQueries = new ArrayList<>();
		private final List<String> brandNameQueries = new ArrayList<>();
		private final List<String> foreignProductNameQueries = new ArrayList<>();
		private final List<String> foreignBrandNameQueries = new ArrayList<>();
		private final List<String> detailQueries = new ArrayList<>();
		private SafetyKoreaRecallItem detailItem;
		private boolean failForeignProductSearch;

		private FakeSafetyKoreaRecallClient(List<SafetyKoreaRecallItem> items) {
			this.items = items;
		}

		@Override
		public List<SafetyKoreaRecallItem> searchByProductName(String productName) {
			productNameQueries.add(productName);
			return items;
		}

		@Override
		public List<SafetyKoreaRecallItem> searchByBrandName(String brandName) {
			brandNameQueries.add(brandName);
			return List.of();
		}

		public List<SafetyKoreaRecallItem> searchForeignByProductName(String productName) {
			foreignProductNameQueries.add(productName);
			if (failForeignProductSearch) {
				throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_FAILED);
			}
			return foreignItems;
		}

		public List<SafetyKoreaRecallItem> searchForeignByBrandName(String brandName) {
			foreignBrandNameQueries.add(brandName);
			return List.of();
		}

		@Override
		public SafetyKoreaRecallItem findDetail(String recallUid) {
			detailQueries.add(recallUid);
			return detailItem;
		}
	}

	private static class FakeRecallRelevanceDecider implements RecallRelevanceDecider {

		private final List<SafetyKoreaRecallItem> relevantItems;
		private List<SafetyKoreaRecallItem> lastCandidates = List.of();

		private FakeRecallRelevanceDecider(List<SafetyKoreaRecallItem> relevantItems) {
			this.relevantItems = relevantItems;
		}

		@Override
		public List<SafetyKoreaRecallItem> selectRelevant(ProductNormalizeResult normalized, List<SafetyKoreaRecallItem> candidates) {
			lastCandidates = candidates;
			return relevantItems;
		}
	}
}
