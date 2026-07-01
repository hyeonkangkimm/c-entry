package com.example.gonggong.domain.analysis.recall;

import java.util.List;

public record SafetyKoreaRecallItem(
	String recallUid,
	String recallProductName,
	String recallBrandName,
	String recallModelName,
	String recallCmpnyName,
	String publishDate,
	String recallStaDate,
	String recallEndDate,
	String barcodeNum,
	String certNum,
	String harmDscr,
	String accidentCaseDscr,
	String publishActionDscr,
	List<String> imageUrls,
	String matchedQuery,
	RecallSource source,
	String sourceUrl
) {
	public SafetyKoreaRecallItem {
		if (source == null) {
			source = RecallSource.DOMESTIC;
		}
		if (imageUrls == null) {
			imageUrls = List.of();
		}
	}

	public SafetyKoreaRecallItem(
		String recallUid,
		String recallProductName,
		String recallBrandName,
		String recallModelName,
		String recallCmpnyName,
		String publishDate,
		String recallStaDate,
		String recallEndDate,
		String barcodeNum,
		String certNum,
		String harmDscr,
		String accidentCaseDscr,
		String publishActionDscr,
		List<String> imageUrls,
		String matchedQuery
	) {
		this(
			recallUid,
			recallProductName,
			recallBrandName,
			recallModelName,
			recallCmpnyName,
			publishDate,
			recallStaDate,
			recallEndDate,
			barcodeNum,
			certNum,
			harmDscr,
			accidentCaseDscr,
			publishActionDscr,
			imageUrls,
			matchedQuery,
			RecallSource.DOMESTIC,
			null
		);
	}

	public SafetyKoreaRecallItem(
		String recallUid,
		String recallProductName,
		String recallBrandName,
		String recallModelName,
		String recallCmpnyName,
		String publishDate,
		String recallStaDate,
		String recallEndDate,
		String barcodeNum,
		String certNum,
		String harmDscr,
		String accidentCaseDscr,
		String publishActionDscr,
		List<String> imageUrls
	) {
		this(
			recallUid,
			recallProductName,
			recallBrandName,
			recallModelName,
			recallCmpnyName,
			publishDate,
			recallStaDate,
			recallEndDate,
			barcodeNum,
			certNum,
			harmDscr,
			accidentCaseDscr,
			publishActionDscr,
			imageUrls,
			null,
			RecallSource.DOMESTIC,
			null
		);
	}

	public SafetyKoreaRecallItem(
		String recallUid,
		String recallProductName,
		String recallBrandName,
		String recallModelName,
		String recallCmpnyName,
		String publishDate,
		String harmDscr,
		String accidentCaseDscr,
		String publishActionDscr,
		List<String> imageUrls
	) {
		this(
			recallUid,
			recallProductName,
			recallBrandName,
			recallModelName,
			recallCmpnyName,
			publishDate,
			null,
			null,
			null,
			null,
			harmDscr,
			accidentCaseDscr,
			publishActionDscr,
			imageUrls,
			null,
			RecallSource.DOMESTIC,
			null
		);
	}

	public SafetyKoreaRecallItem withMatchedQuery(String matchedQuery) {
		return new SafetyKoreaRecallItem(
			recallUid,
			recallProductName,
			recallBrandName,
			recallModelName,
			recallCmpnyName,
			publishDate,
			recallStaDate,
			recallEndDate,
			barcodeNum,
			certNum,
			harmDscr,
			accidentCaseDscr,
			publishActionDscr,
			imageUrls,
			matchedQuery,
			source,
			sourceUrl
		);
	}

	public SafetyKoreaRecallItem withSource(RecallSource source) {
		return new SafetyKoreaRecallItem(
			recallUid,
			recallProductName,
			recallBrandName,
			recallModelName,
			recallCmpnyName,
			publishDate,
			recallStaDate,
			recallEndDate,
			barcodeNum,
			certNum,
			harmDscr,
			accidentCaseDscr,
			publishActionDscr,
			imageUrls,
			matchedQuery,
			source,
			sourceUrl
		);
	}
}
