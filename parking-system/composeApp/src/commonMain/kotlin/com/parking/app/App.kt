package com.parking.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.parking.app.navigation.AppNav
import com.parking.app.navigation.Screen
import com.parking.app.state.AppState
import com.parking.app.state.LocalAppState
import com.parking.app.ui.shell.AppShell
import com.parking.app.ui.theme.ParkingTheme

/**
 * Composable raíz consumido por TODAS las plataformas.
 *
 *  - El mockup Figma no tiene pantalla de login: el rol se cambia desde el header.
 *  - La pantalla inicial es [Screen.MainMenu].
 *  - Todo el contenido vive dentro del [AppShell] (header + tab nav).
 */
@Composable
fun App() {
    val appState = remember { AppState() }
    var current by remember { mutableStateOf<Screen>(Screen.MainMenu) }

    CompositionLocalProvider(LocalAppState provides appState) {
        ParkingTheme(useDarkTheme = appState.darkTheme) {
            AppShell(current = current, onNavigate = { current = it }) {
                AppNav(current = current, navigateTo = { current = it })
            }
        }
    }
}
