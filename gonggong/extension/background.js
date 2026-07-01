const API_BASE_URL = "http://localhost:8080";

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message?.type === "ANALYZE_RISK_DASHBOARD") {
    analyzeRiskDashboard(message.payload)
      .then((data) => sendResponse({ ok: true, data }))
      .catch((error) => {
        sendResponse({
          ok: false,
          error: error instanceof Error ? error.message : "Unknown error",
        });
      });

    return true;
  }

  if (message?.type === "MATCH_HSK") {
    matchHsk(message.payload)
      .then((data) => sendResponse({ ok: true, data }))
      .catch((error) => {
        sendResponse({
          ok: false,
          error: error instanceof Error ? error.message : "Unknown error",
        });
      });

    return true;
  }

  if (!message || message.type !== "ANALYZE_PRODUCT") {
    return false;
  }

  analyzeProduct(message.payload)
    .then((data) => sendResponse({ ok: true, data }))
    .catch((error) => {
      sendResponse({
        ok: false,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    });

  return true;
});

async function analyzeRiskDashboard(payload) {
  const response = await fetch(`${API_BASE_URL}/api/v1/risk-dashboard/analyze`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(await errorMessage(response, "Risk dashboard API failed"));
  }

  return response.json();
}

async function matchHsk(payload) {
  const response = await fetch(`${API_BASE_URL}/api/seller/hsk/match`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(await errorMessage(response, "HSK match API failed"));
  }

  return response.json();
}

async function analyzeProduct(payload) {
  const response = await fetch(`${API_BASE_URL}/api/products/analyze`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(await errorMessage(response, "Analyze API failed"));
  }

  return response.json();
}

async function errorMessage(response, fallback) {
  try {
    const body = await response.json();
    return `${fallback} with status ${response.status}: ${body.message || body.code || "Unknown error"}`;
  } catch (_error) {
    return `${fallback} with status ${response.status}`;
  }
}
