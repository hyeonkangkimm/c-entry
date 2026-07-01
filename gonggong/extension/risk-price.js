(function initializeRiskPrice(globalScope) {
  const SITE_PRICE_SELECTORS = {
    aliexpress: [
      "[data-pl='product-price']",
      "[data-pl='product-price-current']",
      ".product-price-current",
      "[class*='price--current']",
      "[class*='price-current']",
      "[class*='product-price']",
      "meta[property='product:price:amount']",
      "meta[property='og:price:amount']",
      "meta[itemprop='price']",
    ],
    temu: [
      "[data-testid='product-price']",
      "[data-testid*='price']",
      "[class*='price']",
      "meta[property='product:price:amount']",
      "meta[property='og:price:amount']",
      "meta[itemprop='price']",
    ],
  };

  function extractProductPrice(site, documentLike = globalScope.document) {
    if (!documentLike?.querySelectorAll) {
      return null;
    }

    const selectors = [
      ...(SITE_PRICE_SELECTORS[site] || []),
      "meta[property='product:price:amount']",
      "meta[property='og:price:amount']",
      "meta[itemprop='price']",
    ];

    for (const selector of selectors) {
      const elements = Array.from(documentLike.querySelectorAll(selector));
      for (const element of elements) {
        const parsed = parsePriceText(readPriceText(element));
        if (parsed) {
          return parsed;
        }
      }
    }

    return null;
  }

  function readPriceText(element) {
    if (!element) {
      return "";
    }
    const content = element.getAttribute?.("content");
    const ariaLabel = element.getAttribute?.("aria-label");
    const text = element.textContent;
    return normalizeText(content || ariaLabel || text || "");
  }

  function parsePriceText(value) {
    const sourceText = normalizeText(value);
    if (!sourceText) {
      return null;
    }

    const currency = detectCurrency(sourceText);
    if (!currency) {
      return null;
    }

    const amount = extractAmount(sourceText, currency);
    if (amount == null || amount < 0) {
      return null;
    }

    return {
      amount,
      currency,
      sourceText,
    };
  }

  function detectCurrency(text) {
    const normalized = text.toUpperCase();
    if (/[₩￦]/.test(text) || normalized.includes("KRW") || text.includes("원")) {
      return "KRW";
    }
    if (/\$/.test(text) || normalized.includes("USD") || normalized.includes("US $")) {
      return "USD";
    }
    if (normalized.includes("CNY") || normalized.includes("RMB") || /CN¥/.test(text)) {
      return "CNY";
    }
    return null;
  }

  function extractAmount(text, currency) {
    if (currency === "KRW") {
      const wonMatches = [
        ...text.matchAll(/[₩￦]\s*([0-9][0-9,]*(?:\.[0-9]+)?)/g),
        ...text.matchAll(/([0-9][0-9,]*(?:\.[0-9]+)?)\s*원/g),
        ...text.matchAll(/KRW\s*([0-9][0-9,]*(?:\.[0-9]+)?)/gi),
      ];
      return lowestNumericMatch(wonMatches, true);
    }

    if (currency === "USD") {
      const dollarMatches = [
        ...text.matchAll(/\$\s*([0-9][0-9,]*(?:\.[0-9]+)?)/g),
        ...text.matchAll(/USD\s*([0-9][0-9,]*(?:\.[0-9]+)?)/gi),
      ];
      return lowestNumericMatch(dollarMatches, false);
    }

    if (currency === "CNY") {
      const cnyMatches = [
        ...text.matchAll(/(?:CNY|RMB|CN¥)\s*([0-9][0-9,]*(?:\.[0-9]+)?)/gi),
      ];
      return lowestNumericMatch(cnyMatches, false);
    }

    return null;
  }

  function lowestNumericMatch(matches, roundToInteger) {
    const values = matches
      .map((match) => Number(String(match[1]).replaceAll(",", "")))
      .filter((number) => Number.isFinite(number));

    if (!values.length) {
      return null;
    }

    const lowest = Math.min(...values);
    return roundToInteger ? Math.round(lowest) : lowest;
  }

  function normalizeText(value) {
    return String(value || "").replace(/\s+/g, " ").trim();
  }

  const api = { extractProductPrice, parsePriceText };
  globalScope.RiskPrice = api;

  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  }
})(typeof globalThis !== "undefined" ? globalThis : this);
