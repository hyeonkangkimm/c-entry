package com.example.gonggong.domain.analysis.openai;

import com.example.gonggong.domain.analysis.recall.SafetyKoreaRecallItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class OpenAiRecallRelevanceDeciderTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void sendsRecallCandidatesAndKeepsOnlyRelevantIds() {
		FakeOpenAiTransport transport = new FakeOpenAiTransport("""
			{
			  "output_text": "{\\"relevantCandidateIds\\":[\\"c1\\"]}"
			}
			""");
		OpenAiRecallRelevanceDecider decider = new OpenAiRecallRelevanceDecider(
			new OpenAiProperties("test-key", "gpt-test", "https://api.openai.com/v1/responses"),
			transport,
			objectMapper
		);
		ProductNormalizeResult normalized = new ProductNormalizeResult(
			"유선 키보드",
			List.of("키보드"),
			null,
			"전기용품>컴퓨터 입력장치",
			null,
			null,
			null,
			null,
			List.of(),
			"일반",
			List.of(),
			List.of("키보드"),
			"유선 키보드",
			"FINISHED_PRODUCT",
			List.of("키보드"),
			List.of(),
			List.of(),
			0.92
		);
		List<SafetyKoreaRecallItem> candidates = List.of(
			new SafetyKoreaRecallItem("1", "LED 내장된 팽이, 키보드 키캡 모양 키링", null, null, "Maker", "20260201", "reason", null, "action", List.of()),
			new SafetyKoreaRecallItem("2", "유선 키보드", null, null, "Maker", "20260202", "reason", null, "action", List.of())
		);

		List<SafetyKoreaRecallItem> relevant = decider.selectRelevant(normalized, candidates);

		assertThat(relevant).containsExactly(candidates.get(1));
		assertThat(transport.lastApiKey).isEqualTo("test-key");
		assertThat(transport.lastBody).contains("\"model\":\"gpt-test\"");
		assertThat(transport.lastBody).contains("LED 내장된 팽이, 키보드 키캡 모양 키링");
		assertThat(transport.lastBody).contains("유선 키보드");
		assertThat(transport.lastBody).contains("recall_relevance_result");
	}

	@Test
	void logsRecallRelevanceInputAndOutput(CapturedOutput output) {
		FakeOpenAiTransport transport = new FakeOpenAiTransport("""
			{
			  "output_text": "{\\"relevantCandidateIds\\":[\\"c0\\"]}"
			}
			""");
		OpenAiRecallRelevanceDecider decider = new OpenAiRecallRelevanceDecider(
			new OpenAiProperties("test-key", "gpt-test", "https://api.openai.com/v1/responses"),
			transport,
			objectMapper
		);
		ProductNormalizeResult normalized = new ProductNormalizeResult(
			"유선 키보드",
			List.of("키보드"),
			null,
			"전기용품>컴퓨터 입력장치",
			null,
			List.of(),
			"일반",
			List.of(),
			List.of("키보드"),
			0.92
		);
		List<SafetyKoreaRecallItem> candidates = List.of(
			new SafetyKoreaRecallItem("1", "유선 키보드", "Brand", "MODEL", "Maker", "20260202", "reason", null, "action", List.of())
		);

		decider.selectRelevant(normalized, candidates);

		assertThat(output).contains("OpenAI recall relevance started");
		assertThat(output).contains("standardProductName=유선 키보드");
		assertThat(output).contains("candidateCount=1");
		assertThat(output).contains("recallProductName=유선 키보드");
		assertThat(output).contains("OpenAI recall relevance completed");
		assertThat(output).contains("relevantCandidateIds=[c0]");
	}

	@Test
	void keepsOriginalCandidatesWhenApiKeyIsMissing() {
		OpenAiRecallRelevanceDecider decider = new OpenAiRecallRelevanceDecider(
			new OpenAiProperties("", "gpt-test", "https://api.openai.com/v1/responses"),
			new FakeOpenAiTransport("{}"),
			objectMapper
		);
		List<SafetyKoreaRecallItem> candidates = List.of(
			new SafetyKoreaRecallItem("1", "유선 키보드", null, null, "Maker", "20260202", "reason", null, "action", List.of())
		);

		List<SafetyKoreaRecallItem> relevant = decider.selectRelevant(
			new ProductNormalizeResult("유선 키보드", List.of("키보드"), null, "전기용품", null, List.of(), "일반", List.of(), List.of(), 0.9),
			candidates
		);

		assertThat(relevant).isSameAs(candidates);
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
