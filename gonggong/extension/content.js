  const STATE = {
    lastAnalyzedKey: "",
    lastResult: null,
    lastRiskDashboard: null,
    lastHskCandidate: null,
    analyzeTimerId: null,
    hidePanelTimerId: null,
    riskLoadingTimerIds: [],
    pinned: false,
    lastPayload: null,
  };

const SITE_CONFIGS = [
  {
    site: "aliexpress",
    hostIncludes: ["aliexpress.com"],
    productNameSelectors: [
      "h1[data-pl='product-title']",
      "h1.product-title-text",
      "h1",
    ],
    descriptionSelectors: [
      "[data-pl='product-description']",
      "#product-description",
      ".product-description",
      "meta[name='description']",
    ],
    brandSelectors: [
      "meta[property='og:brand']",
      "meta[name='brand']",
      "[data-pl='brand-name']",
      "[class*='brand']",
      "a[href*='store']",
    ],
    sellerSelectors: [
      "[data-pl='store-name']",
      "[class*='store']",
      "[class*='seller']",
      "a[href*='/store/']",
      "a[href*='store.aliexpress']",
      "a[href*='store']",
    ],
    storeLinkSelectors: [
      "a[href*='/store/']",
      "a[href*='store.aliexpress']",
      "a[href*='store']",
    ],
    allowSellerTextFallback: true,
    imageSelectors: [
      "img.magnifier-image",
      "img[data-pl='product-main-image']",
      ".image-view img",
      "img",
    ],
  },
  {
    site: "temu",
    hostIncludes: ["temu.com"],
    productNameSelectors: [
      "h1",
      "[data-testid='product-title']",
      "[class*='title']",
    ],
    descriptionSelectors: [
      "[data-testid='product-description']",
      "[class*='description']",
      "meta[name='description']",
    ],
    brandSelectors: [
      "meta[property='og:brand']",
      "meta[name='brand']",
      "[data-testid*='brand']",
      "a[href*='store']",
    ],
    sellerSelectors: [
      "[data-testid*='seller']",
      "[data-testid*='store']",
      "a[href*='shop']",
      "a[href*='store']",
    ],
    storeLinkSelectors: [
      "a[href*='shop']",
      "a[href*='store']",
      "a[href*='seller']",
    ],
    allowSellerTextFallback: false,
    imageSelectors: [
      "[data-testid='product-image'] img",
      "[class*='image'] img",
      "img",
    ],
  },
];

if (typeof document !== "undefined" && typeof window !== "undefined") {
  init();
}

function init() {
  ensureOverlay();
  scheduleAnalyze("initial-load");
  observeDomChanges();
  observeUrlChanges();
}

function scheduleAnalyze(reason) {
  window.clearTimeout(STATE.analyzeTimerId);
  STATE.analyzeTimerId = window.setTimeout(() => {
    analyzeCurrentPage(reason);
  }, 700);
}

async function analyzeCurrentPage(reason) {
  const payload = extractProductPayload();

  if (!payload || !payload.productName) {
    updateBadge("REVIEW", "상품 정보를 기다리는 중", "상품명 추출을 재시도하고 있습니다.");
    return;
  }

  const analyzeKey = [
    payload.pageUrl,
    payload.productName,
    payload.kcCertificationNumber || "",
    payload.kcCertificationType || "",
    (payload.kcContextTexts || []).join("|").slice(0, 180),
  ].join("|");
  if (STATE.lastAnalyzedKey === analyzeKey) {
    return;
  }

  STATE.lastAnalyzedKey = analyzeKey;
  updateBadge("REVIEW", "분석 중", `상품 정보를 분석하고 있습니다. (${reason})`);

  try {
    const response = await chrome.runtime.sendMessage({
      type: "ANALYZE_PRODUCT",
      payload,
    });

    if (!response || !response.ok) {
      throw new Error(response?.error || "No response from background worker");
    }

    STATE.lastResult = response.data;
    updateOverlayFromResult(response.data, payload);
  } catch (error) {
    updateBadge("REVIEW", "분석 실패", error instanceof Error ? error.message : "알 수 없는 오류");
  }
}

function extractProductPayload() {
  const config = findSiteConfig();
  if (!config) {
    return null;
  }

  const productNameElement = findFirstElement(config.productNameSelectors);
  const productName = resolveProductName(productNameElement, config.site);
  const imageScope = getImageScope(config.site, productNameElement);
  const price = RiskPrice.extractProductPrice(config.site);
  const kcContext = extractKcContext();

  return {
    productName,
    description: mergeDescriptionWithKcContext(readFirstText(config.descriptionSelectors), kcContext),
    brandName: extractBrandName(config.brandSelectors, productName),
    sellerName: extractSellerName(config),
    storePageUrl: extractStorePageUrl(config.storeLinkSelectors),
    imageUrl: readFirstImageUrl(config.imageSelectors, productNameElement, imageScope),
    price,
    kcCertificationNumber: kcContext.certificationNumber || "",
    kcCertificationType: kcContext.certificationType || "",
    kcContextTexts: kcContext.contextTexts,
    pageUrl: window.location.href,
    site: config.site,
  };
}

function resolveProductName(productNameElement, site) {
  const candidates = [];

  if (productNameElement) {
    candidates.push(normalizeText(readElementText(productNameElement)));
  }

  const titleText = normalizeText(document.title.replace(/^\s*AliExpress\s*[-|]\s*/i, ""));
  if (titleText) {
    candidates.push(titleText);
  }

  const metaTitle = document.querySelector("meta[property='og:title'], meta[name='title']");
  const metaTitleText = normalizeText(metaTitle?.getAttribute("content") || "");
  if (metaTitleText) {
    candidates.push(metaTitleText);
  }

  const fallback = candidates.find((candidate) => isMeaningfulProductName(candidate, site));
  return fallback || "";
}

function findSiteConfig() {
  const hostname = window.location.hostname;
  return SITE_CONFIGS.find((config) =>
    config.hostIncludes.some((hostPart) => hostname.includes(hostPart))
  );
}

function readFirstText(selectors) {
  const element = findFirstElement(selectors);
  return normalizeText(readElementText(element));
}

function extractBrandName(selectors, productName) {
  const explicitBrand = normalizeText(readFirstText(selectors));
  if (isMeaningfulBrandName(explicitBrand)) {
    return explicitBrand;
  }

  const metaSiteName = document.querySelector("meta[property='og:site_name']");
  const metaSiteNameText = normalizeText(metaSiteName?.getAttribute("content") || "");
  if (isMeaningfulBrandName(metaSiteNameText)) {
    return metaSiteNameText;
  }

  const titleBrandMatch = normalizeText(productName).match(/^([A-Za-z][A-Za-z0-9'-]*(?:\s+[A-Za-z][A-Za-z0-9'-]*)?)/);
  if (titleBrandMatch && isMeaningfulBrandName(titleBrandMatch[1])) {
    return titleBrandMatch[1];
  }

  return "";
}

function extractSellerName(config) {
  const explicitSeller = SellerView.normalizeSellerName(readFirstText(config.sellerSelectors || []));
  if (explicitSeller) {
    return explicitSeller;
  }
  if (!config.allowSellerTextFallback) {
    return "";
  }
  return SellerView.extractSellerNameFromText(document.body?.innerText || "");
}

function extractStorePageUrl(selectors) {
  const element = findFirstElement(selectors || []);
  const href = element?.getAttribute("href") || "";
  if (!href || href.startsWith("#") || href.startsWith("javascript:")) {
    return "";
  }
  return new URL(href, window.location.href).toString();
}

function isMeaningfulBrandName(value) {
  const normalized = normalizeText(value);
  if (!normalized || normalized.length < 2 || normalized.length > 80) {
    return false;
  }
  if (!SellerView.normalizeSellerName(normalized)) {
    return false;
  }

  const lowered = normalized.toLowerCase();
  return ![
    "aliexpress",
    "temu",
    "store",
    "shop",
    "official store",
    "product",
    "brand",
  ].includes(lowered);
}

function findFirstElement(selectors) {
  for (const selector of selectors) {
    const element = document.querySelector(selector);
    if (!element) {
      continue;
    }

    if (readElementText(element)) {
      return element;
    }
  }

  return null;
}

function readElementText(element) {
  if (!element) {
    return "";
  }

  if (element.tagName === "META") {
    return element.getAttribute("content") || "";
  }

  return element.textContent || "";
}

function isMeaningfulProductName(value, site) {
  const normalized = normalizeText(value);
  if (!normalized) {
    return false;
  }

  if (normalized.length < 3) {
    return false;
  }

  const lowered = normalized.toLowerCase();
  const bannedExact = new Set([
    "aliexpress",
    "temu",
    "product",
    "item",
    "detail",
    "home",
    "page",
  ]);

  if (bannedExact.has(lowered)) {
    return false;
  }

  if (site === "aliexpress") {
    if (lowered.includes("aliexpress")) {
      return false;
    }

    if (lowered.startsWith("buy ") || lowered.startsWith("shop ")) {
      return false;
    }
  }

  return true;
}

function readFirstImageUrl(selectors, titleElement, imageScope) {
  const candidates = collectImageCandidates(selectors, imageScope);
  const bestCandidate = candidates
    .map((candidate) => ({
      candidate,
      score: scoreImageCandidate(candidate.image, candidate.source, titleElement),
    }))
    .filter(({ score }) => score > 0)
    .sort((left, right) => right.score - left.score)[0]?.candidate;

  if (bestCandidate?.image) {
    const { image } = bestCandidate;
    const src = image.currentSrc || image.getAttribute("src") || image.getAttribute("data-src");
    if (src) {
      return new URL(src, window.location.href).href;
    }
  }

  const metaImage = document.querySelector("meta[property='og:image'], meta[name='og:image']");
  const metaContent = metaImage?.getAttribute("content");
  if (metaContent) {
    return new URL(metaContent, window.location.href).href;
  }

  return "";
}

