# Firestore 스키마

컬렉션/필드 구조. 기능 추가·변경 시 이 문서를 함께 갱신한다. (가계부 부분 확정 — 나머지는 초안)

## 개인/공동 이중 경로 원칙 (가계부·카테고리·예산)

미연결(개인 가계부) 상태와 연결(공동) 상태를 모두 지원하기 위해, 가계부 관련 데이터는 **두 경로에 동일한 필드 스키마**로 저장한다. 도메인 모델은 하나만 두고 Repository가 경로만 선택한다.

| 상태 | 경로(SSOT) |
|---|---|
| 미연결 | `users/{userId}/{expenses,schedules,ddays,categories,budget}` |
| 연결 | `couples/{coupleId}/{expenses,schedules,ddays,categories,budget}` |

- 경로 선택은 `data/repository/UserScope`가 전담한다. 저장소들은 경로를 직접 계산하지 않고 아래 두 가지만 쓴다.

  ```kotlin
  val scope: Flow<DocumentReference?>       // 커플이 있으면 couples/{id}, 없으면 users/{uid}, 미로그인이면 null
  fun requireScopeDoc(): DocumentReference  // 쓰기용. 위 스트림의 최신값 캐시
  ```

- **연결 여부 판정의 SSOT는 `couples` 쿼리다** — `couples.where("memberIds", "array-contains", uid).limit(1)` 스냅샷 리스너. `users/{uid}.coupleId` 필드를 판정에 쓰지 않는 이유는 상대가 내 문서를 쓸 수 있는 창구를 최소로 유지하기 위해서다. 성사 배치의 일부가 유실돼 `coupleId`가 비어도 쿼리는 커플을 찾아내고, 앱이 자기 문서의 `coupleId`를 조용히 다시 채운다(self-heal).
- **예외 — 프로필(이름/생일/기념일)은 연결 후에도 `users/{uid}`에 남는다.** 각자의 개인 정보라 공동 경로로 옮기지 않는다. `FirestoreProfileRepository`만 `scope`를 쓰지 않고 계속 사용자 문서를 직접 본다. (상대 생일을 디데이에 띄울지는 아래 미확정 사항 참고)
  - 다만 **표시 이름은 커플 문서 `members` 맵에 사본을 남긴다** — 상대는 내 `users` 문서를 읽을 수 없어서, 지출자 이름을 보여줄 경로가 그 사본뿐이다. SSOT는 여전히 `users/{uid}.displayName`이고 커플 문서 쪽은 표시용 미러다.
- 연결 성사 시 기존 개인 데이터는 **이관하지 않는다**. 개인 데이터는 그대로 유지되어 본인만 열람하고, 연결 이후 신규 기록만 공동 경로에 쓴다. (PRD 1 "연결 전 데이터는 개인 데이터로 유지, 연결 후 데이터만 공동으로 전환")

## couples/{coupleId}
연결된 커플 단위 문서. 하위 컬렉션은 이 문서 기준으로 공유된다.

**문서 id는 두 uid로부터 결정적으로 만든다** — 사전순으로 정렬한 두 uid를 `_`로 이은 값 (예: `AbC1…_XyZ7…`).

| 필드 | 타입 | 설명 |
|---|---|---|
| memberIds | Array\<String\> | 두 사람의 uid. 항상 2개이고, 정렬해 이으면 문서 id와 같아야 한다 |
| members | Map | 표시용 프로필 미러 — `{ "<uid>": { displayName, photoUrl, joinedAt } }` |
| inviteCode | String | 성사에 쓰인 초대 코드. 보안 규칙이 생성 시 검증에 쓴다 |
| createdAt | Timestamp | 연결 성사 시각 |
| deletedAt | Timestamp? | null이면 정상. 값이 있으면 유예기간 중 (PRD 9 연결 해제/탈퇴 정책) — 보안 규칙이 신규 쓰기를 차단 |

**왜 결정적 id인가**
1. 같은 두 사람에 대한 문서가 항상 하나라, 경합으로 커플 문서가 둘 생기는 사고가 원천 차단된다.
2. 보안 규칙이 문서 id와 `memberIds`의 일치를 검증할 수 있다 — 랜덤 id면 불가능하다.
3. 재연결이 같은 문서로 돌아온다. PRD 9의 "유예기간 중 재연결 시 데이터 복원"이 `deletedAt`을 지우는 것으로 끝난다.

