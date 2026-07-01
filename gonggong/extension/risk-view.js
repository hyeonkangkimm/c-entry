(function initializeRiskView(globalScope) {
  const SOURCE_FALLBACK = "국가법령정보센터 근거 URL";
  const CAUTION_FALLBACK = "산업통상자원부 또는 관세청 고시 문구를 확인하세요.";

  function recallRiskCardHtml(recall = {}) {
    const statusClass = riskCardClass(recall.status);
    const statusText = riskStatusText(recall.status);
    const totalCount = Number(recall.totalCount || 0);
    const hasItems = Array.isArray(recall.items) && recall.items.length > 0;

    return `
      <article class="isg-risk-card isg-recall-card ${statusClass}">
        <div class="isg-risk-card-heading">
          <span>리콜 가능성</span>
          <strong>${escapeHtml(statusText)}</strong>
        </div>
        <p>${escapeHtml(recall.message || "리콜 정보를 확인할 수 없습니다.")}</p>
        <small>${totalCount > 0 ? `총 ${totalCount.toLocaleString("ko-KR")}건 · ${escapeHtml(firstRecallReason(recall))}` : ""}</small>
        ${hasItems
          ? `<button class="isg-risk-detail-button" type="button" data-isg-action="open-recall-details">상세 보기</button>`
          : ""}
      </article>
    `;
  }

  function recallDetailListHtml(recall = {}) {
    const items = Array.isArray(recall.items) ? recall.items : [];
    if (!items.length) {
      return `<p class="isg-recall-detail-empty">현재 표시할 리콜 상세 항목이 없습니다.</p>`;
    }

    return `
      <ol class="isg-recall-detail-list">
        ${items.map((item, index) => recallDetailItemHtml(item, index)).join("")}
      </ol>
    `;
  }

  function recallDetailItemHtml(item = {}, index) {
    const reason = item.reason || "사유 미기재";
    const violation = item.violationDetails || "";
    const date = item.announcementDate || "-";
    return `
      <li class="isg-recall-detail-item">
        <div class="isg-recall-detail-heading">
          <span>${index + 1}</span>
          <strong>${escapeHtml(item.productName || "상품명 없음")}</strong>
          <em>${escapeHtml(recallSourceText(item.source))}</em>
        </div>
        <dl>
          <dt>리콜 사유</dt>
          <dd>${escapeHtml(reason)}</dd>
          ${violation ? `<dt>위반/위해 내용</dt><dd>${escapeHtml(violation)}</dd>` : ""}
          <dt>공표일</dt>
          <dd>${escapeHtml(date)}</dd>
        </dl>
      </li>
    `;
  }

  function firstRecallReason(recall = {}) {
    const first = Array.isArray(recall.items) ? recall.items[0] : null;
    return first?.reason || first?.violationDetails || "";
  }

  function recallSourceText(source) {
    switch (source) {
      case "DOMESTIC":
        return "국내";
      case "FOREIGN":
        return "국외";
      default:
        return "출처 미상";
    }
  }

  function customsRiskCardHtml(customs = {}) {
    const statusClass = riskCardClass(customs.status);
    const statusText = riskStatusText(customs.status);
    const tariffLabel = tariffTypeText(customs.tariffType);
    const hasRate = customs.finalTariffRate != null;

    if (!hasRate) {
      return `
        <article class="isg-risk-card isg-customs-card ${statusClass}">
          <div class="isg-risk-card-heading">
            <span>관세 및 추후조치</span>
            <strong>${escapeHtml(statusText)}</strong>
          </div>
          <p class="isg-customs-empty">${escapeHtml(customs.message || "관세 정보를 확인할 수 없습니다.")}</p>
          ${customs.guideUrl
            ? `<a class="isg-customs-link" href="${escapeHtml(customs.guideUrl)}" target="_blank" rel="noreferrer">UNIPASS에서 확인</a>`
            : ""}
          <button class="isg-risk-detail-button" type="button" data-isg-action="open-customs-details">상세 보기</button>
        </article>
      `;
    }

    return `
      <article class="isg-risk-card isg-customs-card ${statusClass}">
        <div class="isg-risk-card-heading">
          <span>관세 및 추후조치</span>
          <strong>${escapeHtml(statusText)}</strong>
        </div>
        <div class="isg-customs-summary">
          <div>
            <small>적용 관세율</small>
            <b>${escapeHtml(formatRate(customs.finalTariffRate))}</b>
          </div>
        </div>
        <div class="isg-customs-meta">
          <span class="isg-customs-type">${escapeHtml(tariffLabel)}</span>
          <dl>
            <dt>적용 근거</dt>
            <dd>${escapeHtml(extractSource(customs.message))}</dd>
            <dt>실제 적용 시 확인</dt>
            <dd>${escapeHtml(extractCaution(customs.message))}</dd>
          </dl>
        </div>
        <button class="isg-risk-detail-button" type="button" data-isg-action="open-customs-details">상세 보기</button>
      </article>
    `;
  }

  function kcRiskCardHtml(kc = {}, detail = "") {
    const matched = kc.certificationValid === true;
    const verificationUrl = safeVerificationUrl(kc.verificationUrl);
    const buttonText = kc.verificationButtonText || "제품안전정보센터에서 실시간 검증하기";

    return `
      <article class="isg-risk-card isg-kc-card ${riskCardClass(kc.status)}">
        <div class="isg-risk-card-heading">
          <span>KC 인증 확인 필요</span>
          <strong>${escapeHtml(riskStatusText(kc.status))}</strong>
        </div>
        <p>${escapeHtml(matched ? (kc.message || "유효한 KC 인증이 확인되었습니다.") : "정보가 없습니다.")}</p>
        <small>${escapeHtml(detail || "")}</small>
        ${matched ? "" : `
          <a class="isg-kc-verification-link" href="${escapeHtml(verificationUrl)}" target="_blank" rel="noopener noreferrer">
            ${escapeHtml(buttonText)}
          </a>
        `}
      </article>
    `;
  }

  function chemicalRiskCardHtml(chemical = {}) {
    const ingredients = Array.isArray(chemical.regulatedIngredients) ? chemical.regulatedIngredients : [];
    const unanalyzed = Array.isArray(chemical.unanalyzedIngredients) ? chemical.unanalyzedIngredients : [];
    const unavailable = chemical.analysisUnavailable === true;
    const safeByEmptyIngredients = !unavailable && ingredients.length === 0;
    const searchUrl = safeChemicalSearchUrl(chemical.searchUrl);
    const buttonText = chemical.searchButtonText || "화학 물질 종합정보시스템에서 직접 성분 검색하기";
    const statusText = safeByEmptyIngredients ? "안전" : riskStatusText(chemical.status);
    const message = chemical.message
      || (safeByEmptyIngredients
        ? "위해 성분이 확인되지 않아 안전으로 표시합니다."
        : unavailable
          ? "시스템 성분 분석이 불가능한 품목입니다."
          : "규제 성분 정보를 확인할 수 없습니다.");

    return `
      <article class="isg-risk-card isg-chemical-card ${riskCardClass(chemical.status)}">
        <div class="isg-risk-card-heading">
          <span>행정처분 및 형사처벌</span>
          <strong>${escapeHtml(statusText)}</strong>
        </div>
        <p>${escapeHtml(message)}</p>
        ${ingredients.length ? `<button class="isg-risk-detail-button" type="button" data-isg-action="open-chemical-details">규제가능성 목록</button>` : ""}
        ${unavailable ? `
          <mark class="isg-chemical-unavailable">[시스템 성분 분석 불가능 품목]</mark>
          ${unanalyzed.length ? `<small>미분석 성분: ${escapeHtml(unanalyzed.join(", "))}</small>` : ""}
          <a class="isg-chemical-search-link" href="${escapeHtml(searchUrl)}" target="_blank" rel="noopener noreferrer">
            ${escapeHtml(buttonText)}
          </a>
        ` : ""}
      </article>
    `;
  }

  function ktlCertificationGuideHtml(guide) {
    if (!guide || !guide.certificationName) {
      return "";
    }
    const testItems = Array.isArray(guide.testItems) ? guide.testItems.filter(Boolean) : [];
    const documents = Array.isArray(guide.requiredDocuments) ? guide.requiredDocuments.filter(Boolean) : [];
    const applicationUrl = safeKtlUrl(guide.applicationUrl);
    const sourceUrl = safeKtlUrl(guide.sourceUrl);
    const markUrl = safeOfficialMarkUrl(guide.certificationMarkUrl);
    return `
      <div class="isg-ktl-guide-heading">
        <div>
          <span>KTL Certification</span>
          <h3>KTL 인증 요건 가이드</h3>
          <strong>${escapeHtml(guide.certificationName)}</strong>
        </div>
        ${markUrl ? `<img src="${escapeHtml(markUrl)}" alt="KC 인증 마크">` : `<b class="isg-ktl-kc-mark">KC</b>`}
      </div>
      <div class="isg-ktl-guide-summary">
        ${guide.legalBasis ? `<div><small>법적 근거</small><b>${escapeHtml(guide.legalBasis)}</b></div>` : ""}
        ${guide.estimatedDuration ? `<div><small>예상 기간</small><b>${escapeHtml(guide.estimatedDuration)}</b></div>` : ""}
        ${guide.estimatedFee ? `<div><small>시험·인증 수수료</small><b>${escapeHtml(guide.estimatedFee)}</b></div>` : ""}
      </div>
      <div class="isg-ktl-guide-details">
        <section>
          <h4>시험 항목</h4>
          ${ktlListHtml(testItems, "공식 시험 항목을 확인하세요.")}
        </section>
        <section>
          <h4>준비 서류</h4>
          ${ktlListHtml(documents, "KTL 신청 페이지에서 제출 서류를 확인하세요.")}
        </section>
      </div>
      <div class="isg-ktl-guide-action">
        <p>${escapeHtml(guide.actionItemGuide || "준비 서류를 확인한 후 KTL에 시험을 신청하세요.")}</p>
        <div>
          ${sourceUrl ? `<a class="is-secondary" href="${escapeHtml(sourceUrl)}" target="_blank" rel="noopener noreferrer">공식 근거 확인</a>` : ""}
          ${applicationUrl ? `<a href="${escapeHtml(applicationUrl)}" target="_blank" rel="noopener noreferrer">KTL에서 인증 신청하기</a>` : ""}
        </div>
      </div>
    `;
  }

  function ktlListHtml(items, emptyText) {
    return items.length
      ? `<ul>${items.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>`
      : `<p>${escapeHtml(emptyText)}</p>`;
  }

  function safeKtlUrl(value) {
    return safeOfficialUrl(value, "ktl.re.kr");
  }

  function safeOfficialMarkUrl(value) {
    return safeOfficialUrl(value, "ktl.re.kr") || safeOfficialUrl(value, "safetykorea.kr");
  }

  function safeOfficialUrl(value, officialHost) {
    try {
      const url = new URL(String(value || ""));
      const host = url.hostname.toLowerCase();
      return url.protocol === "https:" && (host === officialHost || host.endsWith(`.${officialHost}`))
        ? String(value).trim()
        : null;
    } catch (_error) {
      return null;
    }
  }

  function chemicalInlineDetailHtml(chemical = {}) {
    const ingredients = Array.isArray(chemical.regulatedIngredients) ? chemical.regulatedIngredients : [];
    return `
      <div class="isg-inline-detail-head">
        <strong>규제가능성 목록</strong>
        <span>${ingredients.length ? `${ingredients.length.toLocaleString("ko-KR")}개 항목` : "항목 없음"}</span>
      </div>
      <div class="isg-chemical-ingredient-list">
        ${ingredients.length ? ingredients.map(chemicalIngredientRowHtml).join("") : `<p class="isg-chemical-detail-empty">표시할 성분 정보가 없습니다.</p>`}
      </div>
    `;
  }

  function chemicalIngredientRowHtml(ingredient = {}) {
    const regulated = ingredient.regulated === true;
    return `
      <article class="isg-chemical-ingredient">
        <div class="isg-chemical-ingredient-head">
          <span class="isg-chemical-ingredient-pill ${regulated ? "is-regulated" : "is-unregulated"}">
            ${regulated ? "규제 대상" : "비규제/참고"}
          </span>
          <strong>${escapeHtml(ingredient.ingredientName || "성분명 없음")}</strong>
        </div>
        <dl>
          <dt>CAS번호</dt>
          <dd>${escapeHtml(ingredient.casNumber || "-")}</dd>
          <dt>위해성분</dt>
          <dd>${escapeHtml(ingredient.hazardClassification || "-")}</dd>
          ${ingredient.relatedLaw ? `<dt>관련 법령</dt><dd>${escapeHtml(ingredient.relatedLaw)}</dd>` : ""}
          ${ingredient.penaltyProvision ? `<dt><b>적용 가능 처벌 조항</b></dt><dd>${escapeHtml(ingredient.penaltyProvision)}</dd>` : ""}
          ${safeHttpUrl(ingredient.legalSourceUrl) ? `<dt>법령 근거</dt><dd><a href="${escapeHtml(ingredient.legalSourceUrl)}" target="_blank" rel="noopener noreferrer">법령 근거 확인</a></dd>` : ""}
        </dl>
      </article>
    `;
  }

  function safeChemicalSearchUrl(value) {
    return safeHttpUrl(value) || "https://icis.mcee.go.kr/";
  }

  function safeHttpUrl(value) {
    try {
      const url = new URL(String(value || ""));
      return url.protocol === "http:" || url.protocol === "https:" ? String(value).trim() : null;
    } catch (_error) {
      return null;
    }
  }

  function safeVerificationUrl(value) {
    try {
      const url = new URL(String(value || ""));
      if (url.protocol === "http:" || url.protocol === "https:") {
        return String(value).trim();
      }
    } catch (_error) {
      // fallback below
    }
    return "https://www.safetykorea.kr/";
  }

  function extractSource(message) {
    const text = String(message || "");
    const match = text.match(/근거:\s*(.*?)(?:\s*실제 적용 시 확인|$)/);
    return cleanSource(match?.[1] || SOURCE_FALLBACK);
  }

  function cleanSource(source) {
    return String(source)
      .replaceAll("_", " ")
      .replace(/\b(\d{4})(\d{2})(\d{2})\b/g, "$1-$2-$3")
      .replace(/,\s*관세율구분=.*$/, "")
      .replace(/\s+/g, " ")
      .trim();
  }

  function extractCaution(message) {
    const text = String(message || "");
    const match = text.match(/(실제 적용 시 확인.*)$/);
    return match?.[1]?.trim() || CAUTION_FALLBACK;
  }

  function formatRate(value) {
    const rate = Number(value);
    return Number.isFinite(rate) ? `${rate.toLocaleString("ko-KR")}%` : "-";
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
        return "기본관세";
      case "WTO":
        return "WTO 관세";
      case "FTA":
        return "FTA 협정관세";
      case "ANTI_DUMPING":
        return "반덤핑관세";
      case "COUNTERVAILING":
        return "상계관세";
      case "SAFEGUARD":
        return "세이프가드관세";
      case "SPECIAL":
        return "특별관세";
      default:
        return "관세 확인 필요";
    }
  }

  function escapeHtml(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  const api = {
    chemicalRiskCardHtml,
    chemicalInlineDetailHtml,
    customsRiskCardHtml,
    kcRiskCardHtml,
    ktlCertificationGuideHtml,
    recallRiskCardHtml,
    recallDetailListHtml,
  };

  globalScope.RiskView = api;

  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  }
})(typeof globalThis !== "undefined" ? globalThis : this);
