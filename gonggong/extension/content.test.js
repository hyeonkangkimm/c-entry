const test = require("node:test");
const assert = require("node:assert/strict");

const { customsInlineDetailHtml, requestRiskDashboard, toRiskDashboardRequest } = require("./content");

test("risk dashboard request prefers AI refined harmful ingredients", () => {
  const request = toRiskDashboardRequest(
    {
      productName: "테스트 상품",
      description: "plastic case",
      price: { amount: 123, currency: "USD" },
    },
    "1234567890",
    {
      lastResult: {
        harmfulIngredients: ["납"],
      },
    }
  );

  assert.deepEqual(request.ingredients, ["납"]);
  assert.equal(request.productName, "테스트 상품");
  assert.equal(request.hskCode, "1234567890");
});

test("risk dashboard request does not reuse product-normalization chemical candidates", () => {
  const request = toRiskDashboardRequest(
    {
      productName: "테스트 상품",
      description: "plastic case",
      price: { amount: 123, currency: "USD" },
    },
    "1234567890",
    {
      lastResult: {
        harmfulIngredients: ["납"],
        chemicalCandidates: [{ name: "납", casNumber: null, englishName: "lead" }],
      },
    }
  );

  assert.deepEqual(request.ingredients, ["납"]);
  assert.deepEqual(request.chemicalCandidates, []);
});

test("risk dashboard analysis continues without an HSK candidate", async () => {
  const messages = [];
  const response = await requestRiskDashboard(
    { productName: "테스트 상품", description: "lead detected" },
    null,
    { lastResult: { harmfulIngredients: ["납"] } },
    async (message) => {
      messages.push(message);
      return { ok: true, data: { chemicalRisk: { status: "DANGER" } } };
    }
  );

  assert.equal(messages.length, 1);
  assert.equal(messages[0].type, "ANALYZE_RISK_DASHBOARD");
  assert.equal(messages[0].payload.hskCode, null);
  assert.deepEqual(messages[0].payload.ingredients, ["납"]);
  assert.equal(response.data.chemicalRisk.status, "DANGER");
});

test("customs inline detail excludes estimated duty", () => {
  const html = customsInlineDetailHtml({
    status: "SAFE",
    tariffType: "WTO",
    baseTariffRate: 8,
    finalTariffRate: 13,
    estimatedCustomsDuty: 14300,
    message: "관세율 안내",
  });

  assert.match(html, /최종 세율/);
  assert.match(html, /13%/);
  assert.doesNotMatch(html, /예상 관세액/);
  assert.doesNotMatch(html, /14,300원/);
});