**왜 `members`를 맵으로 두는가**
- `users/{uid}`는 본인만 읽을 수 있어 상대의 이름·사진을 가져올 수 없다. 규칙을 푸는 대신 표시용 최소 정보만 커플 문서에 복제한다.
- 서브컬렉션이 아닌 이유: 커플 문서를 만드는 **같은 배치** 안에서 서브컬렉션 쓰기 규칙이 아직 존재하지 않는 부모 문서를 참조해야 해서 검증이 꼬인다. 맵은 커플 문서 생성 규칙 하나로 함께 검증된다.
- 채우는 시점은 성사 배치와 **프로필 저장** 두 곳이다. 성사 때 한 번 채우고, 그 뒤 설정 > 프로필에서 이름을 바꾸면 `FirestoreProfileRepository.save`가 자기 항목의 `displayName`을 다시 쓴다 — 상대 화면의 지출자 이름이 이 사본에서만 오기 때문이다. 쓰기는 커플 문서 `update` 권한(멤버면 허용)으로 통과한다.
- `photoUrl`은 여전히 갱신 경로가 없다. 초대 문서에 사진을 담지 않기로 해서 상대 항목이 비어 있고, 구글 프로필 사진이 바뀌어도 낡은 채로 남는다 — 지금은 사진을 쓰는 화면이 없어(공용 아이콘으로 그린다) 드러나지 않지만, 프로필 사진을 실제로 띄우게 되면 각자 앱을 열 때 자기 항목을 다시 쓰는 처리가 필요하다.
- 감수하는 부작용: 두 사람이 서로의 표시 이름을 덮어쓸 수 있다. 연결된 당사자 사이라 수용한다.

### {expenses}/{expenseId}
지출 내역. (PRD 4. 가계부) — `users/{userId}/expenses` / `couples/{coupleId}/expenses` **공통 필드 스키마**.

| 필드 | 타입 | 구현 | 설명 |
|---|---|---|---|
| amount | Long | ✅ | 금액 (원 단위 정수) |
| spentAtMillis | Long | ✅ | 지출 일시 (UTC millis, 날짜+시간 합산). **날짜별 그룹 헤더**의 소스 |
| spenderId | String (uid) | ➕ | 지출자의 uid |
| createdAtMillis | Long | ✅ | 등록 시각. **전체보기 정렬 = 입력 시간순**(PRD 4)의 정렬 키 |
| categoryName | String | ✅ | 저장 시점의 카테고리명. `categoryId`가 없거나 카테고리를 찾지 못할 때만 표시에 쓴다 |
| memo | String? | ✅ | 메모 |
| categoryId | String? | ✅ | categories 문서 id 참조. **표시는 이 id로 지금 이름·아이콘을 찾는다** — 이름을 바꾸면 기존 지출에도 새 이름이 보인다(PRD 7) |
| yearMonth | String | ⬜ | `"2026-08"` 형식. 필터 조합용 등가 조회 키 |
| updatedAtMillis | Long? | ✅ | 수정 시각. 지출 수정(PRD 4) 때만 채운다. 수정은 문서를 통째로 다시 쓰되 `createdAtMillis`는 원래 값을 유지해 정렬 위치가 바뀌지 않는다 |

> ✅ 구현됨 · ➕ 상대방 연결과 함께 추가 · ⬜ 아직 미구현(카테고리/필터 기능 도입 시)
>
> 날짜를 Timestamp가 아닌 `*Millis`(Long)로 두는 것은 schedules·ddays와 같은 이유이며, 현재 코드도 그렇게 저장한다. 월 조회는 `spentAtMillis` 범위 쿼리를 쓰고 있어 `yearMonth`는 아직 필요하지 않다 — 카테고리·지출자 필터가 붙어 복합 인덱스가 늘어날 때 도입한다.

**지출자를 uid로 저장한다 (연결 도입과 함께 바뀌는 지점)**

지금 코드는 `spender` 필드에 `"ME"` / `"PARTNER"`라는 **보는 사람 기준 상대값**을 저장한다. 개인 경로에서는 문제가 없지만 공동 경로에서는 깨진다 — A가 저장한 `"ME"`를 B가 읽으면 "나"로 보인다.

