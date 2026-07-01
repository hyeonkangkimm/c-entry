package com.example.gonggong.domain.brand.controller;

import com.example.gonggong.domain.brand.dto.BrandRecallResponse;
import com.example.gonggong.domain.brand.service.BrandRecallService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brands")
public class BrandRecallController {

	private final BrandRecallService brandRecallService;

	public BrandRecallController(BrandRecallService brandRecallService) {
		this.brandRecallService = brandRecallService;
	}

	@GetMapping("/{brandName}/recalls")
	public BrandRecallResponse getBrandRecalls(@PathVariable String brandName) {
		return brandRecallService.findByBrandName(brandName);
	}
}
