# Chemical Ingredient Source Switch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Use the AI-refined `위해 성분` result as the primary input for the chemical safety API, while keeping the legacy recall-text extractor as a fallback.

**Architecture:** The analysis pipeline already produces `ProductNormalizeResult.riskIngredientKeywords`, and the dashboard flow already carries `ProductAnalyzeResponse.harmfulIngredients` through the extension. The implementation will move the chemical lookup to prefer the AI-refined list, keep the regex-based extractor only when the refined list is empty, and update the dashboard request builder to send the refined list first. This keeps behavior deterministic and reduces repeated string matching on raw recall text.

**Tech Stack:** Java 17, Spring Boot, Jackson, browser extension content script JavaScript, JUnit 5, Node test runner

---

### Task 1: Prefer AI-refined ingredients in product analysis output

**Files:**
- Modify: `src/main/java/com/example/gonggong/domain/analysis/service/ProductAnalyzeService.java`
- Test: `src/test/java/com/example/gonggong/domain/analysis/service/ProductAnalyzeServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void usesNormalizedRiskIngredientKeywordsBeforeLegacyExtractor() {
    ProductNormalizeResult normalized = new ProductNormalizeResult(
        "테스트 상품",
        List.of("테스트"),
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(),
        null,
        List.of("납"),
        List.of("테스트"),
        "테스트 상품",
        "FINISHED_PRODUCT",
        List.of("테스트"),
        List.of(),
        List.of(),
        0.9
    );

    ProductAnalyzeService service = new ProductAnalyzeService(
        request -> normalized,
        keyword -> List.of(),
        item -> item,
        new HarmfulIngredientExtractor(),
        RecallRelevanceDecider.keepAll()
    );

    ProductAnalyzeResponse response = service.analyze(new ProductAnalyzeRequest(
        "테스트",
        "납 관련 리콜 사유가 없는 설명",
        null,
        null,
        "CN"
    ));

    assertThat(response.harmfulIngredients()).containsExactly("납");
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat test --tests com.example.gonggong.domain.analysis.service.ProductAnalyzeServiceTest.usesNormalizedRiskIngredientKeywordsBeforeLegacyExtractor`
Expected: FAIL because `harmfulIngredients` still comes from recall-text extraction.

- [ ] **Step 3: Write minimal implementation**

```java
List<String> harmfulIngredients = safeList(normalized.riskIngredientKeywords());
if (harmfulIngredients.isEmpty()) {
    harmfulIngredients = collectIngredients(recalls);
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew.bat test --tests com.example.gonggong.domain.analysis.service.ProductAnalyzeServiceTest.usesNormalizedRiskIngredientKeywordsBeforeLegacyExtractor`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/gonggong/domain/analysis/service/ProductAnalyzeService.java src/test/java/com/example/gonggong/domain/analysis/service/ProductAnalyzeServiceTest.java
git commit -m "feat: prefer ai refined ingredient keywords"
```

### Task 2: Send refined ingredients into the dashboard request first

**Files:**
- Modify: `extension/content.js`
- Test: `extension/content.test.js` or the nearest existing Node test file for dashboard payload construction

- [ ] **Step 1: Write the failing test**

```javascript
test("risk dashboard request uses last AI refined harmful ingredients first", () => {
  const payload = { productName: "test", description: "" };
  const state = { lastResult: { harmfulIngredients: ["납"] } };
  const request = toRiskDashboardRequest(payload, "1234567890", state);
  assert.deepStrictEqual(request.ingredients, ["납"]);
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `node --test extension/content.test.js`
Expected: FAIL because the request still uses `inferIngredientKeywords(payload)` first.

- [ ] **Step 3: Write minimal implementation**

```javascript
const refinedIngredients = Array.isArray(normalized.harmfulIngredients) ? normalized.harmfulIngredients : [];
ingredients: refinedIngredients.length ? refinedIngredients : inferIngredientKeywords(payload),
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `node --test extension/content.test.js`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add extension/content.js extension/content.test.js
git commit -m "feat: use ai refined ingredients for dashboard lookup"
```

### Task 3: Verify the end-to-end chemical lookup still handles fallback cases

**Files:**
- Review: `src/main/java/com/example/gonggong/domain/risk/service/ChemicalRiskService.java`
- Review: `src/test/java/com/example/gonggong/domain/risk/service/ChemicalRiskServiceTest.java`

- [ ] **Step 1: Confirm the existing risk service still accepts the ingredient list from the dashboard request**

```java
assertThat(response.analysisUnavailable()).isFalse();
assertThat(response.regulatedIngredients()).isNotEmpty();
```

- [ ] **Step 2: Run the targeted tests**

Run: `./gradlew.bat test --tests com.example.gonggong.domain.risk.service.ChemicalRiskServiceTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/gonggong/domain/risk/service/ChemicalRiskService.java src/test/java/com/example/gonggong/domain/risk/service/ChemicalRiskServiceTest.java
git commit -m "test: keep chemical lookup fallback behavior stable"
```
