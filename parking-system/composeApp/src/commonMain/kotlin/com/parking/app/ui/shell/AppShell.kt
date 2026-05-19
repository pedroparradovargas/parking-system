package com.parking.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parking.app.navigation.Screen
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.OfflineBanner
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing

/**
 * Shell estilo Figma "ParkSmart": Header + TabNav + OfflineBanner + content.
 *
 * REGLA DE LAYOUT INTERNO: el shell scrollea verticalmente con un único
 * `verticalScroll`.  Esto impide que las pantallas hijas usen `Lazy*` en su
 * árbol porque Compose lanza `IllegalStateException` cuando un componente
 * `Lazy*` ve `maxHeight = ∞` (caso que produce todo ancestor con verticalScroll).
 * Las pantallas deben usar `Column`/`Row`/`FlowRow` con `forEach` en lugar de
 * `LazyColumn`/`LazyVerticalGrid`.
 */
@Composable
fun AppShell(
    current: Screen,
    onNavigate: (Screen) -> Unit,
    content: @Composable () -> Unit,
) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val app = LocalAppState.current
    val pageScroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().background(semantic.pageBackground)) {
        HeaderBar()
        TabNavBar(current = current, onNavigate = onNavigate)
        OfflineBanner(visible = !app.isOnline)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(pageScroll)
                .padding(horizontal = spacing.l, vertical = spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 1280.dp)) {
                content()
            }
        }
    }
}
