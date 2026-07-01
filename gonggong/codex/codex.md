# C-Entry README - 공공데이터 기반 수입 안보 관제 시스템

> 이 문서는 `codex/공공데이터_활용_공모전_수정3.pdf` 최종 수정본을 기준으로 정리한 구현용 README이다.  
> 기존 백엔드 MVP 범위를 확장하여 Chrome Extension, 소비자 위해 경고, 셀러용 HSK/KC/관세/처벌 리스크 대시보드, 공공데이터 연동까지 포함한다.

---

## 1. 프로젝트 개요

제품명은 **수입 안보 관제 시스템(C-Entry)** 이다.

C-Entry는 알리익스프레스, 테무 등 C-커머스 해외직구 상품 페이지에서 상품명, 설명, 이미지, 브랜드 정보를 수집하고, 공공데이터와 AI 분석을 결합하여 위해성, 리콜 이력, 인증 누락, 통관 리스크를 사전에 알려주는 시스템이다.

핵심 가치는 해외직구 상품의 위험 물품 유입을 줄이고, 소비자와 셀러가 공공데이터 기반으로 더 안전한 구매/수입 결정을 하도록 돕는 것이다.

### 핵심 목표

- 소비자에게 해외직구 상품의 리콜 이력과 위해 성분 위험을 실시간 경고
- 브랜드 단위의 과거 리콜 이력을 조회하여 신뢰도 판단 근거 제공
- 상품 텍스트와 이미지를 AI로 분석하여 표준 카테고리 자동 분류
- 셀러에게 HSK Code, KC/KTL 인증 요건, 관세, 행정/형사 리스크 제공
- 공공데이터를 배치로 동기화하여 최신 리콜/인증/화학물질 정보를 유지

---

## 2. 사용자와 주요 흐름

### 2.1 소비자 흐름

```text
사용자
  ↓
알리/테무 등 해외직구 상품 페이지 접속
  ↓
Chrome Extension content script 실행
  ↓
상품명, 설명, 브랜드, 이미지 URL, 현재 URL 수집
  ↓
POST /api/products/analyze 호출
  ↓
백엔드가 리콜 DB, 위해 성분 DB, AI 카테고리 분석 수행
  ↓
위험 등급, 리콜 이력, 사고 사례, 위해 사유 반환
  ↓
확장 프로그램의 삼각형 뱃지와 상세 팝업에 표시
```

### 2.2 셀러 흐름

```text
셀러
  ↓
확장 프로그램의 "셀러이신가요?" 진입
  ↓
상품명, 설명, 이미지 또는 HSK Code 입력
  ↓
AI 기반 HSK Code 후보 산출 또는 수동 HSK Code 검증
  ↓
KC/KTL 인증 요건, 리콜 가능성, 관세, 규제 성분 조회
  ↓
리스크 대시보드 반환
```

---

## 3. 시스템 구성

| 영역 | 책임 |
|---|---|
| Chrome Extension | 상품 페이지 DOM 파싱, 삼각형 뱃지/팝업 표시, 셀러 화면 진입 |
| Backend API | 분석 요청 수신, 리콜/성분/HSK/KC/관세/처벌 리스크 계산 |
| Public Data Sync | 공공데이터 API 수집, DB 색인, 매일 새벽 갱신 |
| AI Analyzer | 상품명/설명/이미지 기반 카테고리 분류, 성분/재질/용도 추출 |
| Seller Dashboard | HSK 후보, KC 인증 요건, 관세, 행정/형사 리스크 표시 |

---

## 4. 확장 프로그램 요구사항

### 4.1 도메인 제한

- 알리익스프레스, 테무 등 지정된 해외직구 도메인에서만 content script를 실행한다.
- 전체 브라우저 성능 저하를 막기 위해 manifest 권한을 최소화한다.

### 4.2 삼각형 뱃지 UI

