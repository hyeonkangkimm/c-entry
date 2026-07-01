const test = require("node:test");
const assert = require("node:assert/strict");

const {
  chemicalRiskCardHtml,
  chemicalInlineDetailHtml,
  customsRiskCardHtml,
  kcRiskCardHtml,
  ktlCertificationGuideHtml,
  recallRiskCardHtml,
  recallDetailListHtml,
} = require("./risk-view.js");

test("KTL guide renders verified certification details in a full panel", () => {
  const html = ktlCertificationGuideHtml({
    certificationName: "전기용품 안전인증",
    certificationMarkUrl: null,
    legalBasis: "전기용품 및 생활용품 안전관리법 제5조",
    testItems: ["절연 내력 시험", "온도 상승 시험"],
    requiredDocuments: ["제품사용설명서(국문)", "회로도"],
    estimatedDuration: "평균 45영업일",
    estimatedFee: "제품 사양에 따라 별도 견적",
    applicationUrl: "https://customer.ktl.re.kr/web/contents/login.do",
    actionItemGuide: "준비 서류를 확인한 후 KTL에서 시험을 신청하세요.",
    sourceUrl: "https://customer.ktl.re.kr/web/contents/K101010200.do",
  });

  assert.match(html, /KTL 인증 요건 가이드/);
  assert.match(html, /전기용품 안전인증/);
  assert.match(html, /절연 내력 시험/);
  assert.match(html, /제품사용설명서/);
  assert.match(html, /평균 45영업일/);
  assert.match(html, /KTL에서 인증 신청하기/);
});

test("KTL guide stays hidden without a verified mapped guide", () => {
  assert.equal(ktlCertificationGuideHtml(null), "");
});

test("chemical danger card renders a detail button for the ingredient list", () => {
  const html = chemicalRiskCardHtml({
    status: "DANGER",
    message: "규제 성분이 확인되었습니다.",
    regulatedIngredients: [{
      ingredientName: "납",
      casNumber: "50-00-0",
      hazardClassification: "유해성분",
      relatedLaw: "화학물질관리법 제0조",
      penaltyProvision: null,
      legalSourceUrl: "https://www.law.go.kr/법령/화학물질관리법",
      regulated: true,
    }, {
      ingredientName: "카드뮴",
      casNumber: "51-00-0",
      hazardClassification: "유해성분",
      relatedLaw: null,
      penaltyProvision: null,
      legalSourceUrl: null,
      regulated: false,
    }],
    analysisUnavailable: false,
  });

  assert.match(html, /규제가능성 목록/);
  assert.match(html, /data-isg-action="open-chemical-details"/);
  assert.doesNotMatch(html, /<details class="isg-chemical-ingredient">/);
  assert.doesNotMatch(html, /시스템 성분 분석 불가능 품목/);
});

test("chemical card shows safe when no ingredients are returned", () => {
  const html = chemicalRiskCardHtml({
    status: "UNKNOWN",
    message: "",
    regulatedIngredients: [],
    analysisUnavailable: false,
  });

  assert.match(html, /안전/);
  assert.match(html, /위해 성분이 확인되지 않아 안전으로 표시합니다\./);
  assert.doesNotMatch(html, /규제가능성 목록/);
  assert.doesNotMatch(html, /시스템 성분 분석 불가능 품목/);
});

test("chemical inline detail renders list with per-item status", () => {
  const html = chemicalInlineDetailHtml({
    regulatedIngredients: [
      {
        ingredientName: "납",
        casNumber: "50-00-0",
        hazardClassification: "유해성분",
        relatedLaw: "화학물질관리법",
        penaltyProvision: "처벌 조항",
        legalSourceUrl: "https://www.law.go.kr/법령/화학물질관리법",
        regulated: true,
      },
      {
        ingredientName: "카드뮴",
        casNumber: "51-00-0",
        hazardClassification: "참고 성분",
        relatedLaw: null,
        penaltyProvision: null,
        legalSourceUrl: null,
        regulated: false,
      },
    ],
  });

  assert.match(html, /규제가능성 목록/);
  assert.match(html, /납/);
  assert.match(html, /카드뮴/);
  assert.match(html, /규제 대상/);
  assert.match(html, /비규제\/참고/);
  assert.match(html, /법령 근거 확인/);
});