function collectImageCandidates(selectors, imageScope) {
  const images = [];

  for (const selector of selectors) {
    (imageScope || document).querySelectorAll(selector).forEach((image) => {
      images.push({ image, source: "selector" });
    });
  }

  (imageScope || document).querySelectorAll("img").forEach((image) => {
    images.push({ image, source: "generic" });
  });

  if (images.length === 0 && imageScope) {
    document.querySelectorAll(selectors.join(",")).forEach((image) => {
      images.push({ image, source: "selector-fallback" });
    });

    document.querySelectorAll("img").forEach((image) => {
      images.push({ image, source: "generic-fallback" });
    });
  }

  return dedupeImageCandidates(images);
}

function dedupeImageCandidates(candidates) {
  const seen = new Set();
  const unique = [];

  for (const candidate of candidates) {
    if (!candidate.image) {
      continue;
    }

    const key = candidate.image.currentSrc || candidate.image.getAttribute("src") || candidate.image.getAttribute("data-src");
    if (!key) {
      continue;
    }

    if (seen.has(key)) {
      continue;
    }

    seen.add(key);
    unique.push(candidate);
  }

  return unique;
}

function scoreImageCandidate(image, source, titleElement) {
  const style = window.getComputedStyle(image);
  if (!style || style.display === "none" || style.visibility === "hidden" || Number(style.opacity) === 0) {
    return 0;
  }

  if (style.position === "fixed" || style.position === "sticky") {
    return 0;
  }

  const rect = image.getBoundingClientRect();
  const viewportWidth = window.innerWidth || document.documentElement.clientWidth || 0;
  const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 0;
  if (viewportWidth <= 0 || viewportHeight <= 0) {
    return 0;
  }

  const visibleWidth = Math.max(
    0,
    Math.min(rect.right, viewportWidth) - Math.max(rect.left, 0)
  );
  const visibleHeight = Math.max(
    0,
    Math.min(rect.bottom, viewportHeight) - Math.max(rect.top, 0)
  );
  const visibleArea = visibleWidth * visibleHeight;
  const rectArea = Math.max(0, rect.width) * Math.max(0, rect.height);
  const naturalArea = Math.max(0, image.naturalWidth || 0) * Math.max(0, image.naturalHeight || 0);
  const candidateArea = Math.max(visibleArea, rectArea, naturalArea);

  if (candidateArea <= 0) {
    return 0;
  }

  if (visibleArea <= 0) {
    return 0;
  }

  const minUsefulArea = 120 * 120;
  if (visibleArea < minUsefulArea && naturalArea < minUsefulArea) {
    return 0;
  }

  if (isDecorativeImage(image)) {
    return 0;
  }

  const contextBonus = getProductContextBonus(image);
  const titleBonus = getTitleProximityBonus(image, titleElement);
  const sourceBonus = source === "selector" ? 1.55 : 1;
  const aspectPenalty = getAspectScore(rect.width, rect.height);
  const centerDistance = Math.abs((rect.left + rect.width / 2) - viewportWidth / 2)
    + Math.abs((rect.top + rect.height / 2) - viewportHeight / 2);
  const centerBonus = Math.max(0.6, 1 - centerDistance / Math.max(viewportWidth + viewportHeight, 1));
  const sizeScore = Math.min(visibleArea / (viewportWidth * viewportHeight), 1);
  const visibilityBonus = rect.top >= 0 && rect.left >= 0 ? 1.05 : 1;

  return visibleArea * (0.65 + sizeScore) * aspectPenalty * centerBonus * visibilityBonus * contextBonus * titleBonus * sourceBonus;
}

function getAspectScore(width, height) {
  if (width <= 0 || height <= 0) {
    return 0.8;
  }

  const ratio = Math.max(width, height) / Math.max(Math.min(width, height), 1);
  if (ratio <= 2) {
    return 1;
  }

  if (ratio <= 3.5) {
    return 0.75;
  }

  return 0.45;
}

function isDecorativeImage(image) {
  const className = `${image.className || ""}`.toLowerCase();
  const id = `${image.id || ""}`.toLowerCase();
  const alt = `${image.getAttribute("alt") || ""}`.toLowerCase();
  const src = `${image.currentSrc || image.getAttribute("src") || image.getAttribute("data-src") || ""}`.toLowerCase();
  const haystack = `${className} ${id} ${alt} ${src}`;
  const patterns = [
    "logo",
    "banner",
    "advert",
    "ad-",
    "/ads/",
    "sponsor",
    "promo",
    "icon",
    "sprite",
    "avatar",
    "badge",
    "footer",
    "header",
    "nav",
  ];

  return patterns.some((pattern) => haystack.includes(pattern));
}

function getProductContextBonus(image) {
  let current = image;
  let depth = 0;

  while (current && depth < 6) {
    const className = `${current.className || ""}`.toLowerCase();
    const id = `${current.id || ""}`.toLowerCase();
    const role = `${current.getAttribute?.("role") || ""}`.toLowerCase();
    const tagName = `${current.tagName || ""}`.toLowerCase();
    const haystack = `${className} ${id} ${role} ${tagName}`;

    if ([
      "product",
      "gallery",
      "main",
      "detail",
      "pdp",
      "item",
      "goods",
      "image",
      "media",
    ].some((pattern) => haystack.includes(pattern))) {
      return 1.35;
    }

    if (["main", "article"].includes(tagName) || role === "main") {
      return 1.2;
    }

    current = current.parentElement;
    depth += 1;
  }

  return 1;
}

function getImageScope(site, titleElement) {
  if (site !== "aliexpress" || !titleElement) {
    return null;
  }

  const modalScope = titleElement.closest(
    "[role='dialog'], [aria-modal='true'], .modal, .dialog, .popup, .drawer"
  );
  if (modalScope) {
    return modalScope;
  }

  const structuralScope = titleElement.closest(
    ".product, .product-detail, .product-main, .pdp, .image-view, .gallery"
  );
  if (structuralScope) {
    return structuralScope;
  }

  let current = titleElement.parentElement;
  let depth = 0;
  while (current && depth < 8) {
    const imageMatch = current.querySelector("img");
    const selectorMatch = current.querySelector(
      "img.magnifier-image, img[data-pl='product-main-image'], .image-view img, img"
    );

    if (imageMatch && selectorMatch) {
      return current;
    }

    current = current.parentElement;
    depth += 1;
  }

  return null;
}

function getTitleProximityBonus(image, titleElement) {
  if (!titleElement) {
    return 1;
  }

  const titleAncestors = new Set();
  let currentTitle = titleElement;
  let titleDepth = 0;
  while (currentTitle && titleDepth < 6) {
    titleAncestors.add(currentTitle);
    currentTitle = currentTitle.parentElement;
    titleDepth += 1;
  }

  let current = image;
  let depth = 0;
  while (current && depth < 8) {
    if (titleAncestors.has(current)) {
      return depth <= 2 ? 1.8 : 1.25;
    }

    const className = `${current.className || ""}`.toLowerCase();
    const id = `${current.id || ""}`.toLowerCase();
    const haystack = `${className} ${id}`;
    if (titleAncestors.size > 0 && (haystack.includes("product") || haystack.includes("gallery") || haystack.includes("detail"))) {
      return 1.2;
    }

    current = current.parentElement;
    depth += 1;
  }

  return 0.75;
}

function normalizeText(value) {
  return (value || "").replace(/\s+/g, " ").trim();
}

function mergeDescriptionWithKcContext(description, kcContext) {
  const baseDescription = normalizeText(description);
  const kcText = normalizeText((kcContext?.contextTexts || []).join(" "));
  if (!kcText) {
    return baseDescription;
  }
  if (!baseDescription) {
    return kcText;
  }
  if (baseDescription.includes(kcText)) {
    return baseDescription;
  }
  return normalizeText(`${baseDescription} ${kcText}`);
}

function extractKcContext() {
  const contexts = collectKcContextTexts();
  return {
    certificationNumber: findKcCertificationNumber(contexts),
    certificationType: findKcCertificationType(contexts),
    contextTexts: contexts.slice(0, 8),
  };
}

function collectKcContextTexts() {
  const candidates = [];
  const seen = new Set();
  const bodyText = "";
  for (const match of bodyText.matchAll(/.{0,80}(KC|KC인증|인증번호|인증 번호|안전인증|안전확인|공급자적합성|적합성평가|전파인증|전기용품|생활용품|어린이제품).{0,120}/gi)) {
    addKcContext(candidates, seen, match[0]);
  }

  for (const match of bodyText.matchAll(/.{0,80}(KC|인증번호|인증 번호|안전인증|안전확인|공급자적합성|적합성평가|전파인증).{0,120}/gi)) {
    addKcContext(candidates, seen, match[0]);
  }

  const visibleElements = document.body?.querySelectorAll("body *") || [];
  for (const element of visibleElements) {
    if (candidates.length >= 20) {
      break;
    }
    if (!isVisibleElement(element)) {
      continue;
    }
    const text = normalizeText(element.innerText || element.textContent || "");
    if (text.length > 1000) {
      continue;
    }
    if (!hasKcKeyword(text)) {
      continue;
    }
    addKcContext(candidates, seen, text);
  }

  return candidates;
}

function addKcContext(candidates, seen, value) {
  const text = normalizeText(value);
  if (!text || text.length < 4) {
    return;
  }
  const clipped = text.length > 260 ? `${text.slice(0, 260)}...` : text;
  if (seen.has(clipped)) {
    return;
  }
  seen.add(clipped);
  candidates.push(clipped);
}

