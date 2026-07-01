# KC Verification Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render a clear KC lookup failure message and a safe link to Safety Korea whenever certification matching is not valid.

**Architecture:** Add a pure, exported KC card renderer to `risk-view.js`, following the existing recall and customs renderer pattern. `content.js` delegates both API-result and HSK-failure KC cards to it, while `overlay.css` owns the link presentation.

**Tech Stack:** Browser extension JavaScript, CommonJS-compatible browser module, Node.js built-in test runner, CSS

---

### Task 1: KC card renderer

**Files:**
- Modify: `extension/risk-view.js`
- Test: `extension/risk-view.test.js`

- [ ] **Step 1: Write failing tests for failure and success cards**

Add tests that call `kcRiskCardHtml` with `certificationValid: false` for `DANGER`, `WARNING`, `UNKNOWN`, and `UNAVAILABLE`. Assert that each result contains `정보가 없습니다.`, the configured button text, an escaped HTTP(S) URL, `target="_blank"`, and `rel="noopener noreferrer"`. Add a valid result test asserting that the failure text and link are absent.

- [ ] **Step 2: Run the tests and verify RED**

Run: `node --test extension/risk-view.test.js`

Expected: FAIL because `kcRiskCardHtml` is not exported.

- [ ] **Step 3: Implement the minimal renderer**

Add `kcRiskCardHtml(kc, detail)` and a URL normalizer to `risk-view.js`. Treat only `certificationValid === true` as matched. Prefer API-provided button text and URL, fall back to `제품안전정보센터에서 실시간 검증하기` and `https://www.safetykorea.kr/`, and reject non-HTTP(S) URL schemes. Export the renderer through the existing `api` object.

- [ ] **Step 4: Run the tests and verify GREEN**

Run: `node --test extension/risk-view.test.js`

Expected: all `risk-view` tests PASS.

### Task 2: Connect both KC rendering paths

**Files:**
- Modify: `extension/content.js`
- Test: `extension/risk-view.test.js`

- [ ] **Step 1: Add a failing HSK-failure rendering test**

Add a renderer test with an empty KC object and assert that the default failure message and default Safety Korea link are present. This represents the HSK failure path where no KC API result exists.

- [ ] **Step 2: Run the test and verify RED**

Run: `node --test extension/risk-view.test.js`

Expected: FAIL until missing KC fields use the required fallback UI.

- [ ] **Step 3: Replace inline KC cards**

In `renderRiskDashboard`, call `RiskView.kcRiskCardHtml(kc, kcDetailText(...))`. In `kcCardFromDomPayload`, call the same renderer with `certificationValid: false`, DOM-derived certification details, and default verification fields. Keep non-KC cards unchanged.

- [ ] **Step 4: Run syntax and unit checks**

Run: `node --check extension/content.js` and `node --test extension/risk-view.test.js`

Expected: syntax check succeeds and all tests PASS.

### Task 3: Style and regression verification

**Files:**
- Modify: `extension/overlay.css`

- [ ] **Step 1: Add focused link styles**

Add `.isg-kc-verification-link` styles that render the external action as a visible button, preserve keyboard focus visibility, and fit the existing risk-card colors and spacing.

- [ ] **Step 2: Run the extension test suite**

Run: `node --test extension/*.test.js`

Expected: all extension tests PASS.

- [ ] **Step 3: Run JavaScript syntax checks**

Run: `Get-ChildItem extension -Filter *.js | ForEach-Object { node --check $_.FullName }`

Expected: every JavaScript file exits successfully.

- [ ] **Step 4: Inspect the final diff manually**

Confirm that only the KC failure UI, its reusable renderer, tests, CSS, and planning documents changed. Confirm that no `javascript:`, `data:`, or malformed external link can reach the rendered `href`.

> Git commit steps are omitted because the workspace is not currently recognized as a Git repository.
