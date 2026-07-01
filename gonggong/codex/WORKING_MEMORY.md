# Codex Working Memory

이 파일은 이 프로젝트에서 Codex가 다시 시작되거나 컨텍스트가 초기화되어도 바로 작업을 이어갈 수 있도록 유지하는 작업 메모리이다. 새 세션은 먼저 이 파일과 `codex/codex.md`를 읽고 진행한다.

## 1. 프로젝트 목적

- 해외직구 상품 페이지에서 수집된 상품 정보를 백엔드로 받아 공공데이터 기반 위해성을 분석한다.
- 분석 결과는 Chrome Extension 팝업/오버레이에서 사용할 JSON으로 반환한다.
- 핵심 결과: 리콜 이력, 위해 성분, 위험 점수, 위험 등급.

## 2. 원본 문서

- `codex/codex.md`: 백엔드 구현용 README. MVP 구현 기준 문서.
- `codex/공공데이터_활용_공모전_-_PRD_수정.pdf`: 전체 서비스 PRD. 브라우저 확장, 소비자 기능, 셀러 기능, HSK Code, KTL/KC 인증 가이드까지 포함.
- 판단: 1차 구현은 `codex.md`의 백엔드 MVP를 우선한다. PDF의 Vision API, HSK Code, 인증 가이드는 후속 고도화 범위다.

## 3. 사용자가 추가로 확정한 기술 방향

- DB는 최종적으로 AWS RDS MySQL을 사용한다.
- 개발 초기에는 로컬 MySQL을 사용한다.
- JPA 기반으로 구현하되, 동적 검색/조건 조합/검색 쿼리는 QueryDSL로 간략화한다.
- DB 인덱싱을 고려해서 Entity와 조회 패턴을 설계한다.
- 민감 정보는 코드나 문서에 하드코딩하지 않는다.

## 4. 현재 프로젝트 상태

- 프로젝트 루트: `C:\Users\user\Desktop\gonggong\gonggong`
- 현재 Spring Boot 프로젝트는 루트에 존재한다. 별도 `backend/` 폴더는 없다.
- 현재 기본 패키지: `com.example.gonggong`
- 문서 권장 패키지 `com.example.importsafe`와 다르지만, 실제 프로젝트에 맞춰 `com.example.gonggong`을 유지하는 방향이 안전하다.
- 현재 주요 파일:
  - `build.gradle`
  - `settings.gradle`
  - `src/main/java/com/example/gonggong/GonggongApplication.java`
  - `src/main/resources/application.yaml`
  - `src/test/java/com/example/gonggong/GonggongApplicationTests.java`
- 현재 Git 저장소로 인식되지 않는다. `git status`는 실패한다.

## 5. 현재 확인된 기술 이슈

- `build.gradle`의 `spring-boot-starter-data-jpa` 중복 선언은 2026-06-15에 제거했다.
- `build.gradle`은 Spring Boot `4.1.0`을 사용 중이다.
- 문서는 Spring Boot `3.x`, Java 17 기준이다.
- 로컬 `java -version`은 Java 8로 잡혀 있었지만, Gradle toolchain은 Java 17을 요구한다.
- `application.yaml`에 로컬 MySQL 계정/비밀번호가 직접 들어가 있다. 실제 비밀번호는 이 메모리에 기록하지 않는다.
- 테스트 실행 시 로컬 MySQL 의존성이 있으면 CI/로컬 환경에서 깨질 수 있으므로 테스트 프로파일 또는 H2/Testcontainers 전략이 필요하다.
- QueryDSL `5.1.0` Jakarta 의존성과 annotation processor 설정은 2026-06-15에 추가했다.
- `./gradlew.bat compileJava`는 2026-06-15에 성공했다. 최초 실행은 Gradle 배포본 다운로드 때문에 네트워크 승인이 필요했다.

## 6. DB 설계 방향

### 6.1 로컬 개발 DB

- DBMS: MySQL
- 기본 DB명 후보: `gonggong`
- 설정은 환경변수 기반으로 전환한다.
- 예시:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/gonggong?serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 6.2 RDS 전환

- RDS도 동일한 `DB_URL`, `DB_USER`, `DB_PASSWORD` 환경변수만 교체해서 연결한다.
- SSL, timezone, character encoding, connection pool 설정은 운영 프로파일에서 분리한다.

### 6.3 인덱싱 기본안

- `recall_product`
  - `product_name`: 리콜 상품명 검색 후보 조회
  - `model_name`: 모델명 직접 매칭
  - `manufacturer`: 제조사 보조 검색
  - `category`: 카테고리별 리콜 빈도 계산
  - `recalled_at`: 최신 리콜 우선 정렬
  - 후보 복합 인덱스: `(category, recalled_at)`, `(model_name)`, `(product_name)`
- `harmful_ingredient`
  - `name`: 한글 성분명 탐지
  - `english_name`: 영문 성분명 탐지
  - 후보 유니크/일반 인덱스: `(name)`, `(english_name)`
- `product_analysis_log`
  - `site`: 사이트별 통계
  - `risk_level`: 위험 등급별 조회
  - `created_at`: 기간별 조회
  - 후보 복합 인덱스: `(site, created_at)`, `(risk_level, created_at)`

주의: MySQL 일반 B-Tree 인덱스는 `%keyword%` LIKE에 효과가 제한적이다. MVP에서는 후보 수를 줄이는 용도로 사용하고, 고도화 시 FULLTEXT 인덱스 또는 별도 검색엔진을 검토한다.

## 7. QueryDSL 적용 방향

- 정적 CRUD는 Spring Data JPA Repository 사용.
- 검색/동적 조건은 QueryDSL custom repository로 분리.
- 예상 구조:

```text
domain/recall/repository/
  RecallProductRepository.java
  RecallProductQueryRepository.java
  RecallProductQueryRepositoryImpl.java

domain/ingredient/repository/
  HarmfulIngredientRepository.java

domain/analysis/repository/
  ProductAnalysisLogRepository.java
```

- `RecallProductQueryRepository` 책임:
  - 키워드 기반 리콜 후보 조회
  - 카테고리 조건 조회
  - 최신 리콜 우선 정렬
  - 상위 N개 제한
- 문자열 유사도 계산은 DB가 아니라 Java 서비스에서 수행한다. QueryDSL은 후보군 축소에 집중한다.

## 8. MVP API 계약

### 8.1 상품 위험도 분석 API

```http
POST /api/products/analyze
Content-Type: application/json
```

Request:

```json
{
  "productName": "baby plastic bowl",
  "description": "children plastic tableware, cute baby feeding bowl",
  "imageUrl": "https://example.com/product.jpg",
  "pageUrl": "https://www.aliexpress.com/item/123.html",
  "site": "aliexpress"
}
```

Response:

```json
{
  "riskLevel": "DANGER",
  "riskScore": 86,
  "category": "유아용 식기류",
  "recallReason": "유사 리콜 제품 존재: 납 성분 검출",
  "harmfulIngredients": ["납", "카드뮴"],
  "matchedRecalls": [
    {
      "recallProductName": "유아용 플라스틱 식기",
      "modelName": "ABC-123",
      "manufacturer": "Example Factory",
      "reason": "납 성분 검출",
      "similarity": 0.86
    }
  ],
  "message": "유사 리콜 이력이 있는 상품입니다. 구매 전 상세 정보를 확인하세요."
}
```

### 8.2 리콜 검색 API

```http
GET /api/recalls/search?keyword=plastic%20bowl
```

### 8.3 공공데이터 동기화 API

```http
POST /api/admin/public-data/sync/recalls
```

## 9. 패키지 구조 결정

문서의 구조를 따르되 루트 패키지는 현재 프로젝트에 맞춰 `com.example.gonggong`으로 사용한다.

```text
src/main/java/com/example/gonggong/
  domain/
    analysis/
    recall/
    ingredient/
    publicdata/
  global/
    common/
    config/
    exception/
```

## 10. 구현 원칙

- 컨트롤러에는 비즈니스 로직을 넣지 않는다.
- 위험도 계산은 `RiskScoreCalculator`로 분리한다.
- 텍스트 정규화는 `ProductTextNormalizeService`로 분리한다.
- 리콜 후보 조회는 QueryDSL repository로 분리한다.
- 민감 정보는 환경변수로 분리한다.
- 테스트 데이터는 `data.sql` 또는 별도 seed 구성으로 분리한다.
- 새 기능은 테스트를 먼저 작성하고 실패를 확인한 뒤 구현한다.

## 11. 우선 구현 순서

1. 빌드 설정 정리
   - JPA 중복 제거: 완료
   - MySQL 드라이버 유지
   - QueryDSL 의존성 및 QClass 생성 설정 추가: 완료
   - 필요 시 Spring Boot 3.x로 조정 여부 결정
2. 설정 정리
   - `application.yaml`은 현재 구조를 유지한다. RDS 전환 시 환경변수 기반으로 변경한다.
   - 테스트 프로파일 분리
3. MVP 고정 응답 API
   - `ProductAnalyzeController`: 완료
   - `ProductAnalyzeRequest`: 완료
   - `ProductAnalyzeResponse`: 완료
   - `MatchedRecallDto`: 완료
   - `RiskLevel`: 완료
   - `ProductAnalyzeService`: 완료
   - 현재 상태: DB 연동 없이 문서의 예시 JSON 계약에 맞춘 고정 응답 반환
   - 테스트: `ProductAnalyzeControllerTest` 추가
4. Entity 및 Repository
   - `BaseTimeEntity`: 완료
   - `RecallProduct`
   - `HarmfulIngredient`
   - `ProductAnalysisLog`
   - Repository 및 QueryDSL custom repository
5. 샘플 데이터
   - 리콜 제품 샘플
   - 위해 성분 샘플
6. 분석 로직
   - 텍스트 정규화
   - 위해 성분 탐지
   - 리콜 후보 검색 및 유사도 계산
   - 위험 점수 계산
   - 분석 로그 저장
7. 리콜 검색 API
8. 공공데이터 동기화 API 뼈대

## 12. 미해결 결정 사항

- Spring Boot `4.1.0`을 유지할지, 문서 기준인 Spring Boot `3.x`로 낮출지 결정 필요.
- 테스트 DB를 H2로 둘지, 로컬 MySQL 전용으로 둘지 결정 필요.
- 로컬 MySQL DB명과 계정은 환경변수로 주입해야 한다.
- QueryDSL 버전은 Spring Boot/Jakarta 버전에 맞춰 결정한다.
- 실제 공공데이터 API URL/API Key는 아직 없다.

## 13. 다음 세션 시작 절차

1. `codex/WORKING_MEMORY.md` 읽기.
2. `codex/codex.md` 읽기.
3. `build.gradle`, `application.yaml`, `src/main/java` 구조 확인.
4. 구현 전 현재 변경사항이 사용자 변경인지 확인.
5. 코드 변경은 테스트 먼저 작성하고 진행.