function hasKcKeyword(text) {
  if (/(KC|KC인증|인증번호|인증 번호|안전인증|안전확인|공급자적합성|적합성평가|전파인증|전기용품|생활용품|어린이제품)/i.test(text || "")) {
    return true;
  }
  return /(KC|인증번호|인증 번호|안전인증|안전확인|공급자적합성|적합성평가|전파인증)/i.test(text || "");
}

function findKcCertificationType(contexts) {
  for (const context of contexts || []) {
    const normalized = normalizeText(context);
    const directMatch = normalized.match(/KC\s*인증\s*([가-힣A-Za-z0-9ㆍ·\s]{2,40}?(?:안전확인|안전인증|공급자적합성|적합성평가|전파인증))/i);
    if (directMatch) {
      return cleanupKcCertificationType(directMatch[1]);
    }
    const typeMatch = normalized.match(/((?:전기용품|생활용품|전기용품\s*및\s*생활용품|어린이제품)[가-힣A-Za-z0-9ㆍ·\s]{0,30}?(?:안전확인|안전인증|공급자적합성|적합성평가|전파인증))/i);
    if (typeMatch) {
      return cleanupKcCertificationType(typeMatch[1]);
    }
    if (/KC\s*인증/i.test(normalized)) {
      return "KC 인증";
    }
  }
  return "";
}

function cleanupKcCertificationType(value) {
  return normalizeText(value)
    .replace(/[|/,:;()[\]{}]+$/g, "")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, 80);
}

function findKcCertificationNumber(contexts) {
  for (const context of contexts || []) {
    const normalized = normalizeText(context);
    const directMatch = normalized.match(/(?:KC\s*)?(?:인증\s*번호|인증번호|안전인증번호|안전확인번호|인증\s*No\.?|Certification\s*No\.?)\s*[:：#-]?\s*([A-Z0-9가-힣][A-Z0-9가-힣._/-]{4,40})/i);
    if (directMatch && isLikelyKcCertificationNumber(directMatch[1])) {
      return cleanupKcCertificationNumber(directMatch[1]);
    }

    const genericMatches = normalized.match(/\b[A-Z]{1,4}[-\s]?[A-Z0-9]{2,6}[-\s]?[A-Z0-9]{4,12}\b/g) || [];
    for (const candidate of genericMatches) {
      if (isLikelyKcCertificationNumber(candidate)) {
        return cleanupKcCertificationNumber(candidate);
      }
    }
  }
  return "";
}

function isLikelyKcCertificationNumber(value) {
  const normalized = cleanupKcCertificationNumber(value);
  if (normalized.length < 6 || normalized.length > 42) {
    return false;
  }
  if (/^(KC|NO|MODEL|STORE|SHOP|PRICE)$/i.test(normalized)) {
    return false;
  }
  if (!normalized.includes("-")) {
    return false;
  }
  return /[0-9]/.test(normalized) && /[A-Z가-힣]/i.test(normalized);
}

function cleanupKcCertificationNumber(value) {
  return normalizeText(value)
    .replace(/[),.;\]]+$/g, "")
    .replace(/\s+/g, "")
    .trim();
}

function isVisibleElement(element) {
  if (!element || !(element instanceof Element)) {
    return false;
  }
  const style = window.getComputedStyle(element);
  if (!style || style.display === "none" || style.visibility === "hidden" || Number(style.opacity) === 0) {
    return false;
  }
  const rect = element.getBoundingClientRect();
  return rect.width > 0 && rect.height > 0;
}

function observeDomChanges() {
  const observer = new MutationObserver(() => scheduleAnalyze("dom-change"));
  observer.observe(document.documentElement, {
    childList: true,
    subtree: true,
    characterData: true,
  });
}

function observeUrlChanges() {
  let currentUrl = window.location.href;

  window.setInterval(() => {
    if (currentUrl === window.location.href) {
      return;
    }

    currentUrl = window.location.href;
    STATE.lastAnalyzedKey = "";
    STATE.lastResult = null;
    STATE.lastPayload = null;
    scheduleAnalyze("url-change");
  }, 700);
}

function ensureOverlay() {
  if (document.getElementById("import-safety-guard-root")) {
    return;
  }

  const root = document.createElement("div");
  root.id = "import-safety-guard-root";
  root.innerHTML = `
    <button id="import-safety-guard-badge" type="button" aria-label="위해상품 분석 결과">
      <svg class="isg-warning-icon" viewBox="0 0 64 56" aria-hidden="true" focusable="false">
        <path d="M32 4L60 52H4L32 4Z"></path>
        <line x1="32" y1="20" x2="32" y2="35"></line>
        <circle cx="32" cy="43" r="2.8"></circle>
      </svg>
    </button>
    <div id="isg-pinned-ribbon">고정됨</div>
    <section id="import-safety-guard-panel" aria-live="polite">
      <div class="isg-product-card">
        <img id="isg-product-image" alt="" />
        <div id="isg-product-fallback" class="isg-product-fallback">이미지 없음</div>
      </div>
      <div class="isg-panel-header">
        <strong id="isg-panel-title">분석 대기 중</strong>
        <button id="isg-panel-close" type="button" aria-label="닫기">×</button>
      </div>
      <p id="isg-panel-message">상품 페이지 정보를 확인하고 있습니다.</p>
      <dl id="isg-panel-details"></dl>
      <button id="isg-risk-dashboard-button" type="button">셀러이신가요?</button>
    </section>
    <div id="isg-risk-modal-backdrop" aria-hidden="true">
      <section id="isg-risk-modal" role="dialog" aria-modal="true" aria-labelledby="isg-risk-title">
        <button id="isg-risk-close" type="button" aria-label="셀러이신가요? 닫기">×</button>
        <div class="isg-risk-hero">
          <p class="isg-eyebrow">Seller Risk</p>
          <h2 id="isg-risk-title">셀러이신가요?</h2>
          <p id="isg-risk-summary">더미데이터 기준으로 수입 전 확인해야 할 리스크를 표시합니다.</p>
        </div>
        <div class="isg-risk-product">
          <span id="isg-risk-product-name">상품명 확인 중</span>
          <strong id="isg-risk-hsk-code">HSK 3924100000</strong>
        </div>
        <div id="isg-risk-scoreboard" class="isg-risk-scoreboard is-loading" aria-live="polite">
          <div class="isg-risk-score-ring" style="--score: 0; --score-color: #64748b;">
            <div class="isg-risk-score-ring-inner">
              <strong>--</strong>
              <span>종합 상태</span>
            </div>
          </div>
          <div class="isg-risk-score-copy">
            <b>HSK 코드 분석중...</b>
            <p>공식 품목 데이터와 리스크 API를 순서대로 확인합니다.</p>
          </div>
          <div class="isg-risk-bars">
            ${riskMetricBarHtml("리콜", 0, "UNKNOWN", "대기")}
            ${riskMetricBarHtml("관세", 0, "UNKNOWN", "대기")}
            ${riskMetricBarHtml("KC", 0, "UNKNOWN", "대기")}
            ${riskMetricBarHtml("성분", 0, "UNKNOWN", "대기")}
          </div>
        </div>
        <div class="isg-risk-grid">
          <article class="isg-risk-card is-warning">
            <span>리콜 가능성</span>
            <strong>주의</strong>
            <p>최근 3개년 동종 품목 리콜 4건. 최신 공표일 2025-09-02.</p>
            <small>조치: 제품명, 모델명, 리콜 사유를 확인하고 공급자에게 시험성적서를 요청하세요.</small>
          </article>
          <article class="isg-risk-card is-review">
            <span>관세 및 사후추징금</span>
            <strong>검토 필요</strong>
            <p>기본 관세율과 실제 적용 조건을 확인하세요.</p>
            <small>조치: 원산지, 신고가격, 운임/보험료, FTA 적용 여부를 확인하세요.</small>
          </article>
          <article class="isg-risk-card is-danger">
            <span>KC 인증 확인 필요</span>
            <strong>위험</strong>
            <p>어린이제품 안전확인 또는 전자파 적합성평가 대상 가능성이 있습니다.</p>
            <small>조치: 제품안전정보센터에서 인증번호와 모델명 일치 여부를 검증하세요.</small>
          </article>
          <article class="isg-risk-card is-review">
            <span>행정처분 및 형사처벌</span>
            <strong>검토 필요</strong>
            <p>성분표 미확인 상태입니다. 규제 성분 여부를 판단할 수 없습니다.</p>
            <small>조치: 성분표와 CAS 번호를 확보한 뒤 화학물질 종합정보시스템에서 확인하세요.</small>
          </article>
        </div>
        <section class="isg-ktl-guide" hidden></section>
      </section>
    </div>
    <div id="isg-recall-detail-backdrop" aria-hidden="true">
      <section id="isg-recall-detail-modal" role="dialog" aria-modal="true" aria-labelledby="isg-recall-detail-title">
        <button id="isg-recall-detail-close" type="button" aria-label="리콜 상세 닫기">×</button>
        <div class="isg-recall-detail-hero">
          <p class="isg-eyebrow">Recall Details</p>
          <h2 id="isg-recall-detail-title">리콜 상세 이력</h2>
          <p id="isg-recall-detail-summary">리콜 항목을 확인하세요.</p>
        </div>
        <div id="isg-recall-detail-content"></div>
      </section>
    </div>
  `;

  document.documentElement.appendChild(root);
  prepareRiskDashboardLayout();

  const badge = document.getElementById("import-safety-guard-badge");
  const panel = document.getElementById("import-safety-guard-panel");
  const closeButton = document.getElementById("isg-panel-close");
  const riskDashboardButton = document.getElementById("isg-risk-dashboard-button");
  const riskBackdrop = document.getElementById("isg-risk-modal-backdrop");
  const riskCloseButton = document.getElementById("isg-risk-close");
  const recallDetailBackdrop = document.getElementById("isg-recall-detail-backdrop");
  const recallDetailCloseButton = document.getElementById("isg-recall-detail-close");

  badge.addEventListener("mouseenter", () => showPanelPreview());
  badge.addEventListener("mouseleave", () => {
    schedulePanelHide();
  });
  badge.addEventListener("click", () => {
    pinPanel();
  });
  panel.addEventListener("mouseenter", () => {
    window.clearTimeout(STATE.hidePanelTimerId);
  });
  panel.addEventListener("mouseleave", () => {
    schedulePanelHide();
  });
  closeButton.addEventListener("click", () => {
    unpinPanel();
  });
  riskDashboardButton.addEventListener("click", () => {
    const payload = extractProductPayload();
    renderRiskDashboardHskAnalyzing(payload);
    openRiskDashboardModal();
    loadRiskDashboard();
  });
  riskCloseButton.addEventListener("click", () => {
    closeRiskDashboardModal();
  });
  document.querySelector(".isg-risk-grid").addEventListener("click", (event) => {
    const trigger = event.target.closest("[data-isg-action='open-recall-details'], [data-isg-action='open-customs-details'], [data-isg-action='open-chemical-details']");
    if (trigger) {
      event.preventDefault();
      toggleInlineRiskDetail(trigger);
    }
  });
  riskBackdrop.addEventListener("click", (event) => {
    if (event.target === riskBackdrop) {
      closeRiskDashboardModal();
    }
  });
  recallDetailCloseButton.addEventListener("click", () => {
    closeRecallDetailModal();
  });
  recallDetailBackdrop.addEventListener("click", (event) => {
    if (event.target === recallDetailBackdrop) {
      closeRecallDetailModal();
    }
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeRiskDashboardModal();
      closeRecallDetailModal();
      closeRiskPolicyPopover();
    }
  });
}

