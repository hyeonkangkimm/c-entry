package com.example.gonggong.domain.risk.domain;

import com.example.gonggong.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
	name = "tariff_rate",
	indexes = {
		@Index(name = "idx_tariff_rate_hsk_origin", columnList = "hsk_code, origin_country"),
		@Index(name = "idx_tariff_rate_code_scope", columnList = "tariff_code, country_scope"),
		@Index(name = "idx_tariff_rate_effective", columnList = "effective_from, effective_to")
	}
)
public class TariffRate extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "hsk_code", nullable = false, length = 10)
	private String hskCode;

	@Column(name = "origin_country", length = 2)
	private String originCountry;

	@Column(name = "tariff_code", length = 30)
	private String tariffCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "tariff_type", nullable = false, length = 30)
	private TariffType tariffType;

	@Column(name = "base_rate", precision = 10, scale = 4)
	private BigDecimal baseRate;

	@Column(name = "additional_rate", nullable = false, precision = 10, scale = 4)
	private BigDecimal additionalRate;

	@Column(name = "unit_amount", length = 100)
	private String unitAmount;

	@Column(name = "base_price", length = 100)
	private String basePrice;

	@Column(name = "country_scope", length = 10)
	private String countryScope;

	@Column(name = "usage_rate_code", length = 50)
	private String usageRateCode;

	@Column(name = "effective_from")
	private LocalDate effectiveFrom;

	@Column(name = "effective_to")
	private LocalDate effectiveTo;

	@Column(name = "legal_notice", length = 1000)
	private String legalNotice;

	@Column(name = "active", nullable = false)
	private boolean active;

	protected TariffRate() {
	}

	public TariffRate(
		String hskCode,
		String originCountry,
		TariffType tariffType,
		BigDecimal baseRate,
		BigDecimal additionalRate,
		LocalDate effectiveFrom,
		LocalDate effectiveTo,
		String legalNotice,
		boolean active
	) {
		this(
			hskCode,
			originCountry,
			null,
			tariffType,
			baseRate,
			additionalRate,
			null,
			null,
			null,
			null,
			effectiveFrom,
			effectiveTo,
			legalNotice,
			active
		);
	}

	public TariffRate(
		String hskCode,
		String originCountry,
		String tariffCode,
		TariffType tariffType,
		BigDecimal baseRate,
		BigDecimal additionalRate,
		String unitAmount,
		String basePrice,
		String countryScope,
		String usageRateCode,
		LocalDate effectiveFrom,
		LocalDate effectiveTo,
		String legalNotice,
		boolean active
	) {
		this.hskCode = hskCode;
		this.originCountry = normalizeCountry(originCountry);
		this.tariffCode = tariffCode;
		this.tariffType = tariffType;
		this.baseRate = baseRate;
		this.additionalRate = additionalRate == null ? BigDecimal.ZERO : additionalRate;
		this.unitAmount = unitAmount;
		this.basePrice = basePrice;
		this.countryScope = countryScope;
		this.usageRateCode = usageRateCode;
		this.effectiveFrom = effectiveFrom;
		this.effectiveTo = effectiveTo;
		this.legalNotice = legalNotice;
		this.active = active;
	}

	public String getHskCode() {
		return hskCode;
	}

	public String getOriginCountry() {
		return originCountry;
	}

	public TariffType getTariffType() {
		return tariffType;
	}

	public String getTariffCode() {
		return tariffCode;
	}

	public BigDecimal getBaseRate() {
		return baseRate;
	}

	public BigDecimal getAdditionalRate() {
		return additionalRate;
	}

	public String getUnitAmount() {
		return unitAmount;
	}

	public String getBasePrice() {
		return basePrice;
	}

	public String getCountryScope() {
		return countryScope;
	}

	public String getUsageRateCode() {
		return usageRateCode;
	}

	public LocalDate getEffectiveFrom() {
		return effectiveFrom;
	}

	public LocalDate getEffectiveTo() {
		return effectiveTo;
	}

	public String getLegalNotice() {
		return legalNotice;
	}

	public boolean isActive() {
		return active;
	}

	private String normalizeCountry(String value) {
		return value == null || value.isBlank() ? null : value.trim().toUpperCase();
	}
}
