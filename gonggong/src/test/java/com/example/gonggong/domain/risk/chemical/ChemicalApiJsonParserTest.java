package com.example.gonggong.domain.risk.chemical;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChemicalApiJsonParserTest {

	private final ChemicalApiJsonParser parser = new ChemicalApiJsonParser(new ObjectMapper());

	@Test
	void parsesDocumentedChemicalSubstanceFields() {
		String body = """
			{
			    "header": {"resultCode": "200", "resultMsg": "NORMAL SERVICE."},
			    "body": {
			      "items": [{
			        "sbstnNmKor": "폼알데하이드",
			        "sbstnNmEng": "Formaldehyde",
			        "casNo": "50-00-0",
			        "typeList": [{
			          "sbstnClsfTypeNm": "유독물질",
			          "unqNo": "97-1-5",
			          "contInfo": "0.1% 이상",
			          "excpInfo": "법정 예외 제외",
			          "ancmntYmd": "2025-08-07",
			          "ancmntInfo": "유독물질 지정"
			        }]
			      }]
			    }
			}
			""";

		ChemicalLookupResult result = parser.parse(body, "formaldehyde");

		assertThat(result.status()).isEqualTo(ChemicalLookupStatus.MATCHED);
		assertThat(result.substance()).isNotNull();
		assertThat(result.substance().koreanName()).isEqualTo("폼알데하이드");
		assertThat(result.substance().englishName()).isEqualTo("Formaldehyde");
		assertThat(result.substance().casNumber()).isEqualTo("50-00-0");
		assertThat(result.substance().classifications()).singleElement().satisfies(classification -> {
			assertThat(classification.type()).isEqualTo("유독물질");
			assertThat(classification.identifier()).isEqualTo("97-1-5");
			assertThat(classification.concentration()).isEqualTo("0.1% 이상");
			assertThat(classification.exceptionInformation()).isEqualTo("법정 예외 제외");
			assertThat(classification.noticeDate()).isEqualTo("2025-08-07");
			assertThat(classification.noticeInformation()).isEqualTo("유독물질 지정");
		});
	}

	@Test
	void returnsNotFoundForEmptyItems() {
		ChemicalLookupResult result = parser.parse(
			"{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":[]}}",
			"unknown"
		);

		assertThat(result.status()).isEqualTo(ChemicalLookupStatus.NOT_FOUND);
	}

	@Test
	void selectsExactNameInsteadOfFirstPartialMatch() {
		String body = """
			{"header":{"resultCode":"200"},"body":{"items":[
			  {"sbstnNmEng":"Formaldehyde polymer","casNo":"111-11-1","typeList":[]},
			  {"sbstnNmKor":"포르말린","sbstnNmEng":"Formaldehyde","casNo":"50-00-0","typeList":[]}
			]}}
			""";

		ChemicalLookupResult result = parser.parse(body, "formaldehyde");

		assertThat(result.status()).isEqualTo(ChemicalLookupStatus.MATCHED);
		assertThat(result.substance().casNumber()).isEqualTo("50-00-0");
	}

	@Test
	void selectsPartialMatchWhenExactNameIsAbsent() {
		String body = """
			{"header":{"resultCode":"200"},"body":{"items":[
			  {"sbstnNmEng":"Diantimony trilead octaoxide","casNo":"1314-60-9","typeList":[]}
			]}}
			""";

		ChemicalLookupResult result = parser.parse(body, "lead");

		assertThat(result.status()).isEqualTo(ChemicalLookupStatus.MATCHED);
		assertThat(result.substance().englishName()).isEqualTo("Diantimony trilead octaoxide");
	}

	@Test
	void returnsUnavailableForUnknownSchema() {
		ChemicalLookupResult result = parser.parse("{\"unexpected\":true}", "broken");

		assertThat(result.status()).isEqualTo(ChemicalLookupStatus.UNAVAILABLE);
		assertThat(result.reason()).isEqualTo("unrecognized-response-schema");
	}
}
