package com.example.gonggong.global.exception;

import org.springframework.http.HttpStatus;

public enum GlobalErrorCode implements BaseCode {
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL_001", "서버 내부 오류가 발생했습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "GLOBAL_002", "잘못된 요청입니다."),
	DATA_INITIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL_003", "초기 데이터 적재 중 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	GlobalErrorCode(HttpStatus status, String code, String message) {
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
