# Chemical Candidate Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand the AI product normalization output to return up to five chemical ingredient candidates with `name`, `casNumber`, and `englishName`, then search the environmental API using those candidates with `CAS` first and name fallback.

**Architecture:** The normalization layer already produces ingredient-like signals, but they are currently unstructured strings. This change will turn them into a first-class candidate list in `ProductNormalizeResult`, update the OpenAI schema and prompt to emit the richer shape, and update the chemical lookup pipeline to search each candidate by CAS when present, then by English name, then by Korean name. The extension will continue sending the AI-refined data through the dashboard request so the backend receives the richer candidate list without re-extracting raw text.

**Tech Stack:** Java 17, Spring Boot, Jackson, OpenAI Responses API, browser extension JavaScript, JUnit 5, Node test runner

---

### Task 1: Add structured chemical candidate output to product normalization

**Files:**
- Modify: `src/main/java/com/example/gonggong/domain/analysis/openai/ProductNormalizeResult.java`
- Modify: `src/main/java/com/example/gonggong/domain/analysis/openai/ProductNormalizePromptBuilder.java`
- Modify: `src/main/java/com/example/gonggong/domain/analysis/openai/OpenAiProductNormalizeClient.java`
- Test: `src/test/java/com/example/gonggong/domain/analysis/openai/OpenAiProductNormalizeClientTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void parsesStructuredChemicalCandidatesFromOpenAiResponse() {
    FakeOpenAiTransport transport = new FakeOpenAiTransport("""
        {
          "output": [
            {
              "content": [
                {
                  "type": "output_text",
                  "text": "{\\"standardProductName\\":\\"테스트 상품\\",\\"searchKeywords\\":[\\"테스트\\"],\\"brandName\\":null,\\"category\\":\\"기타\\",\\"matchedRecallProductName\\":null,\\"modelName\\":null,\\"barcodeNum\\":null,\\"certNum\\":null,\\"materialKeywords\\":[],\\"targetUser\\":\\"일반\\",\\"chemicalCandidates\\":[{\\"name\\":\\"납\\",\\"casNumber\\":null,\\"englishName\\":\\"lead\\"}],\\"hskCandidateKeywords\\":[\\"테스트\\"],\\"primaryProductName\\":\\"테스트 상품\\",\\"productForm\\":\\"UNKNOWN\\",\\"primarySearchKeywords\\":[\\"테스트\\"],\\"kcCertificationSearchKeywords\\":[],\\"componentKeywords\\":[],\\"featureKeywords\\":[],\\"confidence\\":0.8}"
                }
              ]
            }
          ]
        }
        """);
    OpenAiProductNormalizeClient client = new OpenAiProductNormalizeClient(
        new OpenAiProperties("test-key", "gpt-test", "https://api.openai.com/v1/responses"),
        new ProductNormalizePromptBuilder(),
        transport,
        objectMapper
    );

    ProductNormalizeResult result = client.normalize(new ProductAnalyzeRequest("test", "", "", "", "aliexpress"));

    assertThat(result.chemicalCandidates()).hasSize(1);
    assertThat(result.chemicalCandidates().get(0).name()).isEqualTo("납");
    assertThat(result.chemicalCandidates().get(0).englishName()).isEqualTo("lead");
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat test --tests com.example.gonggong.domain.analysis.openai.OpenAiProductNormalizeClientTest.parsesStructuredChemicalCandidatesFromOpenAiResponse`
Expected: FAIL because the schema and record do not yet expose `chemicalCandidates`.

- [ ] **Step 3: Write minimal implementation**

```java
public record ChemicalIngredientCandidate(String name, String casNumber, String englishName) {}
```

```java
// Add `chemicalCandidates` to ProductNormalizeResult, wire it through all constructors,
// and add schema/prompt instructions that cap the list at 5 items and require name/cas/englishName.
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew.bat test --tests com.example.gonggong.domain.analysis.openai.OpenAiProductNormalizeClientTest.parsesStructuredChemicalCandidatesFromOpenAiResponse`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/gonggong/domain/analysis/openai/ProductNormalizeResult.java src/main/java/com/example/gonggong/domain/analysis/openai/ProductNormalizePromptBuilder.java src/main/java/com/example/gonggong/domain/analysis/openai/OpenAiProductNormalizeClient.java src/test/java/com/example/gonggong/domain/analysis/openai/OpenAiProductNormalizeClientTest.java
git commit -m "feat: add structured chemical candidates"
```

### Task 2: Search chemical candidates in the backend with CAS-first fallback

**Files:**
- Modify: `src/main/java/com/example/gonggong/domain/risk/service/ChemicalRiskService.java`
- Modify: `src/main/java/com/example/gonggong/domain/risk/chemical/HttpChemicalInformationClient.java`
- Modify: `src/main/java/com/example/gonggong/domain/risk/chemical/ChemicalApiJsonParser.java`
- Test: `src/test/java/com/example/gonggong/domain/risk/service/ChemicalRiskServiceTest.java`
- Test: `src/test/java/com/example/gonggong/domain/risk/chemical/HttpChemicalInformationClientTest.java`
- Test: `src/test/java/com/example/gonggong/domain/risk/chemical/ChemicalApiJsonParserTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void searchesCandidatesByCasThenEnglishThenKoreanName() {
    // candidate 1: CAS present
    // candidate 2: English only
    // candidate 3: Korean only
    // assert the client gets three lookup attempts in the expected order
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew.bat test --tests com.example.gonggong.domain.risk.service.ChemicalRiskServiceTest.searchesCandidatesByCasThenEnglishThenKoreanName`
Expected: FAIL because the service still expects flat ingredient strings.

- [ ] **Step 3: Write minimal implementation**

```java
for (ChemicalIngredientCandidate candidate : normalized.chemicalCandidates()) {
    // resolve lookup keys in order: casNumber, englishName, name
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew.bat test --tests com.example.gonggong.domain.risk.service.ChemicalRiskServiceTest.searchesCandidatesByCasThenEnglishThenKoreanName`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/gonggong/domain/risk/service/ChemicalRiskService.java src/main/java/com/example/gonggong/domain/risk/chemical/HttpChemicalInformationClient.java src/main/java/com/example/gonggong/domain/risk/chemical/ChemicalApiJsonParser.java src/test/java/com/example/gonggong/domain/risk/service/ChemicalRiskServiceTest.java src/test/java/com/example/gonggong/domain/risk/chemical/HttpChemicalInformationClientTest.java src/test/java/com/example/gonggong/domain/risk/chemical/ChemicalApiJsonParserTest.java
git commit -m "feat: search chemical candidates with cas first"
```

### Task 3: Keep the extension payload aligned with the structured candidates

**Files:**
- Modify: `extension/content.js`
- Modify: `extension/content.test.js`

- [ ] **Step 1: Write the failing test**

```javascript
test("risk dashboard request forwards structured chemical candidates when present", () => {
  const request = toRiskDashboardRequest(
    { productName: "테스트", description: "" },
    "1234567890",
    { lastResult: { chemicalCandidates: [{ name: "납", casNumber: null, englishName: "lead" }] } }
  );

  assert.deepEqual(request.chemicalCandidates, [{ name: "납", casNumber: null, englishName: "lead" }]);
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `node --test extension/content.test.js`
Expected: FAIL because the payload does not yet include `chemicalCandidates`.

- [ ] **Step 3: Write minimal implementation**

```javascript
chemicalCandidates: Array.isArray(normalized.chemicalCandidates) ? normalized.chemicalCandidates : [],
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `node --test extension/content.test.js`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add extension/content.js extension/content.test.js
git commit -m "feat: forward structured chemical candidates"
```