- 페이지 우측 상단 최상위 레이어에 고정 오버레이를 주입한다.
- 스크롤해도 같은 위치에 유지한다.
- 기본 상태는 파란색 삼각형 뱃지이다.
- 위해 상품 또는 리콜 이력이 감지되면 빨간색 삼각형 뱃지로 전환한다.
- 위험 상태에서는 사용자의 시선을 끌 수 있도록 가벼운 깜빡임 애니메이션을 적용한다.
- 뱃지와 팝업에는 그림자, 둥근 모서리, 원 페이지와 충돌하지 않는 z-index를 적용한다.

### 4.3 팝업 인터랙션

| 행동 | 동작 |
|---|---|
| Hover | 뱃지 옆에 상세 정보 프리뷰 팝업을 표시하고, 마우스가 벗어나면 닫는다. |
| Click | 상세 팝업을 고정 모드로 전환한다. 닫기 버튼을 누르기 전까지 유지한다. |
| Seller CTA | `셀러이신가요?` 버튼을 누르면 셀러용 화면을 연다. |

---

## 5. 소비자 분석 기능

## 5.1 상품 위험도 분석 API

### Endpoint

```http
POST /api/products/analyze
Content-Type: application/json
```

### Request Body

```json
{
  "productName": "baby plastic bowl",
  "description": "children plastic tableware, cute baby feeding bowl",
  "brandName": "Example Brand",
  "imageUrl": "https://example.com/product.jpg",
  "pageUrl": "https://www.aliexpress.com/item/123.html",
  "site": "aliexpress"
}
```

### Response Body

```json
{
  "riskLevel": "DANGER",
  "riskColor": "RED",
  "riskScore": 92,
  "category": "유아용 식기류",
  "recallHistory": {
    "exists": true,
    "summary": "동일 또는 유사 모델 리콜 이력이 있습니다.",
    "items": [
      {
        "productName": "유아용 플라스틱 식기",
        "modelName": "ABC-123",
        "brandName": "Example Brand",
        "manufacturer": "Example Factory",
        "manufacturerCountry": "CN",
        "violationReason": "납 성분 검출",
        "accidentCase": "어린이 제품 안전 기준 위반",
        "recallAction": "판매 중지 및 회수",
        "publishedAt": "2025-01-10",
        "similarity": 0.91
      }
    ]
  },
  "brandRecallHistory": {
    "exists": true,
    "message": "이 브랜드의 다른 제품 리콜 이력이 있습니다.",
    "items": []
  },
  "harmfulIngredients": ["납", "카드뮴"],
  "message": "구매 전 상세 리콜 사유와 위해 성분을 확인하세요."
}
```

### 위험 등급

PDF 기준 UI 색상은 다음과 같이 사용한다.

| 등급 | 조건 | 표시 색상 |
|---|---|---|
| `DANGER` | 동일 모델 리콜 이력 또는 치명적 위해 물질 발견 | 빨강 |
| `WARNING` | 유사 모델 리콜 이력, 물리적 결함, 단순 규격 미달 | 노랑 |
| `REVIEW` | 과거 리콜 이력은 있으나 시정조치 완료 또는 최신 안전 인증 갱신 | 초록 |
| `NORMAL` | 리콜 사례 없음 | 파랑 |

### 분석 기준

- 리콜 모델명, 제품명, 브랜드명, 제조사명, 제조국, 안전 기준 위반 내용, 사고 사례, 리콜 조치 내용을 DB에 색인한다.
- 상품명/모델명과 리콜 DB의 문자열 유사도를 계산한다.
- 상품 설명 및 성분표 텍스트에서 금지 성분 키워드를 탐지한다.
- 이미지 URL이 있으면 Vision API로 실제 제품 형태를 교차 검증한다.
- 상세 팝업에는 `리콜 이력`, `사고 사례`, `위해 위험도`를 표시한다.

---

## 5.2 브랜드 리콜 이력 API

현재 보고 있는 상품의 브랜드가 과거 다른 제품에서 리콜된 이력이 있는지 조회한다.

```http
GET /api/brands/{brandName}/recalls
```

### Response Body

```json
{
  "brandName": "Example Brand",
  "exists": true,
  "message": "이 브랜드의 다른 제품 리콜 이력이 있습니다.",
  "items": [
    {
      "productName": "Example Brand Toy",
      "recallReason": "안전 기준 위반",
      "publishedAt": "2025-03-01",
      "sourceUrl": "https://www.safetykorea.kr/release/openapi"
    }
  ]
}
```

