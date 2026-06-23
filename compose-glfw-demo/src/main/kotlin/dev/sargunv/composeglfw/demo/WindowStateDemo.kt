package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.GlfwWindowInfo
import kotlin.math.roundToInt

@Composable
internal fun WindowStateCard(windowInfo: GlfwWindowInfo, modifier: Modifier = Modifier) {
  val framebufferScaleX = windowInfo.framebufferWidth.toFloat() / windowInfo.windowWidth
  val framebufferScaleY = windowInfo.framebufferHeight.toFloat() / windowInfo.windowHeight

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text("Window state", style = MaterialTheme.typography.titleMedium)

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusValue("Platform", windowInfo.platform.displayLabel)
        StatusValue("Renderer", windowInfo.renderBackend.displayLabel)
        StatusValue("Display", windowInfo.displayName ?: "<unset>")
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricRow("Window size", "${windowInfo.windowWidth} x ${windowInfo.windowHeight} px")
        MetricRow("Framebuffer size", "${windowInfo.framebufferWidth} x ${windowInfo.framebufferHeight} px")
        MetricRow("Content scale", "${(windowInfo.contentScale * 100).roundToInt()}%")
        MetricRow("Framebuffer scale", "${framebufferScaleX.formatScale()} x ${framebufferScaleY.formatScale()}")
      }
    }
  }
}

@Composable
private fun StatusValue(label: String, value: String) {
  Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
internal fun MetricRow(label: String, value: String, modifier: Modifier = Modifier) {
  Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(
      label,
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
  }
}

private fun Float.formatScale(): String = "${(this * 100).roundToInt()}%"
