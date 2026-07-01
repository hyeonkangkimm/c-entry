package com.example.gonggong.domain.risk.chemical;

public record ChemicalClassification(
	String type,
	String identifier,
	String concentration,
	String exceptionInformation,
	String noticeDate,
	String noticeInformation
) {
}
