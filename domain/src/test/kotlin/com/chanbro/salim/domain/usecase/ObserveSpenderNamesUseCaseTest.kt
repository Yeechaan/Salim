package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.ConnectResult
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.Invite
import com.chanbro.salim.domain.model.InviteLookup
import com.chanbro.salim.domain.model.PartnerProfile
import com.chanbro.salim.domain.model.SpenderNames
import com.chanbro.salim.domain.model.UserProfile
import com.chanbro.salim.domain.repository.ConnectionRepository
import com.chanbro.salim.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveSpenderNamesUseCaseTest {

    @Test
    fun `이름을 정해 두면 두 사람 모두 이름으로 불린다`() {
        val names = observe(profile = profile("해리"), connection = connected("지수"))

        assertEquals(SpenderNames(mine = "해리", partner = "지수"), names)
    }

    @Test
    fun `이름이 비어 있으면 예전 표기로 떨어진다`() {
        val names = observe(profile = profile(null), connection = connected("  "))

        assertEquals(SpenderNames(mine = "나", partner = "배우자"), names)
    }

    @Test
    fun `미연결이면 상대 이름 자리는 기본값이다`() {
        val names = observe(profile = profile("해리"), connection = Connection.None)

        assertEquals(SpenderNames(mine = "해리", partner = "배우자"), names)
    }

    /** 이름 조회가 실패해도 화면이 멈추면 안 된다 — 기본 표기로 계속 그린다. */
    @Test
    fun `프로필 조회가 실패하면 기본 표기를 내보낸다`() {
        val useCase = ObserveSpenderNamesUseCase(
            profileRepository = FakeProfileRepository(flow { throw IllegalStateException("권한 없음") }),
            connectionRepository = FakeConnectionRepository(Connection.None),
        )

        assertEquals(SpenderNames(), runBlocking { useCase().first() })
    }

    private fun observe(profile: UserProfile, connection: Connection): SpenderNames {
        val useCase = ObserveSpenderNamesUseCase(
            profileRepository = FakeProfileRepository(flowOf(profile)),
            connectionRepository = FakeConnectionRepository(connection),
        )
        return runBlocking { useCase().first() }
    }

    private fun profile(displayName: String?) =
        UserProfile(displayName = displayName, birthdayMillis = null, anniversaryMillis = null)

    private fun connected(partnerName: String?) = Connection.Connected(
        coupleId = "a_b",
        partner = PartnerProfile(uid = "b", displayName = partnerName, photoUrl = null),
        connectedAtMillis = 0L,
    )
}

private class FakeProfileRepository(private val profiles: Flow<UserProfile>) : ProfileRepository {
    override fun observe(): Flow<UserProfile> = profiles
    override suspend fun save(profile: UserProfile) = Unit
}

private class FakeConnectionRepository(private val connection: Connection) : ConnectionRepository {
    override fun observeConnection(): Flow<Connection> = flowOf(connection)
    override suspend fun currentInvite(): Invite? = notUsed()
    override suspend fun createInvite(): Invite = notUsed()
    override suspend fun lookupInvite(code: String): InviteLookup = notUsed()
    override suspend fun connect(invite: Invite): ConnectResult = notUsed()

    private fun notUsed(): Nothing = error("이 테스트에서는 쓰지 않는다")
}