test("chemical unavailable card renders marker and safe ICIS button", () => {
  const html = chemicalRiskCardHtml({
    status: "UNAVAILABLE",
    analysisUnavailable: true,
    unanalyzedIngredients: ["unknown ingredient"],
    searchButtonText: "화학 물질 종합정보시스템에서 직접 성분 검색하기",
    searchUrl: "javascript:alert(1)",
  });

  assert.match(html, /시스템 성분 분석 불가능 품목/);
  assert.match(html, /unknown ingredient/);
  assert.match(html, /화학 물질 종합정보시스템에서 직접 성분 검색하기/);
  assert.match(html, /href="https:\/\/icis\.mcee\.go\.kr\//);
  assert.doesNotMatch(html, /javascript:/);
  assert.match(html, /rel="noopener noreferrer"/);
});

for (const status of ["DANGER", "WARNING", "UNKNOWN", "UNAVAILABLE"]) {
  test(`KC ${status} card shows the verification fallback`, () => {
    const html = kcRiskCardHtml({
      status,
      certificationValid: false,
      verificationButtonText: "제품안전정보센터에서 실시간 검증하기",
      verificationUrl: "https://www.safetykorea.kr/search/?keyword=<KC>",
    }, "KC 번호: 없음");

    assert.match(html, /정보가 없습니다\./);
    assert.match(html, /제품안전정보센터에서 실시간 검증하기/);
    assert.match(html, /href="https:\/\/www\.safetykorea\.kr\/search\/\?keyword=&lt;KC&gt;"/);
    assert.match(html, /target="_blank"/);
    assert.match(html, /rel="noopener noreferrer"/);
  });
}

test("valid KC card does not show the verification fallback", () => {
  const html = kcRiskCardHtml({
    status: "SAFE",
    certificationValid: true,
    message: "유효한 KC 인증입니다.",
  }, "KC 번호: HU****22");

  assert.match(html, /유효한 KC 인증입니다\./);
  assert.doesNotMatch(html, /정보가 없습니다\./);
  assert.doesNotMatch(html, /isg-kc-verification-link/);
});

test("KC card uses safe defaults when API verification fields are missing or unsafe", () => {
  const missingFieldsHtml = kcRiskCardHtml({}, "KC 번호: 없음");
  const unsafeUrlHtml = kcRiskCardHtml({
    certificationValid: false,
    verificationUrl: "javascript:alert(1)",
  });

  assert.match(missingFieldsHtml, /정보가 없습니다\./);
  assert.match(missingFieldsHtml, /제품안전정보센터에서 실시간 검증하기/);
  assert.match(missingFieldsHtml, /href="https:\/\/www\.safetykorea\.kr\//);
  assert.match(unsafeUrlHtml, /href="https:\/\/www\.safetykorea\.kr\//);
  assert.doesNotMatch(unsafeUrlHtml, /javascript:/);
});

test("recall risk card shows detail button with total count", () => {
  const html = recallRiskCardHtml({
    status: "WARNING",
    totalCount: 26,
    message: "최근 3개월 내 동일 유형 제품 리콜 이력을 확인했습니다.",
    items: [
      {
        productName: "제품A",
        reason: "배터리 과열 위험",
        announcementDate: "2025-11-20",
        source: "DOMESTIC",
      },
    ],
  });

  assert.match(html, /26/);
  assert.match(html, /data-isg-action="open-recall-details"/);
});

test("recall detail list renders recall item contents without source links", () => {
  const html = recallDetailListHtml({
    items: [
      {
        productName: "제품A",
        reason: "배터리 과열 위험",
        violationDetails: "소비자 위해 발생",
        announcementDate: "2025-11-20",
        sourceUrl: "https://example.test/recall",
        source: "DOMESTIC",
      },
    ],
  });

  assert.match(html, /제품A/);
  assert.match(html, /배터리 과열 위험/);
  assert.match(html, /소비자 위해 발생/);
  assert.match(html, /2025-11-20/);
  assert.doesNotMatch(html, /https:\/\/example.test\/recall/);
  assert.doesNotMatch(html, /문서 보기/);
});

test("customs risk card shows tariff rate without estimated duty", () => {
  const html = customsRiskCardHtml({
    status: "SAFE",
    tariffType: "WTO",
    finalTariffRate: 13,
    estimatedCustomsDuty: 14300,
    message:
      "수입 관세율 기준으로 예상 관세액을 계산했습니다. 근거: 관세청_품목번호별관세율_20260211, 관세율구분=WTO 관세(C) 실제 적용 시 확인 필요",
  });

  assert.match(html, /isg-customs-card/);
  assert.match(html, /13%/);
  assert.doesNotMatch(html, /예상 관세액/);
  assert.doesNotMatch(html, /14,300원/);
  assert.doesNotMatch(html, /undefined|null/);
});

test("customs risk card keeps unknown responses concise", () => {
  const html = customsRiskCardHtml({
    status: "UNKNOWN",
    tariffType: "UNKNOWN",
    message: "문서 코드입니다. 관세청 형식 법령 지침을 확인해 주세요",
    guideUrl: "https://unipass.customs.go.kr/",
  });

  assert.match(html, /https:\/\/unipass\.customs\.go\.kr\//);
  assert.match(html, /문서 코드입니다/);
  assert.doesNotMatch(html, /undefined|null/);
});

test("customs risk card omits estimated duty guidance when product price is unavailable", () => {
  const html = customsRiskCardHtml({
    status: "SAFE",
    tariffType: "WTO",
    finalTariffRate: 5,
    estimatedCustomsDuty: null,
    message:
      "수입 관세율 기준으로 예상 관세액을 계산했습니다. 상품 가격 정보가 없어 예상 관세액은 계산할 수 없습니다.",
  });

  assert.match(html, /5%/);
  assert.doesNotMatch(html, /예상 관세액|가격 필요/);
});

test("customs risk card omits estimated duty guidance when currency is unsupported", () => {
  const html = customsRiskCardHtml({
    status: "SAFE",
    tariffType: "WTO",
    finalTariffRate: 5,
    estimatedCustomsDuty: null,
    message:
      "수입 관세율 기준으로 예상 관세액을 계산했습니다. 환율 데이터가 없어 예상 관세액은 계산할 수 없습니다.",
  });

  assert.match(html, /5%/);
  assert.doesNotMatch(html, /예상 관세액|환율 필요/);
});

test("customs risk card escapes external response text", () => {
  const html = customsRiskCardHtml({
    status: "UNKNOWN",
    message: "<script>alert('x')</script>",
  });

  assert.doesNotMatch(html, /<script>/);
  assert.match(html, /&lt;script&gt;/);
});
