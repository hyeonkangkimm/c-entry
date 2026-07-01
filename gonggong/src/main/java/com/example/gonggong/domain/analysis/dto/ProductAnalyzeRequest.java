package com.example.gonggong.domain.analysis.dto;

public record ProductAnalyzeRequest(
	String productName,
	String description,
	String imageUrl,
	String pageUrl,
	String site,
	String sellerName,
	String kcCertificationNumber,
	String kcCertificationType
) {
	public ProductAnalyzeRequest(
		String productName,
		String description,
		String imageUrl,
		String pageUrl,
		String site
	) {
		this(productName, description, imageUrl, pageUrl, site, null, null, null);
	}

	public ProductAnalyzeRequest(
		String productName,
		String description,
		String imageUrl,
		String pageUrl,
		String site,
		String sellerName
	) {
		this(productName, description, imageUrl, pageUrl, site, sellerName, null, null);
	}

	public ProductAnalyzeRequest(
		String productName,
		String description,
		String imageUrl,
		String pageUrl,
		String site,
		String sellerName,
		String kcCertificationNumber
	) {
		this(productName, description, imageUrl, pageUrl, site, sellerName, kcCertificationNumber, null);
	}
}
