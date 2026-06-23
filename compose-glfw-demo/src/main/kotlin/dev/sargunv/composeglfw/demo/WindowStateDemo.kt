package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isAltGraphPressed
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCapsLockOn
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isNumLockOn
import androidx.compose.ui.input.pointer.isScrollLockOn
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.HostWindowInfo
import kotlin.math.roundToInt

@Composable
internal fun WindowStateCard(
  windowInfo: HostWindowInfo,
  darkTheme: Boolean,
  modifier: Modifier = Modifier,
) {
  val composeWindowInfo = LocalWindowInfo.current
  val framebufferScaleX = windowInfo.framebufferWidth.toFloat() / windowInfo.windowWidth
  val framebufferScaleY = windowInfo.framebufferHeight.toFloat() / windowInfo.windowHeight

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text("Window state", style = MaterialTheme.typography.titleMedium)

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusValue("Display server", windowInfo.displayServer.toString())
        StatusValue("Renderer", windowInfo.renderBackend.toString())
        StatusValue("Display", windowInfo.displayName ?: "<unset>")
        StatusValue("Focus", if (composeWindowInfo.isWindowFocused) "Focused" else "Unfocused")
        StatusValue("Theme", if (darkTheme) "Dark" else "Light")
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricRow(
          "Window size",
          "${windowInfo.windowWidth} x ${windowInfo.windowHeight} logical px",
        )
        MetricRow(
          "Framebuffer size",
          "${windowInfo.framebufferWidth} x ${windowInfo.framebufferHeight} px",
        )
        MetricRow(
          "Compose container",
          "${composeWindowInfo.containerSize.width} x ${composeWindowInfo.containerSize.height} px",
        )
        MetricRow(
          "Compose dp size",
          "${composeWindowInfo.containerDpSize.width.value.roundToInt()} x " +
            "${composeWindowInfo.containerDpSize.height.value.roundToInt()} dp",
        )
        MetricRow("Content scale", "${(windowInfo.contentScale * 100).roundToInt()}%")
        MetricRow(
          "Framebuffer scale",
          "${framebufferScaleX.formatScale()} x ${framebufferScaleY.formatScale()}",
        )
      }

      WindowModifierStatus(composeWindowInfo.keyboardModifiers)
    }
  }
}

@Composable
private fun StatusValue(label: String, value: String) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
  ) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
      Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
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

@Composable
private fun WindowModifierStatus(modifiers: PointerKeyboardModifiers) {
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ModifierStatusValue("Ctrl", modifiers.isCtrlPressed)
    ModifierStatusValue("Shift", modifiers.isShiftPressed)
    ModifierStatusValue("Alt", modifiers.isAltPressed)
    ModifierStatusValue("Meta", modifiers.isMetaPressed)
    ModifierStatusValue("AltGraph", modifiers.isAltGraphPressed)
    ModifierStatusValue("Caps", modifiers.isCapsLockOn)
    ModifierStatusValue("Num", modifiers.isNumLockOn)
    ModifierStatusValue("Scroll", modifiers.isScrollLockOn)
  }
}

@Composable
private fun ModifierStatusValue(label: String, active: Boolean) {
  val colors = MaterialTheme.colorScheme
  Text(
    text = "$label ${if (active) "on" else "off"}",
    modifier =
      Modifier.widthIn(min = 72.dp)
        .background(if (active) colors.primaryContainer else colors.surfaceContainer)
        .padding(horizontal = 10.dp, vertical = 6.dp),
    style = MaterialTheme.typography.labelMedium,
    color = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
  )
}

private fun Float.formatScale(): String = "${(this * 100).roundToInt()}%"
