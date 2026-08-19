# Firestore 스키마

컬렉션/필드 구조. 기능 추가·변경 시 이 문서를 함께 갱신한다. (초안 — 실제 개발 시작 시 확정)

## couples/{coupleId}
연결된 커플 단위 문서. 하위 컬렉션은 이 문서 기준으로 공유된다.

| 필드 | 설명 |
|---|---|
| createdAt | 연결 성사 시각 |
| memberIds | 배열, 두 users 문서 참조 |
| deletedAt | null이면 정상. 값이 있으면 유예기간 중 (PRD 9번 연결 해제/탈퇴 정책) — Firestore 보안 규칙에서 이 필드가 있으면 신규 쓰기 차단 |

### couples/{coupleId}/expenses/{expenseId}
지출 내역. (PRD 4. 가계부)

| 필드 | 설명 |
|---|---|
| amount | 금액 |
| date | 날짜 |
| time | 시간 |
| spenderId | 나/배우자 (users 참조) |
| category | 카테고리 |
| memo | 메모 |
| createdAt | 등록 시각 |

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

### couples/{coupleId}/budget/{yearMonth}
월별 예산. (PRD 7. 설정 - 달별 예산)

| 필드 | 설명 |
|---|---|
| totalBudget | 월 전체 예산 |

## users/{userId}
개인 프로필.

| 필드 | 설명 |
|---|---|
| coupleId | 연결된 couples 문서 참조, 미연결 시 null |
| birthday / anniversary | 프로필 정보, ddays에 자동 반영되는 source |
| fcmToken | 푸시 발송용 |
| notificationSettings | 알림 종류별 on/off (PRD 8. 알림 표 기준) |

## 미확정 사항
- 카테고리 목록을 커플 문서 하위 별도 컬렉션으로 둘지, 고정 enum으로 둘지
- 비활성화된 카테고리(PRD 7번, 삭제 대신 비활성화 정책) 필드 설계
