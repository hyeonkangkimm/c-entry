package com.example.gonggong.domain.risk.chemical;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HttpChemicalInformationClientTest {

	@Test
	void encodesIngredientAndConfiguredParameters() {
		ChemicalApiProperties properties = properties();
		AtomicReference<URI> requestedUri = new AtomicReference<>();
		ChemicalHttpTransport transport = (uri, timeout) -> {
			requestedUri.set(uri);
			return new ChemicalHttpResponse(200, successBody());
		};
		HttpChemicalInformationClient client = new HttpChemicalInformationClient(
			properties,
			new ChemicalApiJsonParser(new ObjectMapper()),
			transport
		);

		ChemicalLookupResult result = client.lookup("폼 알데하이드");

		assertThat(result.status()).isEqualTo(ChemicalLookupStatus.MATCHED);
		assertThat(requestedUri.get().toString())
			.contains("serviceKey=test-key")
			.contains("searchGubun=1")
			.contains("searchNm=%ED%8F%BC%20%EC%95%8C%EB%8D%B0%ED%95%98%EC%9D%B4%EB%93%9C")
			.contains("returnType=JSON");
	}

	@Test
	void mapsNonSuccessStatusToUnavailable() {
		HttpChemicalInformationClient client = new HttpChemicalInformationClient(
			properties(),
			new ChemicalApiJsonParser(new ObjectMapper()),
			(uri, timeout) -> new ChemicalHttpResponse(503, "unavailable")
		);

		assertThat(client.lookup("benzene").status()).isEqualTo(ChemicalLookupStatus.UNAVAILABLE);
	}

	private ChemicalApiProperties properties() {
		ChemicalApiProperties properties = new ChemicalApiProperties();
		properties.setServiceKey("test-key");
		return properties;
	}

	private String successBody() {
		return """
			{"header":{"resultCode":"00"},"body":{"items":[{
			  "sbstnNmKor":"폼알데하이드","casNo":"50-00-0","typeList":[]
			}]}}
			""";
	}
}
