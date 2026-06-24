package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.HostWindowInfo
import dev.sargunv.composeglfw.WindowState

@Composable
internal fun ComposeGlfwApp(
  windowInfo: HostWindowInfo,
  windowState: WindowState,
  showcaseState: WindowShowcaseState,
  showcaseActions: WindowShowcaseActions,
) {
  DemoTheme {
    Surface(Modifier.fillMaxSize()) {
      val scrollState = rememberScrollState()
      Box(Modifier.fillMaxSize()) {
        Column(
          Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          DemoHeader(windowInfo)
          WindowStateCard(
            windowInfo,
            windowState,
            darkTheme = isSystemInDarkTheme(),
            Modifier.fillMaxWidth(),
          )
          WindowShowcaseCard(showcaseState, showcaseActions, Modifier.fillMaxWidth())
          LifecycleCard(Modifier.fillMaxWidth())
          LayoutDirectionCard(Modifier.fillMaxWidth())
          PointerInputCard(Modifier.fillMaxWidth())
          PointerIconCard(Modifier.fillMaxWidth())
          PopupMenuCard(Modifier.fillMaxWidth())
          FileDropCard(Modifier.fillMaxWidth())
          InputEventsCard(Modifier.fillMaxWidth())
        }

        VerticalScrollbar(
          adapter = rememberScrollbarAdapter(scrollState),
          modifier = Modifier.align(Alignment.CenterEnd),
        )
      }
    }
  }
}

@Composable
internal fun DemoTheme(content: @Composable () -> Unit) {
  val darkTheme = isSystemInDarkTheme()
  MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
    val contextMenuRepresentation =
      if (darkTheme) {
        DarkDefaultContextMenuRepresentation
      } else {
        LightDefaultContextMenuRepresentation
      }
    CompositionLocalProvider(LocalContextMenuRepresentation provides contextMenuRepresentation) {
      content()
    }
  }
}

@Composable
private fun DemoHeader(windowInfo: HostWindowInfo) {
  Column(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("Compose GLFW", style = MaterialTheme.typography.headlineMedium)
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(windowInfo.displayServer.toString(), style = MaterialTheme.typography.labelLarge)
        Text(
          windowInfo.renderBackend.toString(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