## 14. 2026-06-15 구현 기록

- `codex/IMPLEMENTATION_PLAN.md`를 추가했다.
- `POST /api/products/analyze` MVP 고정 응답 API를 구현했다.
- 추가 파일:
  - `src/main/java/com/example/gonggong/domain/analysis/RiskLevel.java`
  - `src/main/java/com/example/gonggong/domain/analysis/controller/ProductAnalyzeController.java`
  - `src/main/java/com/example/gonggong/domain/analysis/dto/ProductAnalyzeRequest.java`
  - `src/main/java/com/example/gonggong/domain/analysis/dto/ProductAnalyzeResponse.java`
  - `src/main/java/com/example/gonggong/domain/analysis/dto/MatchedRecallDto.java`
  - `src/main/java/com/example/gonggong/domain/analysis/service/ProductAnalyzeService.java`
  - `src/test/java/com/example/gonggong/domain/analysis/controller/ProductAnalyzeControllerTest.java`
- TDD 기록:
  - 먼저 `ProductAnalyzeControllerTest`를 작성했다.
  - `./gradlew.bat test --tests com.example.gonggong.domain.analysis.controller.ProductAnalyzeControllerTest`는 구현 전 `ProductAnalyzeService` 미존재로 실패했다.
  - 최소 구현 후 같은 테스트가 통과했다.
- 최종 검증:
  - `./gradlew.bat compileJava`: 성공
  - `./gradlew.bat test`: 성공
- 다음 권장 작업:
  - Entity와 `BaseTimeEntity` 추가
  - 로컬 MySQL 기반 JPA 매핑 확인
  - QueryDSL custom repository 뼈대 추가
  - 샘플 리콜/위해 성분 데이터 추가

## 15. 2026-06-15 전역 구조 및 예외 처리 구현 기록

- 사용자 지시에 따라 루트 프로젝트 기준으로 `global`과 `domain` 구조를 유지한다.
- `global`은 전역 공통 클래스 집합이다.
- `domain`은 세부 도메인 클래스 집합이다.
- 추가 파일:
  - `src/main/java/com/example/gonggong/global/common/BaseTimeEntity.java`
  - `src/main/java/com/example/gonggong/global/config/JpaAuditingConfig.java`
  - `src/main/java/com/example/gonggong/global/exception/BaseCode.java`
  - `src/main/java/com/example/gonggong/global/exception/GlobalErrorCode.java`
  - `src/main/java/com/example/gonggong/global/exception/CustomException.java`
  - `src/main/java/com/example/gonggong/global/exception/DataInitializationException.java`
  - `src/main/java/com/example/gonggong/global/exception/ErrorResponse.java`
  - `src/main/java/com/example/gonggong/global/exception/ExceptionAdvice.java`
  - `src/main/java/com/example/gonggong/domain/analysis/exception/AnalysisErrorCode.java`
  - `src/test/java/com/example/gonggong/global/common/BaseTimeEntityTest.java`
  - `src/test/java/com/example/gonggong/global/exception/ExceptionAdviceTest.java`
- 예외 처리 구조:
  - `BaseCode`는 `HttpStatus`, `code`, `message`를 제공하는 전역 인터페이스다.
  - 도메인 에러 코드는 `domain/{domainName}/exception` 아래에서 `BaseCode`를 구현한다.
  - 전역 예외는 `CustomException`으로 던지고 `ExceptionAdvice`가 JSON 응답으로 변환한다.
  - 응답 형식은 `status`, `code`, `message`다.
- `BaseTimeEntity`:
  - `@MappedSuperclass`
  - `@EntityListeners(AuditingEntityListener.class)`
  - `createdAt`, `updatedAt` 필드
  - `JpaAuditingConfig`에서 `@EnableJpaAuditing` 활성화
- TDD 기록:
  - 먼저 `ExceptionAdviceTest`, `BaseTimeEntityTest`를 작성했다.
  - 구현 전 `AnalysisErrorCode` 미존재로 실패했다.
  - 최소 구현 후 대상 테스트가 통과했다.
- 최종 검증:
  - `./gradlew.bat compileJava`: 성공
  - `./gradlew.bat test`: 성공
- 다음 권장 작업:
  - `RecallProduct`, `HarmfulIngredient`, `ProductAnalysisLog` Entity 추가
  - 각 Entity에 MySQL 인덱스 반영
  - Repository와 QueryDSL custom repository 추가

## 16. 2026-06-15 Chrome Extension MVP 구현 기록

- 사용자 요청에 따라 백엔드 Entity 구현 전에 Chrome Extension MVP를 먼저 추가했다.
- 추가 디렉터리: `extension/`
- 추가 파일:
  - `extension/manifest.json`
  - `extension/background.js`
  - `extension/content.js`
  - `extension/overlay.css`
  - `extension/popup.html`
  - `extension/popup.css`
  - `extension/popup.js`
- 구조:
  - `content.js`: 알리익스프레스/테무 상품 페이지에서 DOM 정보를 추출하고, 화면 우측 상단 배지와 상세 패널을 표시한다.
  - `background.js`: `http://localhost:8080/api/products/analyze`로 POST 요청을 보낸다.
  - `overlay.css`: 위험도별 배지 색상과 팝업 UI를 담당한다.
  - `popup.*`: 확장 아이콘 클릭 시 간단한 안내 화면을 표시한다.
- 전송 payload:
  - `productName`
  - `description`
  - `imageUrl`
  - `pageUrl`
  - `site`
- 지원 도메인:
  - `*.aliexpress.com`
  - `*.temu.com`
- 백엔드 전제:
  - 로컬 서버가 `http://localhost:8080`에서 실행 중이어야 한다.
  - 현재 백엔드는 `POST /api/products/analyze` 고정 응답 API가 구현되어 있다.
- 검증:
  - `node --check extension/background.js`: 성공
  - `node --check extension/content.js`: 성공
  - `node --check extension/popup.js`: 성공
  - `manifest.json` JSON parse: 성공
- 수동 확인 절차:
  1. 백엔드 실행: `./gradlew.bat bootRun`
  2. Chrome에서 `chrome://extensions` 접속
  3. Developer mode 활성화
  4. Load unpacked 클릭
  5. 프로젝트의 `extension/` 폴더 선택
  6. 알리익스프레스 또는 테무 상품 상세 페이지 접속
  7. 우측 상단 삼각형 배지와 hover/click 팝업 확인
- 2026-06-15 추가 UI 동작:
  - 배지는 삼각형 안에 느낌표가 있는 SVG 아이콘으로 변경했다.
  - 배지 위치는 `top: 72px`, `right: 24px`로 조정했다.
  - 프리뷰 모드: 배지에 마우스를 올리면 상세 팝업이 배지 옆에 표시되고, 배지/팝업 영역에서 마우스가 벗어나면 자동으로 사라진다.
  - 고정 모드: 배지를 클릭하면 팝업이 고정되고, 닫기 버튼을 누르기 전까지 유지된다.
  - 상세 팝업에 `셀러이신가요?` 버튼을 추가했다.
  - `셀러이신가요?` 버튼 클릭 시 새 탭이 아니라 현재 상품 페이지 위에 셀러 안내 모달이 뜬다.
  - 셀러 모달은 스크롤형 안내 UI이며, 셀러가 수입/판매 전 해야 하는 작업 흐름을 카드와 타임라인으로 보여준다.
  - 1.6 UI 조건에 맞춰 배지, 상세 팝업, 셀러 버튼, 셀러 모달에 둥근 모서리와 부드러운 그림자 효과를 적용했다.
  - 이전 새 탭 방식으로 만든 `extension/seller.html`, `extension/seller.css`는 제거했다.

## 17. 2026-06-15 실물 경제 기반 수요 분석 MVP 구현 기록

- 구현 목적:
  - 최근 수입 물동량/수입액이 급증한 HSK 품목을 계산해 TOP 10 집중 관리 품목으로 반환한다.
  - 산업통상자원부/KOTRA/한국산업단지공단 공공데이터 API 연동 전, 동일한 형태의 로컬 샘플 데이터와 계산 API를 먼저 구현했다.
- 추가 API:
  - `GET /api/demand/priority-items/top10`
- 추가 도메인:
  - `domain/demand`
- 추가 파일:
  - `src/main/java/com/example/gonggong/domain/demand/entity/ImportTrend.java`
  - `src/main/java/com/example/gonggong/domain/demand/entity/EssentialIndustryItem.java`
  - `src/main/java/com/example/gonggong/domain/demand/repository/ImportTrendReader.java`
  - `src/main/java/com/example/gonggong/domain/demand/repository/EssentialIndustryItemReader.java`
  - `src/main/java/com/example/gonggong/domain/demand/repository/ImportTrendRepository.java`
  - `src/main/java/com/example/gonggong/domain/demand/repository/EssentialIndustryItemRepository.java`
  - `src/main/java/com/example/gonggong/domain/demand/dto/DemandPriorityItemResponse.java`
  - `src/main/java/com/example/gonggong/domain/demand/dto/DemandPriorityTop10Response.java`
  - `src/main/java/com/example/gonggong/domain/demand/service/DemandPriorityService.java`
  - `src/main/java/com/example/gonggong/domain/demand/controller/DemandPriorityController.java`
  - `src/main/java/com/example/gonggong/domain/demand/initializer/DemandSampleDataInitializer.java`
  - `src/test/java/com/example/gonggong/domain/demand/service/DemandPriorityServiceTest.java`
  - `src/test/java/com/example/gonggong/domain/demand/controller/DemandPriorityControllerTest.java`
- Entity 설계:
  - `ImportTrend`: HSK 코드, 품목명, 기준월, 수입액, 수입중량 저장
  - `EssentialIndustryItem`: HSK 코드, 품목명, 산업명, 지역명 저장
  - 둘 다 `BaseTimeEntity` 상속
- MySQL 주의사항:
  - `year_month`는 MySQL `YEAR_MONTH` 키워드와 충돌해 DDL 오류가 발생했다.
  - Java 필드명 `yearMonth`는 유지하되 실제 컬럼명은 `period_ym`으로 지정했다.
- 점수 계산:
  - 수입액 증가율 점수 최대 35
  - 수입중량 증가율 점수 최대 35
  - 필수 산업 품목 가중치 15
  - 과거 리콜 빈도 가중치 최대 15
  - 현재 리콜 도메인이 아직 없으므로 `recallCount`는 0으로 반환한다.
- 샘플 데이터:
  - `DemandSampleDataInitializer`가 `import_trend`, `essential_industry_item`이 비어 있을 때 샘플 데이터를 적재한다.
  - 현재 샘플 기준 최신월은 `2026-05`, 전년 동월은 `2025-05`다.
