package com.example.gonggong.domain.hsk.service;

public record HskRerankResult(
	String selectedHskCode,
	double confidence,
	String reason
) {

	public static HskRerankResult notSelected() {
		return new HskRerankResult(null, 0.0, null);
	}

	public boolean selected() {
		return selectedHskCode != null && !selectedHskCode.isBlank();
	}
}
