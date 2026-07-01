package com.example.gonggong.domain.risk.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class HttpSafetyKoreaCertificationClient implements SafetyKoreaCertificationClient {

	private static final Logger log = LoggerFactory.getLogger(HttpSafetyKoreaCertificationClient.class);

	private final String apiKey;
	private final String certificationListUrl;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	@Autowired
	public HttpSafetyKoreaCertificationClient(
		@Value("${public-data.safety-korea.api-key:}") String apiKey,
		@Value("${public-data.safety-korea.certification-list-url:}") String certificationListUrl,
		ObjectMapper objectMapper
	) {
		this(apiKey, certificationListUrl, objectMapper, HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build());
	}

	HttpSafetyKoreaCertificationClient(
		String apiKey,
		String certificationListUrl,
		ObjectMapper objectMapper,
		HttpClient httpClient
	) {
		this.apiKey = apiKey;
		this.certificationListUrl = certificationListUrl;
		this.objectMapper = objectMapper;
		this.httpClient = httpClient;
	}

	@Override
	public List<SafetyKoreaCertificationItem> searchByCertificationNumber(String certificationNumber) {
		return search("certNum", certificationNumber);
	}

	@Override
	public List<SafetyKoreaCertificationItem> searchByProductName(String productName) {
		return search("productName", productName);
	}

	@Override
	public List<SafetyKoreaCertificationItem> searchByModelName(String modelName) {
		return search("modelName", modelName);
	}

	@Override
	public List<SafetyKoreaCertificationItem> searchByBrandName(String brandName) {
		return search("brandName", brandName);
	}

	private List<SafetyKoreaCertificationItem> search(String conditionKey, String conditionValue) {
		if (conditionValue == null || conditionValue.isBlank()) {
			return List.of();
		}
		if (apiKey == null || apiKey.isBlank() || certificationListUrl == null || certificationListUrl.isBlank()) {
			log.info("SafetyKorea certification search skipped conditionKey={} reason=missing-api-key-or-url", conditionKey);
			return List.of();
		}

		URI uri = UriComponentsBuilder.fromUriString(certificationListUrl)
			.queryParam("conditionKey", conditionKey)
			.queryParam("conditionValue", conditionValue.trim())
			.build()
			.encode()
			.toUri();

		HttpRequest request = HttpRequest.newBuilder(uri)
			.timeout(Duration.ofSeconds(10))
			.header("AuthKey", apiKey)
			.GET()
			.build();

		try {
			log.info("SafetyKorea certification search started conditionKey={} conditionValue={}", conditionKey, conditionValue.trim());
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			log.info("SafetyKorea certification search response status={} bodyLength={}", response.statusCode(), response.body().length());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return List.of();
			}
			List<SafetyKoreaCertificationItem> items = parseItems(response.body());
			log.info("SafetyKorea certification search parsed conditionKey={} conditionValue={} itemCount={}",
				conditionKey,
				conditionValue.trim(),
				items.size()
			);
			return items;
		}
		catch (IOException exception) {
			log.warn("SafetyKorea certification search failed conditionKey={} error={}", conditionKey, exception.getMessage());
			return List.of();
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			log.warn("SafetyKorea certification search interrupted conditionKey={}", conditionKey);
			return List.of();
		}
	}

	List<SafetyKoreaCertificationItem> parseItems(String responseBody) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			String resultCode = text(root, "resultCode");
			if (resultCode != null && !"2000".equals(resultCode)) {
				return List.of();
			}
			JsonNode resultData = root.get("resultData");
			if (resultData == null || resultData.isNull()) {
				return List.of();
			}
			List<SafetyKoreaCertificationItem> items = new ArrayList<>();
			if (resultData.isArray()) {
				for (JsonNode item : resultData) {
					items.add(toItem(item));
				}
			}
			else {
				items.add(toItem(resultData));
			}
			return items;
		}
		catch (JsonProcessingException exception) {
			log.warn("SafetyKorea certification response parse failed error={}", exception.getMessage());
			return List.of();
		}
	}

	private SafetyKoreaCertificationItem toItem(JsonNode item) {
		return new SafetyKoreaCertificationItem(
			firstNonBlank(text(item, "certNum"), text(item, "certificationNumber"), text(item, "safetyCertNum")),
			firstNonBlank(text(item, "productName"), text(item, "certProductName"), text(item, "itemName"), text(item, "productItemName")),
			firstNonBlank(text(item, "modelName"), text(item, "certModelName"), text(item, "basicModelName")),
			firstNonBlank(text(item, "brandName"), text(item, "makerName"), text(item, "importerName"), text(item, "companyName")),
			firstNonBlank(text(item, "certState"), text(item, "certStateName"), text(item, "certStatusName"), text(item, "statusName"), text(item, "certificationStatus")),
			firstNonBlank(text(item, "certDiv"), text(item, "certTypeName"), text(item, "certDivName"), text(item, "certificationType")),
			firstNonBlank(text(item, "lawName"), inferRelatedLaw(text(item, "certDiv")), text(item, "relatedLaw"), text(item, "safetyLawName"))
		);
	}

	private String inferRelatedLaw(String certDiv) {
		if (certDiv == null || certDiv.isBlank()) {
			return null;
		}
		if (certDiv.contains("어린이")) {
			return "어린이제품 안전 특별법";
		}
		if (certDiv.contains("전안법") || certDiv.contains("전기용품") || certDiv.contains("생활용품")) {
			return "전기용품 및 생활용품 안전관리법";
		}
		return null;
	}

	private String text(JsonNode node, String fieldName) {
		if (node == null) {
			return null;
		}
		JsonNode value = node.get(fieldName);
		if (value == null || value.isNull()) {
			return null;
		}
		return value.asText();
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}
		return null;
	}
}
