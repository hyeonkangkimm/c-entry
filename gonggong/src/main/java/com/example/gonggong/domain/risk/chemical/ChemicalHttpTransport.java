package com.example.gonggong.domain.risk.chemical;

import java.net.URI;
import java.time.Duration;

@FunctionalInterface
public interface ChemicalHttpTransport {
	ChemicalHttpResponse get(URI uri, Duration timeout) throws Exception;
}
