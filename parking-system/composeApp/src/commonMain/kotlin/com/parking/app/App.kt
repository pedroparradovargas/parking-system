package com.parking.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.parking.app.navigation.AppNav
import com.parking.app.navigation.Screen
import com.parking.app.state.AppState
import com.parking.app.state.AuthTokenHolder
import com.parking.app.state.LocalAppState
import com.parking.app.state.MockData
import com.parking.app.state.seedDatabaseIfEmpty
import com.parking.app.ui.shell.AppShell
import com.parking.app.ui.theme.ParkingTheme
import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.LoginRequest
import com.parking.shared.data.local.LocalRepository
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject

/**
 * Composable raíz consumido por TODAS las plataformas.
 *
 *  - El mockup Figma no tiene pantalla de login: el rol se cambia desde el header.
 *  - La pantalla inicial es [Screen.MainMenu].
 *  - Todo el contenido vive dentro del [AppShell] (header + tab nav).
 *  - [AppState] se construye con el [LocalRepository] inyectado vía Koin
 *    (Regla 9: offline-first sobre SQLDelight, no in-memory).
 */
@Composable
fun App() {
    val repo: LocalRepository = koinInject()
    val api: ParkingApiClient = koinInject()
    val tokens: AuthTokenHolder = koinInject()
    val scope = rememberCoroutineScope()
    val appState = remember {
        AppState(
            repo = repo,
            scope = scope,
            parkingId = MockData.PARKING_ID,
            parkingName = MockData.PARKING_NAME,
        )
    }

    // Siembra DB en el primer arranque (zonas y sesiones vacías → carga demo).
    LaunchedEffect(repo, appState.parkingId) {
        seedDatabaseIfEmpty(repo, appState.parkingId)
    }

    // Auto-login dev: pobla AuthTokenHolder al arrancar para que las rutas
    // admin no devuelvan 401.  En producción esto será una pantalla de Login.
    LaunchedEffect(tokens) {
        if (tokens.accessToken != null) return@LaunchedEffect
        runCatching {
            api.login(LoginRequest(username = "admin", password = "admin123"))
        }.onSuccess { resp ->
            tokens.set(resp.accessToken)
            Napier.i("Auto-login OK como ${resp.user.username}")
        }.onFailure { Napier.w("Auto-login fallo: ${it.message}") }
    }

    var current by remember { mutableStateOf<Screen>(Screen.MainMenu) }

    CompositionLocalProvider(LocalAppState provides appState) {
        ParkingTheme(useDarkTheme = appState.darkTheme) {
            AppShell(current = current, onNavigate = { current = it }) {
                AppNav(current = current, navigateTo = { current = it })
            }
        }
    }
}
