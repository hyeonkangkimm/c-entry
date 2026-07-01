package com.example.gonggong.domain.hsk.dataset;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class HskDatasetReader {

	private static final String TEN_DIGIT_SHEET = "HS10단위";
	private static final String TWO_DIGIT_SHEET = "HS2단위";
	private static final String FOUR_DIGIT_SHEET = "HS4단위";
	private static final String SIX_DIGIT_SHEET = "HS6단위(5단위포함)";
	private static final String EIGHT_DIGIT_SHEET = "HS8단위(7, 9단위포함)";

	public List<HskDatasetRow> read(Resource resource) {
		DataFormatter formatter = new DataFormatter();
		try (InputStream inputStream = resource.getInputStream();
			 Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheet(TEN_DIGIT_SHEET);
			if (sheet == null) {
				throw new IllegalStateException("HS10단위 시트를 찾을 수 없습니다.");
			}
			Map<String, String> categoryNames = new LinkedHashMap<>();
			categoryNames.putAll(readCategoryNames(workbook.getSheet(TWO_DIGIT_SHEET), formatter));
			categoryNames.putAll(readCategoryNames(workbook.getSheet(FOUR_DIGIT_SHEET), formatter));
			categoryNames.putAll(readCategoryNames(workbook.getSheet(SIX_DIGIT_SHEET), formatter));
			categoryNames.putAll(readCategoryNames(workbook.getSheet(EIGHT_DIGIT_SHEET), formatter));

			List<HskDatasetRow> rows = new ArrayList<>();
			for (int index = 1; index <= sheet.getLastRowNum(); index++) {
				Row row = sheet.getRow(index);
				if (row == null) {
					continue;
				}
				String code = formatter.formatCellValue(row.getCell(0)).trim();
				if (!code.matches("\\d{10}")) {
					continue;
				}
				rows.add(new HskDatasetRow(
					code,
					formatter.formatCellValue(row.getCell(1)).trim(),
					formatter.formatCellValue(row.getCell(2)).trim(),
					displayName(code, formatter.formatCellValue(row.getCell(1)).trim(), categoryNames)
				));
			}
			return rows;
		} catch (IOException exception) {
			throw new IllegalStateException("HSK 기준 데이터 파일을 읽지 못했습니다.", exception);
		}
	}

	private Map<String, String> readCategoryNames(Sheet sheet, DataFormatter formatter) {
		Map<String, String> names = new LinkedHashMap<>();
		if (sheet == null) {
			return names;
		}
		for (int index = 1; index <= sheet.getLastRowNum(); index++) {
			Row row = sheet.getRow(index);
			if (row == null) {
				continue;
			}
			String code = formatter.formatCellValue(row.getCell(0)).trim();
			String name = formatter.formatCellValue(row.getCell(1)).trim();
			if (code.matches("\\d{2,9}") && !name.isBlank()) {
				names.put(code, name);
			}
		}
		return names;
	}

	private String displayName(String hskCode, String leafName, Map<String, String> categoryNames) {
		List<String> parts = new ArrayList<>();
		for (String prefix : List.of(
			hskCode.substring(0, 4),
			hskCode.substring(0, 6),
			hskCode.substring(0, 8),
			hskCode
		)) {
			addPart(parts, categoryNames.get(prefix));
		}
		addPart(parts, leafName);
		return parts.isEmpty() ? leafName : String.join(" > ", parts);
	}

	private void addPart(List<String> parts, String value) {
		if (value == null || value.isBlank()) {
			return;
		}
		String trimmed = value.trim();
		if (parts.stream().noneMatch(existing -> existing.equals(trimmed))) {
			parts.add(trimmed);
		}
	}
}
