package com.example.gonggong.domain.demand.exception;

import com.example.gonggong.global.exception.BaseCode;
import org.springframework.http.HttpStatus;

public enum DemandErrorCode implements BaseCode {
	IMPORT_TREND_DATA_EMPTY(HttpStatus.INTERNAL_SERVER_ERROR, "DEMAND_001", "수입 추세 데이터가 비어 있습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	DemandErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}

	@Override
	public HttpStatus getStatus() {
		return status;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
