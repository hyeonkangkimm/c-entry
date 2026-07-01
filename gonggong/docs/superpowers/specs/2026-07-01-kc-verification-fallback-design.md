# KC 인증 매칭 실패 안내 설계

## 목표

KC 인증 API가 유효한 인증을 확인하지 못했을 때 확장 프로그램의 KC 카드에 `정보가 없습니다.` 문구와 제품안전정보센터 이동 버튼을 표시한다.

## 표시 조건

- `kcRisk.certificationValid !== true`이면 매칭 실패로 처리한다.
- 따라서 `INVALID`, `FOUND_CANDIDATE`, `UNKNOWN`, `UNAVAILABLE`와 상태 누락을 포함한다.
- `kcRisk.status === "SAFE"`이고 `kcRisk.certificationValid === true`인 결과에는 실패 안내와 버튼을 표시하지 않는다.
- HSK 매칭 실패로 KC API 검증을 진행하지 못한 경우에도 같은 실패 UI를 표시한다.

## UI

- 실패 카드의 본문은 정확히 `정보가 없습니다.`로 표시한다.
- 버튼 문구는 `kcRisk.verificationButtonText`를 우선 사용한다.
- 버튼 문구가 없으면 `제품안전정보센터에서 실시간 검증하기`를 사용한다.
- 이동 URL은 `kcRisk.verificationUrl`을 우선 사용한다.
- URL이 없거나 안전한 HTTP(S) URL이 아니면 `https://www.safetykorea.kr/`를 사용한다.
- 링크는 새 탭에서 열며 `target="_blank"`와 `rel="noopener noreferrer"`를 적용한다.

## 구조

- `extension/risk-view.js`에 KC 카드 HTML을 생성하는 `kcRiskCardHtml` 함수를 추가한다.
- `extension/content.js`의 API 결과 렌더링과 HSK 매칭 실패 렌더링은 이 함수를 사용한다.
- 기존 `riskCardHtml`은 다른 카드의 호환성을 위해 유지한다.
- `extension/overlay.css`에 KC 검증 링크 스타일을 추가한다.

## 데이터 흐름

1. `background.js`가 `/api/v1/risk-dashboard/analyze` 응답을 전달한다.
2. `content.js`가 `result.kcRisk`를 `kcRiskCardHtml`에 전달한다.
3. 렌더러가 `certificationValid`로 성공 여부를 판단한다.
4. 실패이면 고정 안내 문구, 검증 버튼, 기존 KC 세부정보를 함께 렌더링한다.
5. 성공이면 기존 성공 메시지와 세부정보만 렌더링한다.

## 오류 및 보안 처리

- API 필드가 누락돼도 기본 문구와 기본 URL로 렌더링한다.
- 외부 URL은 HTTP(S)만 허용하고 HTML 속성 이스케이프를 적용한다.
- 외부 링크의 opener 접근을 차단한다.

## 테스트

- `certificationValid: false`인 각 실패 상태에서 안내 문구와 버튼이 표시되는지 검증한다.
- `FOUND_CANDIDATE`도 실패 UI에 포함되는지 검증한다.
- 정상 인증에서는 실패 안내와 버튼이 없는지 검증한다.
- API 버튼 문구와 URL이 우선 적용되는지 검증한다.
- 필드 누락 또는 잘못된 URL에서 기본값이 적용되는지 검증한다.
- HSK 매칭 실패용 KC 카드에도 동일한 UI가 표시되는지 검증한다.