- TDD 기록:
  - `DemandPriorityServiceTest`를 먼저 작성했고 demand 클래스 미존재로 실패를 확인했다.
  - `DemandPriorityControllerTest`로 API 계약을 고정했다.
  - 구현 후 대상 테스트가 통과했다.
- 최종 검증:
  - `./gradlew.bat compileJava`: 성공
  - `./gradlew.bat test`: 성공
- 다음 권장 작업:
  - 확장 프로그램 상세 팝업 또는 셀러 모달에서 `GET /api/demand/priority-items/top10` 호출
  - `[집중 관리 품목] TOP 10` 섹션 표시
  - 리콜 도메인 구현 후 리콜 빈도 가중치 연결
  - 실제 공공데이터 API 수집/동기화 구현

## 18. 2026-06-15 Extension 집중 관리 품목 TOP 10 연동 기록

- 기존 상세 팝업 안에는 `[집중 관리 품목 TOP 10]` 버튼만 둔다.
- 위치는 `셀러이신가요?` 버튼 바로 위다.
- 버튼을 클릭하면 현재 페이지 위에 집중 관리 품목 전용 모달이 뜬다.
- `extension/background.js`:
  - `GET_DEMAND_PRIORITY_TOP10` 메시지 추가
  - `GET http://localhost:8080/api/demand/priority-items/top10` 호출
- `extension/content.js`:
  - 집중 관리 품목 버튼을 클릭할 때 TOP 10 목록을 조회한다.
  - 전용 모달의 `새로고침` 버튼으로 다시 조회할 수 있다.
  - 백엔드가 꺼져 있거나 API 오류가 나면 상태 메시지에 오류를 표시한다.
- `extension/overlay.css`:
  - TOP 10 리스트 카드 스타일 추가
  - 점수와 priority level에 따라 HIGH/MEDIUM 색상 강조
  - TOP 10 전용 모달 스타일 추가

## 19. 2026-06-17 상세 팝업 대표 이미지 카드 추가 기록

- 상세 팝업 상단에 대표 이미지 카드(`isg-product-card`)를 추가했다.
- `content.js`:
  - `imageUrl`이 있으면 대표 이미지를 표시한다.
  - `imageUrl`이 없으면 fallback 텍스트 `이미지 없음`을 표시한다.
- `overlay.css`:
  - 이미지 카드의 둥근 모서리와 부드러운 그림자 효과를 추가했다.
  - 모바일에서는 카드 높이를 줄였다.
- 이미지 노출 위치:
  - 상세 팝업의 제목 영역 위
  - 위험도/카테고리 등의 정보보다 상단
- 검증:
  - `node --check extension/background.js`: 성공
  - `node --check extension/content.js`: 성공
  - `node --check extension/popup.js`: 성공
  - `manifest.json` JSON parse: 성공
## 20. 2026-06-17 Image selection update

- Product image extraction now prefers the largest visible `img` on screen.
- It falls back to `currentSrc`, `src`, `data-src`, and finally `og:image`.

## 21. 2026-06-17 Image heuristic refinement

- Added stronger filtering for decorative images like logos, banners, icons, and sticky/fixed UI.
- Added product-context bonus for images inside product/gallery/main/detail containers.
- Selector-matched images now outrank generic `img` fallbacks.

## 22. 2026-06-17 Title proximity bonus

- Image selection now prefers images that live near the detected product title element.
- This is meant to suppress large unrelated banners that happen to occupy more viewport area.

## 23. 2026-06-17 AliExpress scoped image selection

- AliExpress now tries to resolve a local image scope from the product title's popup/modal/container first.
- If a scope is found, only images inside that container are considered before falling back to the full page.

## 24. 2026-06-17 Product name filtering

- Product name extraction now rejects obvious site-brand/page-title values like `AliExpress`.
- This prevents the image card alt text from showing the site name when the real product title is not ready yet.

## 25. 2026-06-20 최종 PDF 반영 후 기능 방향 변경 기록

- `codex/공공데이터_활용_공모전_수정3.pdf` 최종 수정본을 분석해 `codex/codex.md`를 C-Entry 전체 시스템 README로 재작성했다.
- 최종 방향:
  - 기존 `집중 관리 품목 TOP 10`은 확장 프로그램 UI 요구사항에서 제외한다.
  - 상세 팝업에는 `이 브랜드의 다른 제품 리콜 이력` 기능을 우선 배치한다.
  - 셀러 영역은 단순 안내 화면과 별도로 `물품 리스크 예측` 대시보드를 제공한다.
- 주의:
  - `domain/demand`의 `GET /api/demand/priority-items/top10` 백엔드 코드는 남아 있지만, 확장 프로그램에서는 더 이상 호출하지 않는다.
  - 공공데이터 API는 아직 연결하지 않고 더미데이터로 화면과 JSON 계약을 먼저 맞춘다.

## 26. 2026-06-20 브랜드 리콜 이력 더미 API 및 Extension 교체 기록

- 구현 목적:
  - PDF의 `제품 브랜드 관련 리콜 이력` 요구사항에 맞춰 TOP10 UI를 제거하고 브랜드 리콜 이력 기능으로 대체했다.
  - 현재는 공공데이터 API 호출 없이 더미데이터로 동작한다.
- 추가 API:
  - `GET /api/brands/{brandName}/recalls`
- 추가 도메인:
  - `domain/brand`
- 추가 파일:
  - `src/main/java/com/example/gonggong/domain/brand/controller/BrandRecallController.java`
  - `src/main/java/com/example/gonggong/domain/brand/dto/BrandRecallResponse.java`
  - `src/main/java/com/example/gonggong/domain/brand/dto/BrandRecallItemResponse.java`
  - `src/main/java/com/example/gonggong/domain/brand/service/BrandRecallService.java`
  - `src/test/java/com/example/gonggong/domain/brand/controller/BrandRecallControllerTest.java`
- 더미 브랜드 판정:
  - 브랜드명에 `Example`, `Risk`, `위험`이 포함되면 리콜 이력이 있는 것으로 처리한다.
  - 그 외 브랜드는 리콜 이력이 없는 것으로 처리하고 `리콜 이력이 존재하지 않습니다.`를 반환한다.
- 현재 더미 리콜 항목:
  - `Example Brand 전기 온열기` / `온도 상승 시험 기준 초과` / `2025-11-18`
  - `Example Brand 무선 마우스` / `전자파 적합성 기준 부적합` / `2025-09-02`
  - `Example Brand 어린이 완구` / `프탈레이트계 가소제 기준치 초과` / `2025-04-07`
  - `Example Brand LED 조명` / `절연 내력 부적합` / `2024-12-21`
- Extension 변경:
  - `extension/background.js`
    - `GET_DEMAND_PRIORITY_TOP10` 메시지 제거
    - `GET_BRAND_RECALLS` 메시지 추가
    - `GET http://localhost:8080/api/brands/{brandName}/recalls` 호출
  - `extension/content.js`
    - 상품 payload에 `brandName` 추출값 추가
    - `집중 관리 품목 TOP 10` 버튼/모달 제거
    - `이 브랜드의 다른 제품 리콜 이력` 버튼/모달 추가
    - 리콜 이력이 있으면 버튼을 빨간 상태로, 없으면 파란 상태로 표시
  - `extension/overlay.css`
    - TOP10 관련 `isg-demand-*` 스타일 제거
    - 브랜드 리콜 버튼/모달/list 스타일 추가
- 검증:
  - `node --check extension/content.js`: 성공
  - `node --check extension/background.js`: 성공
  - `./gradlew.bat test --tests com.example.gonggong.domain.brand.controller.BrandRecallControllerTest`: 성공
  - `./gradlew.bat test`: 성공

## 27. 2026-06-20 물품 리스크 예측 버튼 및 더미 대시보드 기록

- 구현 목적:
  - 사용자가 요청한 `물품리스크예측` 내용을 `제품리콜이력`과 `셀러이신가요?` 사이의 별도 버튼으로 삽입했다.
  - 버튼 위치는 상세 팝업 기준 다음 순서다.

```text
이 브랜드의 다른 제품 리콜 이력
물품 리스크 예측
셀러이신가요?
```

- 현재 구현 방식:
  - `물품 리스크 예측`은 아직 백엔드 API를 호출하지 않는다.
  - `extension/content.js` 안에 정적 더미 대시보드로 구현했다.
  - 추후 `POST /api/seller/risk-dashboard`, `GET /api/seller/certifications/ktl` 더미 API로 분리하는 것이 권장된다.
- 더미 대시보드 항목:
  - 리콜 가능성: `주의`
    - 최근 3개년 동종 품목 리콜 4건, 최신 공표일 `2025-09-02`
  - 관세 및 사후추징금: `검토 필요`
    - 기본 관세율 8%, 신고가격 기준 예상 관세액 산정 필요
  - KC 인증 누락: `위험`
    - 어린이제품 안전확인 또는 전자파 적합성평가 대상 가능성
  - 행정처분 및 형사처벌: `검토 필요`
    - 성분표 미확인으로 규제 성분 판단 불가
- 더미 HSK Code 추론:
  - 상품명에 `마우스` 또는 `mouse` 포함: `HSK 8471601030`
  - 상품명에 `완구` 또는 `toy` 포함: `HSK 9503003490`
  - 상품명에 `온열` 또는 `heater` 포함: `HSK 8516299000`
  - 그 외: `HSK 3924100000`
- KTL 인증 요건 가이드:
  - 현재는 `어린이제품 안전확인` 기준 정적 더미 표시
  - 표시 항목:
    - 인증 종류명
    - 근거 법령
    - 시험 항목
    - 준비 서류
    - 예상 기간/수수료
    - Action Item 안내
- 검증:
  - `node --check extension/content.js`: 성공

## 28. 다음 권장 작업

- 실제 알리/테무 페이지에서 브랜드명 추출 정확도 확인
- `물품 리스크 예측` 모달 UI 스크린샷 기반 QA
- 상품명별 더미 리스크 내용을 세분화
  - `마우스`: 전자파 적합성평가/KC 리스크 중심
  - `식기`: 어린이제품/유해성분 리스크 중심
  - `온열기`: 전기용품 안전인증 리스크 중심
  - `완구`: 어린이제품 안전확인 리스크 중심
- `물품 리스크 예측` 더미 로직을 백엔드 API로 분리
  - `POST /api/seller/risk-dashboard`
  - `GET /api/seller/certifications/ktl?hskCode=...`
- 공공데이터 API 연결은 이후 단계로 미룬다.

## 29. 2026-06-20 env 디렉토리 기반 키 관리 정책

- 사용자가 공공데이터 API 연결을 시작하기 위해 `env/` 디렉토리에 키를 두고 `application.yaml`은 env 값을 읽는 방식으로 진행하기로 했다.
- 실제 비밀값 파일은 Git에 올리지 않는다.
- 추가 파일:
  - `env/.env.example`
