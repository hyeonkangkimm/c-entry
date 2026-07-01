package com.example.gonggong.domain.risk.provider;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CustomsConfirmationXmlParser {

	public List<CustomsConfirmationItem> parse(String xml) {
		if (xml == null || xml.isBlank()) {
			return List.of();
		}
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);

			Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
			NodeList itemNodes = document.getElementsByTagName("item");
			List<CustomsConfirmationItem> items = new ArrayList<>();
			for (int index = 0; index < itemNodes.getLength(); index++) {
				Node node = itemNodes.item(index);
				if (node instanceof Element itemElement) {
					items.add(new CustomsConfirmationItem(
						text(itemElement, "hsSgn", "hSgn", "hsCd", "hsCode", "HS부호", "품목코드"),
						text(itemElement, "dcerCfrmLworCd", "dclrCnfirmLawCd", "dclrCfrmLawCd", "ntfcCnfirmLawCd", "신고인확인법령코드"),
						text(itemElement, "dcerCfrmLworNm", "dclrCnfirmLawNm", "dclrCfrmLawNm", "ntfcCnfirmLawNm", "신고인확인법령명"),
						text(itemElement, "reqApreIttCd", "reqAproOrgCd", "reqAprovOrgCd", "aprvInstCd", "요건승인기관코드"),
						text(itemElement, "reqApreIttNm", "reqAproOrgNm", "reqAprovOrgNm", "aprvInstNm", "요건승인기관명"),
						text(itemElement, "aplyStrtDt", "aplyStDt", "applyStartDate", "적용시작일자")
					));
				}
			}
			return items;
		}
		catch (Exception exception) {
			throw new IllegalArgumentException("세관장확인대상 XML 응답을 파싱하지 못했습니다.", exception);
		}
	}

	private String text(Element itemElement, String... tagNames) {
		for (String tagName : tagNames) {
			NodeList nodes = itemElement.getElementsByTagName(tagName);
			if (nodes.getLength() > 0) {
				String value = nodes.item(0).getTextContent();
				if (value != null && !value.isBlank()) {
					return value.trim();
				}
			}
		}
		return null;
	}
}
