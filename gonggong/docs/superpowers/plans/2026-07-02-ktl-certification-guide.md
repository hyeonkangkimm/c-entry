# KTL Certification Guide Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Return and render a curated KTL certification guide only when the DOM KC number is verified as valid by SafetyKorea.

**Architecture:** Add a dedicated KTL guide entity/repository/provider keyed by normalized SafetyKorea certification type. `KcRiskService` attaches a nullable guide only for valid verification results. The extension renders the guide as a full-width panel immediately below the four risk cards.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA, PostgreSQL/H2, Chrome Extension JavaScript, Node.js test runner

---

### Task 1: KTL guide lookup domain

**Files:**
- Create: `src/main/java/com/example/gonggong/domain/risk/domain/KtlCertificationGuide.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/repository/KtlCertificationGuideRepository.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/provider/KtlCertificationGuideProvider.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/provider/JpaKtlCertificationGuideProvider.java`
- Create: `src/main/java/com/example/gonggong/domain/risk/dto/response/KtlCertificationGuideResponse.java`
- Test: `src/test/java/com/example/gonggong/domain/risk/provider/JpaKtlCertificationGuideProviderTest.java`

- [ ] Write a failing provider test proving `전기용품 안전인증` normalizes to `ELECTRICAL_SAFETY_CERTIFICATION` and unknown types return empty.
- [ ] Run the provider test and confirm compilation fails because the provider is absent.
- [ ] Implement the entity, repository, response record, normalization, official-URL validation, and deterministic action guide.
- [ ] Run the provider test and confirm it passes.

### Task 2: Valid-KC gating and API response

**Files:**
- Modify: `src/main/java/com/example/gonggong/domain/risk/dto/response/KcRiskResponse.java`
- Modify: `src/main/java/com/example/gonggong/domain/risk/service/KcRiskService.java`
- Modify: `src/test/java/com/example/gonggong/domain/risk/service/KcRiskServiceTest.java`

- [ ] Write failing tests proving valid verification includes `ktlGuide` and invalid/missing verification returns null.
- [ ] Run `KcRiskServiceTest` and confirm failure because `ktlGuide` is absent.
- [ ] Inject `KtlCertificationGuideProvider`, query it only when `verification.valid()` is true, and append the nullable response field.
- [ ] Run `KcRiskServiceTest` and confirm it passes.

### Task 3: Curated initial KTL data

**Files:**
- Create: `src/main/java/com/example/gonggong/domain/risk/initializer/KtlCertificationGuideInitializer.java`
- Test: `src/test/java/com/example/gonggong/domain/risk/initializer/KtlCertificationGuideInitializerTest.java`

- [ ] Write a failing initializer test for the three electrical certification guide keys.
- [ ] Add idempotent official-source seed data for electrical safety certification, safety confirmation, and supplier conformity confirmation.
- [ ] Store unpublished fees as `제품 사양에 따라 별도 견적` and KTL URLs only.
- [ ] Run initializer and backend tests.

### Task 4: Full-width extension panel

**Files:**
- Modify: `extension/risk-view.js`
- Modify: `extension/risk-view.test.js`
- Modify: `extension/content.js`
- Modify: `extension/overlay.css`

- [ ] Write failing view tests proving a valid guide renders all required fields and missing guide renders an empty string.
- [ ] Implement escaped list rendering and KTL-domain URL allowlisting in `ktlCertificationGuideHtml`.
- [ ] Append the panel after `.isg-risk-grid`, never inside one of the four cards.
- [ ] Add full-width desktop and one-column mobile styles.
- [ ] Run all extension tests and syntax checks.

### Task 5: Verification

**Files:**
- Verify all modified files.

- [ ] Run `./gradlew.bat test` and confirm all backend tests pass.
- [ ] Run `node --test extension/*.test.js` and confirm all extension tests pass.
- [ ] Run `node --check extension/content.js` and `node --check extension/risk-view.js`.
- [ ] Confirm the panel is absent for invalid KC and present below the four-card grid for valid KC with mapped guide data.