- ignore 정책:
  - `.gitignore`에 `env/*.env`, `env/*.properties`를 추가했다.
  - `env/.env.example`만 추적 가능하도록 예외 처리했다.
- Spring 설정:
  - `src/main/resources/application.yaml`에 다음 import를 추가했다.

```yaml
spring:
  config:
    import: optional:file:env/local.env[.properties]
```

- 실제 로컬 키 파일 경로:
  - `env/local.env`
- `env/local.env` 예시 형식:

```properties
DB_URL=jdbc:mysql://localhost:3306/gonggong?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USER=root
DB_PASSWORD=실제_DB_비밀번호
SAFETY_KOREA_API_KEY=실제_키
CUSTOMS_API_KEY=실제_키
CHEMICAL_API_KEY=실제_키
```

- `application.yaml`에서 현재 읽는 값:
  - `DB_URL`
  - `DB_USER`
  - `DB_PASSWORD`
  - `SAFETY_KOREA_API_KEY`
  - `SAFETY_KOREA_API_URL`
  - `CUSTOMS_API_KEY`
  - `UNIPASS_URL`
  - `CHEMICAL_API_KEY`
  - `PRODUCT_SAFETY_CENTER_URL`
- 기존 `application.yaml`에 직접 들어가 있던 DB 비밀번호는 제거했다.

## 30. 2026-06-20 예외 처리 정책 고정

- 사용자 지시:
  - 서비스/도메인 예외는 무조건 커스텀 예외로 처리한다.
  - `IllegalStateException`, `RuntimeException` 등을 서비스에서 직접 던지지 않는다.
- 현재 공통 구조:
  - `global/exception/BaseCode.java`
  - `global/exception/CustomException.java`
  - `global/exception/ExceptionAdvice.java`
  - `global/exception/ErrorResponse.java`
- 도메인 예외 작성 규칙:
  - 도메인별 `XxxErrorCode implements BaseCode`를 만든다.
  - 필요하면 `XxxException extends CustomException`을 만든다.
  - 서비스에서 비즈니스/도메인 예외는 `new XxxException(XxxErrorCode.SOME_CODE)`로 던진다.
  - 외부 API 실패도 해당 도메인 ErrorCode로 감싼다.
  - 초기화/시스템 공통 장애만 `GlobalErrorCode`를 사용한다.
- 이번 정리:
  - `DemandPriorityService`의 직접 `IllegalStateException` 사용을 제거했다.
  - 추가 파일:
    - `src/main/java/com/example/gonggong/domain/demand/exception/DemandErrorCode.java`
    - `src/main/java/com/example/gonggong/domain/demand/exception/DemandException.java`
  - `DemandErrorCode.IMPORT_TREND_DATA_EMPTY` 추가
  - `DemandPriorityServiceTest`에 빈 수입 추세 데이터일 때 `DemandException`이 발생하는 테스트 추가
- 현재 프로덕션 코드에서 `new IllegalStateException`, `new RuntimeException`, `throw new RuntimeException`, `throw new IllegalStateException` 직접 사용 없음 확인.
- env 전환 후 전체 테스트가 실제 MySQL 비밀번호에 의존하지 않도록 테스트 환경을 분리했다.
  - `build.gradle`에 `testRuntimeOnly 'com.h2database:h2'` 추가
  - `src/test/resources/application.yaml` 추가
  - 테스트는 H2 인메모리 DB(`jdbc:h2:mem:gonggong-test`)와 `H2Dialect`를 사용한다.
- 검증:
  - `./gradlew.bat test --tests com.example.gonggong.domain.demand.service.DemandPriorityServiceTest`: 성공
  - `./gradlew.bat test`: 성공

## 31. 2026-06-20 OpenAI 상품명 정제 호출 기반

- 목표:
  - 공공데이터 리콜 API를 바로 호출하기 전에, 알리/테무 DOM에서 가져온 원문 상품 데이터를 OpenAI API로 한 번 정제한다.
  - 정제 결과를 이후 국가기술표준원 리콜 데이터 검색 키워드/카테고리 매칭에 사용한다.
- 현재 범위:
  - OpenAI API 호출 기반과 테스트만 구현했다.
  - 아직 `ProductAnalyzeService`, 리콜 API, 확장 UI의 리스크 대시보드 더미 데이터에는 연결하지 않았다.
- 키 관리:
  - 실제 키는 코드에 박지 않는다.
  - `env/local.env`에 `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_RESPONSES_API_URL`을 둔다.
  - `env/.env.example`에 OpenAI 관련 예시 값을 추가했다.
  - `application.yaml`은 `${OPENAI_API_KEY:}` 방식으로 env 값을 읽는다.
- 추가/수정 파일:
  - `build.gradle`
    - `com.fasterxml.jackson.core:jackson-databind` 추가
  - `src/main/resources/application.yaml`
    - `openai.api-key`
    - `openai.model`
    - `openai.responses-api-url`
  - `src/main/java/com/example/gonggong/global/config/JacksonConfig.java`
    - FasterXML `ObjectMapper` Bean 추가
  - `src/main/java/com/example/gonggong/domain/analysis/openai/ProductNormalizePromptBuilder.java`
  - `src/main/java/com/example/gonggong/domain/analysis/openai/OpenAiProductNormalizeClient.java`
  - `src/main/java/com/example/gonggong/domain/analysis/openai/OpenAiTransport.java`
  - `src/main/java/com/example/gonggong/domain/analysis/openai/HttpOpenAiTransport.java`
  - `src/main/java/com/example/gonggong/domain/analysis/openai/OpenAiProperties.java`
  - `src/main/java/com/example/gonggong/domain/analysis/openai/ProductNormalizeResult.java`
- 프롬프트 허용 카테고리:
  - `유아용 식기류`
  - `완구류`
  - `전기 온열기기`
  - `조명기구`
  - `컴퓨터 주변기기`
  - `생활화학제품`
  - `피부 접촉 제품`
  - `주방용품`
  - `기타`
- OpenAI 응답 스키마:
  - `standardProductName`
  - `searchKeywords`
  - `brandName`
  - `category`
  - `materialKeywords`
  - `targetUser`
  - `riskIngredientKeywords`
  - `hskCandidateKeywords`
  - `confidence`
- API 방식:
  - OpenAI Responses API를 호출한다.
  - `text.format.type = json_schema` 기반 Structured Outputs 형식으로 JSON 응답을 강제한다.
- 예외 처리:
  - 사용자 정책대로 직접 `RuntimeException`을 던지지 않고 `AnalysisException`으로 감싼다.
  - 추가 ErrorCode:
    - `OPENAI_API_KEY_MISSING`
    - `OPENAI_API_FAILED`
    - `OPENAI_RESPONSE_PARSE_FAILED`
- 테스트:
  - `ProductNormalizePromptBuilderTest`
    - 허용 카테고리와 DOM 원문 데이터가 프롬프트에 들어가는지 검증
  - `OpenAiProductNormalizeClientTest`
    - Responses API 요청 body 생성 검증
    - Structured Output 응답 파싱 검증
    - API 키 누락 시 `AnalysisException` 검증
- 검증:
  - `./gradlew.bat test --tests com.example.gonggong.GonggongApplicationTests`: 성공
  - `./gradlew.bat test --tests com.example.gonggong.domain.analysis.openai.*`: 성공
  - `node --check extension/content.js`: 성공
  - `node --check extension/background.js`: 성공
  - `./gradlew.bat test`: 성공

## 31-1. 2026-06-22 로컬 API 키 반영

- 실제 실행용 `env/local.env`를 생성했다.
- `SAFETY_KOREA_API_KEY`는 제품안전정보센터에서 받은 실제 키로 설정했다.
- `OPENAI_API_KEY`는 사용자가 넣어둔 값을 `env/local.env`에 유지했다.
- `env/.env.example`에 실제 DB 비밀번호/OpenAI 키가 들어가 있던 상태를 placeholder로 되돌렸다.
- 보안 메모:
  - 실제 키 값은 문서와 샘플 파일에 기록하지 않는다.
  - `env/local.env`는 `.gitignore`의 `env/*.env` 규칙 대상이다.
- 확인:
  - `SAFETY_KOREA_API_KEY=SET`
  - `OPENAI_API_KEY=SET`
  - `OPENAI_MODEL=SET`
  - `OPENAI_RESPONSES_API_URL=SET`
- 제한:
  - 현재 작업 디렉토리가 Git 저장소로 인식되지 않아 `git status`, `git check-ignore`는 실행 실패했다.
  - `.gitignore` 파일 자체에는 `env/*.env`, `env/*.properties`, `!env/.env.example` 규칙이 존재한다.

## 32. 다음 구현 순서 후보

- `ProductAnalyzeService` 앞단에서 `OpenAiProductNormalizeClient.normalize(...)`를 호출한다.
- 기존 더미 위험도 계산 입력값을 원문 상품명 대신 `ProductNormalizeResult` 기반으로 바꾼다.
- 그 다음 단계에서 국가기술표준원 리콜 API 클라이언트를 추가한다.
- 리콜 API 매칭 기준은 우선 `category + searchKeywords + brandName` 조합으로 시작한다.
- 외부 API 실패는 모두 `AnalysisException` 또는 리콜 전용 커스텀 예외로 감싼다.

## 33. 2026-06-22 SafetyKorea Open API 응답 구조 확인

- 공식 페이지:
  - `https://www.safetykorea.kr/release/openapi`
  - `https://www.safetykorea.kr/release/openapi2`
- 공식 설명:
  - KC인증정보, 국내리콜정보, 국외리콜정보 데이터를 Open API로 제공한다.
- 인증 방식:
  - 쿼리 파라미터가 아니라 HTTP Header에 넣는다.
  - Header name: `AuthKey`
  - Header value: 제품안전정보센터에서 발급받은 서비스 ID/API 키
- 리콜 리스트 API:
  - `https://www.safetykorea.kr/openapi/api/recall/recallList.json?conditionKey={검색구분}&conditionValue={검색어}`
  - JSON/XML은 URL 확장자 `.json`, `.xml`로 선택 가능하다.
- 리콜 상세 API:
  - `https://www.safetykorea.kr/openapi/api/recall/recallDetail.json?recallUid={리콜아이디}`
- 리콜 검색 `conditionKey` 후보:
  - `all`: 전체
  - `barcodeNum`: 바코드
  - `recallProductName`: 제품명
  - `recallBrandName`: 상표명/브랜드명
  - `recallModelName`: 모델명
  - `certNum`: 인증번호
  - `publishDate`: 공표일자
  - `fRecallUid`: 국외리콜 아이디 검색조건으로 문서에 언급됨
