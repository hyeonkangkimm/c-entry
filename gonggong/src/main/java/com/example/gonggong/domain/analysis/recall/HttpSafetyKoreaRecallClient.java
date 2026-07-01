package com.example.gonggong.domain.analysis.recall;

import com.example.gonggong.domain.analysis.exception.AnalysisErrorCode;
import com.example.gonggong.domain.analysis.exception.AnalysisException;
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
public class HttpSafetyKoreaRecallClient implements SafetyKoreaRecallClient {

	private static final Logger log = LoggerFactory.getLogger(HttpSafetyKoreaRecallClient.class);

	private static final String DEFAULT_RECALL_LIST_URL = "https://www.safetykorea.kr/openapi/api/recall/recallList.json";
	private static final String DEFAULT_RECALL_DETAIL_URL = "https://www.safetykorea.kr/openapi/api/recall/recallDetail.json";
	private static final String DEFAULT_FOREIGN_RECALL_LIST_URL = "https://www.safetykorea.kr/openapi/api/recall/fRecallList.json";

	private final String apiKey;
	private final String recallListUrl;
	private final String recallDetailUrl;
	private final String foreignRecallListUrl;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	@Autowired
	public HttpSafetyKoreaRecallClient(
		@Value("${public-data.safety-korea.api-key:}") String apiKey,
		@Value("${public-data.safety-korea.recall-list-url:" + DEFAULT_RECALL_LIST_URL + "}") String recallListUrl,
		@Value("${public-data.safety-korea.recall-detail-url:" + DEFAULT_RECALL_DETAIL_URL + "}") String recallDetailUrl,
		@Value("${public-data.safety-korea.foreign-recall-list-url:" + DEFAULT_FOREIGN_RECALL_LIST_URL + "}") String foreignRecallListUrl,
		ObjectMapper objectMapper
	) {
		this(apiKey, recallListUrl, recallDetailUrl, foreignRecallListUrl, objectMapper, HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build());
	}

	HttpSafetyKoreaRecallClient(
		String apiKey,
		String recallListUrl,
		String recallDetailUrl,
		String foreignRecallListUrl,
		ObjectMapper objectMapper,
		HttpClient httpClient
	) {
		this.apiKey = apiKey;
		this.recallListUrl = recallListUrl;
		this.recallDetailUrl = recallDetailUrl;
		this.foreignRecallListUrl = foreignRecallListUrl;
		this.objectMapper = objectMapper;
		this.httpClient = httpClient;
	}

	@Override
	public List<SafetyKoreaRecallItem> searchByProductName(String productName) {
		return search("recallProductName", productName);
	}

	@Override
	public List<SafetyKoreaRecallItem> searchByBrandName(String brandName) {
		return search("recallBrandName", brandName);
	}

	@Override
	public List<SafetyKoreaRecallItem> searchForeignByProductName(String productName) {
		return searchForeign("recallProductName", productName);
	}

	@Override
	public List<SafetyKoreaRecallItem> searchForeignByBrandName(String brandName) {
		return searchForeign("recallBrandName", brandName);
	}

	@Override
	public SafetyKoreaRecallItem findDetail(String recallUid) {
		if (recallUid == null || recallUid.isBlank()) {
			return null;
		}
		if (apiKey == null || apiKey.isBlank()) {
			throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_KEY_MISSING);
		}

		URI uri = UriComponentsBuilder.fromUriString(recallDetailUrl)
			.queryParam("recallUid", recallUid.trim())
			.build()
			.encode()
			.toUri();

		HttpRequest request = HttpRequest.newBuilder(uri)
			.timeout(Duration.ofSeconds(10))
			.header("AuthKey", apiKey)
			.GET()
			.build();

