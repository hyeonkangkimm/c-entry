package com.example.gonggong.domain.brand.dto;

import java.util.List;

public record BrandRecallResponse(
	String brandName,
	boolean exists,
	String message,
	List<BrandRecallItemResponse> items
) {
}