- 공통 응답 envelope:
  - `resultCode`
  - `resultMsg`
  - `resultData`
- 실제 샘플 호출:
  - 요청: `recallList.json?conditionKey=recallProductName&conditionValue=완구`
  - HTTP status: `200`
  - `resultCode`: `2000`
  - `resultMsg`: `Success`
  - `resultData`: 배열
  - 조회 건수: `645`
- 리콜 리스트/상세 주요 필드:
  - `recallUid`
  - `recallProductName`
  - `recallBrandName`
  - `recallModelName`
  - `recallModelCnt`
  - `recallTypeName`
  - `recallMeans`
  - `barcodeNum`
  - `categoryName`
  - `certNum`
  - `productItemName`
  - `recallCmpnyDivName`
  - `recallInqryTel`
  - `recallCmpnyName`
  - `recallCmpnySsn`
  - `recallFrgnCmpnyName`
  - `makerCntryName`
  - `makerName`
  - `makingCntryName`
  - `publishDate`
  - `publishRecallVol`
  - `recallActionAmt`
  - `recallStaDate`
  - `recallEndDate`
  - `harmDscr`
  - `accidentCaseDscr`
  - `publishActionDscr`
  - `recallFiles`
- 실제 샘플 데이터 특징:
  - `publishDate`는 `yyyyMMdd` 문자열이다.
  - `categoryName` 예: `어린이>완구`
  - `productItemName` 예: `어린이용품>완구`
  - `makingCntryName` 예: `국외>아시아지역>중국`
  - `harmDscr`에는 결함/위해 성분 기준치 초과 내용이 문장형 문자열로 들어온다.
  - `accidentCaseDscr`에는 위해 가능성/사고 사례 설명이 들어온다.
  - `publishActionDscr`에는 소비자 조치 안내가 들어온다.
- 구현 메모:
  - 리콜 위험도 더미 교체 시 1차 매칭은 `recallProductName`, `recallBrandName`, `categoryName`, `productItemName`, `harmDscr` 중심으로 한다.
  - 브랜드 리콜 이력은 `conditionKey=recallBrandName&conditionValue={brandName}`로 조회한다.
  - 상품 리콜 가능성은 GPT 정제 결과의 `searchKeywords`를 `recallProductName`으로 순차 조회하고 결과를 합치는 방식이 적합하다.
  - 중복 제거 기준은 `recallUid`가 적합하다.
  - 최신순 정렬은 `publishDate` 문자열을 날짜로 파싱해 내림차순 처리한다.
  - API 실패/파싱 실패/인증키 누락은 직접 `RuntimeException`을 던지지 말고 커스텀 예외로 감싼다.

## 34. 2026-06-22 실데이터 기반 리콜 매칭 파이프라인 작업 계획

- 샘플 JSON 기반 판단은 중단하고, 크롬 익스텐션이 DOM에서 가져온 상품 제목/설명 데이터를 기준으로 실시간 분석한다.
- 목표 흐름:
  1. DOM 상품 제목/설명/URL을 `POST /api/products/analyze`로 전달한다.
  2. OpenAI가 상품명을 한국 리콜 API 검색에 맞게 정제하고, `recallProductName` 후보 기준으로 분류한다.
  3. SafetyKorea 국내리콜정보 조회 API를 `recallProductName` 중심으로 호출한다.
  4. 필요 시 브랜드명이 있으면 `recallBrandName` 조회도 병행한다.
  5. 결과는 `recallUid` 기준으로 중복 제거한다.
  6. `recallProductName`, `harmDscr`, `accidentCaseDscr`, `publishActionDscr`를 분석해 카테고리, 리콜사유, 위해성분, 매칭리콜을 만든다.
- `recallProductName`은 실제 데이터에서 카테고리 역할을 하는 핵심 필드로 사용한다.
  - 예: `가정용섬유제품(책가방)` -> `생활용품>가방/섬유제품`
  - 예: `운동완구(완구)`, `미술공예 완구(완구)` -> `어린이용품>완구`
- `categoryName`, `productItemName`, `harmDscr`는 오래된 국내리콜 데이터에서 `null`일 수 있으므로 보조 필드로만 사용한다.
- 구현 대상:
  - `SafetyKoreaRecallClient`: SafetyKorea 국내리콜정보 조회 API 호출
  - `RecallProductNameClassifier`: `recallProductName` 원문을 서비스용 카테고리로 매핑
  - `HarmfulIngredientExtractor`: `harmDscr`/`accidentCaseDscr`에서 위해성분 추출
  - `RecallMatchService`: OpenAI 정제 결과와 리콜 조회 결과를 합쳐 매칭 점수/위험도/응답 생성
  - `ProductNormalizePromptBuilder`/`ProductNormalizeResult`: `matchedRecallProductName`, `serviceCategory` 중심으로 프롬프트와 Structured Output 스키마 수정
- 국내/국외 리콜:
  - MVP는 국내리콜정보 조회/상세 조회부터 붙인다.
  - 알리/테무 상품 커버리지를 높이기 위해 이후 국외리콜정보 조회를 같은 구조로 추가한다.

## 35. 2026-06-22 실데이터 리콜 분석 파이프라인 구현 현황

- 샘플 JSON/더미 응답 기반 분석을 제거하고 실제 호출 흐름으로 전환했다.
- 현재 `POST /api/products/analyze` 흐름:
  1. 크롬 익스텐션이 알리/테무 DOM에서 상품명, 설명, 이미지 URL, 페이지 URL, 사이트명을 추출한다.
  2. 백엔드 `ProductAnalyzeService`가 `OpenAiProductNormalizeClient`를 호출한다.
  3. OpenAI가 상품명을 SafetyKorea 리콜 검색에 맞게 정제한다.
  4. OpenAI 결과의 `searchKeywords`로 SafetyKorea 국내리콜정보 조회 API를 호출한다.
  5. 브랜드명이 있으면 `recallBrandName` 조회도 추가한다.
  6. 조회 결과는 `recallUid` 기준으로 중복 제거한다.
  7. `harmDscr`/`accidentCaseDscr`가 비어 있으면 `recallDetail.json?recallUid=...`로 상세 조회를 수행한다.
  8. 리콜 사유, 위해성분, 매칭리콜, 위험도를 계산해 익스텐션에 반환한다.
- 추가/수정된 주요 백엔드 구성:
  - `ProductNormalizer`
  - `OpenAiProductNormalizeClient`
  - `ProductNormalizePromptBuilder`
  - `ProductNormalizeResult`
  - `SafetyKoreaRecallClient`
  - `HttpSafetyKoreaRecallClient`
  - `SafetyKoreaRecallItem`
  - `RecallProductNameClassifier`
  - `HarmfulIngredientExtractor`
  - `ProductAnalyzeService`
- SafetyKorea 설정:
  - `SAFETY_KOREA_API_KEY`
  - `SAFETY_KOREA_RECALL_LIST_URL`
  - `SAFETY_KOREA_RECALL_DETAIL_URL`
- OpenAI 설정:
  - `OPENAI_API_KEY`
  - `OPENAI_MODEL`
  - `OPENAI_RESPONSES_API_URL`
- OpenAI Structured Output 결과에 추가한 동일성 판단 후보:
  - `matchedRecallProductName`
  - `modelName`
  - `barcodeNum`
  - `certNum`
- SafetyKorea 리콜 항목에서 추가로 보는 필드:
  - `recallModelName`
  - `barcodeNum`
  - `certNum`
  - `recallStaDate`
  - `recallEndDate`
  - 내부 추적용 `matchedQuery`

## 36. 2026-06-22 위험도 정책 정리

- 기존에는 리콜 결과가 하나라도 있으면 대부분 `DANGER`로 나오는 문제가 있었다.
- 현재 위험도는 동일 제품 근거, 구체 상품명 매칭, 위해성분 강도, 조치 완료성 정보를 함께 본다.
- `DANGER`:
  - 모델명, 바코드, KC 인증번호가 일치하는 경우
  - 브랜드와 모델/품목이 강하게 일치하고 위해성분이 있는 경우
  - 동일 제품 근거가 부족해도 구체 상품명 검색어로 리콜이 매칭되고 치명 위해성분이 있는 경우
- `WARNING`:
  - 동일 제품 근거는 약하지만 유사 품목/유사 모델 리콜에 위해성분이 있는 경우
  - 규격 미달, 표시사항 미비, 물리적 결함 등 리콜 사유가 있는 경우
- `REVIEW`:
  - 품목 수준 매칭만 있는 경우
  - 과거 리콜 이력이 있으나 환불, 교환, 수거, 조치 완료성 문구가 중심인 경우
- `NORMAL`:
  - 현재 검색 조건에서 매칭 리콜이 없는 경우
- 치명 위해성분 키워드:
  - `프탈레이트계 가소제`
  - `DEHP`
  - `DBP`
  - `DINP`
  - `DIBP`
  - `납`
  - `카드뮴`
  - `폼알데하이드`
  - `비스페놀A`
- 구체 상품명 검색어로 보는 예:
  - `키즈 안전 축구공`
  - `리얼 무전기가 되는 경찰특공대 세트`
  - `행복한 우리 학교 스퀴시북`
- 너무 넓은 품목명으로 보아 `DANGER` 승격 근거로 쓰지 않는 예:
  - `완구`
  - `책가방`
  - `식기`
  - `운동완구`
  - `가정용섬유제품`
  - `생활용품`

## 37. 2026-06-22 익스텐션 UI 변경사항

- 위해 위험도 옆에 작은 `?` 버튼을 추가했다.
- `?` 버튼 클릭 시 위험도 산정 흐름 설명 팝업을 표시한다.
- 설명 팝업 내용:
  - DOM 상품명에서 모델명, 브랜드, 바코드, 인증번호 후보 추출
  - GPT가 상품명을 SafetyKorea 리콜 검색어와 품목 후보로 정제
  - OpenAPI 리콜 결과를 모델명, 인증번호, 브랜드, 구체 상품명, 품목 순서로 대조
  - 리콜 사유에서 납, 카드뮴, 프탈레이트, DEHP 등 치명 위해성분 추출
  - `위험`, `주의`, `검토 필요`, `정상` 기준 설명
- 설명 팝업은 화면 아래로 잘리지 않도록 버튼 위쪽으로 열린다.
- 설명 팝업 자체에 최대 높이와 내부 스크롤을 적용했다.
- 분석 패널 전체에도 `max-height: calc(100vh - 96px)`와 세로 스크롤을 적용했다.
  - 리콜 사유/위해성분/매칭리콜 텍스트가 길어져도 하단 버튼 3개를 스크롤해서 볼 수 있다.
- `고정됨` 리본은 패널 내부가 아니라 패널 바깥 요소로 이동했다.
  - 패널 내부 스크롤과 분리된다.
  - `white-space: nowrap`, `width: max-content`, `display: inline-flex`로 가로 표시되도록 수정했다.
