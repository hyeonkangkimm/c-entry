const test = require("node:test");
const assert = require("node:assert/strict");

const { parsePriceText, extractProductPrice } = require("./risk-price.js");

test("parses Korean won prices", () => {
  assert.deepEqual(parsePriceText("₩ 12,345"), {
    amount: 12345,
    currency: "KRW",
    sourceText: "₩ 12,345",
  });
  assert.deepEqual(parsePriceText("12,345원"), {
    amount: 12345,
    currency: "KRW",
    sourceText: "12,345원",
  });
});

test("parses US dollar prices and uses the lower bound from a range", () => {
  assert.deepEqual(parsePriceText("US $5.99 - $7.99"), {
    amount: 5.99,
    currency: "USD",
    sourceText: "US $5.99 - $7.99",
  });
});

test("ignores numeric text without a currency marker", () => {
  assert.equal(parsePriceText("할인 50% 평점 4.8"), null);
  assert.equal(parsePriceText("무료배송 100개 판매"), null);
});

test("extracts the first valid product price from matching DOM selectors", () => {
  const documentLike = {
    querySelectorAll(selector) {
      if (selector === "[data-pl='product-price']") {
        return [{ textContent: "US $18.50" }];
      }
      return [];
    },
  };

  assert.deepEqual(extractProductPrice("aliexpress", documentLike), {
    amount: 18.5,
    currency: "USD",
    sourceText: "US $18.50",
  });
});
