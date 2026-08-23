package com.chanbro.salim.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 온보딩 노출 여부. (PRD 1. "앱 미설치/미로그인 상태에서 첫 진입 시 노출")
 * 공유 데이터가 아니라 기기 로컬 전용이므로 Firestore가 아닌 로컬 저장소를 쓴다.
 */
interface OnboardingRepository {
    /** 온보딩을 이미 본 적이 있는지. */
    val completed: Flow<Boolean>

    /** 마지막 슬라이드의 "시작하기"를 누른 시점에 기록. */
    suspend fun markCompleted()
}
