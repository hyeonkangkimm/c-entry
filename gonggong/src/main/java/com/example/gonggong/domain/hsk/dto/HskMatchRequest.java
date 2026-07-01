package com.example.gonggong.domain.hsk.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record HskMatchRequest(
	@NotBlank(message = "상품명은 필수입니다.")
	String productName,
	String description,
	String imageUrl,
	String standardProductName,
	List<String> hskCandidateKeywords,
	String primaryProductName,
	String productForm,
	List<String> primarySearchKeywords,
	List<String> componentKeywords,
	List<String> featureKeywords
) {
	public HskMatchRequest(String productName, String description, String imageUrl) {
		this(
			productName,
			description,
			imageUrl,
			null,
			List.of(),
			null,
			null,
			List.of(),
			List.of(),
			List.of()
		);
	}
}
