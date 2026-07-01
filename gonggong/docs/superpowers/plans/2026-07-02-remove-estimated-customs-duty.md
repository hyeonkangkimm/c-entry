# Remove Estimated Customs Duty Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove estimated customs-duty amounts from every frontend customs view while retaining tariff rates, types, guidance, and links.

**Architecture:** Update the shared customs summary renderer and the inline customs detail renderer only. Keep the backend response contract unchanged so this remains a frontend presentation change.

**Tech Stack:** Chrome Extension JavaScript, Node.js built-in test runner

---

### Task 1: Remove estimated duty from customs UI

**Files:**
- Modify: `extension/risk-view.js`
- Modify: `extension/content.js`
- Test: `extension/risk-view.test.js`
- Test: `extension/content.test.js`

- [ ] Change the customs-card test to reject both the estimated-duty label and formatted amount.
- [ ] Add an inline-detail test that rejects both the estimated-duty label and formatted amount.
- [ ] Run both tests and confirm they fail against the current renderers.
- [ ] Remove the estimated-duty summary block and detail row.
- [ ] Remove frontend formatting helpers that become unused.
- [ ] Run all extension tests and JavaScript syntax checks.
