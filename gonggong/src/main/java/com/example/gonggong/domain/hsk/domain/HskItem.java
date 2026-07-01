package com.example.gonggong.domain.hsk.domain;

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
	name = "hsk_item",
	indexes = {
		@Index(name = "idx_hsk_item_code", columnList = "hsk_code", unique = true)
	}
)
public class HskItem extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "hsk_code", nullable = false, unique = true, length = 10)
	private String hskCode;

	@Column(name = "korean_name", nullable = false, length = 1000)
	private String koreanName;

	@Column(name = "english_name", nullable = false, length = 2000)
	private String englishName;

	@Column(name = "display_name", length = 3000)
	private String displayName;

	protected HskItem() {
	}

	public HskItem(String hskCode, String koreanName, String englishName) {
		this(hskCode, koreanName, englishName, koreanName);
	}

	public HskItem(String hskCode, String koreanName, String englishName, String displayName) {
		this.hskCode = hskCode;
		this.koreanName = koreanName;
		this.englishName = englishName;
		this.displayName = displayName;
	}

	public String getHskCode() {
		return hskCode;
	}

	public String getKoreanName() {
		return koreanName;
	}

	public String getEnglishName() {
		return englishName;
	}

	public String getDisplayName() {
		return displayName == null || displayName.isBlank() ? koreanName : displayName;
	}
}
