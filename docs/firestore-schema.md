# Firestore 스키마

컬렉션/필드 구조. 기능 추가·변경 시 이 문서를 함께 갱신한다. (가계부 부분 확정 — 나머지는 초안)

## 개인/공동 이중 경로 원칙 (가계부·카테고리·예산)

미연결(개인 가계부) 상태와 연결(공동) 상태를 모두 지원하기 위해, 가계부 관련 데이터는 **두 경로에 동일한 필드 스키마**로 저장한다. 도메인 모델은 하나만 두고 Repository가 경로만 선택한다.

| 상태 | 경로(SSOT) |
|---|---|
| 미연결 | `users/{userId}/expenses`, `users/{userId}/categories`, `users/{userId}/budget` |
| 연결 | `couples/{coupleId}/expenses`, `couples/{coupleId}/categories`, `couples/{coupleId}/budget` |

- 연결 성사 시 기존 개인 데이터는 **이관하지 않는다**. 개인 데이터는 그대로 유지되어 본인만 열람하고, 연결 이후 신규 기록만 공동 경로에 쓴다. (PRD 1 "연결 전 데이터는 개인 데이터로 유지, 연결 후 데이터만 공동으로 전환")

## couples/{coupleId}
연결된 커플 단위 문서. 하위 컬렉션은 이 문서 기준으로 공유된다.

| 필드 | 설명 |
|---|---|
| createdAt | 연결 성사 시각 |
| memberIds | 배열, 두 users 문서 참조 |
| deletedAt | null이면 정상. 값이 있으면 유예기간 중 (PRD 9번 연결 해제/탈퇴 정책) — Firestore 보안 규칙에서 이 필드가 있으면 신규 쓰기 차단 |

### {expenses}/{expenseId}
지출 내역. (PRD 4. 가계부) — `users/{userId}/expenses` / `couples/{coupleId}/expenses` **공통 필드 스키마**.

| 필드 | 타입 | 설명 |
|---|---|---|
| amount | Long | 금액 (원 단위 정수) |
| spentAt | Timestamp | 지출 일시 (날짜+시간 입력을 합쳐 저장). **날짜별 그룹 헤더**의 소스 |
| yearMonth | String | `"2026-08"` 형식. 월 단위 등가(equality) 조회 키 (Timestamp range 대신 저비용 조회) |
| spenderId | String (uid) | 지출자. 미연결 시 항상 본인 uid, 연결 후 나/배우자 |
| categoryId | String | categories 문서 id 참조 |
| categoryName | String | 표시용 denormalize (카테고리 비활성화·이름변경 후에도 과거 지출 표시 안정) |
| categoryIcon | String | 표시용 denormalize (아이콘 키) |
| memo | String? | 메모 |
| createdAt | Timestamp | 등록 시각. **전체보기 정렬 = 입력 시간순**(PRD 4)의 정렬 키 |
| updatedAt | Timestamp | 수정 시각 |

**정렬/그룹**: 리스트 정렬 키는 `createdAt`, 날짜 그룹 헤더는 `spentAt`. (두 값을 분리 저장하는 이유 — 지출 발생일과 입력 순서가 다를 수 있음)

**쿼리 시나리오와 필요한 복합 인덱스**

| 화면/필터 | 쿼리 | 필요 인덱스 |
|---|---|---|
| 월 전체보기 | `where yearMonth == ? orderBy createdAt desc` | (yearMonth, createdAt) |
| + 카테고리 필터 | `where yearMonth == ? where categoryId == ? orderBy createdAt desc` | (yearMonth, categoryId, createdAt) |
| + 지출자 필터 | `where yearMonth == ? where spenderId == ? orderBy createdAt desc` | (yearMonth, spenderId, createdAt) |

- 기간 필터(월 범위)는 `yearMonth` `in` 조건으로 여러 달을 조회.
- **메모 검색**: Firestore는 전문검색을 지원하지 않는다. 1차는 로드된 해당 월 결과를 **클라이언트 측 부분일치**로 필터링한다(스코프가 "이번 달"이라 비용 문제 없음). 전역 검색이 필요해지면 외부 검색(Algolia 등) 재검토.

### couples/{coupleId}/schedules/{scheduleId}
일정. (PRD 5. 일정)

| 필드 | 설명 |
|---|---|
| title | 제목 |
| date / time | 일시 |
| type | 우리 일정 / 개인(나) / 개인(배우자) |

### couples/{coupleId}/ddays/{ddayId}
디데이. (PRD 6. 디데이)

| 필드 | 설명 |
|---|---|
| title | 제목 |
| date | 날짜 |
| repeatYearly | 매년 반복 여부 |
| source | auto(프로필 생일/기념일 연동) / manual |

### {categories}/{categoryId}
가계부 카테고리. (PRD 7. 설정 - 카테고리 수정) — `users/{userId}/categories` / `couples/{coupleId}/categories` **공통 필드 스키마**.

