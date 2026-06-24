package dev.sargunv.composeglfw.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.sargunv.composeglfw.Window
import dev.sargunv.composeglfw.WindowOptions
import dev.sargunv.composeglfw.glfwApplication
import dev.sargunv.composeglfw.rememberWindowState

internal fun main(): Unit {
  glfwApplication {
    val windowState = rememberWindowState(size = DpSize(960.dp, 640.dp))
    val toolsWindowState = rememberWindowState(size = DpSize(420.dp, Dp.Unspecified))
    val hudWindowState = rememberWindowState(size = DpSize(460.dp, 360.dp))
    val passiveWindowState = rememberWindowState(size = DpSize(420.dp, 260.dp))
    val textToolbarWindowState = rememberWindowState(size = DpSize(460.dp, 320.dp))
    var toolsOpen by remember { mutableStateOf(false) }
    var hudOpen by remember { mutableStateOf(false) }
    var hudTransparent by remember { mutableStateOf(true) }
    var hudUndecorated by remember { mutableStateOf(true) }
    var hudAlwaysOnTop by remember { mutableStateOf(true) }
    var hudResizable by remember { mutableStateOf(true) }
    var passiveOpen by remember { mutableStateOf(false) }
    var textToolbarOpen by remember { mutableStateOf(false) }
    val showcaseState =
      WindowShowcaseState(
        toolsOpen = toolsOpen,
        hudOpen = hudOpen,
        passiveOpen = passiveOpen,
        textToolbarOpen = textToolbarOpen,
      )
    val showcaseActions =
      WindowShowcaseActions(
        setToolsOpen = { toolsOpen = it },
        setHudOpen = { hudOpen = it },
        setPassiveOpen = { passiveOpen = it },
        setTextToolbarOpen = { textToolbarOpen = it },
      )

    Window(
      onCloseRequest = ::exitApplication,
      title = "Compose GLFW demo",
      state = windowState,
    ) {
      ComposeGlfwApp(windowInfo, windowState, showcaseState, showcaseActions)
    }

    if (toolsOpen) {
      Window(
        onCloseRequest = { toolsOpen = false },
        title = "Always-on-top tools",
        state = toolsWindowState,
        alwaysOnTop = true,
        focusOnShow = false,
      ) {
        ToolsWindowContent(windowInfo, toolsWindowState)
      }
    }

    if (hudOpen) {
      Window(
        onCloseRequest = { hudOpen = false },
        title = "Transparent HUD",
        state = hudWindowState,
        undecorated = hudUndecorated,
        transparent = hudTransparent,
        resizable = hudResizable,
        alwaysOnTop = hudAlwaysOnTop,
        focusOnShow = false,
      ) {
        TransparentHudWindowContent(
          windowInfo = windowInfo,
          windowState = hudWindowState,
          state =
            HudWindowControlState(
              transparent = hudTransparent,
              undecorated = hudUndecorated,
              alwaysOnTop = hudAlwaysOnTop,
              resizable = hudResizable,
            ),
          actions =
            HudWindowControlActions(
              setTransparent = {
                hudTransparent = it
              },
              setUndecorated = { hudUndecorated = it },
              setAlwaysOnTop = { hudAlwaysOnTop = it },
              setResizable = { hudResizable = it },
            ),
        )
      }
    }

    if (passiveOpen) {
      Window(
        onCloseRequest = { passiveOpen = false },
        title = "Passive disabled-input window",
        state = passiveWindowState,
        enabled = false,
        focusOnShow = false,
      ) {
        PassiveWindowContent(windowInfo)
      }
    }

    if (textToolbarOpen) {
      Window(
        onCloseRequest = { textToolbarOpen = false },
        title = "Custom text toolbar",
        state = textToolbarWindowState,
        options =
          WindowOptions {
            textToolbar = { state, actions ->
              DemoTheme {
                ShowcaseTextToolbar(state, actions)
              }
            }
          },
      ) {
        TextToolbarWindowContent(windowInfo)
      }
    }
  }
}