- 저장은 `spenderId`(uid), 표시는 상대적으로: 읽을 때 `spenderId == 내 uid ? 나 : 배우자`.
- 도메인 모델의 `Spender` enum(`ME`/`PARTNER`)은 그대로 둔다. UI는 손대지 않고 Repository가 uid ↔ enum 매핑을 담당한다.
- **하위호환**: 기존 개인 경로 문서에는 `spenderId`가 없다. 없으면 예전 `spender` 필드로 폴백한다. 개인 데이터는 본인만 열람하므로 폴백 결과가 항상 옳고, 마이그레이션이 필요 없다.

**정렬/그룹**: 리스트 정렬 키는 `createdAtMillis`, 날짜 그룹 헤더는 `spentAtMillis`. (두 값을 분리 저장하는 이유 — 지출 발생일과 입력 순서가 다를 수 있음)

**쿼리 시나리오와 필요한 복합 인덱스**

| 화면/필터 | 쿼리 | 필요 인덱스 |
|---|---|---|
| 월 전체보기 (현재 구현) | `where spentAtMillis >= ? < ? orderBy spentAtMillis desc` | 단일 필드(자동) |
| 월 전체보기 (yearMonth 도입 후) | `where yearMonth == ? orderBy createdAtMillis desc` | (yearMonth, createdAtMillis) |
| + 카테고리 필터 | `where yearMonth == ? where categoryId == ? orderBy createdAtMillis desc` | (yearMonth, categoryId, createdAtMillis) |
| + 지출자 필터 | `where yearMonth == ? where spenderId == ? orderBy createdAtMillis desc` | (yearMonth, spenderId, createdAtMillis) |

- 기간 필터(월 범위)는 `yearMonth` `in` 조건으로 여러 달을 조회.
- **메모 검색**: Firestore는 전문검색을 지원하지 않는다. 1차는 로드된 해당 월 결과를 **클라이언트 측 부분일치**로 필터링한다(스코프가 "이번 달"이라 비용 문제 없음). 전역 검색이 필요해지면 외부 검색(Algolia 등) 재검토.

### couples/{coupleId}/schedules/{scheduleId}
일정. (PRD 5. 일정)

| 필드 | 타입 | 설명 |
|---|---|---|
| title | String | 제목 |
| dateMillis | Number | 날짜 (UTC 자정 millis) |
| minuteOfDay | Number? | 시작 시각(0~1439). **없으면 종일 일정** |
| type | String | SHARED(우리 일정) / PERSONAL(개인 일정) |
| ownerId | String (uid) | 일정 주인. PERSONAL일 때 "나 / 배우자"를 가르는 소스. SHARED에도 등록자 기록용으로 채운다 |
| createdAtMillis | Number | 등록 시각 |

- **유형도 지출자와 같은 이유로 바뀐다.** 기존 `MINE`/`PARTNER`는 보는 사람 기준 상대값이라 공동 경로에서 뒤집힌다. `PERSONAL` + `ownerId`로 저장하고, 표시할 때 `우리 일정 / 개인(나) / 개인(배우자)` 3종으로 환원한다. 도메인의 `ScheduleType` enum 3종은 유지한다.
- **하위호환**: `ownerId`가 없는 기존 문서는 예전 `type` 값(`MINE`/`PARTNER`)으로 폴백한다.

- 캘린더가 월 단위로 그려지므로 조회도 월 단위(`dateMillis` 범위 쿼리 + 오름차순)로 한다.
- 날짜와 시각을 한 값으로 합치지 않고 분리한다 — 종일 여부를 `minuteOfDay` 유무로만 표현할 수 있고, 날짜 그룹핑도 추가 계산 없이 된다.
- 반복 일정은 1차 범위에서 제외 (wireframe/schedule.md 5-2).
- 현재 구현 경로는 `users/{uid}/schedules` (미연결 개인 경로). 연결 도입 시 `UserScope`에서 `couples/{coupleId}`로 분기한다.

### {budget}/{yyyy-MM}
월 예산. (PRD 3. 홈 "이번 달 예산") — `users/{userId}/budget` / `couples/{coupleId}/budget` **공통 필드 스키마**.

