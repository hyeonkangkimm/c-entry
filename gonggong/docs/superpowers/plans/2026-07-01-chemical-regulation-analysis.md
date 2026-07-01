# Chemical Regulation Analysis Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Query the Korea Environment Corporation chemical API for extracted ingredients and render evidence-based regulation results or a direct-search fallback in the extension dashboard.

**Architecture:** A provider package owns API configuration, HTTP transport, JSON parsing, and immutable lookup results. `ChemicalRiskService` performs bounded asynchronous lookups and applies an effective-dated YAML legal rule table; `risk-view.js` renders the resulting card without embedding domain decisions in the browser.

**Tech Stack:** Java 17, Spring Boot 4.1, Java HttpClient, Jackson JSON/YAML, CompletableFuture, JUnit 5, AssertJ, browser JavaScript, Node.js test runner, CSS

---

### Task 1: Chemical API contract and parser

**Files:**
- Create: `src/main/java/com/example/gonggong/domain/risk/chemical/ChemicalApiProperties.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/chemical/ChemicalClassification.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/chemical/ChemicalSubstance.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/chemical/ChemicalLookupStatus.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/chemical/ChemicalLookupResult.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/chemical/ChemicalInformationClient.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/chemical/ChemicalApiJsonParser.java`
- Test: `src/test/java/com/example/gonggong/domain/risk/chemical/ChemicalApiJsonParserTest.java`
- Modify: `src/main/resources/application.yaml`

- [ ] **Step 1: Write parser tests using a representative `chemSbstnList` JSON fixture**

Test array and singleton item shapes. Assert extraction of Korean/English substance names, CAS number, classification type, classification identifier, concentration, exception, notice date, and notice text. Test an empty result and a malformed schema separately.

- [ ] **Step 2: Run parser tests and verify RED**

Run: `.\gradlew.bat test --tests "*ChemicalApiJsonParserTest"`

Expected: compilation fails because the parser and contract records do not exist.

- [ ] **Step 3: Implement immutable API contract and strict parser**

`ChemicalInformationClient.lookup(String ingredient)` returns `ChemicalLookupResult`. `MATCHED` contains one `ChemicalSubstance`; `NOT_FOUND` contains no substance; `UNAVAILABLE` records a sanitized reason. The parser accepts the documented response envelope and returns `UNAVAILABLE` when the envelope cannot be recognized rather than treating it as an empty successful response.

- [ ] **Step 4: Add configuration defaults**

Configure base URL `https://apis.data.go.kr/B552584/kecoapi/ncissbstn/chemSbstnList`, `serviceKey`, `serviceKeyParamName`, ingredient-name parameter, `type=json`, page size 10, timeout 3 seconds, concurrency 4, and direct-search URL/button text. Bind the API key from `${CHEMICAL_API_KEY:}` and allow every parameter name to be overridden from environment variables.

- [ ] **Step 5: Run parser tests and verify GREEN**

Run: `.\gradlew.bat test --tests "*ChemicalApiJsonParserTest"`

Expected: all parser tests PASS.

### Task 2: HTTP client and official API request

**Files:**
- Create: `src/main/java/com/example/gonggong/domain/risk/chemical/HttpChemicalInformationClient.java`
- Test: `src/test/java/com/example/gonggong/domain/risk/chemical/HttpChemicalInformationClientTest.java`

- [ ] **Step 1: Write failing request construction and failure-mapping tests**

Inject a fake `HttpClient` and assert URL encoding of Korean ingredient names, service-key inclusion without logging, JSON response parsing, non-2xx mapping to `UNAVAILABLE`, timeout mapping to `UNAVAILABLE`, and interrupted-thread restoration.

- [ ] **Step 2: Run client tests and verify RED**

Run: `.\gradlew.bat test --tests "*HttpChemicalInformationClientTest"`

Expected: compilation fails because the HTTP client does not exist.

- [ ] **Step 3: Implement the HTTP client**

Build a GET request from `ChemicalApiProperties`, use the configured timeout, return `UNAVAILABLE` when configuration is disabled, and log only ingredient, status code, and response length. Delegate all body interpretation to `ChemicalApiJsonParser`.

- [ ] **Step 4: Run client tests and verify GREEN**

Run: `.\gradlew.bat test --tests "*HttpChemicalInformationClientTest"`

Expected: all client tests PASS.

- [ ] **Step 5: Perform one sanitized live contract check**

With `CHEMICAL_API_KEY` configured, query one known ingredient without printing the key or full response. Confirm the official parameter names and actual JSON field names against the parser; update only configuration defaults or parser aliases where the observed contract differs.

### Task 3: Effective-dated regulation rules

**Files:**
- Modify: `build.gradle`
- Create: `src/main/resources/data/chemical-regulation-rules.yaml`
- Create: `src/main/java/com/example/gonggong/domain/risk/chemical/ChemicalRegulationRule.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/chemical/ChemicalRegulationRuleRepository.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/chemical/YamlChemicalRegulationRuleRepository.java`
- Test: `src/test/java/com/example/gonggong/domain/risk/chemical/YamlChemicalRegulationRuleRepositoryTest.java`

- [ ] **Step 1: Write failing rule-loading and effective-date tests**

Assert lookup by classification identifier, active date filtering, official obligation source URL retention, omission of penalties without a verified penalty source, and rejection of rules whose source is not HTTPS on `law.go.kr`.

- [ ] **Step 2: Run rule tests and verify RED**

Run: `.\gradlew.bat test --tests "*YamlChemicalRegulationRuleRepositoryTest"`

Expected: compilation fails because the rule repository does not exist.

- [ ] **Step 3: Implement the YAML repository**

