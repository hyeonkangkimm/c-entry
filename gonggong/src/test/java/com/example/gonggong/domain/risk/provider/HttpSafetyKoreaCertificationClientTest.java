package com.example.gonggong.domain.risk.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpSafetyKoreaCertificationClientTest {

	@Test
	void parsesCertificationItemsFromSafetyKoreaResponse() {
		HttpSafetyKoreaCertificationClient client = new HttpSafetyKoreaCertificationClient(
			"key",
			"https://example.test/certList.json",
			new ObjectMapper(),
			null
		);
		String response = """
			{
			  "resultCode": "2000",
			  "resultMsg": "Success",
			  "resultData": [
			    {
			      "certNum": "SU12345-67890",
			      "productName": "노트북 컴퓨터",
			      "modelName": "N4000",
			      "brandName": "Azeyou",
			      "certState": "적합",
			      "certDiv": "전안법 대상>안전확인 대상"
			    }
			  ]
			}
			""";

		List<SafetyKoreaCertificationItem> items = client.parseItems(response);

		assertThat(items).hasSize(1);
		assertThat(items.get(0).certificationNumber()).isEqualTo("SU12345-67890");
		assertThat(items.get(0).productName()).isEqualTo("노트북 컴퓨터");
		assertThat(items.get(0).modelName()).isEqualTo("N4000");
		assertThat(items.get(0).brandName()).isEqualTo("Azeyou");
		assertThat(items.get(0).statusName()).isEqualTo("적합");
		assertThat(items.get(0).certificationType()).isEqualTo("전안법 대상>안전확인 대상");
		assertThat(items.get(0).relatedLaw()).isEqualTo("전기용품 및 생활용품 안전관리법");
	}

	@Test
	void returnsEmptyListWhenSafetyKoreaResultCodeIsNotSuccess() {
		HttpSafetyKoreaCertificationClient client = new HttpSafetyKoreaCertificationClient(
			"key",
			"https://example.test/certList.json",
			new ObjectMapper(),
			null
		);

		assertThat(client.parseItems("{\"resultCode\":\"5000\",\"resultData\":[]}")).isEmpty();
	}
}