| 필드 | 타입 | 설명 |
|---|---|---|
| amount | Number | 해당 월 예산 (원 단위 정수) |

- 문서 id는 정렬 가능하도록 `yyyy-MM` (예: `2026-08`). 월마다 예산을 다르게 잡을 수 있다.
- 문서가 없으면 "예산 미설정" 상태. 홈 예산 카드가 "예산을 설정해보세요"를 노출하고, 카드를 탭해 설정한다.
- 사용액(분자)은 예산에 저장하지 않고 해당 월 expenses 합계로 계산한다.

### couples/{coupleId}/ddays/{ddayId}
디데이. (PRD 6. 디데이)

| 필드 | 타입 | 설명 |
|---|---|---|
| title | String | 제목 |
| dateMillis | Number | 기준 날짜 (UTC 자정 millis) |
| repeatYearly | Boolean | 매년 반복 여부 |
| source | String | AUTO(프로필 생일/기념일 연동) / MANUAL(직접 추가) |
| createdAtMillis | Number | 등록 시각 |

- 정렬(가까운 순)은 저장 시점이 아니라 표시 시점에 계산한다. 매년 반복 항목은 저장된 날짜와 다음 기념일이 다르기 때문에 Firestore `orderBy`로는 정렬할 수 없다.
- AUTO 항목은 `users/{userId}`의 birthday/anniversary에서 파생된다. 디데이 탭에서 수정·삭제 불가.
- 가계부와 동일하게, 현재 구현 경로는 `users/{uid}/ddays` (미연결 개인 경로).

### {categories}/{categoryId}
가계부 카테고리. (PRD 7. 설정 - 카테고리 수정) — `users/{userId}/categories` / `couples/{coupleId}/categories` **공통 필드 스키마**.

| 필드 | 타입 | 구현 | 설명 |
|---|---|---|---|
| name | String | ✅ | 카테고리명 (최대 8자, 같은 경로 안에서 중복 불가 — 클라이언트 검증) |
| icon | String | ✅ | 아이콘 키. 기본 카테고리는 고유 키(`food`, `cafe` …), 사용자가 추가한 항목은 `custom`. 이름을 바꿔도 유지 |
| color | String | ✅ | 색 키(`peach`, `sage` … design.md "카테고리 색"). **고정 항목끼리는 색 계열이 겹치지 않는다** — 추가 시 안 쓰는 색 배정, 고정 교체 시 계열이 겹치면 올라온 항목 색만 변경. 필드가 없는 예전 기본 카테고리 문서는 기본 색으로 읽고 다음 저장 때 채운다 |
| fixed | Boolean | ✅ | `true`면 지출 입력의 고정 칩, `false`면 "+더보기" 시트. **항상 5개가 `true`** |
| order | Int | ✅ | 표시 순서. 고정/더보기 구역 안에서 이 값으로 정렬 |
| isDefault | Boolean | ✅ | 시드된 기본 카테고리 여부 (시드할 때만 쓴다) |
| active | Boolean | ⬜ | `false`면 "사용 중지"(PRD 7 비활성화). 목록엔 중지 표시, 신규 지출 선택 불가. 기존 지출은 그대로 유지 — 삭제 기능과 함께 도입 |