function prepareRiskDashboardLayout() {
  const modal = document.getElementById("isg-risk-modal");
  const scoreboard = document.getElementById("isg-risk-scoreboard");
  const grid = document.querySelector(".isg-risk-grid");
  const guide = document.querySelector(".isg-ktl-guide");
  if (!modal || !scoreboard || !grid || !guide || modal.querySelector(".isg-risk-body")) {
    return;
  }

  const body = document.createElement("div");
  body.className = "isg-risk-body";

  const main = document.createElement("main");
  main.className = "isg-risk-main";

  const sectionTitle = document.createElement("div");
  sectionTitle.className = "isg-risk-section-title";
  sectionTitle.textContent = "항목별 위험도";

  const side = document.createElement("aside");
  side.className = "isg-risk-side";

  const hskPanel = document.createElement("section");
  hskPanel.id = "isg-risk-hsk-panel";
  hskPanel.className = "isg-risk-side-panel";

  scoreboard.parentNode.insertBefore(body, scoreboard);
  main.append(scoreboard, sectionTitle, grid, guide);
  side.append(hskPanel);
  body.append(main, side);
  renderRiskHskPanelIdle();
}

function showPanelPreview() {
  window.clearTimeout(STATE.hidePanelTimerId);
  document.getElementById("import-safety-guard-panel").classList.add("is-visible");
}

function schedulePanelHide() {
  if (STATE.pinned) {
    return;
  }

  window.clearTimeout(STATE.hidePanelTimerId);
  STATE.hidePanelTimerId = window.setTimeout(() => {
    document.getElementById("import-safety-guard-panel").classList.remove("is-visible");
    closeRiskDashboardModal();
    closeRecallDetailModal();
    closeRiskPolicyPopover();
  }, 180);
}

function pinPanel() {
  STATE.pinned = true;
  window.clearTimeout(STATE.hidePanelTimerId);

  const panel = document.getElementById("import-safety-guard-panel");
  const ribbon = document.getElementById("isg-pinned-ribbon");
  panel.classList.add("is-visible", "is-pinned");
  ribbon.classList.add("is-visible");
}

function unpinPanel() {
  STATE.pinned = false;
  const panel = document.getElementById("import-safety-guard-panel");
  const ribbon = document.getElementById("isg-pinned-ribbon");
  panel.classList.remove("is-visible", "is-pinned");
  ribbon.classList.remove("is-visible");
  closeRiskDashboardModal();
  closeRiskPolicyPopover();
}

function openRiskDashboardModal() {
  const backdrop = document.getElementById("isg-risk-modal-backdrop");
  document.getElementById("import-safety-guard-root")?.classList.add("is-risk-dashboard-open");
  backdrop.classList.add("is-visible");
  backdrop.setAttribute("aria-hidden", "false");
}

async function loadRiskDashboard() {
  const payload = extractProductPayload();
  if (!payload?.productName) {
    renderRiskDashboardError("상품명을 찾지 못했습니다.");
    return;
  }

  document.getElementById("isg-risk-summary").textContent = "공식 관세청 HSK 품목 데이터에서 코드를 찾는 중입니다.";
  document.getElementById("isg-risk-product-name").textContent = payload.productName;
  document.getElementById("isg-risk-hsk-code").textContent = "HSK 분석 중";
  renderRiskScoreboardLoading("HSK 코드 분석중...", "공식 HSK 데이터에서 현재 상품과 맞는 품목을 찾고 있습니다.");
  renderRiskHskPanelLoading();

  try {
    const hskResponse = await chrome.runtime.sendMessage({
      type: "MATCH_HSK",
      payload: toHskMatchRequest(payload),
    });

    if (!hskResponse || !hskResponse.ok) {
      throw new Error(hskResponse?.error || "HSK match response missing");
    }
    if (!hskResponse.data?.matched || !hskResponse.data.candidates?.length) {
      const noMatchMessage = hskResponse.data?.message || "매칭된 HSK 코드가 없습니다.";
      renderRiskDashboardHskNoMatch(noMatchMessage, payload);

      const response = await requestRiskDashboard(payload, null);
      if (!response || !response.ok) {
        throw new Error(response?.error || "No response from background worker");
      }

      renderRiskDashboardHskNoMatch(noMatchMessage, payload, response.data?.chemicalRisk);
      return;
    }

    const hskCandidate = hskResponse.data.candidates[0];
    STATE.lastHskCandidate = hskCandidate;
    renderRiskDashboardLoading(payload, hskCandidate);

    const response = await requestRiskDashboard(payload, hskCandidate);

    if (!response || !response.ok) {
      throw new Error(response?.error || "No response from background worker");
    }

    renderRiskDashboard(response.data);
  } catch (error) {
    renderRiskDashboardError(error instanceof Error ? error.message : "알 수 없는 오류");
  }
}

function requestRiskDashboard(
  payload,
  hskCandidate,
  state = STATE,
  sendMessage = (message) => chrome.runtime.sendMessage(message)
) {
  return sendMessage({
    type: "ANALYZE_RISK_DASHBOARD",
    payload: toRiskDashboardRequest(payload, hskCandidate?.hskCode || null, state),
  });
}

function toHskMatchRequest(payload) {
  const normalized = STATE.lastResult || {};
  return {
    productName: payload.productName,
    description: payload.description || "",
    imageUrl: payload.imageUrl || null,
    kcCertificationNumber: payload.kcCertificationNumber || null,
    kcCertificationType: payload.kcCertificationType || null,
    standardProductName: normalized.standardProductName || null,
    hskCandidateKeywords: normalized.hskCandidateKeywords || [],
      primaryProductName: normalized.primaryProductName || null,
      productForm: normalized.productForm || null,
      primarySearchKeywords: normalized.primarySearchKeywords || [],
      kcCertificationSearchKeywords: normalized.kcCertificationSearchKeywords || [],
      componentKeywords: normalized.componentKeywords || [],
      featureKeywords: normalized.featureKeywords || [],
  };
}

function toRiskDashboardRequest(payload, hskCode, state = STATE) {
  const normalized = state.lastResult || {};
  const refinedIngredients = Array.isArray(normalized.harmfulIngredients)
    ? normalized.harmfulIngredients.map((ingredient) => String(ingredient).trim()).filter(Boolean)
    : [];
  return {
    hskCode,
    productName: payload.productName,
    productDescription: payload.description || "",
    ingredients: refinedIngredients.length
      ? refinedIngredients
      : inferIngredientKeywords(payload),
    chemicalCandidates: [],
    originCountry: "CN",
    declaredValue: payload.price?.amount ?? null,
    currency: payload.price?.currency || "KRW",
    quantity: 1,
    shippingCost: null,
    insuranceCost: null,
    kcCertificationNumber: payload.kcCertificationNumber || normalized.certNum || null,
    kcCertificationType: payload.kcCertificationType || null,
    modelName: normalized.modelName || null,
    brandName: normalized.brandName || payload.brandName || null,
      standardProductName: normalized.standardProductName || null,
      primaryProductName: normalized.primaryProductName || null,
      primarySearchKeywords: normalized.primarySearchKeywords || [],
      kcCertificationSearchKeywords: normalized.kcCertificationSearchKeywords || [],
      prefilteredRecalls: toPrefilteredRecalls(normalized.matchedRecalls),
    };
  }

function toPrefilteredRecalls(matchedRecalls) {
  if (!Array.isArray(matchedRecalls)) {
    return [];
  }
  return matchedRecalls
    .filter((recall) => recall && recall.recallProductName)
    .map((recall) => ({
      recallProductName: recall.recallProductName,
      modelName: recall.modelName || null,
      manufacturer: recall.manufacturer || null,
      reason: recall.reason || null,
      announcementDate: recall.announcementDate || null,
      similarity: typeof recall.similarity === "number" ? recall.similarity : null,
      source: recall.source || null,
    }));
}