리콜 이력이 없으면 `exists=false`와 함께 `리콜 이력이 존재하지 않습니다.` 메시지를 반환한다.

---

## 5.3 AI 카테고리 분류 API

상품명, 설명, 이미지 URL을 기반으로 소비자 친화적인 표준 카테고리를 산출한다.

```http
POST /api/products/classify
Content-Type: application/json
```

### Request Body

```json
{
  "productName": "portable mini heater",
  "description": "220V electric heater for home office",
  "imageUrl": "https://example.com/heater.jpg"
}
```

### Response Body

```json
{
  "category": "전기 난방기기",
  "attributes": {
    "material": "plastic, metal",
    "usage": "home heating",
    "targetUser": "general"
  },
  "confidence": 0.87
}
```

---

## 6. 셀러용 기능

## 6.1 HSK Code 지능형 매칭 API

제품 이미지 또는 텍스트 설명을 분석하여 10단위 HSK Code 후보를 자동 분류한다.

```http
POST /api/seller/hsk/match
Content-Type: application/json
```

### Request Body

```json
{
  "productName": "plastic baby bowl",
  "description": "plastic tableware for children",
  "imageUrl": "https://example.com/bowl.jpg"
}
```

### Response Body

```json
{
  "matched": true,
  "candidates": [
    {
      "hskCode": "3924100000",
      "itemName": "플라스틱제 식탁용품 및 주방용품",
      "confidence": 0.91,
      "reason": "플라스틱제 식기는 일반 플라스틱 제품보다 구체적인 품목명에 우선 분류됩니다."
    }
  ],
  "message": "가장 적합한 HSK Code 후보를 선택하세요."
}
```

### 수동 입력 폴백

AI가 HSK Code를 찾지 못하면 다음 문구와 함께 10자리 HSK Code 수동 입력을 제공한다.

```text
HSK Code를 찾을 수 없습니다.
```

수동 입력 검증 실패 시 다음 메시지를 반환한다.

```text
올바르지 않은 HSK 코드입니다. 관세청 기준 10자리 숫자를 다시 입력해주세요.
```

---

## 6.2 셀러 리스크 대시보드 API

HSK Code와 제품 정보를 기준으로 셀러가 수입 시 확인해야 할 리스크를 반환한다.

```http
POST /api/seller/risk-dashboard
Content-Type: application/json
```

### Request Body

```json
{
  "hskCode": "3924100000",
  "productName": "plastic baby bowl",
  "description": "plastic tableware for children"
}
```

### Response Body

```json
{
  "hskCode": "3924100000",
  "itemName": "플라스틱제 식탁용품 및 주방용품",
  "recallRisk": {
    "countLast3Years": 4,
    "latestPublishedAt": "2025-08-12",
    "reasons": ["납 성분 검출", "안전 기준 위반"],
    "message": "동종 품목의 국외 리콜 이력이 있습니다."
  },
  "tariffRisk": {
    "tariffRate": 8.0,
    "specialTariff": false,
    "message": "일반 관세율 기준입니다."
  },
  "certificationRisk": {
    "required": true,
    "status": "NEEDS_VERIFICATION",
    "message": "KC 인증 대상 여부를 실시간 검증해야 합니다."
  },
  "legalRisk": {
    "restrictedIngredients": [],
    "message": "현재 입력된 성분 기준 규제 성분이 확인되지 않았습니다."
  }
}
```

### 대시보드 리스크 항목

| 항목 | 기준 |
|---|---|
| 리콜 가능성 | 최근 3개년 동종 품목 리콜 건수, 최신 공표일, 위반 내용 |
| 관세 및 사후 추징금 | 관세율 구분 코드, 일반/특별 가중 관세 여부 |
| KC 누락 | 세관장대상확인 API와 KC/KTL 인증 DB 매칭 |
| 행정 처분 및 형사 처벌 | 화학물질 정보 조회 서비스 기반 규제 성분 확인 |

