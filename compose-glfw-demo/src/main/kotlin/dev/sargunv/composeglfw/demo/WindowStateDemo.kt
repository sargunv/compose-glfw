package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.HostWindowInfo
import dev.sargunv.composeglfw.WindowPlacement
import dev.sargunv.composeglfw.WindowPosition
import dev.sargunv.composeglfw.WindowState
import kotlin.math.roundToInt

@Composable
internal fun WindowStateCard(
  windowInfo: HostWindowInfo,
  windowState: WindowState,
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
        MetricRow("WindowState placement", windowState.placement.toString())
        MetricRow("WindowState minimized", windowState.isMinimized.toString())
        MetricRow("WindowState position", windowState.position.toDisplayText())
      }

      WindowStateControls(windowState)
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
private fun WindowStateControls(windowState: WindowState) {
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    OutlinedButton(
      onClick = {
        windowState.isMinimized = false
        windowState.placement = WindowPlacement.Floating
        windowState.size = DpSize(960.dp, 640.dp)
      }
    ) {
      Text("960 x 640")
    }
    OutlinedButton(onClick = { windowState.size = DpSize(1280.dp, 720.dp) }) {
      Text("1280 x 720")
    }
    OutlinedButton(onClick = { windowState.position = WindowPosition(Alignment.Center) }) {
      Text("Center")
    }
    Button(
      onClick = {
        windowState.isMinimized = false
        windowState.placement =
          if (windowState.placement == WindowPlacement.Maximized) {
            WindowPlacement.Floating
          } else {
            WindowPlacement.Maximized
          }
      }
    ) {
      Text(if (windowState.placement == WindowPlacement.Maximized) "Restore" else "Maximize")
    }
    Button(
      onClick = {
        windowState.isMinimized = false
        windowState.placement =
          if (windowState.placement == WindowPlacement.Fullscreen) {
            WindowPlacement.Floating
          } else {
            WindowPlacement.Fullscreen
          }
      }
    ) {
      Text(
        if (windowState.placement == WindowPlacement.Fullscreen) "Exit fullscreen" else "Fullscreen"
      )
    }
    OutlinedButton(onClick = { windowState.isMinimized = true }) {
      Text("Minimize")
    }
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

private fun WindowPosition.toDisplayText(): String =
  when (this) {
    is WindowPosition.Absolute -> "${x.value.roundToInt()}, ${y.value.roundToInt()}"
    is WindowPosition.Aligned -> "Aligned($alignment)"
    WindowPosition.PlatformDefault -> "PlatformDefault"
  }