- **문서 id**: 기본 카테고리는 고정 id(`food`, `cafe`, `shopping`, `culture`, `travel`, `transport`, `living`, `health`, `housing`, `gift`, `etc`), 사용자가 추가한 항목은 UUID. 기본 목록은 domain `DefaultCategories`.
- **시드**: 경로(개인/커플)의 categories가 **서버 기준으로 비어 있을 때** 앱이 기본 목록을 배치로 심는다(`FirestoreCategoryRepository`). 캐시만 비어 있는 경우엔 심지 않고 기본 목록을 화면에만 먼저 그린다. id가 고정값이라 두 사람이 동시에 심어도 같은 문서를 덮어쓸 뿐 중복이 생기지 않는다 — 그래서 연결 성사 배치에 시드를 넣지 않았다.
- **연결 시**: 다른 가계부 데이터와 같이 개인 카테고리를 커플 경로로 옮기지 않는다. 연결 후에는 커플 경로에 기본 목록이 새로 심기고, 한쪽이 수정하면 둘 다 바뀐다.
- **고정 교체**: 더보기 항목과 고정 항목의 `fixed`·`order`를 한 배치에서 맞바꾼다 — 고정 개수가 5개에서 벗어나는 순간이 없다. 올라온 항목의 `color`가 남은 고정 항목과 겹치면 같은 배치에서 색도 바꾼다.
- **쓰기**: `SetOptions.merge()` — 이름만 바꿔 저장할 때 `isDefault` 같은 다른 필드를 지우지 않는다. 보안 규칙은 기존 `users/{uid}/**`, `couples/{id}/**` 규칙으로 통과한다.
- **예전 지출 호환**: `categoryId` 없이 `categoryName`만 있는 지출은 저장된 이름 그대로 보여주고, 이름이 기본 카테고리와 같으면 그 아이콘을 쓴다(`"문화/여가"`는 `culture`).
- **삭제 정책**(PRD 7, 미구현): 해당 categoryId를 참조하는 지출이 없을 때만 완전 삭제 허용, 그 외엔 `active=false` 처리.

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
| displayName | String? | 표시 이름 (소셜 프로필 기본값, 이후 설정 > 프로필에서 사용자 수정 가능). 가계부 지출자 표시에 쓴다 |
| photoUrl | String? | 프로필 이미지 URL (소셜 프로필 기본값) |
| createdAt | Timestamp | 최초 가입 시각 |
| lastLoginAt | Timestamp | 최근 로그인 시각 |
| deletedAt | Timestamp? | null이면 정상. 값이 있으면 회원탈퇴 유예기간 중 (PRD 9). 종료 후 완전 삭제 |

- **구글 로그인**(1차): Firebase Authentication Google provider 사용. 로그인 성공 시 uid로 `users` 문서 생성/갱신(`lastLoginAt` 업데이트), 최초 생성 시 `providers=["google"]`, `primaryProvider="google"`.
- **`displayName`은 재로그인 때 구글 값으로 덮지 않는다.** 사용자가 설정에서 바꿀 수 있는 값이라(PRD 7), 덮으면 바꾼 이름이 로그인할 때마다 되돌아간다. 구글 이름은 최초 생성과 값이 비어 있는 문서를 채울 때만 쓴다.
- **추후 확장**:
  - 애플 — Firebase Auth 기본 지원 provider. `providers`에 `"apple"` 추가.
  - 카카오 — Firebase Auth 기본 provider가 아니므로 **Cloud Functions로 커스텀 토큰 발급**이 필요. 이때도 동일 `users` 문서 스키마에 `"kakao"`만 추가.

### 프로필/설정 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| coupleId | String? | 연결된 couples 문서 id, 미연결 시 null. **경로 판정의 SSOT가 아니다** — 아래 참고 |
| birthdayMillis / anniversaryMillis | Number? | 생일/기념일 (UTC 자정 millis). ddays의 AUTO 항목이 파생되는 source — ddays 컬렉션에 쓰지 않는다 |
| inviteCode | String? | 지금 발급해 둔 내 초대 코드. `invites`는 `list`를 막아 뒀기 때문에 "내 코드 찾기"를 쿼리로 할 수 없어, 코드를 내 문서에 적어 둔다 |
| fcmToken | String? | 푸시 발송용 |
| notificationSettings | Map | 알림 종류별 on/off (PRD 8. 알림 표 기준) |

- `coupleId`의 쓰임: ① 보안 규칙이 "이미 다른 사람과 연결된 사용자인가"를 판정하는 근거 ② 향후 Cloud Functions/FCM이 uid만 가지고 커플 경로를 찾는 지름길. 앱의 경로 판정은 `couples` 쿼리로 하고, 이 필드는 값이 어긋나면 조용히 다시 채운다(맨 위 "개인/공동 이중 경로 원칙" 참고).
- **`coupleId` 키는 항상 존재해야 한다.** 보안 규칙이 `resource.data.coupleId == null`을 평가하는데, 키 자체가 없으면 규칙이 오류로 거절된다. `FirebaseAuthRepository`의 최초 사용자 문서 생성이 `coupleId: null`을 심는 것이 이 전제를 만든다.
- 날짜를 Timestamp가 아닌 millis로 두는 이유는 ddays/schedules와 같다 — 시각 없는 '날짜'라서 타임존 해석이 끼어들 여지를 없앤다.
- 로그인한 사용자의 `users/{uid}` 문서에 프로필 필드만 merge로 쓴다. (같은 문서에 로그인/계정 필드가 함께 살기 때문)

