const test = require("node:test");
const assert = require("node:assert/strict");

const SellerView = require("./seller-view");

test("extractSellerNameFromText returns AliExpress store name before seller markers", () => {
  const text = `
    WEYLI HOME Store
    (거래 업체)
    AliExpress의 약속
    무료 배송
  `;

  assert.equal(SellerView.extractSellerNameFromText(text), "WEYLI HOME Store");
});

test("extractSellerNameFromText returns repeated store line near rating text", () => {
  const text = `
    수량
    1
    재고수량 1000
    92
    WEYLI HOME Store
    96.0% 긍정적인 평가 | 1563 팔로워
  `;

  assert.equal(SellerView.extractSellerNameFromText(text), "WEYLI HOME Store");
});

test("extractSellerNameFromText returns value after Korean store-name label", () => {
  const text = `
    스토어명:
    Shop1105238735 Store
    스토어 번호:
    1105238735
    영업소재지:
    China
    업체정보
  `;

  assert.equal(SellerView.extractSellerNameFromText(text), "Shop1105238735 Store");
});

test("normalizeSellerName removes seller suffix noise but keeps Store when part of displayed name", () => {
  assert.equal(SellerView.normalizeSellerName("  WEYLI HOME Store  "), "WEYLI HOME Store");
  assert.equal(SellerView.normalizeSellerName("AliExpress"), "");
  assert.equal(SellerView.normalizeSellerName("aliexpress."), "");
  assert.equal(SellerView.normalizeSellerName("AliExpress.com"), "");
});

test("normalizeSellerName rejects Temu shipping and minimum-order notices", () => {
  assert.equal(SellerView.normalizeSellerName("최소 주문 13,000원(판매자 배송 상품 제외"), "");
  assert.equal(SellerView.normalizeSellerName("무료 배송"), "");
  assert.equal(SellerView.normalizeSellerName("₩1,300 쿠폰 지급"), "");
});

test("normalizeSellerName rejects generic Temu seller button labels", () => {
  assert.equal(SellerView.normalizeSellerName("판매자 >"), "");
  assert.equal(SellerView.normalizeSellerName("판매자"), "");
  assert.equal(SellerView.normalizeSellerName("14 판매됨 | 판매자 >"), "");
});

test("normalizeSellerName rejects app download and marketplace chrome text", () => {
  assert.equal(SellerView.normalizeSellerName("다운로드 Google Play"), "");
  assert.equal(SellerView.normalizeSellerName("Google Play"), "");
  assert.equal(SellerView.normalizeSellerName("App Store에서 다운로드"), "");
  assert.equal(SellerView.normalizeSellerName("참여하기"), "");
  assert.equal(SellerView.normalizeSellerName("가입하기"), "");
});