---

## 6.3 KTL 인증 요건 가이드 API

HSK Code와 제품 속성을 기반으로 KC/KTL 인증 요건을 반환한다.

```http
GET /api/seller/certifications/ktl?hskCode=3924100000
```

### Response Body

```json
{
  "certificationName": "전기용품 안전인증",
  "required": true,
  "markImageUrl": "https://example.com/kc_mark.png",
  "legalBasis": "전기용품 및 생활용품 안전관리법 제15조",
  "testItems": ["절연 내력 시험", "온도 상승 시험", "전자파 적합성"],
  "requiredDocuments": ["사업자등록증", "제품설명서", "회로도", "부품명세서"],
  "estimatedPeriod": "평균 15~30영업일",
  "estimatedFee": "약 500,000원(VAT 별도)",
  "applyUrl": "https://customer.ktl.re.kr/web/contents/login.do",
  "actionItem": "준비 서류를 갖추어 하단 버튼을 통해 신청을 진행하세요."
}
```

---

## 7. 공공데이터 연동

| 데이터 | 용도 | URL |
|---|---|---|
| 국가기술표준원 제품안전인증 및 리콜 정보 | 브랜드/제품 리콜 이력, 안전 기준 위반, 사고 사례 | `https://www.safetykorea.kr/release/openapi` |
| 산업통상부 국외리콜 데이터 | 실시간 위해 제품 필터링, 최근 1~3개년 리콜 건수 | `https://www.safetykorea.kr/release/openapi` |
| 관세청 HSK Code | HSK 10단위 품목 분류 | 관세청 HSK 데이터 |
| 무역안보관리원 HSK 연계표 | 전략물자 통제 번호 충돌 확인 | 무역안보관리원 데이터 |
| 관세청 세관장대상확인 API | KC 인증 대상 확인 | `https://www.data.go.kr/data/15101589/openapi.do` |
| 한국환경공단 화학물질 정보 조회 서비스 | 규제 성분, CAS 번호, 처벌 조항 확인 | `https://www.data.go.kr/data/15149420/openapi.do` |
| KTL 인증 정보 | 품목별 인증 대상, 시험 항목, 준비 서류 | `https://customer.ktl.re.kr/web/contents/login.do` |
| 제품안전정보센터 | 인증/제품 안전 정보 실시간 검증 링크 | `https://www.safetykorea.kr/` |
| 화학물질 종합정보시스템 | 성분 직접 검색 링크 | `https://icis.me.go.kr/chmClsCl/chmClsClView.do?hlhsn_sn=4823` |
| 유니패스 | 관세청 법령 지침 확인 링크 | `https://unipass.customs.go.kr/` |

### 동기화 정책

- 리콜 데이터는 매일 새벽 배치로 upsert한다.
- 리콜 모델명, 제품명, 브랜드명, 제조사명, 제조국, 위반 내용, 사고 사례, 조치 내용을 색인한다.
- HSK와 리콜 품목명은 직접 1:1 매칭이 어렵기 때문에 `HSK 표준 품목명 -> 리콜 품목 분류명` 텍스트 표준화 매핑 마스터 테이블을 구축한다.
- 외부 API 장애 시 빈 화면을 반환하지 않고 대체 안내 문구와 공식 사이트 링크를 반환한다.

---

## 8. 권장 백엔드 패키지 구조

현재 프로젝트 패키지는 `com.example.gonggong` 기준으로 유지한다.

```text
src/main/java/com/example/gonggong
 ├─ domain
 │   ├─ analysis
 │   │   ├─ controller
 │   │   ├─ dto
 │   │   ├─ entity
 │   │   └─ service
 │   ├─ recall
 │   │   ├─ entity
 │   │   ├─ repository
 │   │   └─ service
 │   ├─ brand
 │   │   └─ service
 │   ├─ ingredient
 │   │   ├─ entity
 │   │   ├─ repository
 │   │   └─ service
 │   ├─ hsk
 │   │   ├─ entity
 │   │   ├─ repository
 │   │   └─ service
 │   ├─ certification
 │   │   ├─ entity
 │   │   ├─ repository
 │   │   └─ service
 │   ├─ tariff
 │   │   └─ service
 │   ├─ seller
 │   │   ├─ controller
 │   │   ├─ dto
 │   │   └─ service
 │   └─ publicdata
 │       ├─ scheduler
 │       └─ service
 └─ global
     ├─ config
     ├─ exception
     └─ common
```

