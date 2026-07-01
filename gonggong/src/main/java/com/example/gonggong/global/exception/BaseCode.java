package com.example.gonggong.global.exception;

import org.springframework.http.HttpStatus;

public interface BaseCode {

	HttpStatus getStatus();

	String getCode();

	String getMessage();
}
