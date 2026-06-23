package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.HostWindowInfo
import dev.sargunv.composeglfw.TextToolbarActions
import dev.sargunv.composeglfw.TextToolbarState
import dev.sargunv.composeglfw.WindowPlacement
import dev.sargunv.composeglfw.WindowPosition
import dev.sargunv.composeglfw.WindowState
import kotlin.math.roundToInt

internal data class WindowShowcaseState(
  val toolsOpen: Boolean,
  val hudOpen: Boolean,
  val passiveOpen: Boolean,
  val textToolbarOpen: Boolean,
)

internal data class WindowShowcaseActions(
  val setToolsOpen: (Boolean) -> Unit,
  val setHudOpen: (Boolean) -> Unit,
  val setPassiveOpen: (Boolean) -> Unit,
  val setTextToolbarOpen: (Boolean) -> Unit,
)

internal data class HudWindowControlState(
  val transparent: Boolean,
  val undecorated: Boolean,
  val alwaysOnTop: Boolean,
  val resizable: Boolean,
)

internal data class HudWindowControlActions(
  val setTransparent: (Boolean) -> Unit,
  val setUndecorated: (Boolean) -> Unit,
  val setAlwaysOnTop: (Boolean) -> Unit,
  val setResizable: (Boolean) -> Unit,
)

@Composable
internal fun WindowShowcaseCard(
  state: WindowShowcaseState,
  actions: WindowShowcaseActions,
  modifier: Modifier = Modifier,
) {
  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text("Window showcase", style = MaterialTheme.typography.titleMedium)

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ShowcaseToggle(
          label = "Always-on-top tools",
          open = state.toolsOpen,
          onOpenChange = actions.setToolsOpen,
        )
        ShowcaseToggle(
          label = "Transparent HUD",
          open = state.hudOpen,
          onOpenChange = actions.setHudOpen,
        )
        ShowcaseToggle(
          label = "Disabled input",
          open = state.passiveOpen,
          onOpenChange = actions.setPassiveOpen,
        )
        ShowcaseToggle(
          label = "Custom text toolbar",
          open = state.textToolbarOpen,
          onOpenChange = actions.setTextToolbarOpen,
        )
      }
    }
  }
}

@Composable
private fun ShowcaseToggle(
  label: String,
  open: Boolean,
  onOpenChange: (Boolean) -> Unit,
) {
  if (open) {
    OutlinedButton(onClick = { onOpenChange(false) }) {
      Text("Close $label")
    }
  } else {
    Button(onClick = { onOpenChange(true) }) {
      Text("Open $label")
    }
  }
}

@Composable
internal fun ToolsWindowContent(
  windowInfo: HostWindowInfo,
  windowState: WindowState,
) {
  DemoTheme {
    Surface(Modifier.fillMaxSize()) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Always-on-top tools", style = MaterialTheme.typography.titleMedium)
        MetricRow("Display server", windowInfo.displayServer.toString())
        MetricRow("Size", "${windowInfo.windowWidth} x ${windowInfo.windowHeight}")
        MetricRow("Placement", windowState.placement.toString())

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(onClick = { windowState.position = WindowPosition(Alignment.CenterEnd) }) {
            Text("Dock right")
          }
          OutlinedButton(onClick = { windowState.size = DpSize(420.dp, 360.dp) }) {
            Text("Reset size")
          }
          Button(
            onClick = {
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
        }
      }
    }
  }
}

