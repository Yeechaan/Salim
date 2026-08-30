package com.chanbro.salim.domain.model

import kotlin.random.Random

/**
 * 초대 코드 규격. (firestore-schema.md invites/{code})
 *
 * Crockford Base32 — 0-9와 A-Z에서 혼동 문자 `I L O U`를 뺀 32글자. 6자리면 약 10.7억 조합.
 * 발급(:data)과 입력 정규화(UI)가 같은 규격을 봐야 해서 도메인에 둔다.
 */
object InviteCode {
    const val LENGTH = 6
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun random(): String =
        (1..LENGTH).map { ALPHABET[Random.nextInt(ALPHABET.length)] }.joinToString("")

    /**
     * 입력값 정규화. 대문자로 올리고 Crockford 관례대로 헷갈리는 글자를 숫자로 접는다
     * (I·L → 1, O → 0). 손으로 옮겨 적다 나는 오타를 실패로 만들지 않기 위한 것.
     */
    fun normalize(raw: String): String = raw.trim().uppercase()
        .map {
            when (it) {
                'I', 'L' -> '1'
                'O' -> '0'
                else -> it
            }
        }
        .filter { it in ALPHABET }
        .joinToString("")

    fun isComplete(code: String): Boolean = code.length == LENGTH
}