		try {
			log.info("SafetyKorea recall detail started recallUid={}", recallUid.trim());
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			log.info("SafetyKorea recall detail response status={} bodyLength={}", response.statusCode(), response.body().length());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_FAILED);
			}
			List<SafetyKoreaRecallItem> items = parseItems(response.body(), RecallSource.DOMESTIC);
			return items.isEmpty() ? null : items.get(0);
		} catch (IOException exception) {
			throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_FAILED, exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_FAILED, exception);
		}
	}

	private List<SafetyKoreaRecallItem> search(String conditionKey, String conditionValue) {
		return search(recallListUrl, RecallSource.DOMESTIC, conditionKey, conditionValue);
	}

	private List<SafetyKoreaRecallItem> searchForeign(String conditionKey, String conditionValue) {
		return search(foreignRecallListUrl, RecallSource.FOREIGN, conditionKey, conditionValue);
	}

	private List<SafetyKoreaRecallItem> search(String url, RecallSource source, String conditionKey, String conditionValue) {
		if (conditionValue == null || conditionValue.isBlank()) {
			return List.of();
		}
		if (apiKey == null || apiKey.isBlank()) {
			throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_KEY_MISSING);
		}

		URI uri = UriComponentsBuilder.fromUriString(url)
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
			log.info("SafetyKorea recall search started source={} conditionKey={} conditionValue={}", source, conditionKey, conditionValue.trim());
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			log.info("SafetyKorea recall search response source={} status={} bodyLength={}", source, response.statusCode(), response.body().length());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_FAILED);
			}
			List<SafetyKoreaRecallItem> items = parseItems(response.body(), source);
			log.info("SafetyKorea recall search parsed source={} conditionKey={} conditionValue={} itemCount={}",
				source,
				conditionKey,
				conditionValue.trim(),
				items.size()
			);
			return items;
		} catch (IOException exception) {
			throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_FAILED, exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_FAILED, exception);
		}
	}

	private List<SafetyKoreaRecallItem> parseItems(String responseBody, RecallSource source) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			if (!"2000".equals(text(root, "resultCode"))) {
				throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_API_FAILED);
			}

			JsonNode resultData = root.get("resultData");
			if (resultData == null || resultData.isNull()) {
				return List.of();
			}

			List<SafetyKoreaRecallItem> items = new ArrayList<>();
			if (resultData.isArray()) {
				for (JsonNode item : resultData) {
					items.add(toItem(item, source));
				}
				return items;
			}

			return List.of(toItem(resultData, source));
		} catch (JsonProcessingException exception) {
			throw new AnalysisException(AnalysisErrorCode.SAFETY_KOREA_RESPONSE_PARSE_FAILED, exception);
		}
	}

	private SafetyKoreaRecallItem toItem(JsonNode item, RecallSource source) {
		return new SafetyKoreaRecallItem(
			source == RecallSource.FOREIGN ? firstNonBlank(text(item, "fRecallUid"), text(item, "recallUid")) : text(item, "recallUid"),
			text(item, "recallProductName"),
			text(item, "recallBrandName"),
			text(item, "recallModelName"),
			firstNonBlank(text(item, "recallCmpnyName"), text(item, "makerName")),
			text(item, "publishDate"),
			text(item, "recallStaDate"),
			text(item, "recallEndDate"),
			text(item, "barcodeNum"),
			text(item, "certNum"),
			firstNonBlank(text(item, "harmDscr"), text(item, "violateDscr")),
			text(item, "accidentCaseDscr"),
			text(item, "publishActionDscr"),
			imageUrls(item, source),
			null,
			source,
			firstNonBlank(text(item, "recallUrl"), text(item, "recallurl"))
		);
	}

	private List<String> imageUrls(JsonNode item, RecallSource source) {
		if (source == RecallSource.FOREIGN) {
			String imageUrl = text(item, "imageUrl");
			if (imageUrl == null || imageUrl.isBlank()) {
				return List.of();
			}
			return List.of(imageUrl);
		}
		JsonNode recallFiles = item.get("recallFiles");
		if (recallFiles == null || !recallFiles.isArray()) {
			return List.of();
		}
		List<String> urls = new ArrayList<>();
		for (JsonNode file : recallFiles) {
			String imageUrl = text(file, "imageUrl");
			if (imageUrl != null && !imageUrl.isBlank()) {
				urls.add(imageUrl);
			}
		}
		return urls;
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

	private String firstNonBlank(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) {
			return primary;
		}
		return fallback;
	}
}
