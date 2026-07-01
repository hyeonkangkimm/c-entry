package com.example.gonggong.domain.risk.domain;

import com.example.gonggong.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
	name = "hsk_kc_requirement",
	indexes = {
		@Index(name = "idx_hsk_kc_requirement_code_prefix", columnList = "hsk_code_prefix")
	}
)
public class HskKcRequirement extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "hsk_code_prefix", nullable = false, length = 10)
	private String hskCodePrefix;

	@Column(name = "certification_required", nullable = false)
	private boolean certificationRequired;

	@Column(name = "certification_type", length = 200)
	private String certificationType;

	@Column(name = "related_law", length = 300)
	private String relatedLaw;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	protected HskKcRequirement() {
	}

	public HskKcRequirement(
		String hskCodePrefix,
		boolean certificationRequired,
		String certificationType,
		String relatedLaw
	) {
		this.hskCodePrefix = hskCodePrefix;
		this.certificationRequired = certificationRequired;
		this.certificationType = certificationType;
		this.relatedLaw = relatedLaw;
	}

	public String getHskCodePrefix() {
		return hskCodePrefix;
	}

	public boolean isCertificationRequired() {
		return certificationRequired;
	}

	public String getCertificationType() {
		return certificationType;
	}

	public String getRelatedLaw() {
		return relatedLaw;
	}

	public boolean isActive() {
		return active;
	}
}
