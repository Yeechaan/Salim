package com.chanbro.salim.domain.model

/**
 * 지출자를 사람 이름으로 부르기 위한 표시 이름 묶음. (PRD 4. 가계부 - 지출 입력)
 *
 * 내 이름은 프로필에서, 상대 이름은 커플 문서의 members 맵에서 온다 — 상대의 users 문서는
 * 읽을 수 없기 때문이다. 둘 중 하나라도 비어 있으면 예전 표기("나"/"배우자")로 떨어진다.
 * "우리"는 사람 이름이 아니라 언제나 "우리"다.
 */
data class SpenderNames(
    val mine: String = Spender.ME.label,
    val partner: String = Spender.PARTNER.label,
) {
    fun labelOf(spender: Spender): String = when (spender) {
        Spender.SHARED -> Spender.SHARED.label
        Spender.ME -> mine
        Spender.PARTNER -> partner
    }
}
