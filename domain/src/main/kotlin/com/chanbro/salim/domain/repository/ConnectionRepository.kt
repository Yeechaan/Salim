package com.chanbro.salim.domain.repository

import com.chanbro.salim.domain.model.ConnectResult
import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.model.Invite
import com.chanbro.salim.domain.model.InviteLookup
import kotlinx.coroutines.flow.Flow

/** 상대방 연결. (PRD 9) 해제는 1차 범위 밖이라 아직 없다. */
interface ConnectionRepository {
    fun observeConnection(): Flow<Connection>

    /** 아직 유효한 내 초대 코드. 없거나 만료됐으면 null. */
    suspend fun currentInvite(): Invite?

    /** 새 코드를 만들고 이전 코드는 회수한다. */
    suspend fun createInvite(): Invite

    suspend fun lookupInvite(code: String): InviteLookup

    suspend fun connect(invite: Invite): ConnectResult
}
