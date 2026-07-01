package com.example.gonggong.domain.risk.provider;

import com.example.gonggong.domain.risk.dto.response.KtlCertificationGuideResponse;

import java.util.Optional;

public interface KtlCertificationGuideProvider {

	Optional<KtlCertificationGuideResponse> findByCertificationType(String certificationType);
}