| 필드 | 타입 | 설명 |
|---|---|---|
| name | String | 카테고리명 |
| icon | String | 아이콘 키 |
| order | Int | 표시 순서 (자주 쓰는 항목 상단 노출용) |
| active | Boolean | `false`면 "사용 중지"(PRD 7 비활성화). 목록엔 중지 표시, 신규 지출 선택 불가. 기존 지출은 그대로 유지 |
| isDefault | Boolean | 시드된 기본 카테고리 여부 |
| createdAt | Timestamp | 생성 시각 |

- **시드**: 최초 진입(개인) / 연결 성사(커플) 시 기본 카테고리 집합을 이 컬렉션에 생성한다. 기본 목록·아이콘 키는 design-brief와 대조 후 확정(미확정 사항 참고).
- **삭제 정책**(PRD 7): 해당 categoryId를 참조하는 지출이 없을 때만 완전 삭제 허용, 그 외엔 `active=false` 처리.

### {budget}/{yearMonth}
월별 예산. (PRD 7. 설정 - 달별 예산) — `users/{userId}/budget` / `couples/{coupleId}/budget` **공통 필드 스키마**. 문서 id는 `"2026-08"` 형식.

| 필드 | 타입 | 설명 |
|---|---|---|
| totalBudget | Long | 월 전체 예산 (원). 카테고리별 예산은 1차 제외 |

- **홈 집계**(PRD 3, 달별/카테고리별 종합·예산 대비 현황): 별도 롤업 문서 없이 해당 월 expenses를 **클라이언트에서 집계**한다(1차, 커플 단위 소규모 데이터).
- **예산 알림**(PRD 8, 80% 도달 / 100% 초과): Cloud Function이 지출 쓰기 시 해당 월 합계를 재계산해 판단한다. 스키마상 별도 집계 필드는 두지 않는다.

## users/{userId}
개인 프로필 + 로그인 정보. (PRD 1. 온보딩 및 로그인) 문서 id는 **Firebase Auth uid**와 동일하게 둔다. 미연결 상태에서는 이 문서 하위 컬렉션(expenses/categories/budget)이 가계부 데이터의 SSOT다.

> 인증 크리덴셜(비밀번호/OAuth 토큰)의 SSOT는 **Firebase Authentication**이다. Firestore `users` 문서에는 크리덴셜을 저장하지 않고, 프로필·연동 상태만 미러링한다.

### 로그인/계정 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| providers | Array\<String\> | 연동된 로그인 방식. 1차는 `["google"]`만. 추후 `"apple"`, `"kakao"` 추가(한 계정에 복수 연동 대비 배열) |
| primaryProvider | String | 최초 가입에 사용한 방식. 1차 `"google"` |
| email | String? | 로그인 이메일 (구글 계정 이메일). provider가 이메일 미제공 시 null 가능 |
| displayName | String? | 표시 이름 (소셜 프로필 기본값, 이후 사용자 수정 가능) |
| photoUrl | String? | 프로필 이미지 URL (소셜 프로필 기본값) |
| createdAt | Timestamp | 최초 가입 시각 |
| lastLoginAt | Timestamp | 최근 로그인 시각 |
| deletedAt | Timestamp? | null이면 정상. 값이 있으면 회원탈퇴 유예기간 중 (PRD 9). 종료 후 완전 삭제 |

- **구글 로그인**(1차): Firebase Authentication Google provider 사용. 로그인 성공 시 uid로 `users` 문서 생성/갱신(`lastLoginAt` 업데이트), 최초 생성 시 `providers=["google"]`, `primaryProvider="google"`.
- **추후 확장**:
  - 애플 — Firebase Auth 기본 지원 provider. `providers`에 `"apple"` 추가.
  - 카카오 — Firebase Auth 기본 provider가 아니므로 **Cloud Functions로 커스텀 토큰 발급**이 필요. 이때도 동일 `users` 문서 스키마에 `"kakao"`만 추가.

### 프로필/설정 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| coupleId | String? | 연결된 couples 문서 참조, 미연결 시 null |
| birthday / anniversary | Timestamp? | 프로필 정보, ddays에 자동 반영되는 source |
| fcmToken | String? | 푸시 발송용 |
| notificationSettings | Map | 알림 종류별 on/off (PRD 8. 알림 표 기준) |

## 보안 규칙 메모 (설계 의도 — 규칙 파일은 별도 작성)
- `users/{userId}/**`: 본인만 read/write.
- `couples/{coupleId}/**`: `memberIds`에 포함된 uid만 read/write.
- `couples/{coupleId}`에 `deletedAt`가 있으면 신규 쓰기 차단(유예기간), 열람만 허용. (PRD 9)

## 미확정 사항
- 기본 카테고리 최종 목록과 아이콘 키 (design-brief 대조 후 확정)
- 카테고리별 예산 향후 도입 여부 (PRD 10 남은 결정 필요 사항)