function inferIngredientKeywords(payload) {
  const text = `${payload.productName || ""} ${payload.description || ""}`.toLowerCase();
  const ingredients = [];
  if (text.includes("pvc")) {
    ingredients.push("PVC");
  }
  if (text.includes("phthalate") || text.includes("프탈레이트")) {
    ingredients.push("프탈레이트");
  }
  if (text.includes("plastic") || text.includes("플라스틱")) {
    ingredients.push("플라스틱");
  }
  return ingredients;
}

function renderRiskDashboardHskAnalyzing(payload = {}) {
  STATE.lastRiskDashboard = null;
  STATE.lastHskCandidate = null;
  closeRecallDetailModal();
  document.getElementById("isg-risk-summary").textContent = "HSK 코드 분석중...";
  document.getElementById("isg-risk-product-name").textContent = payload.productName || "상품명 없음";
  document.getElementById("isg-risk-hsk-code").textContent = "HSK 코드 분석중...";
  renderRiskScoreboardLoading("HSK 코드 분석중...", "상품명을 기준으로 HSK 후보를 찾는 중입니다.");
  renderRiskHskPanelLoading();
  document.querySelector(".isg-risk-grid").innerHTML = `
    <article class="isg-risk-card is-unknown isg-hsk-analyzing-card">
      <span>HSK 코드</span>
      <strong>분석 중</strong>
      <p>HSK 코드 분석중...</p>
      <small>공식 HSK 품목 데이터에서 후보를 확인하고 있습니다.</small>
    </article>
  `;
  document.querySelector(".isg-ktl-guide").innerHTML = "";
}

function renderRiskDashboardHskNoMatch(message, payload = {}, chemicalRisk = null) {
  stopRiskLoadingSequence();
  STATE.lastRiskDashboard = chemicalRisk ? { chemicalRisk } : null;
  STATE.lastHskCandidate = null;
  document.getElementById("isg-risk-summary").textContent = "HSK 매칭 결과가 없습니다.";
  document.getElementById("isg-risk-hsk-code").textContent = "매칭된 HSK 코드가 없습니다.";
  renderRiskScoreboardNoMatch(message || "매칭된 HSK 코드가 없습니다.");
  renderRiskHskPanelNoMatch(message || "매칭된 HSK 코드가 없습니다.");
  document.querySelector(".isg-risk-grid").innerHTML = `
    ${riskCardHtml("HSK 매칭", "UNKNOWN", "매칭 없음", message || "매칭된 HSK 코드가 없습니다.", "관세와 세관장확인대상 기반 판단은 진행하지 않았습니다.")}
    ${kcCardFromDomPayload(payload)}
    ${chemicalRisk ? RiskView.chemicalRiskCardHtml(chemicalRisk) : ""}
  `;
  document.querySelector(".isg-ktl-guide").innerHTML = "";
}

function kcCardFromDomPayload(payload = {}) {
  const certificationNumber = normalizeText(payload.kcCertificationNumber || "");
  const certificationType = normalizeText(payload.kcCertificationType || "");
  return RiskView.kcRiskCardHtml(
    {
      status: certificationNumber ? "UNKNOWN" : "DANGER",
      certificationValid: false,
    },
    kcDetailText(certificationNumber, certificationType, "")
  );
}

function hskDisplayText(hskCandidate, fallbackCode) {
  const code = hskCandidate?.hskCode || fallbackCode || "-";
  return `HSK ${code}`;
}

function renderRiskDashboardLoading(payload, hskCandidate) {
  document.getElementById("isg-risk-summary").textContent = "실제 리스크 API를 조회하는 중입니다.";
  document.getElementById("isg-risk-product-name").textContent = payload.productName || "상품명 없음";
  document.getElementById("isg-risk-hsk-code").textContent = hskDisplayText(hskCandidate);
  renderRiskScoreboardLoading("리스크 데이터 조회중...", "리콜, 관세, KC, 성분 정보를 순서대로 확인합니다.");
  renderRiskHskPanel(hskCandidate, null);
  document.querySelector(".isg-risk-grid").innerHTML = `
    ${riskCardHtml("리콜 가능성", "UNKNOWN", "조회 중", "SafetyKorea 리콜 정보를 조회하고 있습니다.", "")}
    ${riskCardHtml("관세 및 사후추징", "UNKNOWN", "조회 중", "관세 리스크를 계산하고 있습니다.", "")}
    ${riskCardHtml("KC 인증 확인 필요", "UNKNOWN", "조회 중", "KC 인증 요구 여부를 확인하고 있습니다.", "")}
    ${riskCardHtml("행정처분 및 형사처벌", "UNKNOWN", "조회 중", "성분 리스크를 확인하고 있습니다.", "")}
  `;
}

function renderRiskDashboard(result) {
  stopRiskLoadingSequence();
  STATE.lastRiskDashboard = result;
  document.getElementById("isg-risk-summary").textContent =
    `종합 위험도 ${overallRiskText(result.overallRiskLevel)} · ${Number(result.overallRiskScore || 0)}점`;
  document.getElementById("isg-risk-product-name").textContent = result.productName || "상품명 없음";
  document.getElementById("isg-risk-hsk-code").textContent = hskDisplayText(STATE.lastHskCandidate, result.hskCode);
  renderRiskScoreboard(result);
  renderRiskHskPanel(STATE.lastHskCandidate, result);

  const recall = result.recallRisk || {};
  const customs = result.customsRisk || {};
  const kc = result.kcRisk || {};
  const chemical = result.chemicalRisk || {};

  document.querySelector(".isg-risk-grid").innerHTML = `
    ${riskCardHtml(
      "리콜 가능성",
      recall.status,
      riskStatusText(recall.status),
      `${recall.message || "리콜 정보를 확인할 수 없습니다."} ${recall.totalCount ? `총 ${recall.totalCount}건` : ""}`.trim(),
      recall.latestAnnouncementDate ? `최신 공표일: ${recall.latestAnnouncementDate}` : firstRecallReason(recall),
      recall.score
    )}
    ${customsRiskSummaryCardHtml(customs)}
    ${RiskView.kcRiskCardHtml(
      kc,
      kcDetailText(
        STATE.lastPayload?.kcCertificationNumber || kc.certificationNumber || "",
        kc.certificationType || "",
        kc.relatedLaw || ""
      )
    )}
    ${RiskView.chemicalRiskCardHtml(chemical)}
  `;

  const riskGrid = document.querySelector(".isg-risk-grid");
  if (riskGrid.firstElementChild) {
    riskGrid.firstElementChild.outerHTML = RiskView.recallRiskCardHtml(recall);
  }

  renderRiskDashboardGuide(result);
}

function renderRiskDashboardError(message) {
  stopRiskLoadingSequence();
  STATE.lastRiskDashboard = null;
  document.getElementById("isg-risk-summary").textContent = "리스크 대시보드 조회에 실패했습니다.";
  renderRiskScoreboardError(message);
  renderRiskHskPanelError(message);
  document.querySelector(".isg-risk-grid").innerHTML = `
    ${riskCardHtml("리스크 조회", "UNAVAILABLE", "조회 실패", message, "백엔드 서버와 API 설정을 확인하세요.")}
  `;
}

function customsRiskSummaryCardHtml(customs = {}) {
  const rate = customs.finalTariffRate != null ? `최종 세율: ${formatRiskPercent(customs.finalTariffRate)}` : "";
  const tariff = customs.tariffType ? tariffTypeText(customs.tariffType) : "";
  const detail = [tariff, rate].filter(Boolean).join(" · ");
  return riskCardHtml(
    "관세 및 사후추징",
    customs.status,
    riskStatusText(customs.status),
    customs.message || "관세 정보를 확인할 수 없습니다.",
    detail || customs.guideUrl || "",
    null,
    "open-customs-details"
  );
}

function renderRiskScoreboard(result = {}) {
  const score = clampRiskScore(result.overallRiskScore);
  const level = result.overallRiskLevel || "UNKNOWN";
  const color = riskVisualColor(level);
  const metrics = [
    ["리콜 가능성", result.recallRisk],
    ["관세 및 사후추징", result.customsRisk],
    ["KC 인증", result.kcRisk],
    ["성분/처벌", result.chemicalRisk],
  ];

  setRiskScoreboardHtml(`
    <div class="isg-risk-score-ring ${riskVisualClass(level)}" style="--score: ${score}; --score-color: ${color};">
      <div class="isg-risk-score-ring-inner">
        <strong>${escapeHtml(overallRiskText(level))}</strong>
        <span>종합 상태</span>
      </div>
    </div>
    <div class="isg-risk-score-copy">
      <b>${escapeHtml(overallRiskText(level))}</b>
      <p>${escapeHtml(scoreSummaryText(level, score))}</p>
    </div>
    <div class="isg-risk-bars">
      ${metrics.map(([label, risk]) => riskMetricBarHtml(label, risk?.score, risk?.status, riskStatusText(risk?.status))).join("")}
    </div>
  `, riskVisualClass(level));
}

