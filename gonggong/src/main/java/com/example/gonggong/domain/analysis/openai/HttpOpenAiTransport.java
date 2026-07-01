package com.example.gonggong.domain.analysis.openai;

import com.example.gonggong.domain.analysis.exception.AnalysisErrorCode;
import com.example.gonggong.domain.analysis.exception.AnalysisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HttpOpenAiTransport implements OpenAiTransport {

	private static final Logger log = LoggerFactory.getLogger(HttpOpenAiTransport.class);

	private final HttpClient httpClient;

	public HttpOpenAiTransport() {
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	}

	@Override
	public String postJson(String endpoint, String apiKey, String body) {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(endpoint))
			.timeout(Duration.ofSeconds(30))
			.header("Authorization", "Bearer " + apiKey)
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();

		try {
			log.info("OpenAI request started endpoint={}", endpoint);
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			log.info("OpenAI response received status={} bodyLength={}", response.statusCode(), response.body().length());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new AnalysisException(AnalysisErrorCode.OPENAI_API_FAILED);
			}
			return response.body();
		} catch (IOException exception) {
			throw new AnalysisException(AnalysisErrorCode.OPENAI_API_FAILED, exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AnalysisException(AnalysisErrorCode.OPENAI_API_FAILED, exception);
		}
	}
}
