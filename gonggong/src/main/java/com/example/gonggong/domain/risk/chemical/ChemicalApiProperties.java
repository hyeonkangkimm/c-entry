package com.example.gonggong.domain.risk.chemical;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "public-data.chemical")
public class ChemicalApiProperties {

	private String apiUrl = "https://apis.data.go.kr/B552584/kecoapi/ncissbstn/chemSbstnList";
	private String serviceKey = "";
	private String serviceKeyParamName = "serviceKey";
	private String searchTypeParamName = "searchGubun";
	private String searchNameParamName = "searchNm";
	private String responseTypeParamName = "returnType";
	private String responseType = "JSON";
	private int pageSize = 100;
	private Duration timeout = Duration.ofSeconds(3);
	private int concurrency = 4;
	private String searchUrl = "https://icis.mcee.go.kr/";
	private String searchButtonText = "화학 물질 종합정보시스템에서 직접 성분 검색하기";

	public boolean enabled() { return hasText(apiUrl) && hasText(serviceKey); }
	public String getApiUrl() { return apiUrl; }
	public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
	public String getServiceKey() { return serviceKey; }
	public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
	public String getServiceKeyParamName() { return serviceKeyParamName; }
	public void setServiceKeyParamName(String value) { this.serviceKeyParamName = value; }
	public String getSearchTypeParamName() { return searchTypeParamName; }
	public void setSearchTypeParamName(String value) { this.searchTypeParamName = value; }
	public String getSearchNameParamName() { return searchNameParamName; }
	public void setSearchNameParamName(String value) { this.searchNameParamName = value; }
	public String getResponseTypeParamName() { return responseTypeParamName; }
	public void setResponseTypeParamName(String value) { this.responseTypeParamName = value; }
	public String getResponseType() { return responseType; }
	public void setResponseType(String responseType) { this.responseType = responseType; }
	public int getPageSize() { return pageSize; }
	public void setPageSize(int pageSize) { this.pageSize = pageSize; }
	public Duration getTimeout() { return timeout; }
	public void setTimeout(Duration timeout) { this.timeout = timeout; }
	public int getConcurrency() { return concurrency; }
	public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
	public String getSearchUrl() { return searchUrl; }
	public void setSearchUrl(String searchUrl) { this.searchUrl = searchUrl; }
	public String getSearchButtonText() { return searchButtonText; }
	public void setSearchButtonText(String value) { this.searchButtonText = value; }
	private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