function renderRiskScoreboardLoading(title, message) {
  setRiskScoreboardHtml(`
    <div class="isg-risk-score-ring is-unknown" style="--score: 0; --score-color: #64748b;">
      <div class="isg-risk-score-ring-inner">
        <span class="isg-risk-loading-spinner" aria-hidden="true"></span>
        <span>분석 중</span>
      </div>
    </div>
    <div class="isg-risk-score-copy">
      <b id="isg-risk-loading-message">${escapeHtml(title || "데이터를 불러오는 중입니다...")}</b>
      <p>${escapeHtml(message || "상품 정보를 확인하고 있습니다.")}</p>
    </div>
    <div class="isg-risk-bars">
      ${riskMetricBarHtml("리콜 가능성", 0, "UNKNOWN", "대기")}
      ${riskMetricBarHtml("관세 및 사후추징", 0, "UNKNOWN", "대기")}
      ${riskMetricBarHtml("KC 인증", 0, "UNKNOWN", "대기")}
      ${riskMetricBarHtml("성분/처벌", 0, "UNKNOWN", "대기")}
    </div>
  `, "is-loading");
  startRiskLoadingSequence();
}

function renderRiskScoreboardNoMatch(message) {
  setRiskScoreboardHtml(`
    <div class="isg-risk-score-ring is-unknown" style="--score: 0; --score-color: #64748b;">
      <div class="isg-risk-score-ring-inner">
        <strong>--</strong>
        <span>HSK 없음</span>
      </div>
    </div>
    <div class="isg-risk-score-copy">
      <b>매칭된 HSK 코드가 없습니다.</b>
      <p>${escapeHtml(message || "관세와 세관장확인대상 기반 판단은 진행하지 않았습니다.")}</p>
    </div>
    <div class="isg-risk-bars">
      ${riskMetricBarHtml("HSK 매칭", 0, "UNKNOWN", "매칭 없음")}
      ${riskMetricBarHtml("KC 인증", 100, "DANGER", "확인 필요")}
    </div>
  `, "is-unknown");
}

function renderRiskScoreboardError(message) {
  setRiskScoreboardHtml(`
    <div class="isg-risk-score-ring is-unavailable" style="--score: 0; --score-color: #475569;">
      <div class="isg-risk-score-ring-inner">
        <strong>--</strong>
        <span>조회 실패</span>
      </div>
    </div>
    <div class="isg-risk-score-copy">
      <b>리스크 조회 실패</b>
      <p>${escapeHtml(message || "백엔드 서버와 API 설정을 확인하세요.")}</p>
    </div>
    <div class="isg-risk-bars">
      ${riskMetricBarHtml("리스크 조회", 0, "UNAVAILABLE", "실패")}
    </div>
  `, "is-unavailable");
}

function setRiskScoreboardHtml(html, stateClass) {
  const scoreboard = document.getElementById("isg-risk-scoreboard");
  if (!scoreboard) {
    return;
  }
  scoreboard.className = `isg-risk-scoreboard ${stateClass || ""}`.trim();
  scoreboard.innerHTML = html;
}

function startRiskLoadingSequence() {
  stopRiskLoadingSequence();
  const messages = [
    "데이터를 불러오는 중입니다...",
    "AI 분석중입니다...",
    "거의 다 되었습니다...",
  ];
  setRiskLoadingMessage(messages[0]);
  STATE.riskLoadingTimerIds = messages.slice(1).map((message, index) =>
    window.setTimeout(() => setRiskLoadingMessage(message), (index + 1) * 1800)
  );
}

function stopRiskLoadingSequence() {
  for (const timerId of STATE.riskLoadingTimerIds || []) {
    window.clearTimeout(timerId);
  }
  STATE.riskLoadingTimerIds = [];
}

function setRiskLoadingMessage(message) {
  const element = document.getElementById("isg-risk-loading-message");
  if (element) {
    element.textContent = message;
  }
}

function riskMetricBarHtml(label, score, status, statusText) {
  const normalizedScore = clampRiskScore(score);
  const normalizedStatus = status || "UNKNOWN";
  const displayStatus = statusText || riskStatusText(normalizedStatus);
  return `
    <div class="isg-risk-bar-row ${riskVisualClass(normalizedStatus)}">
      <div class="isg-risk-bar-label">
        <span>${escapeHtml(label)}</span>
        <b>${escapeHtml(displayStatus)}</b>
      </div>
      <div class="isg-risk-bar-track" aria-hidden="true">
        <i style="width: ${normalizedScore}%;"></i>
      </div>
      <em>${escapeHtml(displayStatus)}</em>
    </div>
  `;
}

function clampRiskScore(value) {
  const score = Number(value);
  if (!Number.isFinite(score)) {
    return 0;
  }
  return Math.max(0, Math.min(100, Math.round(score)));
}

function riskVisualClass(value) {
  switch (value) {
    case "CRITICAL":
    case "DANGER":
      return "is-danger";
    case "HIGH":
    case "WARNING":
      return "is-warning";
    case "LOW":
    case "SAFE":
      return "is-safe";
    case "MEDIUM":
      return "is-review";
    case "UNAVAILABLE":
      return "is-unavailable";
    default:
      return "is-unknown";
  }
}

function riskVisualColor(value) {
  switch (riskVisualClass(value)) {
    case "is-danger":
      return "#b91320";
    case "is-warning":
      return "#d27d00";
    case "is-safe":
      return "#22a06b";
    case "is-review":
      return "#2563eb";
    case "is-unavailable":
      return "#475569";
    default:
      return "#64748b";
  }
}

function scoreSummaryText(level, score) {
  switch (level) {
    case "CRITICAL":
      return "수입 전 인증, 리콜, 성분 근거를 반드시 확인해야 합니다.";
    case "HIGH":
      return "판매 또는 수입 전 추가 확인이 필요한 상태입니다.";
    case "MEDIUM":
      return "일부 항목은 확인이 필요합니다.";
    case "LOW":
      return "현재 확인된 리스크는 낮은 편입니다.";
    default:
      return "확인할 수 없는 항목이 있어 결과를 확정하지 않았습니다.";
  }
}

function renderRiskHskPanelIdle() {
  setRiskHskPanelHtml(`
    <div class="isg-risk-panel-title">
      <span class="isg-risk-panel-icon">HSK</span>
      <h3>HSK 분류 정보</h3>
    </div>
    <p class="isg-risk-panel-empty">상품 분석을 시작하면 매칭된 HSK 코드와 품목 분류가 표시됩니다.</p>
  `);
}

function renderRiskHskPanelLoading() {
  setRiskHskPanelHtml(`
    <div class="isg-risk-panel-title">
      <span class="isg-risk-panel-icon">HSK</span>
      <h3>HSK 분류 정보</h3>
    </div>
    <div class="isg-risk-panel-loading">
      <i></i><i></i><i></i>
    </div>
    <p class="isg-risk-panel-empty">공식 HSK 품목 데이터에서 후보를 찾고 있습니다.</p>
  `);
}

function renderRiskHskPanelNoMatch(message) {
  setRiskHskPanelHtml(`
    <div class="isg-risk-panel-title">
      <span class="isg-risk-panel-icon">HSK</span>
      <h3>HSK 분류 정보</h3>
    </div>
    <div class="isg-risk-hsk-code-card is-unknown">
      <small>매칭 결과</small>
      <strong>매칭된 HSK 코드가 없습니다.</strong>
    </div>
    <p class="isg-risk-panel-empty">${escapeHtml(message || "공식 HSK 데이터에서 적절한 후보를 찾지 못했습니다.")}</p>
  `);
}

function renderRiskHskPanelError(message) {
  setRiskHskPanelHtml(`
    <div class="isg-risk-panel-title">
      <span class="isg-risk-panel-icon">HSK</span>
      <h3>HSK 분류 정보</h3>
    </div>
    <div class="isg-risk-hsk-code-card is-unavailable">
      <small>조회 상태</small>
      <strong>조회 실패</strong>
    </div>
    <p class="isg-risk-panel-empty">${escapeHtml(message || "HSK 또는 리스크 조회에 실패했습니다.")}</p>
  `);
}

function renderRiskHskPanel(hskCandidate = {}, result = null) {
  const hskCode = hskCandidate?.hskCode || result?.hskCode || "-";
  const displayName = hskCandidate?.displayName || hskCandidate?.koreanName || result?.productName || "-";
  const categoryPath = hskCandidate?.categoryPath || hskCandidate?.path || hskCandidate?.hierarchy || "";
  const confidence = hskCandidate?.confidence ?? hskCandidate?.score ?? null;
  const customs = result?.customsRisk || {};
  const finalRate = customs.finalTariffRate != null ? `${Number(customs.finalTariffRate).toLocaleString("ko-KR")}%` : "-";
  const tariffType = customs.tariffType ? tariffTypeText(customs.tariffType) : "-";

  setRiskHskPanelHtml(`
    <div class="isg-risk-panel-title">
      <span class="isg-risk-panel-icon">HSK</span>
      <h3>HSK 분류 정보</h3>
    </div>
    <div class="isg-risk-hsk-code-card">
      <small>매칭 코드</small>
      <strong>${escapeHtml(hskCode)}</strong>
      <p>${escapeHtml(shortenHskName(displayName))}</p>
    </div>
    <div class="isg-risk-hsk-name-box">
      <small>품목 설명</small>
      <p>${escapeHtml(displayName)}</p>
    </div>
    ${hskCategoryChipsHtml(categoryPath || displayName)}
    <dl class="isg-risk-info-list">
      ${riskInfoRowHtml("적용 관세율", finalRate)}
      ${riskInfoRowHtml("관세 유형", tariffType)}
      ${riskInfoRowHtml("매칭 신뢰도", formatPanelConfidence(confidence))}
    </dl>
  `);
}

function shortenHskName(value) {
  const text = normalizeText(value || "");
  if (!text) {
    return "-";
  }
  const first = text.split(">").map((part) => part.trim()).filter(Boolean).at(-1) || text;
  return first.length > 42 ? `${first.slice(0, 42)}...` : first;
}

