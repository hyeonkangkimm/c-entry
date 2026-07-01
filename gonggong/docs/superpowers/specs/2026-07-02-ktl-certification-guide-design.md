# KTL 인증 요건 가이드 설계

## 목표

DOM에서 추출한 KC 인증번호가 SafetyKorea API에서 실제 인증 데이터로 확인된 상품에 한해, 검수된 KTL 인증 요건 가이드를 제공한다. HSK 코드는 참고 정보로만 사용하며 HSK만으로 인증을 추정하지 않는다.

## 판정 흐름

1. DOM에서 KC 인증번호와 상품 정보를 추출한다.
2. 기존 SafetyKorea 인증 조회를 수행한다.
3. 정확한 인증 데이터가 확인된 경우에만 인증 유형을 내부 표준 Key로 정규화한다.
4. 정규화된 Key로 `ktl_certification_guide`를 조회한다.
5. 가이드가 있으면 규칙 기반 `actionItemGuide`를 생성한다.
6. 인증 미확인, API 오류, 유형 미매핑, 비활성 가이드인 경우 KTL 가이드를 반환하거나 표시하지 않는다.

## 데이터 모델

`ktl_certification_guide`는 다음 값을 관리한다.

- `certificationTypeKey`: 정규화된 인증 유형 Key
- `certificationName`: 정확한 인증 명칭
- `certificationMarkUrl`: KC 마크 또는 관련 인증 로고 URL
- `legalBasis`: 법률과 조항
- `testItems`: KTL 시험 항목 JSON 배열
- `requiredDocuments`: 신청 서류 JSON 배열
- `estimatedDuration`: 예상 영업일 안내
- `estimatedFee`: 공개된 범위 또는 `제품 사양에 따라 별도 견적`
- `applicationUrl`: KTL 공식 신청 URL
- `sourceUrl`: 근거가 되는 KTL 공식 페이지
- `active`: 사용 여부
- `verifiedAt`: 마지막 공식 자료 검증일

초기 인증 유형은 전기용품 안전인증·안전확인·공급자적합성확인을 우선 적재한다. 공식 근거가 확인된 유형만 점진적으로 추가한다.

## API 응답

기존 `kcRisk` 응답에 nullable `ktlGuide`를 추가한다.

```json
{
  "certificationName": "전기용품 안전인증",
  "certificationMarkUrl": "https://...",
  "legalBasis": "전기용품 및 생활용품 안전관리법",
  "testItems": ["절연 내력 시험", "온도 상승 시험"],
  "requiredDocuments": ["사업자등록증", "제품설명서", "회로도"],
  "estimatedDuration": "평균 45영업일",
  "estimatedFee": "제품 사양에 따라 별도 견적",
  "applicationUrl": "https://customer.ktl.re.kr/...",
  "actionItemGuide": "제품설명서와 회로도를 준비한 후 KTL에 시험을 신청하세요."
}
```

AI는 법령, 시험 항목, 기간, 비용 또는 행동 문구 생성에 사용하지 않는다.

## 프론트 배치

기존 4개 위험 카드 아래에 가로 전체 폭의 긴 직사각형 `KTL 인증 요건 가이드` 패널을 추가한다.

- 상단: 제목, 확인된 인증명, KC 마크
- 요약 영역: 법적 근거, 예상 기간, 비용 안내
- 상세 영역: 시험 항목과 준비 서류
- 하단: 규칙 기반 다음 행동 문구와 `KTL에서 인증 신청하기` 버튼
- 데스크톱: 전체 폭 다열 구성
- 모바일: 한 열로 순차 배치
- 새 창 형태의 별도 모달은 만들지 않는다.
- KC 미확인 또는 가이드 미매핑이면 패널 자체를 렌더링하지 않는다.

## 보안 및 데이터 품질

- `applicationUrl`, `sourceUrl`, 로고 URL은 KTL 및 승인된 공식 도메인만 허용한다.
- 비용이 공개되지 않은 경우 금액을 추정하지 않는다.
- HSK 코드만으로 인증 유형을 확정하지 않는다.
- 공식 출처와 검증일을 저장해 갱신 가능하게 한다.

## 테스트

- SafetyKorea 인증 확인 여부에 따른 가이드 조회 분기
- 인증 유형 정규화와 미매핑 처리
- DB 가이드 JSON 직렬화
- 공식 URL 허용 규칙
- 4개 카드 아래 전체 폭 패널 렌더링
- KC 미확인 및 가이드 없음 상태에서 패널 미렌더링
- HTML escaping과 모바일 CSS 회귀 검사
