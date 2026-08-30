package com.chanbro.salim.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 커플 문서 id 규칙은 클라이언트와 보안 규칙 양쪽에 같은 내용이 적혀 있다
 * (`firestore.rules`의 `coupleIdOf`). 둘이 어긋나면 연결이 통째로 거절되므로,
 * 여기서 계약을 고정해 둔다.
 */
class CoupleIdTest {

    @Test
    fun `순서를 바꿔도 같은 id가 나온다`() {
        assertEquals(coupleIdOf("bbb", "aaa"), coupleIdOf("aaa", "bbb"))
    }

    @Test
    fun `사전순으로 정렬해 밑줄로 잇는다`() {
        assertEquals("aaa_bbb", coupleIdOf("bbb", "aaa"))
    }

    /** 규칙은 memberIds를 정렬된 상태로 받아 `memberIds[0] + "_" + memberIds[1]`을 기대한다. */
    @Test
    fun `정렬된 memberIds를 이은 값과 같다`() {
        val members = listOf("zY9", "aB1").sorted()
        assertEquals("${members[0]}_${members[1]}", coupleIdOf("zY9", "aB1"))
    }
}
