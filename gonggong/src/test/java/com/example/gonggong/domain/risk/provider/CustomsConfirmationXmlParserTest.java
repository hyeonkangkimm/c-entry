package com.example.gonggong.domain.risk.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomsConfirmationXmlParserTest {

	private final CustomsConfirmationXmlParser parser = new CustomsConfirmationXmlParser();

	@Test
	void parsesOfficialLawAndApprovalAgencyFromXmlItems() {
		String xml = """
			<response>
			  <body>
			    <items>
			      <item>
			        <hsSgn>9503003910</hsSgn>
			        <dclrCnfirmLawCd>KC01</dclrCnfirmLawCd>
			        <dclrCnfirmLawNm>어린이제품 안전 특별법</dclrCnfirmLawNm>
			        <reqAproOrgCd>141</reqAproOrgCd>
			        <reqAproOrgNm>국가기술표준원</reqAproOrgNm>
			        <aplyStrtDt>20260101</aplyStrtDt>
			      </item>
			    </items>
			  </body>
			</response>
			""";

		List<CustomsConfirmationItem> items = parser.parse(xml);

		assertThat(items).hasSize(1);
		assertThat(items.get(0).hskCode()).isEqualTo("9503003910");
		assertThat(items.get(0).lawCode()).isEqualTo("KC01");
		assertThat(items.get(0).lawName()).isEqualTo("어린이제품 안전 특별법");
		assertThat(items.get(0).approvalAgencyName()).isEqualTo("국가기술표준원");
	}

	@Test
	void parsesOfficialTechnicalDocumentSampleTags() {
		String xml = """
			<response>
			  <header>
			    <resultCode>00</resultCode>
			    <resultMsg>정상서비스.</resultMsg>
			  </header>
			  <body>
			    <items>
			      <item>
			        <aplyStrtDt>20140101</aplyStrtDt>
			        <bfhnAffcRtmTpcd>2</bfhnAffcRtmTpcd>
			        <dcerCfrmLworCd>01</dcerCfrmLworCd>
			        <dcerCfrmLworNm>약사법</dcerCfrmLworNm>
			        <hsSgn>3307903000</hsSgn>
			        <reqApreIttCd>243</reqApreIttCd>
			        <reqApreIttNm>한국의약품수출입협회</reqApreIttNm>
			        <reqCfrmIstmNm>표준통관예정보고서(의약품등)</reqCfrmIstmNm>
			      </item>
			    </items>
			  </body>
			</response>
			""";

		List<CustomsConfirmationItem> items = parser.parse(xml);

		assertThat(items).hasSize(1);
		assertThat(items.get(0).hskCode()).isEqualTo("3307903000");
		assertThat(items.get(0).lawCode()).isEqualTo("01");
		assertThat(items.get(0).lawName()).isEqualTo("약사법");
		assertThat(items.get(0).approvalAgencyName()).isEqualTo("한국의약품수출입협회");
	}

	@Test
	void parsesKoreanFieldNamesWhenTechnicalDocumentUsesDescriptions() {
		String xml = """
			<response>
			  <body>
			    <items>
			      <item>
			        <HS부호>8516790000</HS부호>
			        <신고인확인법령명>전기용품 및 생활용품 안전관리법</신고인확인법령명>
			        <요건승인기관명>한국제품안전관리원</요건승인기관명>
			      </item>
			    </items>
			  </body>
			</response>
			""";

		List<CustomsConfirmationItem> items = parser.parse(xml);

		assertThat(items).hasSize(1);
		assertThat(items.get(0).hskCode()).isEqualTo("8516790000");
		assertThat(items.get(0).lawName()).isEqualTo("전기용품 및 생활용품 안전관리법");
		assertThat(items.get(0).approvalAgencyName()).isEqualTo("한국제품안전관리원");
	}
}