- 수정 파일:
  - `extension/content.js`
  - `extension/overlay.css`

## 38. 2026-06-22 검증 기록

- 위험도 정책 변경 후 테스트:
  - `.\gradlew.bat test`
  - 결과: `BUILD SUCCESSFUL`
  - 테스트 수: 26개
- 익스텐션 JS 문법 확인:
  - `node --check extension/content.js`
  - 결과: 성공
- 이후 UI CSS 조정 때마다 다음 검증을 반복했다.
  - `node --check extension/content.js`
  - `.\gradlew.bat test`

## 39. 2026-06-23 국외 리콜 포함

- 국내 리콜 검색 흐름에 국외 리콜 목록 API를 같이 붙였다.
- 사용 endpoint:
  - 국내 목록: `https://www.safetykorea.kr/openapi/api/recall/recallList.json`
  - 국내 상세: `https://www.safetykorea.kr/openapi/api/recall/recallDetail.json`
  - 국외 목록: `https://www.safetykorea.kr/openapi/api/recall/fRecallList.json`
- 국외 리콜은 문서에서 목록 API만 확인되어 상세 조회는 하지 않는다.
- 국외 응답 매핑:
  - `fRecallUid` -> 내부 `recallUid`
  - `violateDscr` -> 내부 `harmDscr`
  - `makerName` -> 내부 `recallCmpnyName` fallback
  - `imageUrl` -> 내부 `imageUrls`
  - `recallUrl` 또는 `recallurl` -> 내부 `sourceUrl`
- 내부 리콜 항목에 `RecallSource`를 추가했다.
  - `DOMESTIC`: 국내 리콜
  - `FOREIGN`: 국외 리콜
- 중복 제거 키는 `source|recallUid` 형태로 변경했다.
  - 국내/국외에서 같은 ID 문자열이 나와도 서로 다른 리콜로 유지한다.
- 분석 흐름:
  - GPT가 만든 `searchKeywords`로 국내 `searchByProductName`과 국외 `searchForeignByProductName`을 모두 호출한다.
  - 브랜드명이 있으면 국내 `searchByBrandName`과 국외 `searchForeignByBrandName`을 모두 호출한다.
  - 국내 결과만 사유가 비어 있을 때 `recallDetail.json`으로 상세 보강한다.
  - 국외 결과는 목록 응답의 `violateDscr`, `accidentCaseDscr`, `publishActionDscr`로 위험도와 매칭 리콜을 산정한다.
- 응답의 `matchedRecalls[]`에 `source` 필드를 추가했다.
  - 확장 팝업의 매칭 리콜 표시도 `[국내]`, `[국외]`를 붙인다.
- 검증:
  - `.\gradlew.bat test --tests com.example.gonggong.domain.analysis.service.ProductAnalyzeServiceTest`: `BUILD SUCCESSFUL`
  - `.\gradlew.bat test`: `BUILD SUCCESSFUL`
  - `node --check extension/content.js`: 성공

## 40. 2026-06-23 분석 실패 원인 및 방어 처리

- 증상:
  - 크롬 확장에서 `분석 실패`가 표시됐다.
  - API 요청 로그는 백엔드에 들어오고 있었다.
- 원인:
  - 국외 리콜 API를 추가한 뒤, 리콜 조회 중 하나라도 `AnalysisException`을 던지면 `/api/products/analyze` 전체가 실패했다.
  - 특히 국외 `fRecallList.json` 응답 실패/형식 차이/외부 API 오류가 국내 결과까지 죽일 수 있었다.
- 조치:
  - `ProductAnalyzeService`의 리콜 조회를 `safeSearch`로 감쌌다.
  - 국내/국외, 상품명/브랜드 조회 중 일부가 실패해도 실패한 조회만 로그로 남기고 나머지 결과로 분석을 계속한다.
  - OpenAI 정제 실패는 여전히 전체 분석 실패로 둔다.
  - 확장 `background.js`는 non-2xx 응답일 때 백엔드 오류 본문을 읽어서 화면에 더 구체적인 메시지를 보여주게 했다.
- 검증:
  - 국외 리콜 조회 실패 재현 테스트 추가 후 RED 확인.
  - 방어 처리 후 `.\gradlew.bat test --tests com.example.gonggong.domain.analysis.service.ProductAnalyzeServiceTest`: `BUILD SUCCESSFUL`
  - `.\gradlew.bat test`: `BUILD SUCCESSFUL`
  - `node --check extension/background.js`: 성공
  - `node --check extension/content.js`: 성공
## 41. 2026-06-23 국외 리콜 파싱 로그 해석

- `source=FOREIGN status=200 bodyLength=...`는 HTTP 응답 수신 성공을 뜻한다.
- `SafetyKorea recall search parsed source=FOREIGN ... itemCount=N`까지 찍히면 국외 리콜 JSON 파싱도 성공한 것이다.
- 실제 확인 로그 예:
  - `가정용섬유제품`: 국내 47건, 국외 265건
  - `침구류`: 국내 9건, 국외 21건
  - `이불`: 국내 1건, 국외 21건
- `bodyLength=69` 이후 `skipped scope=foreign-product ... code=ANALYSIS_006`가 찍히는 검색어는 국외 검색 결과 없음 또는 SafetyKorea의 비표준 빈 결과 응답으로 본다.
  - 예: `가정용섬유제품(침구류)`, `여름이불`, `에어컨이불`
- 현재 상세 조회는 국내 리콜에만 수행한다.
  - 국내: `recallList.json` -> 필요 시 `recallDetail.json`
  - 국외: `fRecallList.json` 목록 응답만 사용
  - 이유: 확인한 OpenAPI 문서에서 국외 상세 조회 endpoint를 확정하지 못했다.
- 국내와 국외가 모두 있으면 최종 분석에는 둘 다 합쳐서 들어간다.
  - 중복 제거 키: `source|recallUid`
  - 위험도 산정: 국내 + 국외 전체 리콜 결과 기준
  - 위해성분 추출: 국내 + 국외의 사유/위반 설명 기준
  - `matchedRecalls[]`: 국내/국외 모두 포함, 각 항목에 `source` 표시

## 42. 2026-06-23 확장 UI 문구 및 위해위험도 팝업 정리

- 화면 문구를 정확도에 맞게 정정했다.
  - `매칭 리콜` -> `관련 리콜 이력`
  - `매칭 리콜이 없는 경우` -> `관련 리콜 이력이 없는 경우`
- 위해 위험도 설명 팝업에서 `GPT` 표기를 `AI`로 바꿨다.
  - 사용자에게 모델/벤더명을 직접 노출하지 않고 기능 역할 중심으로 표현한다.
- 위해 위험도 설명 팝업에 닫기 버튼을 추가했다.
  - 팝업 상단 제목 오른쪽에 `×` 버튼 표시
  - 클릭 시 기존 `closeRiskPolicyPopover()` 호출
  - `Escape` 닫기 동작은 기존대로 유지
- 수정 파일:
  - `extension/content.js`
  - `extension/overlay.css`
- 검증:
  - `node --check extension/content.js`: 성공
## 43. 2026-06-23 물품 리스크 대시보드 domain.risk 분리 구현

- 기존 `/api/products/analyze` 소비자용 상품 분석과 섞지 않고 신규 `domain.risk` 도메인으로 분리했다.
- 신규 API:
  - `POST /api/v1/risk-dashboard/analyze`
- 신규 패키지:
  - `com.example.gonggong.domain.risk.controller`
  - `com.example.gonggong.domain.risk.service`
  - `com.example.gonggong.domain.risk.dto.request`
  - `com.example.gonggong.domain.risk.dto.response`
  - `com.example.gonggong.domain.risk.domain`
  - `com.example.gonggong.domain.risk.calculator`
- 신규 enum:
  - `RiskStatus`: `SAFE`, `WARNING`, `DANGER`, `UNKNOWN`, `UNAVAILABLE`
  - `OverallRiskLevel`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`, `UNKNOWN`
  - `TariffType`: `BASIC`, `WTO`, `FTA`, `ANTI_DUMPING`, `COUNTERVAILING`, `SAFEGUARD`, `SPECIAL`, `UNKNOWN`
- 요청 DTO:
  - `RiskDashboardAnalyzeRequest`
  - `hskCode`: 10자리 숫자 validation
  - `productName`: 필수/공백 불가 validation
  - `declaredValue`, `shippingCost`, `insuranceCost`: 0 이상 validation
  - `quantity`: 1 이상 validation
  - `ingredients` null은 빈 배열처럼 처리
- 응답 DTO:
  - `RiskDashboardResponse`
  - `RecallRiskResponse`
  - `CustomsRiskResponse`
  - `KcRiskResponse`
  - `ChemicalRiskResponse`
  - 각 하위 item/ingredient DTO
- `spring-boot-starter-validation` 의존성을 추가했다.
- validation 예외는 `ExceptionAdvice`에서 `GLOBAL_002` 400 응답으로 처리한다.

## 44. 2026-06-23 risk 실제 데이터 연결 범위

- 리콜 위험:
  - 기존 `SafetyKoreaRecallClient`를 재사용한다.
  - 국내 `searchByProductName`
  - 국외 `searchForeignByProductName`
  - 상품명, 상품 설명, HSK code를 검색어로 사용한다.
  - 최근 3개년 기준으로 필터링한다.
  - 리콜 사유가 비어 있으면 `사유 미명시(단순 안전기준 미달 적발)`로 대체한다.
  - 외부 리콜 API가 모두 실패하면 `UNAVAILABLE`과 `리콜 정보를 현재 조회할 수 없습니다.`를 반환한다.
  - 검색 결과가 없으면 `SAFE`와 “리콜 이력이 없다는 것이 절대적인 안전을 의미하지 않는다”는 안내를 반환한다.
- 관세 위험:
  - 아직 관세율 DB/API Provider가 없다.
  - 임의 세율을 만들지 않고 `UNKNOWN`, `finalTariffRate=null`, `estimatedCustomsDuty=null`로 반환한다.
  - `guideUrl`은 기존 `public-data.customs.unipass-url` 설정을 사용한다.
- KC 위험:
  - 아직 세관장확인대상/KC 인증 API Adapter가 없다.
  - 임의 인증 요구 여부를 만들지 않고 `UNKNOWN`으로 반환한다.
  - 인증번호가 입력되면 응답에는 마스킹해서 넣는다.
  - 제품안전정보센터 실시간 검증 버튼 문구와 URL을 반환한다.
- 화학물질 위험:
  - 아직 화학물질 API 응답 스키마가 확정되지 않았다.
  - 임의 법령/처벌 조항을 만들지 않는다.
  - 성분별로 `시스템 성분 분석 불가능 품목` 상태와 직접 검색 URL을 반환한다.
- 종합 점수:
  - `RiskScoreCalculator`로 분리했다.
  - 가중치: 리콜 25%, 관세 20%, KC 25%, 화학물질 30%
  - 프롬프트 예시의 `78`은 공식과 맞지 않아 공식 기준 점수로 계산한다.
    - 예: `70*0.25 + 60*0.20 + 50*0.25 + 90*0.30 = 69`
- 검증:
  - `.\gradlew.bat test --tests com.example.gonggong.domain.risk.*`: 성공
  - `.\gradlew.bat test --tests com.example.gonggong.global.exception.ExceptionAdviceTest`: 성공
  - `.\gradlew.bat test`: `BUILD SUCCESSFUL`
## 45. 2026-06-23 확장 물품 리스크 버튼 실제 API 연결

- 기존 확장 프로그램의 `물품 리스크 예측` 버튼은 정적 더미 카드를 보여주고 있었다.
- `extension/background.js`에 새 메시지 타입을 추가했다.
  - `ANALYZE_RISK_DASHBOARD`
  - 호출 API: `POST http://localhost:8080/api/v1/risk-dashboard/analyze`
