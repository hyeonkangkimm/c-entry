package com.example.gonggong.domain.analysis.service;

import com.example.gonggong.domain.analysis.dto.ProductAnalyzeRequest;
import com.example.gonggong.domain.analysis.openai.ProductNormalizeResult;

public interface ProductNormalizer {

	ProductNormalizeResult normalize(ProductAnalyzeRequest request);
}
