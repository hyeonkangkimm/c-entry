package com.example.gonggong.global.exception;

public class DataInitializationException extends CustomException {

	public DataInitializationException() {
		super(GlobalErrorCode.DATA_INITIALIZATION_FAILED);
	}

	public DataInitializationException(Throwable cause) {
		super(GlobalErrorCode.DATA_INITIALIZATION_FAILED, cause);
	}
}
