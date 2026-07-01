package com.example.gonggong.domain.risk.repository;

import com.example.gonggong.domain.risk.domain.KtlCertificationGuide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KtlCertificationGuideRepository extends JpaRepository<KtlCertificationGuide, Long> {

	Optional<KtlCertificationGuide> findByCertificationTypeKeyAndActiveTrue(String certificationTypeKey);

	boolean existsByCertificationTypeKey(String certificationTypeKey);
}
