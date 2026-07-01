package com.example.gonggong.domain.risk.chemical;

@FunctionalInterface
public interface ChemicalInformationClient {
	ChemicalLookupResult lookup(String ingredient);
}