- `extension/content.js`의 버튼 클릭 흐름을 변경했다.
  - 기존: `openRiskDashboardModal()`만 호출해서 더미 데이터 표시
  - 변경: 모달을 열고 `loadRiskDashboard()`로 실제 API 호출
- 요청 payload 구성:
  - `productName`: DOM에서 추출한 상품명
  - `productDescription`: DOM에서 추출한 설명
  - `ingredients`: 상품명/설명에서 `PVC`, `프탈레이트`, `플라스틱` 키워드 추론
  - `originCountry`: 현재는 `CN` fallback
  - `hskCode`: 실제 HSK 매칭 API가 아직 없어서 기존 추론 fallback 사용
    - mouse/마우스 -> `8471601030`
    - toy/완구 -> `9503003490`
    - heater/온열 -> `8516299000`
    - 기본값 -> `3924100000`
  - 금액/수량: `declaredValue=0`, `quantity=1`, `shippingCost=0`, `insuranceCost=0`
- 응답 렌더링:
  - `recallRisk`
  - `customsRisk`
  - `kcRisk`
  - `chemicalRisk`
  - `overallRiskLevel`, `overallRiskScore`
- 카드 상태 스타일 추가:
  - `SAFE`
  - `UNKNOWN`
  - `UNAVAILABLE`
- KTL 더미 가이드는 실시간 검증 안내 영역으로 대체했다.
- 검증:
  - `node --check extension/content.js`: 성공
  - `node --check extension/background.js`: 성공
  - `.\gradlew.bat test`: `BUILD SUCCESSFUL`
## 46. 2026-06-24 관세청 공식 HSK 기준 데이터 실연동

- 공식 데이터:
  - 데이터명: `관세청_HS부호 단위별 품목명_20260101`
  - 출처: `https://www.data.go.kr/data/15130660/fileData.do`
  - 파일: `src/main/resources/data/customs-hsk-items-20260101.xlsx`
  - 전체 파일 데이터: 17,072행
  - 실제 사용하는 시트: `HS10단위`
  - 시트 헤더: `HS10단위`, `한글품목명`, `영문품목명`
  - 10자리 HSK 데이터: 11,327건
- Apache POI `5.5.1`을 추가해 XLSX를 읽는다.
- 신규 `domain.hsk` 구조:
  - `dataset/HskDatasetReader`
  - `dataset/HskDatasetRow`
  - `domain/HskItem`
  - `repository/HskItemRepository`
  - `repository/HskItemReader`
  - `repository/JpaHskItemReader`
  - `initializer/HskDatasetInitializer`
  - `service/HskFeatureExtractor`
  - `service/OpenAiHskFeatureExtractor`
  - `service/HskMatchService`
  - `controller/HskMatchController`
  - HSK 요청/응답 DTO
- 신규 API:
  - `POST /api/seller/hsk/match`
- 처리 흐름:
  - 상품명/설명 입력
  - 기존 OpenAI 상품 정제 결과에서 `standardProductName`, `hskCandidateKeywords`, `materialKeywords`, `searchKeywords` 추출
  - 요청 원문의 상품명/설명 토큰을 우선 검색
  - 공식 HSK DB의 한글/영문 품목명 검색
  - 관련도 점수로 최대 5개 후보 재정렬
  - DB에 존재하는 10자리 코드만 응답
  - 후보가 없으면 코드를 생성하지 않고 `matched=false`
- MySQL:
  - 신규 테이블 `hsk_item`
  - 컬럼: `id`, `hsk_code`, `korean_name`, `english_name`, `created_at`, `updated_at`
  - 애플리케이션 시작 시 테이블이 비어 있으면 XLSX의 11,327건을 배치 적재
  - 실제 적재 로그: `Official HSK dataset initialized itemCount=11327`
- 설정:
  - `HSK_DATASET_INITIALIZE` 기본값 `true`
  - `HSK_DATASET_RESOURCE` 기본값 `classpath:data/customs-hsk-items-20260101.xlsx`
  - 테스트에서는 초기화 비활성화

## 47. 2026-06-24 확장 HSK fallback 제거

- 기존 확장 프로그램의 하드코딩 HSK 분기를 제거했다.
  - 마우스/완구/온열/기본값 분기 삭제
  - 기본 `3924100000` 강제 사용 삭제
- 물품 리스크 버튼 흐름:
  - `MATCH_HSK` 메시지로 `/api/seller/hsk/match` 호출
  - `candidates[0].hskCode` 획득
  - 해당 공식 코드로 `/api/v1/risk-dashboard/analyze` 호출
  - HSK 후보가 없으면 리스크 API를 호출하지 않고 오류 안내
- 실제 검증:
  - 입력: `baby plastic bowl`, `plastic tableware for children`
  - 최초 오매칭: `3304993000 어린이용 제품류`
  - 원인: 범용 키워드가 요청 원문의 `tableware`보다 먼저 검색됨
  - 수정: 요청 원문 우선, 범용 대상 사용자/불용어 제외
  - 최종 첫 후보: `3924100000 식탁용품과 주방용품 / Tableware and kitchenware`
  - confidence: `0.49`
- HSK에서 리스크 API 연속 호출 검증:
  - `hskCode=3924100000`
  - `overallRiskLevel=MEDIUM`
  - `overallRiskScore=41`
  - `recallStatus=SAFE`
  - 관세/KC/화학물질은 아직 `UNKNOWN`
- 서버 상태:
  - 최신 검증 서버: `http://localhost:8082`
  - 확장 설정은 `http://localhost:8080`
  - 8080은 2026-06-23부터 실행 중인 이전 Java 서버이므로 새 코드 반영을 위해 재시작 필요
- 검증:
  - `.\gradlew.bat test --tests com.example.gonggong.domain.hsk.*`: 성공
  - `.\gradlew.bat test`: `BUILD SUCCESSFUL`
  - `node --check extension/content.js`: 성공
  - `node --check extension/background.js`: 성공
## 48. 2026-06-24 노트북 HSK 오매칭 수정

- 증상:
  - 노트북 상품이 `3506911000` 광학용 투명 접착제로 분류됨.
- 원인:
  - 관세청 공식 품목명은 일상어 `노트북` 대신 `휴대용 자동자료처리기계`를 사용한다.
  - 기존 단순 문자열 검색에서 노트북 완제품 후보가 검색되지 않았다.
  - 상품명의 `디스플레이` 단어가 접착제 품목 설명의 `평판디스플레이 제조용`과 일치해 잘못된 후보가 올라왔다.
- 공식 데이터에서 확인한 노트북 품목:
  - HSK: `8471300000`
  - 한글명: `휴대용 자동자료처리기계(중량이 10킬로그램 이하인 것으로서 적어도 중앙처리장치, 키보드, 디스플레이를 갖추고 있는 것으로 한정한다)`
  - 영문명: `Portable automatic data processing machines, weighing not more than 10 kg, consisting of at least a central processing unit, a keyboard and a display`
- 수정:
  - `노트북`, `랩탑`, `laptop`, `notebook PC`, `notebook computer`를 공식 검색어로 확장한다.
  - 확장 검색어:
    - `휴대용 자동자료처리기계`
    - `portable automatic data processing machine`
  - 공식 완제품 후보가 부품/재료 후보보다 우선되도록 검색 후보군을 보강했다.
- 실제 API 재검증:
  - 첫 후보: `8471300000`
  - confidence: `0.60`
  - 후순위: `8524111000` 등 휴대용 자동자료처리기계용 부품
- 검증:
  - 노트북 오매칭 재현 테스트 RED 확인
  - `.\gradlew.bat test --tests com.example.gonggong.domain.hsk.* --rerun-tasks`: 성공
  - 실제 API `http://localhost:8083/api/seller/hsk/match`: 첫 후보 `8471300000`

## 49. 2026-06-24 AI 주 판매물품 중심 HSK 분류

- 상품 정제 프롬프트에서 판매 대상의 핵심 물품을 가장 먼저 판별하도록 변경했다.
  - 완제품, 부품, 액세서리, 원재료, 세트 상품을 구분한다.
  - 성능, 규격, 소재, 사용처, 구성 부품은 주 판매물품으로 분류하지 않는다.
  - 예: 디스플레이 품질이 강조된 노트북의 주 판매물품은 디스플레이가 아니라 노트북이다.
  - 단, 교체용 디스플레이처럼 부품 자체가 판매 대상이면 해당 부품을 주 판매물품으로 처리한다.
- AI 정제 결과에 다음 필드를 추가했다.
  - `primaryProductName`
  - `productForm`
  - `primarySearchKeywords`
  - `componentKeywords`
  - `featureKeywords`
- HSK 후보 검색 순서를 변경했다.
  - `primaryProductName`과 `primarySearchKeywords`로 공식 HSK 데이터 후보를 먼저 찾는다.
  - 주 판매물품 후보를 찾지 못한 경우에만 기존 검색어와 원문을 fallback으로 사용한다.
  - 부품 및 특징 키워드는 후보 검색의 주 키워드로 사용하지 않고 보조 점수에만 반영한다.
  - 완제품으로 판정된 상품은 `부분품`, `제조용`, `교체용` 성격의 HSK 후보에 감점을 적용한다.
- 실제 API 검증:
  - 입력: 초슬림 노트북, 14.1인치 디스플레이, 16GB RAM, 2TB SSD 등이 포함된 상품명
  - 첫 번째 후보: `8471300000`
  - 품목: 휴대용 자동자료처리기계
  - confidence: `0.66`
  - 기존의 디스플레이용 접착제 오분류가 재현되지 않았다.