@Composable
internal fun TransparentHudWindowContent(
  windowInfo: HostWindowInfo,
  windowState: WindowState,
  state: HudWindowControlState,
  actions: HudWindowControlActions,
) {
  DemoTheme {
    val background =
      if (state.transparent) {
        Color(0xaa111827)
      } else {
        MaterialTheme.colorScheme.surface
      }
    val contentColor =
      if (state.transparent) {
        Color.White
      } else {
        MaterialTheme.colorScheme.onSurface
      }
    Box(
      Modifier.fillMaxSize().background(background).padding(20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          "Transparent HUD",
          style = MaterialTheme.typography.titleMedium,
          color = contentColor,
        )
        Text(
          "${windowInfo.displayServer} / ${windowInfo.renderBackend}",
          style = MaterialTheme.typography.bodyMedium,
          color = contentColor,
        )
        Text(
          "${windowInfo.windowWidth} x ${windowInfo.windowHeight}",
          style = MaterialTheme.typography.bodyMedium,
          color = contentColor,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          HudSwitchRow(
            label = "Alpha framebuffer",
            checked = state.transparent,
            onCheckedChange = actions.setTransparent,
            contentColor = contentColor,
          )
          HudSwitchRow(
            label = "Native frame",
            checked = !state.undecorated,
            onCheckedChange = { actions.setUndecorated(!it) },
            contentColor = contentColor,
          )
          HudSwitchRow(
            label = "Always on top",
            checked = state.alwaysOnTop,
            onCheckedChange = actions.setAlwaysOnTop,
            contentColor = contentColor,
          )
          HudSwitchRow(
            label = "Resizable",
            checked = state.resizable,
            onCheckedChange = actions.setResizable,
            contentColor = contentColor,
          )
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(onClick = { windowState.size = DpSize(460.dp, 360.dp) }) {
            Text("Reset size")
          }
          OutlinedButton(onClick = { windowState.position = WindowPosition(Alignment.TopCenter) }) {
            Text("Top center")
          }
        }
      }
    }
  }
}

@Composable
private fun HudSwitchRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  contentColor: Color,
) {
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      label,
      style = MaterialTheme.typography.bodyMedium,
      color = contentColor,
    )
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
    )
  }
}

@Composable
internal fun PassiveWindowContent(windowInfo: HostWindowInfo) {
  DemoTheme {
    Surface(Modifier.fillMaxSize()) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Disabled-input window", style = MaterialTheme.typography.titleMedium)
        MetricRow("Display server", windowInfo.displayServer.toString())
        MetricRow("Input callbacks", "disabled")
        var text by remember {
          mutableStateOf("This field renders but does not accept host input.")
        }
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Text field") },
        )
      }
    }
  }
}

@Composable
internal fun TextToolbarWindowContent(windowInfo: HostWindowInfo) {
  DemoTheme {
    Surface(Modifier.fillMaxSize()) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Custom text toolbar", style = MaterialTheme.typography.titleMedium)
        MetricRow("Display server", windowInfo.displayServer.toString())
        var text by remember {
          mutableStateOf("Select part of this text to show the custom host text toolbar.")
        }
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Editable text") },
          minLines = 4,
        )
      }
    }
  }
}

@Composable
internal fun ShowcaseTextToolbar(
  state: TextToolbarState,
  actions: TextToolbarActions,
) {
  Box(Modifier.fillMaxSize()) {
    Surface(
      modifier =
        Modifier.offset {
            IntOffset(
              x = state.rect.left.roundToInt(),
              y = state.rect.bottom.roundToInt(),
            )
          }
          .padding(8.dp),
      shape = MaterialTheme.shapes.small,
      tonalElevation = 6.dp,
      shadowElevation = 8.dp,
    ) {
      Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.canCopy) {
          OutlinedButton(onClick = actions::copy) {
            Text("Copy")
          }
        }
        if (state.canCut) {
          OutlinedButton(onClick = actions::cut) {
            Text("Cut")
          }
        }
        if (state.canPaste) {
          OutlinedButton(onClick = actions::paste) {
            Text("Paste")
          }
        }
        if (state.canSelectAll) {
          OutlinedButton(onClick = actions::selectAll) {
            Text("All")
          }
        }
        OutlinedButton(onClick = actions::dismiss) {
          Text("Dismiss")
        }
      }
    }
  }
}
