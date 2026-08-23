package com.chanbro.salim.domain.repository

import com.chanbro.salim.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** 사용자 프로필 저장소. (users/{uid} 문서) */
interface ProfileRepository {
    /** 프로필을 관찰. 문서가 없으면 값이 모두 null인 프로필. */
    fun observe(): Flow<UserProfile>

    /** 프로필 저장(생일/기념일). */
    suspend fun save(profile: UserProfile)
}
