(function initializeBrandView(globalScope) {
  function shouldEnableBrandRecall(brandName) {
    return Boolean(normalizeBrandName(brandName));
  }

  function resolveBrandDisplayName(state = {}) {
    return normalizeCandidate(state.sellerName)
      || normalizeCandidate(state.domBrandName)
      || normalizeCandidate(state.aiBrandName);
  }

  function brandRecallSummaryText(state = {}) {
    const brandName = normalizeBrandName(state.brandName);
    if (!brandName) {
      return "브랜드명을 찾지 못했습니다.";
    }
    if (state.loading) {
      return `${brandName} 브랜드 리콜 이력을 조회하는 중입니다.`;
    }
    if (state.error) {
      return `브랜드 리콜 이력을 불러오지 못했습니다. ${state.error}`;
    }
    if (state.exists === true) {
      return state.message || `${brandName} 브랜드의 다른 제품 리콜 이력이 있습니다.`;
    }
    if (state.exists === false) {
      return `${brandName} 브랜드 리콜 이력이 확인되지 않았습니다.`;
    }
    return `${brandName} 브랜드 리콜 이력을 아직 조회하지 않았습니다.`;
  }

  function normalizeBrandName(brandName) {
    return String(brandName || "").replace(/\s+/g, " ").trim();
  }

  function normalizeCandidate(value) {
    const normalized = normalizeBrandName(value);
    if (!normalized) {
      return "";
    }
    if (globalScope.SellerView && typeof globalScope.SellerView.normalizeSellerName === "function") {
      const sellerNormalized = globalScope.SellerView.normalizeSellerName(normalized);
      if (isLikelyTitleBrandWithModelCode(sellerNormalized)) {
        return "";
      }
      return sellerNormalized;
    }
    if (isLikelyTitleBrandWithModelCode(normalized)) {
      return "";
    }
    return normalized;
  }

  function isLikelyTitleBrandWithModelCode(value) {
    const parts = normalizeBrandName(value).split(" ");
    return parts.length === 2 && /[A-Za-z]/.test(parts[0]) && /\d/.test(parts[1]);
  }

  const api = {
    brandRecallSummaryText,
    resolveBrandDisplayName,
    shouldEnableBrandRecall,
  };
  globalScope.BrandView = api;

  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  }
})(typeof globalThis !== "undefined" ? globalThis : this);
