(function initializeSellerView(globalScope) {
  function extractSellerNameFromText(text) {
    const normalizedText = String(text || "").replace(/\r/g, "\n");
    if (!normalizedText.trim()) {
      return "";
    }

    const lines = normalizedText
      .split("\n")
      .map((line) => normalizeSellerName(line))
      .filter(Boolean);

    for (let index = 0; index < lines.length; index += 1) {
      if (isStoreNameLabel(lines[index])) {
        const nextValue = lines.slice(index + 1).find((line) => isStoreLikeName(line) && !isStoreNameLabel(line));
        if (nextValue) {
          return nextValue;
        }
      }
    }

    for (let index = 0; index < lines.length; index += 1) {
      const current = lines[index];
      const next = lines[index + 1] || "";
      if (isStoreLikeName(current) && !isStoreNameLabel(current) && isSellerContext(next)) {
        return current;
      }
    }

    for (let index = 0; index < lines.length; index += 1) {
      const current = lines[index];
      const next = lines[index + 1] || "";
      if (isStoreLikeName(current) && !isStoreNameLabel(current) && isRatingContext(next)) {
        return current;
      }
    }

    return lines.find((line) => isStoreLikeName(line) && !isStoreNameLabel(line)) || "";
  }

  function normalizeSellerName(value) {
    const normalized = String(value || "")
      .replace(/\s+/g, " ")
      .replace(/^\d+\s+/, "")
      .trim();
    if (!normalized || normalized.length < 2 || normalized.length > 80) {
      return "";
    }

    const lowered = normalized.toLowerCase();
    const canonical = lowered.replace(/[.\s]+$/g, "").replace(/\.com$/i, "");
    if (["aliexpress", "temu", "store", "shop", "seller", "official store"].includes(canonical)) {
      return "";
    }
    if (isGenericSellerButton(normalized)) {
      return "";
    }
    if (isCommerceNotice(normalized)) {
      return "";
    }
    if (isMarketplaceChromeText(normalized)) {
      return "";
    }
    return normalized;
  }

  function isStoreLikeName(value) {
    return /\b(store|shop|official)\b/i.test(value) || /스토어|상점|셀러|판매자/i.test(value);
  }

  function isStoreNameLabel(value) {
    return /^스토어\s*명\s*:?\s*$/i.test(value) || /^store\s*name\s*:?\s*$/i.test(value);
  }

  function isSellerContext(value) {
    return /거래 업체|판매자|셀러|store|seller|shop/i.test(value);
  }

  function isRatingContext(value) {
    return /긍정적인 평가|팔로워|follower|followers|positive/i.test(value);
  }

  function isCommerceNotice(value) {
    return /최소\s*주문|판매자\s*배송|무료\s*배송|배송|쿠폰|반품|개인\s*정보|안심\s*결제|₩|원\b|할인|장바구니/i.test(value);
  }

  function isGenericSellerButton(value) {
    return /^(?:(?:\d+\s*)?판매됨\s*\|\s*)?판매자\s*>?$/i.test(value.trim());
  }

  function isMarketplaceChromeText(value) {
    return /google\s*play|app\s*store|다운로드|download|temu\s*app|앱에서|참여하기|가입하기|로그인|sign\s*in|join\s*now/i.test(value);
  }

  const api = {
    extractSellerNameFromText,
    normalizeSellerName,
  };
  globalScope.SellerView = api;

  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  }
})(typeof globalThis !== "undefined" ? globalThis : this);
