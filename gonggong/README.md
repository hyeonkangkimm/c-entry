# Gonggong

해외 직구 상품의 수입 가능성과 안전 리스크를 분석하는 Spring Boot 기반 백엔드와 Chrome 확장 프로그램입니다. 쇼핑몰 상품 페이지에서 상품명, 설명, 이미지, 판매자 정보 등을 수집하고, 서버에서 HSK 코드 후보, KC 인증, 리콜 이력, 화학물질 규제, 관세 정보를 종합해 구매 전 확인해야 할 위험 요소를 보여줍니다.

## 주요 기능

- 해외 쇼핑몰 상품 정보 분석
  - 상품명, 설명, 이미지 URL, 판매자명, KC 인증번호를 기반으로 표준 상품명과 검색 키워드를 정규화합니다.
- HSK 코드 매칭
  - 관세청 HSK 품목 데이터를 로딩하고, OpenAI 임베딩과 pgvector를 활용해 유사 품목 후보를 찾습니다.
- 수입 리스크 대시보드
  - HSK 코드, 원산지, 신고 가격, 수량, 성분, KC 인증 정보를 바탕으로 통관/인증/리콜/화학물질 리스크를 통합 분석합니다.
- KC 인증 및 리콜 확인
  - Safety Korea 공개 API를 통해 인증 정보와 리콜 이력을 조회하고 상품 위험도를 계산합니다.
- 화학물질 규제 분석
  - 성분 후보와 공공 화학물질 API, 로컬 규제 룰을 이용해 제한/금지 성분 여부를 확인합니다.
- Chrome 확장 프로그램
  - AliExpress, Temu 상품 페이지에 오버레이를 삽입해 분석 결과를 바로 확인할 수 있습니다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 17, Spring Boot 4.1, Spring Web MVC |
| Persistence | Spring Data JPA, PostgreSQL, pgvector |
| AI | Spring AI, OpenAI Responses API, OpenAI Embeddings |
| Data | Apache POI, QueryDSL, YAML rule repository |
| Extension | Chrome Extension Manifest V3, JavaScript, CSS |
| Test | JUnit 5, Spring Boot Test, H2 |
| Infra | Docker Compose, pgvector/pgvector:pg16 |

## 프로젝트 구조

```text
.
├── src/main/java/com/example/gonggong
│   ├── domain/analysis    # 상품 정보 정규화 및 분석
│   ├── domain/brand       # 브랜드별 리콜 조회
│   ├── domain/demand      # 수요 우선순위 상품 조회
│   ├── domain/hsk         # HSK 데이터셋, 벡터 검색, 후보 재랭킹
│   ├── domain/risk        # 통관/KC/리콜/화학물질 리스크 분석
│   └── global             # 공통 설정, 예외, 로깅
├── src/main/resources/data
│   ├── customs-hsk-items-20260101.xlsx
│   ├── customs-tariff-rates-20260211.xlsx
│   └── chemical-regulation-rules.yaml
├── extension              # Chrome 확장 프로그램
├── docker/postgres/init   # pgvector 확장 초기화 SQL
└── env                    # 로컬 환경변수 예시 및 로컬 설정
```

## 시작하기

### 1. 요구 사항

- Java 17
- Docker Desktop 또는 Docker Compose
- OpenAI API Key
- Safety Korea, 관세청, 화학물질 공공 API Key

일부 외부 API 키가 비어 있어도 애플리케이션은 실행할 수 있지만, 해당 API를 사용하는 분석 결과는 제한될 수 있습니다.

### 2. 환경변수 설정

`env/.env.example`을 `env/local.env`로 복사한 뒤 필요한 값을 채웁니다.

```bash
cp env/.env.example env/local.env
```

주요 환경변수:

