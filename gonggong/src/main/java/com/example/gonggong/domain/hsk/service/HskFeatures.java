package com.example.gonggong.domain.hsk.service;

import java.util.List;

public record HskFeatures(
	String standardProductName,
	List<String> searchKeywords,
	String primaryProductName,
	String productForm,
	List<String> primarySearchKeywords,
	List<String> componentKeywords,
	List<String> featureKeywords
) {
	public HskFeatures(String standardProductName, List<String> searchKeywords) {
		this(
			standardProductName,
			searchKeywords,
			standardProductName,
			"UNKNOWN",
			searchKeywords,
			List.of(),
			List.of()
		);
	}
}
