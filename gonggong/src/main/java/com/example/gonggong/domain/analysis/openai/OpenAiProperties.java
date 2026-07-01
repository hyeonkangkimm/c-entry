package com.example.gonggong.domain.analysis.openai;

public record OpenAiProperties(
	String apiKey,
	String model,
	String responsesApiUrl
) {
}