- 검증:
  - OpenAI 프롬프트 및 응답 스키마 테스트 추가
  - HSK 특징 분리 테스트 추가
  - `.\gradlew.bat test --rerun-tasks`: 성공
  - `node --check extension/content.js`: 성공
  - `node --check extension/background.js`: 성공
- 실행 참고:
  - 최신 검증 서버는 `http://localhost:8084`
  - 확장 프로그램은 `http://localhost:8080`을 사용하므로 8080 서버를 재시작해야 변경 코드가 반영된다.

## 50. 2026-06-25 관세 및 사후추징 위험 구현

- 기존 `CustomsRiskService`의 고정 `UNKNOWN` 응답을 DB 기반 실제 계산 구조로 교체했다.
- 신규 테이블:
  - `tariff_rate`
  - 주요 필드: `hsk_code`, `origin_country`, `tariff_type`, `base_rate`, `additional_rate`, `effective_from`, `effective_to`, `legal_notice`, `active`
- 신규 구성:
  - `TariffRate` 엔티티
  - `TariffRateRepository`
  - `TariffRateProvider`
  - `JpaTariffRateProvider`
  - `TariffRateResult`
  - `CustomsDutyCalculator`
- 관세율 선택:
  - 요청 HSK 10자리와 분석일 기준 유효한 활성 세율만 조회
  - 원산지 정확 일치 세율을 공통 세율보다 우선
  - 실제 등록 데이터가 없으면 세율을 생성하지 않고 `UNKNOWN`
- 계산:
  - 과세가격 = 신고가격 + 운임 + 보험료
  - 예상 관세액 = 과세가격 × 최종 관세율 / 100
  - 금액 계산은 `BigDecimal` 사용
  - KRW가 아닌 통화는 환율 데이터가 없으므로 세율만 반환하고 예상 관세액은 계산하지 않음
- 위험 판정:
  - 일반 등록 세율: `SAFE`, 점수 20
  - 덤핑방지/상계/긴급/특별 관세 또는 추가세율 존재: `WARNING`, 점수 70
  - 미등록 HSK: `UNKNOWN`, 점수 50
- 주의:
  - 공식 관세율 원천 데이터는 아직 `tariff_rate`에 적재되지 않았다.
  - 따라서 현재 DB가 비어 있으면 기존과 같이 미등록 코드 안내가 반환된다.
  - 임의 세율과 샘플 관세율은 운영 데이터로 추가하지 않았다.
- 검증:
  - 일반 세율 관세액 계산
  - 특별 가중관세 안내
  - 원산지별 세율 우선순위
  - 미등록 HSK에서 임의 세율 미생성
  - 전체 51개 테스트 성공

## 51. 2026-06-25 관세청 공식 관세율 전체 적재

- 공식 데이터 원천:
  - 공공데이터포털 `관세청_품목번호별 관세율표_20260211`
  - 데이터셋 ID: `15051179`
  - 원본 파일: `관세율(2026).xlsx`
  - 프로젝트 리소스: `data/customs-tariff-rates-20260211.xlsx`
- 파일 구조:
  - 최신 시트: `2.12`
  - 헤더 포함 행 수: `380,217`
  - 실제 데이터 행 수: `380,216`
  - 열: 품목번호, 관세율구분, 관세율, 단위당세액, 기준가격, 적용국가구분, 용도세율구분, 적용개시일, 적용만료일
- 적재:
  - 공식 최신 시트의 실제 데이터 `380,216건` 전체를 `tariff_rate`에 저장
  - 대용량 XLSX를 메모리에 모두 올리지 않고 SAX 스트리밍 방식으로 읽음
  - MySQL JDBC 500건 배치 삽입 사용
  - 실제 완료 로그: `Official customs tariff dataset initialized itemCount=380216`
- `tariff_rate` 확장 필드:
  - `tariff_code`
  - `unit_amount`
  - `base_price`
  - `country_scope`
  - `usage_rate_code`
- 관세율 구분:
  - `A`, `A1`: `BASIC`
  - `C*`: `WTO`
  - `F*`: `FTA`
  - 그 외 원본 코드는 보존하고 `UNKNOWN`
- 계산 적용 원칙:
  - 전체 380,216건은 원본 보존
  - 현재 자동 계산에는 원산지 증명 없이 확정 가능한 일반 퍼센트 세율만 사용
  - 원본 코드 `A` 또는 `C`
  - 적용국가구분 `1`
  - 용도세율구분 없음
  - 관세율 숫자 존재
  - FTA 세율은 원산지 증명 여부가 요청에 없으므로 자동 적용하지 않음
- 실제 API 검증:
  - HSK: `3924100000`
  - 신고가격: 100,000원
  - 운임: 10,000원
  - 과세가격: 110,000원
  - 적용 세율: WTO 6.5%
  - 예상 관세액: 7,150원
  - 상태: `SAFE`
- 검증 서버:
  - `http://localhost:8085`
  - PID `25292`
- 전체 테스트 성공
## 2026-06-29 최신 작업 기록 - HSK/리콜/리스크 대시보드

- 리스크 대시보드의 리콜 가능성 상세 표시를 개선했다.
  - 기존에는 `총 N건`만 표시되어 사용자가 어떤 리콜인지 알기 어려웠다.
  - `extension/risk-view.js`에 `recallRiskCardHtml`, `recallDetailListHtml`를 추가했다.
  - 리콜 가능성 카드에 `상세 보기` 버튼을 표시하고, 버튼 클릭 시 별도 팝업에서 리콜 목록을 보여준다.
  - 상세 팝업 표시 항목: 리콜 제품명, 국내/국외 출처, 리콜 사유, 위반/위험 내용, 공표일.
  - `원문 보기` 링크는 제거했다. SafetyKorea 응답 URL 필드가 실제 원문인지 보장하기 어렵기 때문이다.

- 리스크 대시보드의 리콜 산정 기준을 2차 AI 필터링 기준으로 변경했다.
  - 기존 리스크 리콜 가능성은 1차 정제 키워드로 SafetyKorea를 다시 검색한 결과를 그대로 집계했다.
  - `RecallRiskService`에 `RecallRelevanceDecider`를 주입했다.
  - SafetyKorea 검색 후보를 모은 뒤 최근 3년 집계 전에 2차 AI 필터링을 통과한 리콜만 사용한다.
  - 단독 리스크 API 호출 시에도 관련 없는 리콜 후보를 줄일 수 있다.

- 중복 GPT 호출을 줄이기 위해 상품 분석 결과의 2차 필터링 리콜을 리스크 요청에서 재사용하도록 변경했다.
  - 상품 분석 응답의 `matchedRecalls`를 확장 프로그램에서 `prefilteredRecalls`로 변환해 리스크 API에 전달한다.
  - `RiskDashboardAnalyzeRequest`에 `prefilteredRecalls`를 추가했다.
  - `PrefilteredRecallRequest` DTO를 추가했다.
  - `RecallRiskService`는 `prefilteredRecalls`가 있으면 SafetyKorea 재검색과 2차 AI 필터링을 건너뛴다.
  - `prefilteredRecalls`가 비어 있으면 기존처럼 SafetyKorea 검색 + 2차 AI 필터링을 수행한다.

- 리콜 공표일을 재사용 경로에도 전달하도록 수정했다.
  - `MatchedRecallDto`에 `announcementDate` 필드를 추가했다.
  - 상품 분석 단계에서 SafetyKorea `publishDate`를 `yyyy-MM-dd` 형식으로 변환해 `matchedRecalls[].announcementDate`에 담는다.
  - 확장 프로그램은 이 값을 `prefilteredRecalls[].announcementDate`로 리스크 요청에 전달한다.
  - 리스크 응답의 `recallRisk.items[].announcementDate`, `recallRisk.latestAnnouncementDate`에 반영된다.

- HSK 매칭 실패 및 분석 중 UI를 정리했다.
  - HSK 매칭 실패 시 초록색 HSK 표시 영역에 `매칭된 HSK 코드가 없습니다.`를 표시한다.
  - HSK 매칭 실패 시 리스크 백엔드 호출을 진행하지 않는다.
  - 리스크탭을 다시 열 때 이전 HSK/리스크 결과가 잠깐 보이던 문제를 줄이기 위해 버튼 클릭 즉시 상태를 초기화한다.
  - HSK 매칭 전까지 상단 요약, HSK 표시 영역, 하단 카드 영역 모두 `HSK 코드 분석중...`으로 통일했다.
  - 기존 `리콜 가능성 조회 중`, `관세 조회 중`, `KC 조회 중` 등 4개 로딩 카드는 더 이상 실행하지 않는다.
  - HSK 분석중 카드는 grid 전체 너비를 차지하도록 크게 표시한다.

- 주요 수정 파일:
  - `src/main/java/com/example/gonggong/domain/risk/service/RecallRiskService.java`
  - `src/main/java/com/example/gonggong/domain/risk/dto/request/RiskDashboardAnalyzeRequest.java`
  - `src/main/java/com/example/gonggong/domain/risk/dto/request/PrefilteredRecallRequest.java`
  - `src/main/java/com/example/gonggong/domain/analysis/dto/MatchedRecallDto.java`
  - `src/main/java/com/example/gonggong/domain/analysis/service/ProductAnalyzeService.java`
  - `extension/content.js`
  - `extension/risk-view.js`
  - `extension/overlay.css`
  - `extension/risk-view.test.js`
  - `src/test/java/com/example/gonggong/domain/risk/service/RecallRiskServiceTest.java`
  - `src/test/java/com/example/gonggong/domain/analysis/service/ProductAnalyzeServiceTest.java`

- 검증 기록:
  - `node --check extension/content.js`: 성공
  - `node --test extension/risk-view.test.js extension/brand-view.test.js extension/seller-view.test.js extension/risk-price.test.js`: 24개 성공
  - `.\gradlew.bat test --tests com.example.gonggong.domain.risk.service.RecallRiskServiceTest --tests com.example.gonggong.domain.risk.controller.RiskDashboardControllerTest`: 성공
  - `.\gradlew.bat test --tests com.example.gonggong.domain.analysis.service.ProductAnalyzeServiceTest --tests com.example.gonggong.domain.risk.service.RecallRiskServiceTest --tests com.example.gonggong.domain.risk.controller.RiskDashboardControllerTest`: 성공

- 현재 동작 요약:
  - 상품 분석: 1차 AI 정제 -> SafetyKorea 후보 조회 -> 2차 AI 리콜 필터링 -> `matchedRecalls` 생성
  - 리스크 대시보드: HSK 분석 -> HSK 코드 확정 시 리스크 API 호출
  - 리스크 리콜 가능성:
    - `matchedRecalls`가 있으면 재사용해 GPT 중복 호출 방지
    - 없으면 SafetyKorea 검색 및 2차 AI 필터링 수행
  - 하단 리콜 상세 팝업은 최종 필터링된 리콜만 보여준다.
