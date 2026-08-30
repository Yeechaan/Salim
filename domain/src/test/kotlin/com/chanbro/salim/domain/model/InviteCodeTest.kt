package com.chanbro.salim.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodeTest {

    @Test
    fun `발급된 코드는 항상 6자리이고 혼동 문자를 쓰지 않는다`() {
        val confusing = setOf('I', 'L', 'O', 'U')
        repeat(500) {
            val code = InviteCode.random()
            assertEquals(InviteCode.LENGTH, code.length)
            assertTrue(code.none { it in confusing })
        }
    }

    @Test
    fun `소문자와 공백은 정규화된다`() {
        assertEquals("7K2M9Q", InviteCode.normalize("  7k2m9q "))
    }

    /** 손으로 옮겨 적을 때 가장 흔한 오타를 실패로 만들지 않는다는 계약. */
    @Test
    fun `헷갈리는 글자는 숫자로 접힌다`() {
        assertEquals("110023", InviteCode.normalize("IlOo23"))
    }

    @Test
    fun `알파벳에 없는 문자는 버린다`() {
        assertEquals("7K2M9Q", InviteCode.normalize("7K2-M9Q!"))
    }

    @Test
    fun `6자리를 채워야 완성으로 본다`() {
        assertFalse(InviteCode.isComplete("7K2M9"))
        assertTrue(InviteCode.isComplete("7K2M9Q"))
    }
}
