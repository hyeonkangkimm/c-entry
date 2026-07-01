package com.example.gonggong.domain.analysis.controller;

import com.example.gonggong.domain.analysis.dto.ProductAnalyzeRequest;
import com.example.gonggong.domain.analysis.dto.ProductAnalyzeResponse;
import com.example.gonggong.domain.analysis.service.ProductAnalyzeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductAnalyzeController {

	private final ProductAnalyzeService productAnalyzeService;

	public ProductAnalyzeController(ProductAnalyzeService productAnalyzeService) {
		this.productAnalyzeService = productAnalyzeService;
	}

	@PostMapping("/analyze")
	public ProductAnalyzeResponse analyze(@RequestBody ProductAnalyzeRequest request) {
		return productAnalyzeService.analyze(request);
	}
}