function hskCategoryChipsHtml(value) {
  const parts = normalizeText(value || "")
    .split(">")
    .map((part) => part.trim())
    .filter(Boolean)
    .slice(0, 5);
  if (!parts.length) {
    return "";
  }
  return `
    <div class="isg-risk-hsk-chips">
      <small>분류 단계</small>
      <div>${parts.map((part) => `<span>${escapeHtml(shortenHskChip(part))}</span>`).join("")}</div>
    </div>
  `;
}

function shortenHskChip(value) {
  const text = normalizeText(value || "");
  return text.length > 24 ? `${text.slice(0, 24)}...` : text;
}

function setRiskHskPanelHtml(html) {
  const panel = document.getElementById("isg-risk-hsk-panel");
  if (panel) {
    panel.innerHTML = html;
  }
}

function riskInfoRowHtml(label, value, highlight = false) {
  return `
    <dt>${escapeHtml(label)}</dt>
    <dd class="${highlight ? "is-highlight" : ""}">${escapeHtml(value || "-")}</dd>
  `;
}

function formatPanelConfidence(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "-";
  }
  return number <= 1 ? `${Math.round(number * 100)}%` : `${Math.round(number)}%`;
}

function renderRiskDashboardGuide(result) {
  const guide = document.querySelector(".isg-ktl-guide");
  const kc = result.kcRisk || {};
  const html = kc.certificationValid === true
    ? RiskView.ktlCertificationGuideHtml(kc.ktlGuide)
    : "";
  guide.innerHTML = html;
  guide.hidden = !html;
}

function riskCardHtml(title, status, statusText, message, detail, explicitScore, detailAction) {
  return `
    <article class="isg-risk-card ${riskCardClass(status)}">
      <div class="isg-risk-card-heading">
        <span><i aria-hidden="true"></i>${escapeHtml(title)}</span>
        <strong>${escapeHtml(statusText)}</strong>
      </div>
      <p>${escapeHtml(message || "-")}</p>
      <small>${escapeHtml(detail || "")}</small>
      ${detailAction ? `<button class="isg-risk-detail-button" type="button" data-isg-action="${escapeHtml(detailAction)}">상세 보기</button>` : ""}
    </article>
  `;
}

function inferRiskCardScore(status) {
  switch (status) {
    case "DANGER":
      return 90;
    case "WARNING":
      return 65;
    case "SAFE":
      return 15;
    case "UNAVAILABLE":
      return 0;
    default:
      return 45;
  }
}

function kcDetailText(certificationNumber, certificationType, relatedLaw) {
  const details = [];
  const normalizedNumber = normalizeText(certificationNumber || "");
  const normalizedType = normalizeText(certificationType || "");
  const normalizedLaw = normalizeText(relatedLaw || "");
  if (normalizedNumber) {
    details.push(`KC 번호: ${normalizedNumber}`);
  } else {
    details.push("KC 번호: 없음");
  }
  if (normalizedType) {
    details.push(`인증 구분: ${normalizedType}`);
  }
  if (normalizedLaw) {
    details.push(`관련 법령: ${normalizedLaw}`);
  }
  return details.join(" · ");
}

function riskCardClass(status) {
  switch (status) {
    case "DANGER":
      return "is-danger";
    case "WARNING":
      return "is-warning";
    case "SAFE":
      return "is-safe";
    case "UNAVAILABLE":
      return "is-unavailable";
    default:
      return "is-unknown";
  }
}

function riskStatusText(status) {
  switch (status) {
    case "DANGER":
      return "위험";
    case "WARNING":
      return "주의";
    case "SAFE":
      return "안전";
    case "UNAVAILABLE":
      return "조회 불가";
    default:
      return "확인 필요";
  }
}

function tariffTypeText(tariffType) {
  switch (tariffType) {
    case "BASIC":
      return "기본세율";
    case "WTO":
      return "WTO 세율";
    case "FTA":
      return "FTA 협정세율";
    case "ANTI_DUMPING":
      return "덤핑방지관세";
    case "COUNTERVAILING":
      return "상계관세";
    case "SAFEGUARD":
      return "긴급관세";
    case "SPECIAL":
      return "특별관세";
    default:
      return "관세율 확인 필요";
  }
}

function overallRiskText(level) {
  switch (level) {
    case "CRITICAL":
      return "매우 높음";
    case "HIGH":
      return "높음";
    case "MEDIUM":
      return "보통";
    case "LOW":
      return "낮음";
    default:
      return "확인 필요";
  }
}

function firstRecallReason(recall) {
  return recall.items?.[0]?.reason || "";
}

function closeRiskDashboardModal() {
  stopRiskLoadingSequence();
  document.getElementById("import-safety-guard-root")?.classList.remove("is-risk-dashboard-open");
  const backdrop = document.getElementById("isg-risk-modal-backdrop");
  backdrop.classList.remove("is-visible");
  backdrop.setAttribute("aria-hidden", "true");
  closeRecallDetailModal();
  closeInlineRiskDetail();
}

function toggleInlineRiskDetail(trigger) {
  const action = trigger?.getAttribute("data-isg-action") || "";
  const card = trigger?.closest(".isg-risk-card");
  if (!card) {
    return;
  }

  const detailType = action === "open-customs-details"
    ? "customs"
    : action === "open-chemical-details"
      ? "chemical"
      : "recall";
  const next = card.nextElementSibling;
  if (next?.classList?.contains("isg-risk-inline-detail") && next.dataset.detailType === detailType) {
    next.remove();
    trigger.textContent = detailType === "chemical" ? "규제가능성 목록" : "상세 보기";
    return;
  }

  closeInlineRiskDetail();

  const panel = document.createElement("section");
  panel.className = "isg-risk-inline-detail";
  panel.dataset.detailType = detailType;
  panel.innerHTML = detailType === "customs"
    ? customsInlineDetailHtml(STATE.lastRiskDashboard?.customsRisk || {})
    : detailType === "chemical"
      ? chemicalInlineDetailHtml(STATE.lastRiskDashboard?.chemicalRisk || {})
      : recallInlineDetailHtml(STATE.lastRiskDashboard?.recallRisk || {});
  card.insertAdjacentElement("afterend", panel);
  trigger.textContent = "접기";
}

function closeInlineRiskDetail() {
  document.querySelectorAll(".isg-risk-inline-detail").forEach((panel) => panel.remove());
  document.querySelectorAll("[data-isg-action='open-recall-details'], [data-isg-action='open-customs-details'], [data-isg-action='open-chemical-details']")
    .forEach((button) => {
      button.textContent = button.getAttribute("data-isg-action") === "open-chemical-details" ? "규제가능성 목록" : "상세 보기";
    });
}

function recallInlineDetailHtml(recall = {}) {
  const totalCount = Number(recall.totalCount || 0);
  return `
    <div class="isg-inline-detail-head">
      <strong>리콜 가능성 상세</strong>
      <span>${totalCount > 0 ? `총 ${totalCount.toLocaleString("ko-KR")}건` : "조회 결과 없음"}</span>
    </div>
    ${RiskView.recallDetailListHtml(recall)}
  `;
}

function customsInlineDetailHtml(customs = {}) {
  const finalRate = Number(customs.finalTariffRate);
  const donutRate = Number.isFinite(finalRate) ? Math.max(0, Math.min(100, finalRate)) : 0;
  const finalRateText = formatRiskPercent(customs.finalTariffRate);
  const rows = [
    ["상태", riskStatusText(customs.status)],
    ["관세 유형", customs.tariffType ? tariffTypeText(customs.tariffType) : "-"],
    ["기본 세율", formatRiskPercent(customs.baseTariffRate)],
    ["추가 세율", formatRiskPercent(customs.additionalTariffRate)],
    ["최종 세율", formatRiskPercent(customs.finalTariffRate)],
  ];
  return `
    <div class="isg-inline-detail-head">
      <strong>관세 및 사후추징 상세</strong>
      <span>${escapeHtml(riskStatusText(customs.status))}</span>
    </div>
    <div class="isg-customs-detail-layout">
      <div class="isg-customs-donut" style="--rate: ${donutRate};">
        <div>
          <strong>${escapeHtml(finalRateText)}</strong>
          <span>최종 세율</span>
        </div>
      </div>
      <dl class="isg-inline-detail-list">
        ${rows.map(([label, value]) => `<dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value)}</dd>`).join("")}
      </dl>
    </div>
    ${customs.guideUrl ? `<a class="isg-inline-detail-link" href="${escapeHtml(customs.guideUrl)}" target="_blank" rel="noreferrer">UNIPASS에서 확인</a>` : ""}
  `;
}

function chemicalInlineDetailHtml(chemical = {}) {
  const items = Array.isArray(chemical.regulatedIngredients) ? chemical.regulatedIngredients : [];
  const regulatedCount = items.filter((item) => item?.regulated === true).length;
  const warningCount = items.length - regulatedCount;
  return `
    ${RiskView.chemicalInlineDetailHtml(chemical)}
    <div class="isg-chemical-inline-summary">
      <span>규제 대상 ${regulatedCount.toLocaleString("ko-KR")}개</span>
      <span>비규제/참고 ${warningCount.toLocaleString("ko-KR")}개</span>
    </div>
  `;
}

function formatRiskPercent(value) {
  if (value == null) {
    return "-";
  }
  const number = Number(value);
  return Number.isFinite(number) ? `${number.toLocaleString("ko-KR")}%` : "-";
}

