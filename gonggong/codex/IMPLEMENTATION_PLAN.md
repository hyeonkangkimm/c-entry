# Implementation Plan

이 파일은 현재 진행 중인 구현 계획이다. 상세 요구사항과 장기 메모리는 `codex/WORKING_MEMORY.md`를 우선한다.

## Current Scope

Phase 1 MVP의 첫 단위만 구현한다.

- `POST /api/products/analyze`
- DB 연동 없음
- 문서의 JSON 계약에 맞춘 고정 응답 반환
- 루트 프로젝트와 `com.example.gonggong` 패키지 유지
- `application.yaml`은 현재 상태 유지

## Steps

1. 컨트롤러 HTTP 계약 테스트 작성
2. 테스트 실패 확인
3. DTO, enum, service, controller 최소 구현
4. 테스트 통과 확인
5. `compileJava`로 빌드 확인
6. `codex/WORKING_MEMORY.md`에 진행 결과 기록

## Target Files

- `src/test/java/com/example/gonggong/domain/analysis/controller/ProductAnalyzeControllerTest.java`
- `src/main/java/com/example/gonggong/domain/analysis/controller/ProductAnalyzeController.java`
- `src/main/java/com/example/gonggong/domain/analysis/dto/ProductAnalyzeRequest.java`
- `src/main/java/com/example/gonggong/domain/analysis/dto/ProductAnalyzeResponse.java`
- `src/main/java/com/example/gonggong/domain/analysis/dto/MatchedRecallDto.java`
- `src/main/java/com/example/gonggong/domain/analysis/service/ProductAnalyzeService.java`
- `src/main/java/com/example/gonggong/domain/analysis/RiskLevel.java`

## Demand Priority MVP

### Scope

- `GET /api/demand/priority-items/top10`
- 월별 HSK 품목 수입액/수입중량 데이터를 기반으로 전년 동월 대비 증가율 계산
- 필수 산업 부품 여부를 가중치로 반영
- 과거 리콜 빈도는 아직 리콜 도메인이 없으므로 `0`으로 반환
- 외부 공공데이터 API 연동은 후속 작업
- 로컬 MySQL에 샘플 데이터를 seed로 적재

### Target Files

- `src/main/java/com/example/gonggong/domain/demand/entity/ImportTrend.java`
- `src/main/java/com/example/gonggong/domain/demand/entity/EssentialIndustryItem.java`
- `src/main/java/com/example/gonggong/domain/demand/repository/ImportTrendRepository.java`
- `src/main/java/com/example/gonggong/domain/demand/repository/EssentialIndustryItemRepository.java`
- `src/main/java/com/example/gonggong/domain/demand/dto/DemandPriorityItemResponse.java`
- `src/main/java/com/example/gonggong/domain/demand/dto/DemandPriorityTop10Response.java`
- `src/main/java/com/example/gonggong/domain/demand/service/DemandPriorityService.java`
- `src/main/java/com/example/gonggong/domain/demand/controller/DemandPriorityController.java`
- `src/main/java/com/example/gonggong/domain/demand/initializer/DemandSampleDataInitializer.java`
- `src/test/java/com/example/gonggong/domain/demand/service/DemandPriorityServiceTest.java`
- `src/test/java/com/example/gonggong/domain/demand/controller/DemandPriorityControllerTest.java`