Add Jackson YAML support, load the classpath resource once, validate identifiers/dates/source URLs, and expose `findActive(String classificationIdentifier, LocalDate date)`. A rule may contain obligation information without a penalty; penalty text is exposed only when penalty article and official source URL are both present.

- [ ] **Step 4: Add conservative initial rules**

Include only classification identifiers confirmed by the live API and official designation sources. Store related-law obligations when verified. Leave penalty fields absent unless the current effective law, violation condition, exemption conditions, and penalty article can all be linked unambiguously; an empty penalty field is an accepted production result.

- [ ] **Step 5: Run rule tests and verify GREEN**

Run: `.\gradlew.bat test --tests "*YamlChemicalRegulationRuleRepositoryTest"`

Expected: all rule tests PASS.

### Task 4: Bounded asynchronous analysis and API response

**Files:**
- Modify: `src/main/java/com/example/gonggong/domain/risk/service/ChemicalRiskService.java`
- Modify: `src/main/java/com/example/gonggong/domain/risk/dto/response/ChemicalRiskResponse.java`
- Modify: `src/main/java/com/example/gonggong/domain/risk/dto/response/RegulatedIngredientResponse.java`
- Modify: `src/main/java/com/example/gonggong/domain/risk/service/RiskDashboardService.java`
- Create: `src/test/java/com/example/gonggong/domain/risk/service/ChemicalRiskServiceTest.java`
- Modify: `src/test/java/com/example/gonggong/domain/risk/controller/RiskDashboardControllerTest.java`

- [ ] **Step 1: Write failing service status tests**

Cover no ingredients (`UNKNOWN`), all not regulated (`SAFE`), regulated match (`DANGER`), partial lookup failure (`WARNING`), all unavailable (`UNAVAILABLE`), and regulated plus one failed ingredient (`DANGER` with unanalyzed warning). Assert CAS, classification, related law, optional penalty, source URLs, unanalyzed ingredients, analysis-unavailable marker, search URL, and button text.

- [ ] **Step 2: Write a failing concurrency test**

Use a blocking fake client with atomic counters for eight ingredients and assert that observed concurrent calls never exceed four, every ingredient is attempted, one failure does not cancel siblings, and output order follows input order.

- [ ] **Step 3: Run service tests and verify RED**

Run: `.\gradlew.bat test --tests "*ChemicalRiskServiceTest"`

Expected: tests fail because `ChemicalRiskService` still returns placeholders.

- [ ] **Step 4: Implement bounded analysis and status calculation**

Inject `ChemicalInformationClient`, `ChemicalRegulationRuleRepository`, properties, `Clock`, and a fixed-size executor. Run one future per normalized ingredient, isolate exceptions, join in input order, map active rules, and compute status exactly as specified in the design. Do not synthesize penalty text.

- [ ] **Step 5: Update response DTOs and warning generation**

Add `unanalyzedIngredients`, `analysisUnavailable`, `searchButtonText`, obligation/penalty source URLs, and nullable penalty fields. Replace the unconditional “Adapter not connected” warning with warnings derived from `WARNING`, `UNKNOWN`, or `UNAVAILABLE` results.

- [ ] **Step 6: Run backend chemical and controller tests**

Run: `.\gradlew.bat test --tests "*ChemicalRiskServiceTest" --tests "*RiskDashboardControllerTest"`

Expected: all selected tests PASS.

### Task 5: Extension chemical card

**Files:**
- Modify: `extension/risk-view.js`
- Modify: `extension/risk-view.test.js`
- Modify: `extension/content.js`
- Modify: `extension/overlay.css`

- [ ] **Step 1: Write failing renderer tests**

Add `chemicalRiskCardHtml` tests for a regulated ingredient with CAS/classification/law/verified penalty, a regulated ingredient without penalty, partial analysis with marker and button, all-unavailable fallback, successful non-regulated response without fallback, response-text escaping, and rejection of `javascript:` search URLs.

- [ ] **Step 2: Run renderer tests and verify RED**

Run: `node --test extension/risk-view.test.js`

Expected: FAIL because `chemicalRiskCardHtml` is not exported.

- [ ] **Step 3: Implement and connect the renderer**

Render `행정처분 및 형사처벌`, ingredient definition lists, optional `적용 가능 처벌 조항`, `[시스템 성분 분석 불가능 품목]`, and the safe external-search button. Export through `RiskView` and replace the inline chemical `riskCardHtml` call in `content.js`.

- [ ] **Step 4: Add focused styles**

Style the ingredient list, analysis-unavailable marker, and ICIS link consistently with existing cards. Preserve visible keyboard focus and card readability at extension panel width.

- [ ] **Step 5: Run extension tests and syntax checks**

Run: `node --test extension\*.test.js`

Run: `Get-ChildItem extension -Filter *.js | ForEach-Object { node --check $_.FullName }`

Expected: all extension tests and syntax checks PASS.

### Task 6: Full regression verification

**Files:**
- Verify only

- [ ] **Step 1: Run the full backend test suite**

Run: `.\gradlew.bat test`

Expected: all backend tests PASS.

- [ ] **Step 2: Run the full extension test suite**

Run: `node --test extension\*.test.js`

Expected: all extension tests PASS.

- [ ] **Step 3: Verify JavaScript syntax and configuration hygiene**

Run: `Get-ChildItem extension -Filter *.js | ForEach-Object { node --check $_.FullName }`

Confirm that API keys are read only from environment/configuration, no key or full external response is logged, unsafe external URLs fall back to the configured ICIS URL, and unverified punishment text is absent.

> Git commit steps are omitted because this workspace is not recognized as a Git repository.