| 변수 | 설명 |
| --- | --- |
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USER` / `DB_PASSWORD` | PostgreSQL 계정 |
| `OPENAI_API_KEY` | OpenAI API Key |
| `OPENAI_MODEL` | 상품 정규화/재랭킹에 사용할 모델 |
| `OPENAI_EMBEDDING_MODEL` | HSK 벡터 검색용 임베딩 모델 |
| `SAFETY_KOREA_API_KEY` | 제품안전정보센터 API Key |
| `CUSTOMS_API_KEY` | 관세청/UNI-PASS API Key |
| `CHEMICAL_API_KEY` | 화학물질 공공 API Key |
| `HSK_EMBEDDING_INITIALIZE` | 실행 시 HSK 임베딩 초기화 여부 |

### 3. PostgreSQL 실행

```bash
docker compose up -d
```

Docker Compose는 `gonggong` 데이터베이스를 생성하고 `vector`, `hstore`, `uuid-ossp` 확장을 활성화합니다.

### 4. 서버 실행

Windows:

```bash
./gradlew.bat bootRun
```

macOS/Linux:

```bash
./gradlew bootRun
```

기본 서버 주소는 `http://localhost:8080`입니다.

### 5. 테스트

```bash
./gradlew.bat test
```

테스트 환경은 H2 인메모리 데이터베이스를 사용하며, HSK 데이터셋 및 임베딩 초기화는 비활성화되어 있습니다.

## 주요 API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/products/analyze` | 상품명/설명/이미지 기반 상품 분석 및 키워드 정규화 |
| `POST` | `/api/seller/hsk/match` | 상품 정보 기반 HSK 코드 후보 매칭 |
| `POST` | `/api/v1/risk-dashboard/analyze` | 통관, KC 인증, 리콜, 화학물질 리스크 통합 분석 |
| `GET` | `/api/brands/{brandName}/recalls` | 브랜드명 기준 리콜 이력 조회 |
| `GET` | `/api/demand/priority-items/top10` | 수요 우선순위 상위 10개 품목 조회 |

### 상품 분석 예시

```bash
curl -X POST http://localhost:8080/api/products/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Kids wireless night light",
    "description": "Rechargeable LED lamp for children",
    "imageUrl": "https://example.com/item.jpg",
    "pageUrl": "https://www.aliexpress.com/item/example",
    "site": "aliexpress",
    "sellerName": "Sample Store"
  }'
```

### 리스크 분석 예시

```bash
curl -X POST http://localhost:8080/api/v1/risk-dashboard/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "hskCode": "9405290000",
    "productName": "LED night light",
    "productDescription": "Rechargeable LED lamp for children",
    "ingredients": ["plastic", "lithium battery"],
    "originCountry": "CN",
    "declaredValue": 18.5,
    "currency": "USD",
    "quantity": 1,
    "shippingCost": 0,
    "insuranceCost": 0,
    "brandName": "Sample Brand"
  }'
```

## Chrome 확장 프로그램 사용

1. 서버를 `http://localhost:8080`에서 실행합니다.
2. Chrome에서 `chrome://extensions`로 이동합니다.
3. 개발자 모드를 켭니다.
4. "압축해제된 확장 프로그램을 로드합니다"를 선택합니다.
5. 이 저장소의 `extension` 디렉터리를 선택합니다.
6. AliExpress 또는 Temu 상품 페이지에서 분석 오버레이를 확인합니다.

확장 프로그램은 현재 다음 페이지에 주입됩니다.

- `*.aliexpress.com/*`
- `*.temu.com/*`

## 데이터 초기화

애플리케이션 시작 시 다음 데이터를 로컬 리소스에서 읽어 데이터베이스에 적재합니다.

- HSK 품목 데이터: `src/main/resources/data/customs-hsk-items-20260101.xlsx`
- 관세율 데이터: `src/main/resources/data/customs-tariff-rates-20260211.xlsx`
- 화학물질 규제 룰: `src/main/resources/data/chemical-regulation-rules.yaml`

HSK 임베딩 초기화는 `HSK_EMBEDDING_INITIALIZE`로 제어합니다. 최초 실행 시에는 OpenAI 임베딩 API 호출이 많이 발생할 수 있으므로, 이미 벡터 테이블을 구성한 뒤에는 필요에 따라 `false`로 변경해 실행 시간을 줄일 수 있습니다.

## 개발 메모

- 로컬 비밀값은 `env/local.env`에 보관하고 Git에 커밋하지 않습니다.
- PostgreSQL 기본 계정은 Docker Compose 기준 `gonggong/gonggong`입니다.
- 테스트는 `src/test/resources/application.yaml` 설정에 따라 외부 DB와 OpenAI 호출 없이 수행되도록 구성되어 있습니다.
- Chrome 확장 프로그램의 API 서버 주소는 `extension/background.js`의 `API_BASE_URL`에 정의되어 있습니다.

