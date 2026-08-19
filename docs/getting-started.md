# 같이살림 - 프로젝트 시작 가이드

지금까지 채팅에서 정리한 문서들을 Claude Code 프로젝트로 옮겨서, 와이어프레임부터 개발까지 이어가기 위한 안내.

## 1. 폴더 구조 (제안)

```
salim/
├── README.md
├── CLAUDE.md                  ← CLAUDE.md (프로젝트 공통 규칙, Claude Code가 자동 로드)
├── design.md                  ← design.md
├── docs/
│   ├── prd.md                 ← PRD (요구사항/정책)
│   ├── design-brief.md        ← AI 디자인 툴 전달용 스타일 가이드
│   ├── firestore-schema.md
│   ├── getting-started.md     ← 이 문서
│   └── wireframe/             ← 화면별 와이어프레임 (onboarding/home/expense/...)
│       └── README.md          ← 화면 인덱스
├── .claude/
│   └── agents/
│       ├── pm-agent.md
│       ├── design-agent.md
│       └── dev-agent.md
└── app/ (Android 프로젝트 구조는 CLAUDE.md 참고, 이후 생성)
```

`design.md`는 프로젝트 전체가 참고하는 단일 기준 문서라 루트에 두는 게 관례에 맞다 (README.md와 같은 위치). PRD(prd.md)/와이어프레임처럼 여러 파일로 늘어나는 상세 문서는 `docs/` 하위로 분리한다.

## 2. 시작하는 방법

Claude Code를 해당 폴더에서 열고, 아래처럼 순서대로 요청하면 자동화 흐름이 시작된다.

**1단계 — 남은 화면 와이어프레임 채우기**
```
docs/prd.md 기준으로 아직 문서화 안 된 화면들
(온보딩, 로그인, 가계부, 일정, 설정, 디데이)의
와이어프레임 문서를 docs/wireframe/ 에 만들어줘.
home.md 문서 형식을 그대로 따라줘.
```

**2단계 — 실제 UI 코드 생성**
```
docs/wireframe/home.md 와 design.md 기준으로
홈 화면 실제 코드를 만들어줘.
```
(화면 하나씩 반복)

**3단계 — 눈으로 보고 대화로 다듬기**
실행해서 보고 "이 카드 간격 좁혀줘" 등으로 계속 수정.

**4단계 — 기획 변경이 생기면**
```
디데이 기능에 [변경사항] 추가하고 싶어.
PRD부터 갱신하고, 영향받는 화면/코드 목록 알려줘.
```
이때 pm-agent → design-agent → dev-agent 순서로 자동 위임되도록 유도.

## 3. 지금 남아있는 미정 사항 (착수 전 참고)

PRD `남은 결정 필요 사항`:
- 카테고리별 예산 향후 도입 여부
- 지출 순서 수동 조정 기능의 실제 필요성
- 위젯 중형 사이즈 도입 여부
- **디데이 탭 포함 하단 5개 탭 구성이 UI상 복잡하지 않은지** (개발 착수 전 재검토 권장)

design.md 미확정 항목:
- 정확한 컬러 코드, 폰트 (코드로 만들면서 자연스럽게 확정 예정)