---

## 9. 주요 DB 테이블

### 9.1 `recall_product`

| 컬럼 | 설명 |
|---|---|
| `id` | PK |
| `product_name` | 리콜 제품명 |
| `model_name` | 리콜 모델명 |
| `brand_name` | 브랜드명 |
| `manufacturer` | 제조사 |
| `manufacturer_country` | 제조국 |
| `category` | 표준 카테고리 |
| `violation_reason` | 안전 기준 위반 내용 |
| `accident_case` | 사고 사례 내용 |
| `recall_action` | 공표문 조치 내용 |
| `source_url` | 원천 URL |
| `published_at` | 공표일 |

### 9.2 `harmful_ingredient`

| 컬럼 | 설명 |
|---|---|
| `id` | PK |
| `name` | 국문 성분명 |
| `english_name` | 영문 성분명 |
| `cas_no` | CAS 번호 |
| `risk_weight` | 위험 점수 가중치 |
| `legal_restriction` | 국내법상 규제 여부 |
| `penalty_clause` | 처벌 조항 |

### 9.3 `hsk_code`

| 컬럼 | 설명 |
|---|---|
| `id` | PK |
| `hsk_code` | 10자리 HSK Code |
| `item_name` | 관세청 표준 품목명 |
| `description` | 품목 설명 |
| `tariff_rate` | 기본 관세율 |
| `special_tariff_type` | 덤핑방지관세 등 특별 관세 유형 |

### 9.4 `hsk_recall_category_mapping`

| 컬럼 | 설명 |
|---|---|
| `id` | PK |
| `hsk_code` | HSK Code |
| `hsk_item_name` | 관세청 품목명 |
| `recall_category_keyword` | 리콜 DB 조회용 표준 키워드 |

### 9.5 `ktl_certification_requirement`

| 컬럼 | 설명 |
|---|---|
| `id` | PK |
| `hsk_code` | HSK Code |
| `certification_name` | 인증 종류명 |
| `legal_basis` | 근거 법령 |
| `test_items` | 시험 항목 JSON |
| `required_documents` | 준비 서류 JSON |
| `estimated_period` | 예상 소요 기간 |
| `estimated_fee` | 예상 수수료 |
| `apply_url` | 시험 신청 딥링크 |

---

## 10. 위험도 계산 로직

```text
위험 점수 =
  리콜 제품명/모델명 유사도 점수
+ 브랜드 리콜 이력 가중치
+ 위해 성분 위험 가중치
+ 동일 카테고리 최근 3개년 리콜 빈도
+ 안전 인증 누락 여부
```

### 소비자 위험 등급 기준

| 조건 | 등급 |
|---|---|
| 동일 모델 리콜, 치명적 위해 물질, 수입 금지 성분 | `DANGER` |
| 유사 모델 리콜, 물리적 결함, 규격 미달 | `WARNING` |
| 과거 리콜 이력 존재하나 시정조치/인증 갱신 확인 | `REVIEW` |
| 리콜 사례와 위해 성분 미탐지 | `NORMAL` |

### 문자열 매칭 전략

1. 상품명, 브랜드명, 모델명, 제조사명을 정규화한다.
2. 특수문자 제거, 대소문자 통일, 다국어 공백 정규화를 수행한다.
3. 완전 포함 관계를 먼저 검사한다.
4. 포함 관계가 없으면 Jaro-Winkler 또는 Levenshtein 유사도를 계산한다.
5. HSK 품목명과 리콜 품목 분류명은 매핑 마스터 테이블을 우선 사용한다.

---

## 11. 환경변수

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}

public-data:
  safety-korea-api-key: ${SAFETY_KOREA_API_KEY:}
  customs-api-key: ${CUSTOMS_API_KEY:}
  chemical-api-key: ${CHEMICAL_API_KEY:}

