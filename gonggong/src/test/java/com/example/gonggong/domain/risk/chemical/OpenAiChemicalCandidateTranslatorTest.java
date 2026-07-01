package com.example.gonggong.domain.risk.chemical;

import com.example.gonggong.domain.analysis.openai.OpenAiProperties;
import com.example.gonggong.domain.analysis.openai.OpenAiTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiChemicalCandidateTranslatorTest {

	@Test
	void translatesIngredientNamesIntoEnglishSearchCandidates() {
		FakeOpenAiTransport transport = new FakeOpenAiTransport("""
			{
			  "output_text": "{\\"chemicalCandidates\\":[{\\"name\\":\\"납\\",\\"casNumber\\":null,\\"englishName\\":\\"lead\\"}]}"
			}
			""");
		OpenAiChemicalCandidateTranslator translator = new OpenAiChemicalCandidateTranslator(
			new OpenAiProperties("test-key", "gpt-test", "https://api.openai.com/v1/responses"),
			new ChemicalCandidatePromptBuilder(),
			transport,
			new ObjectMapper()
		);

		List<ChemicalIngredientCandidate> candidates = translator.translate(List.of("납"));

		assertThat(candidates).singleElement().satisfies(candidate -> {
			assertThat(candidate.name()).isEqualTo("납");
			assertThat(candidate.englishName()).isEqualTo("lead");
		});
		assertThat(transport.lastBody).contains("한국환경공단 화학물질 정보 조회 서비스");
		assertThat(transport.lastBody).contains("납");
		assertThat(transport.lastBody).contains("lead");
	}

	private static class FakeOpenAiTransport implements OpenAiTransport {

		private final String responseBody;
		private String lastBody;

		private FakeOpenAiTransport(String responseBody) {
			this.responseBody = responseBody;
		}

		@Override
		public String postJson(String endpoint, String apiKey, String body) {
			this.lastBody = body;
			return responseBody;
		}
	}
}
