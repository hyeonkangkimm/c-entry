package com.example.gonggong.domain.hsk.service;

import com.example.gonggong.domain.hsk.domain.HskItem;

public record HskVectorCandidate(
	HskItem item,
	double similarity
) {
}
