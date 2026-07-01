package com.example.gonggong.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionAdvice {

	private static final Logger log = LoggerFactory.getLogger(ExceptionAdvice.class);

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
		BaseCode baseCode = exception.getBaseCode();
		log.warn("Handled custom exception code={} message={}", baseCode.getCode(), baseCode.getMessage(), exception);
		return ResponseEntity
			.status(baseCode.getStatus())
			.body(ErrorResponse.from(baseCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(error -> error.getDefaultMessage() == null ? GlobalErrorCode.INVALID_REQUEST.getMessage() : error.getDefaultMessage())
			.orElse(GlobalErrorCode.INVALID_REQUEST.getMessage());
		return ResponseEntity
			.status(GlobalErrorCode.INVALID_REQUEST.getStatus())
			.body(ErrorResponse.of(
				GlobalErrorCode.INVALID_REQUEST.getStatus().value(),
				GlobalErrorCode.INVALID_REQUEST.getCode(),
				message
			));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception) {
		BaseCode baseCode = GlobalErrorCode.INTERNAL_SERVER_ERROR;
		log.error("Unhandled exception", exception);
		return ResponseEntity
			.status(baseCode.getStatus())
			.body(ErrorResponse.from(baseCode));
	}
}
