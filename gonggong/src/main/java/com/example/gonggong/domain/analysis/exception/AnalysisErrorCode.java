package com.example.gonggong.domain.analysis.exception;

import com.example.gonggong.global.exception.BaseCode;
import org.springframework.http.HttpStatus;

public enum AnalysisErrorCode implements BaseCode {
	INVALID_PRODUCT_NAME(HttpStatus.BAD_REQUEST, "ANALYSIS_001", "상품명은 필수입니다."),
	OPENAI_API_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS_002", "OpenAI API 키가 설정되어 있지 않습니다."),
	OPENAI_API_FAILED(HttpStatus.BAD_GATEWAY, "ANALYSIS_003", "OpenAI 상품 정보 정제 요청에 실패했습니다."),
	OPENAI_RESPONSE_PARSE_FAILED(HttpStatus.BAD_GATEWAY, "ANALYSIS_004", "OpenAI 상품 정보 정제 응답을 해석하지 못했습니다."),
	SAFETY_KOREA_API_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS_005", "SafetyKorea API 키가 설정되어 있지 않습니다."),
	SAFETY_KOREA_API_FAILED(HttpStatus.BAD_GATEWAY, "ANALYSIS_006", "SafetyKorea 리콜 정보 조회에 실패했습니다."),
	SAFETY_KOREA_RESPONSE_PARSE_FAILED(HttpStatus.BAD_GATEWAY, "ANALYSIS_007", "SafetyKorea 리콜 정보 응답을 해석하지 못했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	AnalysisErrorCode(HttpStatus status, String code, String message) {
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
