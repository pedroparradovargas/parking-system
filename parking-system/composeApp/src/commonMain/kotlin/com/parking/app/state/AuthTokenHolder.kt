package com.parking.app.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holder mutable in-memory del access token JWT.  El `applyCommonConfig` del
 * HttpClient consulta `accessToken` en cada request vía `tokenProvider`.
 *
 * No persiste a disco — se re-obtiene haciendo login en cada arranque.
 * Para v2 (persistencia segura) usar `androidx.security.crypto.EncryptedSharedPreferences`
 * en Android, Keychain en iOS, etc.
 */
class AuthTokenHolder {
    private val _state = MutableStateFlow<String?>(null)
    val state: StateFlow<String?> = _state.asStateFlow()

    val accessToken: String? get() = _state.value

    fun set(token: String?) { _state.value = token }
    fun clear() { _state.value = null }
}