## 상대방 연결 (PRD 9)

Cloud Functions 없이 **클라이언트 쓰기 + 보안 규칙**만으로 두 사용자 문서를 동시에 바꾼다.

### invites/{code}
초대 코드. **문서 id가 코드 자체**다 — 수락자가 `get` 한 번으로 검증할 수 있고, 쿼리를 쓰지 않아 `list` 권한(=코드 전수 조회)을 열 필요가 없다.

| 필드 | 타입 | 설명 |
|---|---|---|
| inviterUid | String | 코드 발급자 |
| inviterName | String? | 수락 화면의 "○○님과 연결할까요?" 표시용 |
| createdAt | Timestamp | 발급 시각 |
| expiresAt | Timestamp | `createdAt + 30분` |

- **코드 규격**: Crockford Base32 6자리 — `0-9`와 `A-Z`에서 혼동 문자 `I`, `L`, `O`, `U`를 뺀 32글자. 약 10.7억(32⁶) 조합.
  - 입력은 대문자로 올린 뒤 Crockford 관례대로 헷갈리는 글자를 숫자로 접는다(`I`·`L` → `1`, `O` → `0`). 손으로 옮겨 적다 나는 오타를 실패로 만들지 않기 위한 것.
  - 규격은 `domain/model/InviteCode.kt`에 있다 — 발급(:data)과 입력 정규화(UI)가 같은 값을 봐야 한다.
- **발급**: 사용자당 유효 코드 1개. 무작위 코드로 `create`를 시도하고(이미 있으면 실패) 충돌 시 최대 5회 재생성한다. 새로 만들면 이전 코드는 삭제한다.
- **소진**: 성사 배치에서 삭제한다. 삭제가 유실돼도 규칙의 "발급자가 이미 연결됨" 검사에 걸려 재사용되지 않는다.
- **사진 URL은 담지 않는다** — 코드를 맞힌 사람에게 노출되는 정보를 표시 이름 하나로 줄인다.

> **유효기간은 클라이언트 시계로 찍는다**: `expiresAt`을 서버 타임스탬프로 계산하려면 서버 코드가 필요하다. 대신 규칙이 발급·사용 두 시점 모두에서 `expiresAt > request.time`(서버 시각)을 확인하므로, **서버 시각 기준으로 만료된 코드는 절대 사용되지 않는다**. 기기 시계를 앞당기면 자기 코드의 수명을 늘릴 수 있지만 남의 코드에는 영향이 없어 그대로 둔다.

> **감수하는 트레이드오프**: 서버가 없어 코드 무작위 대입에 요청 빈도 제한을 걸 수 없다. 완화책은 ① 10.7억 조합 ② 30분 유효 ③ 1회용 ④ `list` 금지(단건 `get`만) ⑤ 성공해도 얻는 것은 발급자 표시 이름뿐이고 가계부·일정 데이터에는 접근할 수 없음. 요청 빈도 제한이 필요해지면 Cloud Functions callable로 이 컬렉션을 감싸면 되고, 나머지 데이터 모델은 그대로 쓴다.

### 성사 — 단일 WriteBatch

수락자 B가 코드를 확인한 뒤(만료·본인 코드 등은 클라이언트가 먼저 걸러 안내), **하나의 배치**를 커밋한다.

| # | 쓰기 | 규칙이 검증하는 것 |
|---|---|---|
| 1 | `couples/{A_B}` create | 문서 id == 정렬한 `memberIds`, 초대 코드 유효, 발급자가 상대와 일치, 양쪽 모두 `coupleId == null` |
| 2 | `users/{A}` update — `coupleId`만 | `coupleId`가 비어 있었고, 그 한 필드만 바뀌며, `getAfter`로 본 커플 문서에 두 사람이 모두 멤버 |
| 3 | `users/{B}` update — `coupleId`만 | 위와 동일 |
| 4 | `invites/{CODE}` delete | 로그인 상태 |

