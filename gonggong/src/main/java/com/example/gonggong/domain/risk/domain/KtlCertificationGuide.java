package com.example.gonggong.domain.risk.domain;

import com.example.gonggong.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "ktl_certification_guide")
public class KtlCertificationGuide extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "certification_type_key", nullable = false, unique = true, length = 80)
	private String certificationTypeKey;

	@Column(name = "certification_name", nullable = false, length = 200)
	private String certificationName;

	@Column(name = "certification_mark_url", length = 1000)
	private String certificationMarkUrl;

	@Column(name = "legal_basis", nullable = false, length = 500)
	private String legalBasis;

	@Column(name = "test_items_json", nullable = false, columnDefinition = "text")
	private String testItemsJson;

	@Column(name = "required_documents_json", nullable = false, columnDefinition = "text")
	private String requiredDocumentsJson;

	@Column(name = "estimated_duration", nullable = false, length = 100)
	private String estimatedDuration;

	@Column(name = "estimated_fee", nullable = false, length = 200)
	private String estimatedFee;

	@Column(name = "application_url", nullable = false, length = 1000)
	private String applicationUrl;

	@Column(name = "source_url", nullable = false, length = 1000)
	private String sourceUrl;

	@Column(name = "verified_at", nullable = false)
	private LocalDate verifiedAt;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	protected KtlCertificationGuide() {
	}

	public KtlCertificationGuide(
		String certificationTypeKey,
		String certificationName,
		String certificationMarkUrl,
		String legalBasis,
		String testItemsJson,
		String requiredDocumentsJson,
		String estimatedDuration,
		String estimatedFee,
		String applicationUrl,
		String sourceUrl,
		LocalDate verifiedAt
	) {
		this.certificationTypeKey = certificationTypeKey;
		this.certificationName = certificationName;
		this.certificationMarkUrl = certificationMarkUrl;
		this.legalBasis = legalBasis;
		this.testItemsJson = testItemsJson;
		this.requiredDocumentsJson = requiredDocumentsJson;
		this.estimatedDuration = estimatedDuration;
		this.estimatedFee = estimatedFee;
		this.applicationUrl = applicationUrl;
		this.sourceUrl = sourceUrl;
		this.verifiedAt = verifiedAt;
	}

	public String getCertificationTypeKey() { return certificationTypeKey; }
	public String getCertificationName() { return certificationName; }
	public String getCertificationMarkUrl() { return certificationMarkUrl; }
	public String getLegalBasis() { return legalBasis; }
	public String getTestItemsJson() { return testItemsJson; }
	public String getRequiredDocumentsJson() { return requiredDocumentsJson; }
	public String getEstimatedDuration() { return estimatedDuration; }
	public String getEstimatedFee() { return estimatedFee; }
	public String getApplicationUrl() { return applicationUrl; }
	public String getSourceUrl() { return sourceUrl; }
	public LocalDate getVerifiedAt() { return verifiedAt; }
	public boolean isActive() { return active; }
}
