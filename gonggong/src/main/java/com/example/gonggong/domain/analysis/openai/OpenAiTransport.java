package com.example.gonggong.domain.analysis.openai;

public interface OpenAiTransport {

	String postJson(String endpoint, String apiKey, String body);
}
