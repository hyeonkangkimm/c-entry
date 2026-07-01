package com.example.gonggong.domain.risk.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "public-data.customs.confirmation")
public class CustomsConfirmationProperties {

	private String apiUrl = "";
	private String serviceKey = "";
	private String serviceKeyParamName = "serviceKey";
	private String hskCodeParamName = "hsSgn";
	private String importExportCodeParamName = "imexTpcd";
	private String importCode = "2";
	private Duration connectTimeout = Duration.ofSeconds(3);
	private Duration readTimeout = Duration.ofSeconds(5);

	public boolean enabled() {
		return hasText(apiUrl) && hasText(serviceKey);
	}

	public String getApiUrl() {
		return apiUrl;
	}

	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}

	public String getServiceKey() {
		return serviceKey;
	}

	public void setServiceKey(String serviceKey) {
		this.serviceKey = serviceKey;
	}

	public String getServiceKeyParamName() {
		return serviceKeyParamName;
	}

	public void setServiceKeyParamName(String serviceKeyParamName) {
		this.serviceKeyParamName = serviceKeyParamName;
	}

	public String getHskCodeParamName() {
		return hskCodeParamName;
	}

	public void setHskCodeParamName(String hskCodeParamName) {
		this.hskCodeParamName = hskCodeParamName;
	}

	public String getImportExportCodeParamName() {
		return importExportCodeParamName;
	}

	public void setImportExportCodeParamName(String importExportCodeParamName) {
		this.importExportCodeParamName = importExportCodeParamName;
	}

	public String getImportCode() {
		return importCode;
	}

	public void setImportCode(String importCode) {
		this.importCode = importCode;
	}

	public Duration getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
