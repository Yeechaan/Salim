package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.ConnectResult
import com.chanbro.salim.domain.model.Invite
import com.chanbro.salim.domain.model.InviteLookup
import com.chanbro.salim.domain.repository.ConnectionRepository
import javax.inject.Inject

/** 초대 코드 발급. 이미 유효한 코드가 있으면 그것을 그대로 쓴다 (wireframe/connect.md 9-2). */
class GetOrCreateInviteUseCase @Inject constructor(
    private val repository: ConnectionRepository,
) {
    suspend operator fun invoke(forceNew: Boolean = false): Invite =
        if (forceNew) repository.createInvite()
        else repository.currentInvite() ?: repository.createInvite()
}

class LookupInviteUseCase @Inject constructor(
    private val repository: ConnectionRepository,
) {
    suspend operator fun invoke(code: String): InviteLookup = repository.lookupInvite(code)
}

class ConnectWithInviteUseCase @Inject constructor(
    private val repository: ConnectionRepository,
) {
    suspend operator fun invoke(invite: Invite): ConnectResult = repository.connect(invite)
}
