const test = require("node:test");
const assert = require("node:assert/strict");

require("./seller-view.js");
const { brandRecallSummaryText, resolveBrandDisplayName, shouldEnableBrandRecall } = require("./brand-view.js");

test("shows clear text when brand is missing", () => {
  assert.equal(shouldEnableBrandRecall(""), false);
  assert.equal(brandRecallSummaryText({ brandName: "" }), "브랜드명을 찾지 못했습니다.");
});

test("does not leave modal in loading state before lookup starts", () => {
  assert.equal(
    brandRecallSummaryText({ brandName: "CASIO", loading: false }),
    "CASIO 브랜드 리콜 이력을 아직 조회하지 않았습니다."
  );
});

test("shows no-result message with brand name", () => {
  assert.equal(
    brandRecallSummaryText({ brandName: "CASIO", exists: false }),
    "CASIO 브랜드 리콜 이력이 확인되지 않았습니다."
  );
});

test("resolveBrandDisplayName prefers seller or store over DOM brand and AI brand", () => {
  assert.equal(
    resolveBrandDisplayName({
      domBrandName: "DOM Brand",
      aiBrandName: "AI Brand",
      sellerName: "Seller Store",
    }),
    "Seller Store"
  );
});

test("resolveBrandDisplayName falls back to DOM brand then AI brand", () => {
  assert.equal(
    resolveBrandDisplayName({
      domBrandName: "DOM Brand",
      aiBrandName: "AI Brand",
      sellerName: "",
    }),
    "DOM Brand"
  );
  assert.equal(
    resolveBrandDisplayName({
      domBrandName: "",
      aiBrandName: "AI Brand",
      sellerName: "",
    }),
    "AI Brand"
  );
});

test("resolveBrandDisplayName skips marketplace CTA text before falling back", () => {
  assert.equal(
    resolveBrandDisplayName({
      domBrandName: "참여하기",
      aiBrandName: "",
      sellerName: "Seller Store",
    }),
    "Seller Store"
  );
});

test("resolveBrandDisplayName skips title-derived brand plus model number before seller", () => {
  assert.equal(
    resolveBrandDisplayName({
      domBrandName: "Redragon M908",
      aiBrandName: "",
      sellerName: "Redragon Gaming Store",
    }),
    "Redragon Gaming Store"
  );
});
