package com.example.gonggong.domain.demand.entity;

import com.example.gonggong.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(
	name = "import_trend",
	indexes = {
		@Index(name = "idx_import_trend_period_ym", columnList = "period_ym"),
		@Index(name = "idx_import_trend_hsk_period_ym", columnList = "hsk_code, period_ym")
	}
)
public class ImportTrend extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "hsk_code", nullable = false, length = 20)
	private String hskCode;

	@Column(name = "item_name", nullable = false, length = 255)
	private String itemName;

	@Column(name = "period_ym", nullable = false, length = 7)
	private String yearMonth;

	@Column(name = "import_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal importAmount;

	@Column(name = "import_weight", nullable = false, precision = 19, scale = 2)
	private BigDecimal importWeight;

	protected ImportTrend() {
	}

	public ImportTrend(String hskCode, String itemName, String yearMonth, BigDecimal importAmount, BigDecimal importWeight) {
		this.hskCode = hskCode;
		this.itemName = itemName;
		this.yearMonth = yearMonth;
		this.importAmount = importAmount;
		this.importWeight = importWeight;
	}

	public Long getId() {
		return id;
	}

	public String getHskCode() {
		return hskCode;
	}

	public String getItemName() {
		return itemName;
	}

	public String getYearMonth() {
		return yearMonth;
	}

	public BigDecimal getImportAmount() {
		return importAmount;
	}

	public BigDecimal getImportWeight() {
		return importWeight;
	}
}
