package com.chanbro.salim.ui.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.GetCredentialRequest
import com.chanbro.salim.R

/** 구글 로그인 시도 결과. ID 토큰 획득까지만 책임지고, Firebase 인증은 ViewModel이 한다. */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult

    /** 사용자가 시트를 닫음 — 에러가 아니므로 아무 안내도 하지 않는다. */
    data object Cancelled : GoogleSignInResult

    /** 기기에 구글 계정이 없음 — 계정 추가를 안내한다. */
    data object NoAccount : GoogleSignInResult

    data class Failure(val cause: Throwable) : GoogleSignInResult
}

/**
 * Credential Manager로 구글 ID 토큰을 받아온다. (레거시 GoogleSignIn API는 deprecated)
 *
 * Activity Context가 필요해 ViewModel이 아닌 화면 쪽에서 호출한다.
 * serverClientId는 google-services.json의 웹 클라이언트(client_type 3)에서 생성되는
 * `default_web_client_id` 리소스다 — Firebase 콘솔에서 Google 로그인을 켜야 생성된다.
 */
suspend fun requestGoogleIdToken(activity: Activity): GoogleSignInResult = try {
    val option = GetSignInWithGoogleOption.Builder(
        activity.getString(R.string.default_web_client_id),
    ).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

    val credential = CredentialManager.create(activity as Context)
        .getCredential(activity, request)
        .credential

    val idToken = (credential as? CustomCredential)
        ?.takeIf { it.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL }
        ?.let { GoogleIdTokenCredential.createFrom(it.data).idToken }

    if (idToken != null) {
        GoogleSignInResult.Success(idToken)
    } else {
        GoogleSignInResult.Failure(IllegalStateException("예상하지 못한 크리덴셜 타입: ${credential.type}"))
    }
} catch (e: GetCredentialCancellationException) {
    GoogleSignInResult.Cancelled
} catch (e: NoCredentialException) {
    GoogleSignInResult.NoAccount
} catch (e: Exception) {
    GoogleSignInResult.Failure(e)
}
