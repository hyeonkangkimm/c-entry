package com.example.gonggong.domain.analysis.recall;

import java.util.List;

public interface SafetyKoreaRecallClient {

	List<SafetyKoreaRecallItem> searchByProductName(String productName);

	List<SafetyKoreaRecallItem> searchByBrandName(String brandName);

	List<SafetyKoreaRecallItem> searchForeignByProductName(String productName);

	List<SafetyKoreaRecallItem> searchForeignByBrandName(String brandName);

	SafetyKoreaRecallItem findDetail(String recallUid);
}
