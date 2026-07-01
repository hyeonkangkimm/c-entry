package com.example.gonggong.domain.analysis.openai;

import com.example.gonggong.domain.analysis.dto.ProductAnalyzeRequest;
import com.example.gonggong.domain.analysis.exception.AnalysisErrorCode;
import com.example.gonggong.domain.analysis.exception.AnalysisException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiProductNormalizeClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void sendsStructuredOutputRequestAndParsesNormalizedProduct() {
		FakeOpenAiTransport transport = new FakeOpenAiTransport("""
			{
			  "output": [
			    {
			      "content": [
			        {
			          "type": "output_text",
			          "text": "{\\"standardProductName\\":\\"무선 마우스\\",\\"searchKeywords\\":[\\"마우스\\",\\"무선 마우스\\"],\\"brandName\\":null,\\"category\\":\\"기타\\",\\"matchedRecallProductName\\":\\"컴퓨터 주변기기\\",\\"materialKeywords\\":[\\"플라스틱\\"],\\"targetUser\\":\\"일반\\",\\"riskIngredientKeywords\\":[],\\"hskCandidateKeywords\\":[\\"컴퓨터 주변기기\\"],\\"confidence\\":0.84}"
			        }
			      ]
			    }
			  ]
			}
			""");
		OpenAiProductNormalizeClient client = new OpenAiProductNormalizeClient(
			new OpenAiProperties("test-key", "gpt-test", "https://api.openai.com/v1/responses"),
			new ProductNormalizePromptBuilder(),
			transport,
			objectMapper
		);

		ProductNormalizeResult result = client.normalize(new ProductAnalyzeRequest(
			"Wireless Mouse RGB Rechargeable Gaming Mouse",
			"2.4G USB mouse for laptop",
			"https://example.com/mouse.jpg",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(transport.lastApiKey).isEqualTo("test-key");
		assertThat(transport.lastEndpoint).isEqualTo("https://api.openai.com/v1/responses");
		assertThat(transport.lastBody).contains("\"model\":\"gpt-test\"");
		assertThat(transport.lastBody).contains("\"json_schema\"");
		assertThat(transport.lastBody).contains("product_normalization_result");
		assertThat(transport.lastBody).contains("matchedRecallProductName");
		assertThat(transport.lastBody).contains("primaryProductName");
		assertThat(transport.lastBody).contains("primarySearchKeywords");
		assertThat(transport.lastBody).contains("componentKeywords");
		assertThat(transport.lastBody).contains("featureKeywords");
		assertThat(result.standardProductName()).isEqualTo("무선 마우스");
		assertThat(result.category()).isEqualTo("기타");
		assertThat(result.matchedRecallProductName()).isEqualTo("컴퓨터 주변기기");
		assertThat(result.searchKeywords()).containsExactly("마우스", "무선 마우스");
		assertThat(result.confidence()).isEqualTo(0.84);
	}

	@Test
	void parsesStructuredChemicalCandidatesFromOpenAiResponse() {
		FakeOpenAiTransport transport = new FakeOpenAiTransport("""
			{
			  "output": [
			    {
			      "content": [
			        {
			          "type": "output_text",
			          "text": "{\\"standardProductName\\":\\"테스트 상품\\",\\"searchKeywords\\":[\\"테스트\\"],\\"brandName\\":null,\\"category\\":\\"기타\\",\\"matchedRecallProductName\\":null,\\"modelName\\":null,\\"barcodeNum\\":null,\\"certNum\\":null,\\"materialKeywords\\":[],\\"targetUser\\":\\"일반\\",\\"riskIngredientKeywords\\":[\\"납\\"],\\"chemicalCandidates\\":[{\\"name\\":\\"납\\",\\"casNumber\\":null,\\"englishName\\":\\"lead\\"}],\\"hskCandidateKeywords\\":[\\"테스트\\"],\\"primaryProductName\\":\\"테스트 상품\\",\\"productForm\\":\\"UNKNOWN\\",\\"primarySearchKeywords\\":[\\"테스트\\"],\\"kcCertificationSearchKeywords\\":[],\\"componentKeywords\\":[],\\"featureKeywords\\":[],\\"confidence\\":0.8}"
			        }
			      ]
			    }
			  ]
			}
			""");
		OpenAiProductNormalizeClient client = new OpenAiProductNormalizeClient(
			new OpenAiProperties("test-key", "gpt-test", "https://api.openai.com/v1/responses"),
			new ProductNormalizePromptBuilder(),
			transport,
			objectMapper
		);

		ProductNormalizeResult result = client.normalize(new ProductAnalyzeRequest(
			"Wireless Mouse RGB Rechargeable Gaming Mouse",
			"2.4G USB mouse for laptop",
			"https://example.com/mouse.jpg",
			"https://www.aliexpress.com/item/123.html",
			"aliexpress"
		));

		assertThat(result.chemicalCandidates()).singleElement().satisfies(candidate -> {
			assertThat(candidate.name()).isEqualTo("납");
			assertThat(candidate.englishName()).isEqualTo("lead");
		});
	}

	@Test
	void throwsCustomExceptionWhenApiKeyIsMissing() {
		OpenAiProductNormalizeClient client = new OpenAiProductNormalizeClient(
			new OpenAiProperties("", "gpt-test", "https://api.openai.com/v1/responses"),
			new ProductNormalizePromptBuilder(),
			new FakeOpenAiTransport("{}"),
			objectMapper
		);

		assertThatThrownBy(() -> client.normalize(new ProductAnalyzeRequest("mouse", "", "", "", "aliexpress")))
			.isInstanceOf(AnalysisException.class)
			.extracting("baseCode")
			.isEqualTo(AnalysisErrorCode.OPENAI_API_KEY_MISSING);
	}

	private static class FakeOpenAiTransport implements OpenAiTransport {

		private final String responseBody;
		private String lastEndpoint;
		private String lastApiKey;
		private String lastBody;

		private FakeOpenAiTransport(String responseBody) {
			this.responseBody = responseBody;
		}

		@Override
		public String postJson(String endpoint, String apiKey, String body) {
			this.lastEndpoint = endpoint;
			this.lastApiKey = apiKey;
			this.lastBody = body;
			return responseBody;
		}
	}
}
