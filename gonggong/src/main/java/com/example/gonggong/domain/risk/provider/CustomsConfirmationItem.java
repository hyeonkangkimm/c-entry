package com.example.gonggong.domain.risk.provider;

public record CustomsConfirmationItem(
	String hskCode,
	String lawCode,
	String lawName,
	String approvalAgencyCode,
	String approvalAgencyName,
	String effectiveStartDate
) {
}
