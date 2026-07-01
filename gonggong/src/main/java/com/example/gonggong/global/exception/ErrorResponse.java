package com.example.gonggong.global.exception;

public record ErrorResponse(
	int status,
	String code,
	String message
) {
	public static ErrorResponse from(BaseCode baseCode) {
		return new ErrorResponse(
			baseCode.getStatus().value(),
			baseCode.getCode(),
			baseCode.getMessage()
		);
	}

	public static ErrorResponse of(int status, String code, String message) {
		return new ErrorResponse(status, code, message);
	}
}
