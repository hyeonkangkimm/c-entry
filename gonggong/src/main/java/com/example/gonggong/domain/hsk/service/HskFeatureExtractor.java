package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.dto.HskMatchRequest;

@FunctionalInterface
public interface HskFeatureExtractor {

	HskFeatures extract(HskMatchRequest request);
}
