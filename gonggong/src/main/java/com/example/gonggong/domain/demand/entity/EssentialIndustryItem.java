package com.example.gonggong.domain.demand.entity;

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
	name = "essential_industry_item",
	indexes = {
		@Index(name = "idx_essential_industry_item_hsk_code", columnList = "hsk_code"),
		@Index(name = "idx_essential_industry_item_region_industry", columnList = "region_name, industry_name")
	}
)
public class EssentialIndustryItem extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "hsk_code", nullable = false, length = 20)
	private String hskCode;

	@Column(name = "item_name", nullable = false, length = 255)
	private String itemName;

	@Column(name = "industry_name", nullable = false, length = 100)
	private String industryName;

	@Column(name = "region_name", nullable = false, length = 100)
	private String regionName;

	protected EssentialIndustryItem() {
	}

	public EssentialIndustryItem(String hskCode, String itemName, String industryName, String regionName) {
		this.hskCode = hskCode;
		this.itemName = itemName;
		this.industryName = industryName;
		this.regionName = regionName;
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

	public String getIndustryName() {
		return industryName;
	}

	public String getRegionName() {
		return regionName;
	}
}
