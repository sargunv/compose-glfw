package dev.sargunv.composeglfw.demo

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.GlfwWindowInfo

@Composable
internal fun ComposeGlfwApp(windowInfo: GlfwWindowInfo) {
  val darkTheme = isSystemInDarkTheme()
  MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
    Surface(Modifier.fillMaxSize()) {
      val scrollState = rememberScrollState()
      Box(Modifier.fillMaxSize()) {
        Column(
          Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          DemoHeader(windowInfo)
          WindowStateCard(windowInfo, darkTheme, Modifier.fillMaxWidth())
          PointerInputCard(Modifier.fillMaxWidth())
          PointerIconCard(Modifier.fillMaxWidth())
          InputEventsCard(Modifier.fillMaxWidth())
          MaterialControlsCard(Modifier.fillMaxWidth())
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
private fun DemoHeader(windowInfo: GlfwWindowInfo) {
  Column(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("Compose GLFW", style = MaterialTheme.typography.headlineMedium)
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(windowInfo.platform.toString(), style = MaterialTheme.typography.labelLarge)
        Text(
          windowInfo.renderBackend.toString(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
