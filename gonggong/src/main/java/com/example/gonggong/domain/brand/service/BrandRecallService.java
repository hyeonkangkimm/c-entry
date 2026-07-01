package com.example.gonggong.domain.brand.service;

import com.example.gonggong.domain.brand.dto.BrandRecallItemResponse;
import com.example.gonggong.domain.brand.dto.BrandRecallResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class BrandRecallService {

	public BrandRecallResponse findByBrandName(String brandName) {
		String normalizedBrandName = normalizeBrandName(brandName);
		List<BrandRecallItemResponse> items = dummyRecallItems(normalizedBrandName);

		if (items.isEmpty()) {
			return new BrandRecallResponse(
				normalizedBrandName,
				false,
				"리콜 이력이 존재하지 않습니다.",
				List.of()
			);
		}

		return new BrandRecallResponse(
			normalizedBrandName,
			true,
			"이 브랜드의 다른 제품 리콜 이력이 있습니다.",
			items
		);
	}

	private String normalizeBrandName(String brandName) {
		if (brandName == null || brandName.isBlank()) {
			return "Unknown Brand";
		}
		return brandName.trim().replaceAll("\\s+", " ");
	}

	private List<BrandRecallItemResponse> dummyRecallItems(String brandName) {
		String lowered = brandName.toLowerCase(Locale.ROOT);
		if (!lowered.contains("example") && !lowered.contains("risk") && !brandName.contains("위험")) {
			return List.of();
		}

		return List.of(
			new BrandRecallItemResponse(
				"Example Brand 전기 온열기",
				"온도 상승 시험 기준 초과",
				"2025-11-18"
			),
			new BrandRecallItemResponse(
				"Example Brand 무선 마우스",
				"전자파 적합성 기준 부적합",
				"2025-09-02"
			),
			new BrandRecallItemResponse(
				"Example Brand 어린이 완구",
				"프탈레이트계 가소제 기준치 초과",
				"2025-04-07"
			),
			new BrandRecallItemResponse(
				"Example Brand LED 조명",
				"절연 내력 부적합",
				"2024-12-21"
			)
		);
	}
}