ai:
  embedding-api-key: ${EMBEDDING_API_KEY:}
  vision-api-key: ${VISION_API_KEY:}
  llm-api-key: ${LLM_API_KEY:}
```

| 이름 | 설명 |
|---|---|
| `DB_URL` | DB 접속 URL |
| `DB_USER` | DB 사용자 |
| `DB_PASSWORD` | DB 비밀번호 |
| `SAFETY_KOREA_API_KEY` | 제품안전/리콜 공공데이터 API 키 |
| `CUSTOMS_API_KEY` | 관세청/세관장대상확인 API 키 |
| `CHEMICAL_API_KEY` | 화학물질 정보 조회 API 키 |
| `VISION_API_KEY` | 이미지 분석 API 키 |
| `EMBEDDING_API_KEY` | 텍스트 임베딩 API 키 |
| `LLM_API_KEY` | HSK/속성 추출용 LLM API 키 |

---

## 12. 개발 우선순위

### Phase 1 - 소비자 위해상품 MVP

- [ ] `POST /api/products/analyze` 고정 응답 구현
- [ ] 확장 프로그램에서 상품명/설명/이미지 URL/page URL 수집
- [ ] 파란색/빨간색 삼각형 뱃지 표시
- [ ] hover/click 팝업 인터랙션 구현
- [ ] 리콜 샘플 데이터 기반 위험 등급 반환
- [ ] 브랜드 리콜 이력 조회 API 추가

### Phase 2 - 공공데이터 저장 및 배치

- [ ] 국가기술표준원 제품안전인증 및 리콜 정보 수집
- [ ] 리콜 데이터 upsert 및 색인
- [ ] 매일 새벽 배치 스케줄러 구현
- [ ] 위해 성분 키워드 및 화학물질 DB 구축
- [ ] 외부 API 장애 시 대체 메시지 처리

### Phase 3 - AI 카테고리/이미지 분석

- [ ] 상품명/설명 기반 속성 추출
- [ ] Vision API 기반 이미지 교차 검증
- [ ] 표준 소비자 카테고리 분류
- [ ] 카테고리별 최근 1~3개년 리콜 통계 반환

### Phase 4 - 셀러 대시보드

- [ ] HSK Code 자동 후보 추천 API
- [ ] HSK Code 수동 입력 검증
- [ ] HSK-리콜 카테고리 매핑 마스터 테이블 구축
- [ ] KC/KTL 인증 요건 API
- [ ] 관세 및 특별 관세 리스크 API
- [ ] 화학물질 규제/처벌 리스크 API
- [ ] 셀러 리스크 대시보드 통합 응답 구현

---

## 13. Codex 작업 규칙

1. 현재 프로젝트 패키지 `com.example.gonggong`을 유지한다.
2. 백엔드 비즈니스 로직은 controller에 넣지 않고 service로 분리한다.
3. Extension 코드는 `extension/` 폴더에서만 수정한다.
4. API 응답 JSON 계약은 이 문서를 기준으로 한다.
5. 민감 정보와 API 키는 코드에 하드코딩하지 않는다.
6. 외부 공공데이터 API 장애 시 사용자에게 빈 화면을 보여주지 않는다.
7. 리콜/HSK/KC/성분 데이터는 재현 가능한 seed 또는 batch 경로를 둔다.
8. AI 분석 결과는 `confidence`를 함께 반환해 프론트에서 불확실성을 표시할 수 있게 한다.
9. 셀러용 HSK Code는 자동 후보가 실패해도 수동 입력 폴백을 제공한다.
10. 소비자용 위험 표시는 PDF 기준 색상 정책인 파랑/초록/노랑/빨강을 따른다.

---

## 14. 최종 목표

```text
C-Entry는 해외직구 상품 정보를 공공데이터와 AI로 분석하여 소비자에게 위해상품 경고를 제공하고,
셀러에게 HSK Code, KC/KTL 인증, 관세, 리콜, 규제 성분 리스크를 한 화면에서 제공하는 수입 안보 관제 시스템이다.
```
