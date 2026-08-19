ㅎ# CLAUDE.md

같이살림(Salim) Android 프로젝트의 공통 작업 규칙. 모든 작업 전에 이 파일을 기준으로 판단한다.

> 아래 스택은 최신 Android 표준(Kotlin + Jetpack Compose + MVVM/Clean Architecture) 기준으로 초안을 잡았다. 실제 팀/개인 컨벤션과 다르면 이 파일을 먼저 수정하고 시작할 것.

---

## 1. 문서 우선순위 (항상 이 순서로 확인)

1. `docs/prd.md` — 요구사항, 정책 (무엇을/왜)
2. `design.md` — 디자인 방향, 컴포넌트 목록 (어떻게 보일지)
3. `docs/wireframe/*.md` — 화면별 구조 (개발 착수 전 참고용, 착수 후엔 코드가 우선)

이 문서들에 없는 내용은 임의로 만들지 않는다. 모호하면 먼저 질문한다.

---

## 2. 프로젝트 구조 (Clean Architecture 기준)

```
app/
├── data/
│   ├── local/          # 로컬 전용 데이터만 (Room, DataStore) — 필요 시 추가
│   ├── remote/
│   │   └── firestore/   # Firestore 데이터소스 (실시간 리스너 기반)
│   └── repository/     # Repository 구현체 (Firestore ↔ domain 매핑)
├── domain/
│   ├── model/           # 도메인 모델
│   ├── repository/      # Repository 인터페이스
│   └── usecase/         # UseCase (비즈니스 로직 단위)
├── presentation/
│   ├── home/             # PRD 3. 홈
│   ├── expense/           # PRD 4. 가계부 (입력/수정/전체보기)
│   ├── schedule/          # PRD 5. 일정
│   ├── dday/              # PRD 6. 디데이
│   ├── settings/          # PRD 7. 설정
│   └── common/            # design.md 공통 컴포넌트 (Button, Card, Chip 등)
├── notification/         # FCM 토큰 등록/수신 처리
├── analytics/            # Crashlytics 초기화/커스텀 로그
└── di/                   # Hilt 모듈

functions/                # Firebase Cloud Functions (서버 트리거, 별도 Node/TS 프로젝트)
└── src/
    └── notifyOnExpenseCreate.ts   # 예: 지출 등록 시 상대방에게 FCM 발송
```

Firestore가 오프라인 캐시를 기본 제공하므로, 공유 데이터(가계부/일정/디데이)는 Room을 거치지 않고 Firestore를 직접 SSOT로 사용한다. Room은 완전히 로컬 전용 데이터(임시 입력 초안 등)가 필요해질 때만 추가한다.

## 3. 기술 스택 (기본값 — 확정 시 갱신)

- 언어: Kotlin
- UI: Jetpack Compose
- 아키텍처: MVVM + Clean Architecture (UseCase 단위 분리)
- 상태 관리: StateFlow / UiState 단일 객체 패턴
- DI: Hilt
- 비동기: Coroutines + Flow
- 네비게이션: Navigation Compose
- 데이터/백엔드: Firebase
  - Firestore — 공유 데이터(가계부/일정/디데이) 저장 및 실시간 동기화, 오프라인 캐시 기본 제공
  - Cloud Functions — 서버 트리거 (상대방 지출 등록 시 알림 발송 등, "1인 클라이언트만으론 안 되는 로직" 전담)
  - Cloud Messaging (FCM) — 푸시 발송/수신
  - Crashlytics — 크래시 리포팅
  - Authentication — 구글 로그인 연동 (1번 섹션 로그인 방식과 연결)

## 4. Firebase 데이터 구조

컬렉션/필드 스키마는 `docs/firestore-schema.md`에서 관리한다. 기능 추가/변경 시 이 문서를 함께 갱신할 것 (PRD 변경 → 스키마 영향 여부 확인은 dev-agent 원칙 4번 참고).

## 5. 코드 작성 규칙

- 화면(Composable) 하나당 대응하는 `docs/wireframe/*.md`가 있으면 반드시 먼저 읽는다.
- `design.md`의 컴포넌트 목록에 있는 요소는 `presentation/common/`에 재사용 가능한 형태로 만들고, 화면마다 새로 만들지 않는다.
- 새로운 반복 UI 패턴이 발견되면 `design.md`의 컴포넌트 목록에 추가할 것을 제안한다.
- ViewModel은 화면 단위로 하나, UseCase는 기능 단위로 분리한다.
- 문자열은 하드코딩하지 않고 `strings.xml`에 정의한다.

## 6. 기획 변경 시 워크플로우

1. 기획 변경 요청이 들어오면 `.claude/agents/pm-agent.md` 역할로 먼저 `docs/prd.md`를 갱신
2. 영향받는 화면을 `.claude/agents/design-agent.md` 역할로 `docs/wireframe/`에 반영
3. 실제 코드는 `.claude/agents/dev-agent.md` 역할로 반영
4. 각 단계마다 "이 변경이 다른 어디에 영향을 주는지"를 먼저 나열하고 진행 (예: 디데이 추가 시 홈/알림/위젯까지 확인했던 사례 참고)

## 7. 커밋 규칙 (초안)

- 커밋 메시지는 "무엇을 + 왜"를 짧게 포함
- PRD 변경이 포함된 커밋은 `docs:` 접두사, 코드만 변경 시 `feat:`/`fix:` 접두사 사용
