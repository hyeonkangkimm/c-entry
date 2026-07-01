package com.example.gonggong.domain.risk.chemical;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class HttpChemicalInformationClient implements ChemicalInformationClient {

	private static final Logger log = LoggerFactory.getLogger(HttpChemicalInformationClient.class);

	private final ChemicalApiProperties properties;
	private final ChemicalApiJsonParser parser;
	private final ChemicalHttpTransport transport;

	@Autowired
	public HttpChemicalInformationClient(ChemicalApiProperties properties, ChemicalApiJsonParser parser) {
		this(properties, parser, jdkTransport());
	}

	HttpChemicalInformationClient(
		ChemicalApiProperties properties,
		ChemicalApiJsonParser parser,
		ChemicalHttpTransport transport
	) {
		this.properties = properties;
		this.parser = parser;
		this.transport = transport;
	}

	@Override
	public ChemicalLookupResult lookup(String ingredient) {
		if (ingredient == null || ingredient.isBlank()) {
			return ChemicalLookupResult.notFound(ingredient);
		}
		if (!properties.enabled()) {
			return ChemicalLookupResult.unavailable(ingredient, "api-not-configured");
		}
		try {
			String trimmed = ingredient.trim();
			String searchType = trimmed.matches("\\d{2,7}-\\d{2}-\\d") ? "2" : "1";
			URI uri = UriComponentsBuilder.fromUriString(properties.getApiUrl())
				.queryParam(properties.getServiceKeyParamName(), properties.getServiceKey())
				.queryParam(properties.getSearchTypeParamName(), searchType)
				.queryParam(properties.getSearchNameParamName(), trimmed)
				.queryParam(properties.getResponseTypeParamName(), properties.getResponseType())
				.queryParam("pageNo", 1)
				.queryParam("numOfRows", properties.getPageSize())
				.build()
				.encode()
				.toUri();
			log.info(
				"Chemical API request ingredient={} searchType={} searchName={} apiUrl={}",
				trimmed,
				searchType,
				trimmed,
				properties.getApiUrl()
			);
			ChemicalHttpResponse response = transport.get(uri, properties.getTimeout());
			log.info("Chemical API response ingredient={} status={} bodyLength={}",
				trimmed, response.statusCode(), response.body() == null ? 0 : response.body().length());
			log.info(
				"Chemical API response preview ingredient={} preview={}",
				trimmed,
				preview(response.body())
			);
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return ChemicalLookupResult.unavailable(trimmed, "http-status-" + response.statusCode());
			}
			return parser.parse(response.body(), trimmed);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return ChemicalLookupResult.unavailable(ingredient, "interrupted");
		}
		catch (Exception exception) {
			log.warn("Chemical API failed ingredient={} error={}", ingredient.trim(), exception.getClass().getSimpleName());
			return ChemicalLookupResult.unavailable(ingredient.trim(), "request-failed");
		}
	}

	private static ChemicalHttpTransport jdkTransport() {
		HttpClient client = HttpClient.newBuilder().build();
		return (uri, timeout) -> {
			HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return new ChemicalHttpResponse(response.statusCode(), response.body());
		};
	}

	private String preview(String body) {
		if (body == null || body.isBlank()) {
			return "(empty)";
		}
		String normalized = body.replaceAll("\\s+", " ").trim();
		return normalized.length() <= 240 ? normalized : normalized.substring(0, 240);
	}
}
