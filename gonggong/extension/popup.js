const statusElement = document.getElementById("popup-status");

chrome.tabs?.query({ active: true, currentWindow: true }, ([tab]) => {
  const url = tab?.url || "";

  if (url.includes("aliexpress.com") || url.includes("temu.com")) {
    statusElement.textContent = "지원되는 상품 페이지입니다. 화면 우측 상단 배지를 확인하세요.";
    return;
  }

  statusElement.textContent = "알리익스프레스 또는 테무 상품 페이지에서 동작합니다.";
});