- **2·3번이 `getAfter()`를 쓰는 것이 이 설계의 핵심이다.** 커플 문서는 같은 배치에서 만들어져서 `get()`(커밋 전 상태)으로는 보이지 않는다. `getAfter()`가 배치 커밋 후 상태를 보기 때문에, 서버 코드 없이도 "커플 문서가 실제로 만들어졌을 때만 상대의 `coupleId`를 채울 수 있다"가 강제된다.
- 배치는 읽기를 할 수 없다. 만료 검사 같은 사전 검증은 클라이언트가 하지만 그건 UX용이고, **최종 방어선은 규칙이다.**
- 발급자 A는 자기 화면에서 커플 문서를 실시간 구독하고 있어 성사 즉시 완료 화면으로 넘어간다.
- 규칙의 문서 접근 횟수: 1번이 5회(초대 문서 존재/발급자/만료 + 양쪽 사용자), 2·3번이 각 2회 = 배치 전체 9회. 배치 한도(20회) 안이다.

## 보안 규칙

실물은 **[`firestore.rules`](../firestore.rules)** 다. 설계 의도만 여기 적고 규칙 본문은 옮겨 적지 않는다 — 두 벌을 두면 반드시 어긋난다.

| 경로 | 규칙 요지 |
|---|---|
| `invites/{code}` | `get`만 허용하고 `list`는 막는다(코드 전수 조회 차단). `create`는 본인이 발급자일 때만, `update`는 아예 막아 코드 충돌이 거절로 드러나게 한다 |
| `users/{userId}` | 본인만 read/write. **예외 하나** — 연결 성사 순간에 상대가 내 `coupleId` 한 필드를 채우는 것 |
| `couples/{coupleId}` | `memberIds`에 있는 uid만 접근. `create`는 초대 코드 검증을 통과할 때만, `delete`는 막는다(해제는 `deletedAt` 마킹) |
| `couples/{coupleId}/**` | 멤버만 read. write는 `deletedAt`이 없을 때만(유예기간 중 열람만) |

**`users`의 두 번째 `allow update`가 이 설계에서 새로 여는 유일한 구멍이다.** 세 겹으로 좁혀 둔다 — ① `coupleId`가 비어 있을 때만 ② 그 필드 하나만 ③ `getAfter()`로 본 커플 문서에 두 사람이 모두 멤버일 때만. 개인 데이터 하위 컬렉션은 `match /{document=**}`가 본인 전용으로 계속 잠근다.

**클라이언트가 미리 못 잡는 실패**: 상대의 `users` 문서는 읽을 수 없으므로 "상대가 이미 다른 사람과 연결됨"은 규칙 거절(`PERMISSION_DENIED`)로만 알 수 있다. 화면 문구 매핑은 wireframe/connect.md "상태 분기 종합" 참고.

**규칙 ↔ 실패 케이스 대응** (connect.md "상태 분기 종합"과 1:1)

| 실패 | 규칙 조건 |
|---|---|
| 없는 코드 | `exists(inviteDoc(code))` |
| 만료된 코드 | `expiresAt > request.time` |
| 본인이 발급한 코드 | `partnerOf(memberIds) != request.auth.uid` |
| 내가 이미 연결됨 | `unpaired(request.auth.uid)` |
| 상대가 이미 연결됨 | `unpaired(inviterUid)` |
| 남의 코드로 엉뚱한 사람과 묶기 | `get(inviteDoc(code)).data.inviterUid == partnerOf(memberIds)` |
| 문서 id 위조 | `coupleId == coupleIdOf(memberIds[0], memberIds[1])` |

QR 형식 오류와 네트워크 오류는 규칙 이전 단계라 클라이언트만 처리한다.

## 미확정 사항
- 카테고리별 예산 향후 도입 여부 (PRD 10 남은 결정 필요 사항)
- **상대 생일/기념일의 디데이 반영** — 프로필을 개인 경로에 두기로 해서 상대 생일을 읽을 수 없다. `couples.members`에 생일까지 미러링할지, 디데이 AUTO 항목은 각자 것만 볼지 결정 필요 (PRD 6과 직결)
- **연결 해제 후 유예기간 중 공동 데이터 열람 동선** — 경로가 개인으로 되돌아가면 공동 데이터가 어느 화면에도 노출되지 않는다. 해제 설계 시 함께 정한다
