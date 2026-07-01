# Chemical Risk Without HSK Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue chemical-risk analysis and display harmful ingredients when HSK matching returns no candidate.

**Architecture:** Extract the risk-dashboard message request into a testable function that accepts a nullable HSK candidate. The HSK no-match branch calls it with `null`, then renders the existing HSK/KC fallback cards together with the chemical-risk card returned by the backend.

**Tech Stack:** Chrome Extension JavaScript, Node.js built-in test runner

---

### Task 1: Continue chemical analysis after HSK no-match

**Files:**
- Modify: `extension/content.js`
- Test: `extension/content.test.js`

- [ ] Add a failing test proving the risk request is sent with `hskCode: null` and preserved harmful ingredients.
- [ ] Run `node --test extension/content.test.js` and confirm the new test fails because the helper is missing.
- [ ] Add the nullable-HSK request helper and use it from both matched and unmatched HSK branches.
- [ ] Render `chemicalRiskCardHtml` in the HSK no-match grid using the returned API response.
- [ ] Run `node --test extension/content.test.js extension/risk-view.test.js` and confirm all tests pass.
- [ ] Run `node --check extension/content.js` and confirm syntax validation passes.
