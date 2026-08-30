package com.chanbro.salim.domain.usecase

import com.chanbro.salim.domain.model.Connection
import com.chanbro.salim.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository,
) {
    operator fun invoke(): Flow<Connection> = repository.observeConnection()
}
