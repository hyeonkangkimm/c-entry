package com.example.gonggong.domain.risk.provider;

import java.util.List;

public interface SafetyKoreaCertificationClient {

	List<SafetyKoreaCertificationItem> searchByCertificationNumber(String certificationNumber);

	List<SafetyKoreaCertificationItem> searchByProductName(String productName);

	List<SafetyKoreaCertificationItem> searchByModelName(String modelName);

	List<SafetyKoreaCertificationItem> searchByBrandName(String brandName);
}
