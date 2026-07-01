package com.example.gonggong.global.exception;

public class CustomException extends RuntimeException {

	private final BaseCode baseCode;

	public CustomException(BaseCode baseCode) {
		super(baseCode.getMessage());
		this.baseCode = baseCode;
	}

	public CustomException(BaseCode baseCode, Throwable cause) {
		super(baseCode.getMessage(), cause);
		this.baseCode = baseCode;
	}

	public BaseCode getBaseCode() {
		return baseCode;
	}
}
