package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.analysis.openai.OpenAiProperties;
import com.example.gonggong.domain.analysis.openai.OpenAiTransport;
import com.example.gonggong.domain.hsk.domain.HskItem;
import com.example.gonggong.domain.hsk.dto.HskMatchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class OpenAiHskCandidateRerankerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void selectsOnlyHskCodeContainedInVectorCandidates(CapturedOutput output) {
		FakeOpenAiTransport transport = new FakeOpenAiTransport("""
			{
			  "output_text": "{\\"selectedHskCode\\":\\"8471601020\\",\\"confidence\\":0.96,\\"reason\\":\\"컴퓨터용 키보드에 해당합니다.\\"}"
			}
			""");
		OpenAiHskCandidateReranker reranker = new OpenAiHskCandidateReranker(
			new OpenAiProperties("test-key", "gpt-test", "https://api.openai.com/v1/responses"),
			transport,
			objectMapper
		);

		HskRerankResult result = reranker.rerank(
			new HskMatchRequest("유선 키보드", "USB 104키", null),
			new HskFeatures("유선 키보드", List.of("키보드"), "컴퓨터용 키보드", "FINISHED_PRODUCT", List.of("키보드"), List.of(), List.of()),
			List.of(
				new HskVectorCandidate(new HskItem("9503003910", "키보드 모양 완구", "Keyboard shaped toy"), 0.95),
				new HskVectorCandidate(new HskItem("8471601020", "키보드", "Keyboard"), 0.90)
			)
		);

		assertThat(result.selectedHskCode()).isEqualTo("8471601020");
		assertThat(result.confidence()).isEqualTo(0.96);
		assertThat(result.reason()).contains("컴퓨터용 키보드");
		assertThat(transport.lastApiKey).isEqualTo("test-key");
		assertThat(transport.lastBody).contains("hsk_rerank_result");
		assertThat(transport.lastBody).contains("9503003910");
		assertThat(transport.lastBody).contains("8471601020");
		assertThat(transport.lastBody).contains("Do not choose the least-bad candidate");
		assertThat(transport.lastBody).contains("selectedHskCode must be null");
		assertThat(transport.lastBody).contains("broader or narrower candidate");
		assertThat(transport.lastBody).contains("displayName");
		assertThat(transport.lastBody).contains("common commercial name");
		assertThat(transport.lastBody).contains("chicken feet");
		assertThat(transport.lastBody).contains("edible poultry offal");
		assertThat(output).contains("OpenAI HSK rerank started");
		assertThat(output).contains("selectedHskCode=8471601020");
	}

	@Test
	void rejectsSelectedHskCodeNotContainedInCandidates() {
		FakeOpenAiTransport transport = new FakeOpenAiTransport("""
			{
			  "output_text": "{\\"selectedHskCode\\":\\"0000000000\\",\\"confidence\\":0.99,\\"reason\\":\\"invalid\\"}"
			}
			""");
		OpenAiHskCandidateReranker reranker = new OpenAiHskCandidateReranker(
			new OpenAiProperties("test-key", "gpt-test", "https://api.openai.com/v1/responses"),
			transport,
			objectMapper
		);

		HskRerankResult result = reranker.rerank(
			new HskMatchRequest("상품", "", null),
			new HskFeatures("상품", List.of("상품")),
			List.of(new HskVectorCandidate(new HskItem("1111111111", "상품", "Product"), 0.80))
		);

		assertThat(result.selected()).isFalse();
	}

	@Test
	void keepsVectorOrderWhenApiKeyIsMissing() {
		OpenAiHskCandidateReranker reranker = new OpenAiHskCandidateReranker(
			new OpenAiProperties("", "gpt-test", "https://api.openai.com/v1/responses"),
			new FakeOpenAiTransport("{}"),
			objectMapper
		);

		HskRerankResult result = reranker.rerank(
			new HskMatchRequest("상품", "", null),
			new HskFeatures("상품", List.of("상품")),
			List.of(new HskVectorCandidate(new HskItem("1111111111", "상품", "Product"), 0.80))
		);

		assertThat(result).isNull();
	}

	private static class FakeOpenAiTransport implements OpenAiTransport {

		private final String responseBody;
		private String lastApiKey;
		private String lastBody;

		private FakeOpenAiTransport(String responseBody) {
			this.responseBody = responseBody;
		}

		@Override
		public String postJson(String endpoint, String apiKey, String body) {
			this.lastApiKey = apiKey;
			this.lastBody = body;
			return responseBody;
		}
	}
}
