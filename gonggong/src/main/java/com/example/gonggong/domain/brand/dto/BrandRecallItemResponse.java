package com.example.gonggong.domain.brand.dto;

public record BrandRecallItemResponse(
	String productName,
	String defectReason,
	String publishedAt
) {
}