function openRecallDetailModal() {
  const recall = STATE.lastRiskDashboard?.recallRisk || {};
  const totalCount = Number(recall.totalCount || 0);
  const backdrop = document.getElementById("isg-recall-detail-backdrop");
  const summary = document.getElementById("isg-recall-detail-summary");
  const content = document.getElementById("isg-recall-detail-content");

  summary.textContent = totalCount > 0
    ? `조회된 리콜 이력 ${totalCount.toLocaleString("ko-KR")}건의 상세 내용입니다.`
    : "조회된 리콜 상세 이력이 없습니다.";
  content.innerHTML = RiskView.recallDetailListHtml(recall);
  backdrop.classList.add("is-visible");
  backdrop.setAttribute("aria-hidden", "false");
}

function closeRecallDetailModal() {
  const backdrop = document.getElementById("isg-recall-detail-backdrop");
  if (!backdrop) {
    return;
  }
  backdrop.classList.remove("is-visible");
  backdrop.setAttribute("aria-hidden", "true");
}

function toggleRiskPolicyPopover(button) {
  const popover = document.getElementById("isg-risk-policy-popover");
  if (!popover) {
    return;
  }

  const willOpen = !popover.classList.contains("is-visible");
  popover.classList.toggle("is-visible", willOpen);
  popover.setAttribute("aria-hidden", String(!willOpen));
  button.setAttribute("aria-expanded", String(willOpen));

  if (willOpen) {
    positionRiskPolicyPopover(button, popover);
  }
}

function positionRiskPolicyPopover(button, popover) {
  const buttonRect = button.getBoundingClientRect();
  const popoverRect = popover.getBoundingClientRect();
  const margin = 14;
  const viewportWidth = window.innerWidth || document.documentElement.clientWidth;
  const viewportHeight = window.innerHeight || document.documentElement.clientHeight;

  const left = Math.min(
    Math.max(margin, buttonRect.right - popoverRect.width),
    Math.max(margin, viewportWidth - popoverRect.width - margin)
  );
  let top = buttonRect.top - popoverRect.height - 10;

  if (top < margin) {
    top = Math.min(buttonRect.bottom + 10, viewportHeight - popoverRect.height - margin);
  }

  popover.style.left = `${left}px`;
  popover.style.top = `${Math.max(margin, top)}px`;
}

function closeRiskPolicyPopover() {
  const popover = document.getElementById("isg-risk-policy-popover");
  const button = document.getElementById("isg-risk-help-button");
  if (!popover) {
    return;
  }

  popover.classList.remove("is-visible");
  popover.setAttribute("aria-hidden", "true");
  if (button) {
    button.setAttribute("aria-expanded", "false");
  }
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}

function recallSourceLabel(source) {
  if (source === "FOREIGN") {
    return "국외";
  }
  if (source === "DOMESTIC") {
    return "국내";
  }
  return "출처 미상";
}

function updateOverlayFromResult(result, payload) {
  const resolvedBrandName = BrandView.resolveBrandDisplayName({
    domBrandName: payload.brandName,
    aiBrandName: result.brandName,
    sellerName: payload.sellerName,
  });
  const storeInfoLabel = resolvedBrandName || (payload.storePageUrl ? "스토어정보 확인 필요" : "-");
  const resolvedPayload = {
    ...payload,
    brandName: resolvedBrandName,
  };
  STATE.lastPayload = resolvedPayload;
  updateBadge(result.riskLevel, riskTitle(result.riskLevel), result.message);
  updateProductImage(resolvedPayload.imageUrl, resolvedPayload.productName);
  updateRiskDashboardProduct(resolvedPayload);

  const details = document.getElementById("isg-panel-details");
  details.innerHTML = "";
  appendDetail(details, "상품명", resolvedPayload.productName);
  appendDetail(details, "브랜드/스토어", storeInfoLabel);
  appendRiskDetail(details, result.riskLevel, result.riskScore);
  appendDetail(details, "카테고리", result.category || "-");
  appendDetail(details, "리콜 사유", result.recallReason || "-");
  appendDetail(details, "위해 성분", (result.harmfulIngredients || []).join(", ") || "-");

  const firstRecall = result.matchedRecalls?.[0];
  if (firstRecall) {
    const sourceLabel = recallSourceLabel(firstRecall.source);
    appendDetail(details, "관련 리콜 이력", `[${sourceLabel}] ${firstRecall.recallProductName} (${firstRecall.similarity})`);
  }
}

function updateRiskDashboardProduct(payload) {
  const productName = document.getElementById("isg-risk-product-name");
  const hskCode = document.getElementById("isg-risk-hsk-code");

  productName.textContent = payload.productName || "상품명 없음";
  hskCode.textContent = "HSK 분석 전";
}

function updateProductImage(imageUrl, productName) {
  const image = document.getElementById("isg-product-image");
  const fallback = document.getElementById("isg-product-fallback");

  if (!imageUrl) {
    image.removeAttribute("src");
    image.alt = "";
    image.classList.remove("is-visible");
    fallback.classList.add("is-visible");
    return;
  }

  image.src = imageUrl;
  image.alt = productName ? `${productName} 대표 이미지` : "상품 대표 이미지";
  image.classList.add("is-visible");
  fallback.classList.remove("is-visible");
}

function updateBadge(riskLevel, title, message) {
  const badge = document.getElementById("import-safety-guard-badge");
  const panel = document.getElementById("import-safety-guard-panel");
  const panelTitle = document.getElementById("isg-panel-title");
  const panelMessage = document.getElementById("isg-panel-message");

  badge.dataset.riskLevel = riskLevel;
  panel.dataset.riskLevel = riskLevel;
  panelTitle.textContent = title;
  panelMessage.textContent = message;
}

function appendDetail(container, label, value) {
  const term = document.createElement("dt");
  term.textContent = label;

  const description = document.createElement("dd");
  description.textContent = value;

  container.append(term, description);
}

function appendRiskDetail(container, riskLevel, riskScore) {
  document.getElementById("isg-risk-policy-popover")?.remove();

  const term = document.createElement("dt");
  term.textContent = "위해 위험도";

  const description = document.createElement("dd");
  description.className = "isg-risk-detail-cell";
  const riskBadge = document.createElement("span");
  riskBadge.className = `isg-risk-chip is-${String(riskLevel || "NORMAL").toLowerCase()}`;
  riskBadge.innerHTML = `
    <span class="isg-risk-dot" aria-hidden="true"></span>
    <span>${riskDisplayText(riskLevel)}</span>
    <strong>${Number(riskScore || 0)}</strong>
  `;

  const helpButton = document.createElement("button");
  helpButton.id = "isg-risk-help-button";
  helpButton.type = "button";
  helpButton.className = "isg-risk-help-button";
  helpButton.setAttribute("aria-label", "위해 위험도 산정 기준 보기");
  helpButton.setAttribute("aria-expanded", "false");
  helpButton.textContent = "?";
  helpButton.addEventListener("click", (event) => {
    event.stopPropagation();
    toggleRiskPolicyPopover(helpButton);
  });

  const popover = document.createElement("div");
  popover.id = "isg-risk-policy-popover";
  popover.className = "isg-risk-policy-popover";
  popover.setAttribute("aria-hidden", "true");
  popover.innerHTML = `
    <div class="isg-risk-policy-header">
      <strong>위해 위험도 산정 흐름</strong>
      <button id="isg-risk-policy-close" type="button" aria-label="위해 위험도 산정 흐름 닫기">×</button>
    </div>
    <ol>
      <li><span>1</span><p>DOM 상품명에서 모델명, 브랜드, 바코드, 인증번호 후보를 추출합니다.</p></li>
      <li><span>2</span><p>AI가 상품명을 SafetyKorea 리콜 검색어와 품목 후보로 정제합니다.</p></li>
      <li><span>3</span><p>OpenAPI 리콜 결과를 모델명, 인증번호, 브랜드, 구체 상품명, 품목 순서로 대조합니다.</p></li>
      <li><span>4</span><p>리콜 사유에서 납, 카드뮴, 프탈레이트, DEHP 등 치명 위해성분을 추출합니다.</p></li>
    </ol>
    <dl>
      <dt>위험</dt><dd>동일 모델/인증번호/바코드 근거가 있거나, 구체 상품명 매칭과 치명 위해성분이 함께 있는 경우</dd>
      <dt>주의</dt><dd>동일 제품 근거는 약하지만 유사 품목 리콜에 위해성분 또는 규격/물리 결함이 있는 경우</dd>
      <dt>검토 필요</dt><dd>품목 수준 매칭만 있거나 과거 조치 완료성 리콜만 확인된 경우</dd>
      <dt>정상</dt><dd>현재 검색 조건에서 관련 리콜 이력이 없는 경우</dd>
    </dl>
  `;

  description.append(riskBadge, helpButton);
  document.getElementById("import-safety-guard-root").appendChild(popover);
  document.getElementById("isg-risk-policy-close")?.addEventListener("click", (event) => {
    event.stopPropagation();
    closeRiskPolicyPopover();
  });
  container.append(term, description);
}

function riskTitle(riskLevel) {
  switch (riskLevel) {
    case "DANGER":
      return "위험 상품";
    case "WARNING":
      return "주의 필요";
    case "REVIEW":
      return "검토 필요";
    case "NORMAL":
      return "위험 요소 없음";
    default:
      return "분석 결과";
  }
}

function riskDisplayText(riskLevel) {
  switch (riskLevel) {
    case "DANGER":
      return "위험";
    case "WARNING":
      return "주의";
    case "REVIEW":
      return "검토필요";
    case "NORMAL":
      return "정상";
    default:
      return "검토필요";
  }
}

const api = {
  customsInlineDetailHtml,
  requestRiskDashboard,
  toRiskDashboardRequest,
  inferIngredientKeywords,
  toPrefilteredRecalls,
};

if (typeof module !== "undefined" && module.exports) {
  module.exports = api;
}

