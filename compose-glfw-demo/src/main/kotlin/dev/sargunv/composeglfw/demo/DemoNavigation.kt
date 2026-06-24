package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.sargunv.composeglfw.HostWindowInfo
import kotlinx.serialization.Serializable

@Serializable internal object DemoHome

@Serializable internal object WindowStateDemo

@Serializable internal object WindowShowcaseDemo

@Serializable internal object LifecycleDemo

@Serializable internal object FrameRateDemo

@Serializable internal object LayoutDirectionDemo

@Serializable internal object PointerInputDemo

@Serializable internal object PointerIconDemo

@Serializable internal object PopupMenuDemo

@Serializable internal object DialogDemo

@Serializable internal object FileDropDemo

@Serializable internal object FilePickerDemo

@Serializable internal object InputEventsDemo

internal data class DemoDestination(
  val route: Any,
  val title: String,
  val description: String,
)

internal val demoDestinations =
  listOf(
    DemoDestination(
      WindowStateDemo,
      "Window state",
      "Framebuffer scale, placement, and focus metadata.",
    ),
    DemoDestination(
      WindowShowcaseDemo,
      "Window showcase",
      "Open auxiliary GLFW windows with different chrome and behavior.",
    ),
    DemoDestination(LifecycleDemo, "Lifecycle", "Lifecycle owner state and ViewModel survival."),
    DemoDestination(
      FrameRateDemo,
      "Frame rate voting",
      "Request preferred frame rates and observe draw cadence.",
    ),
    DemoDestination(
      LayoutDirectionDemo,
      "Layout direction",
      "Switch between LTR and RTL layout direction.",
    ),
    DemoDestination(PointerInputDemo, "Mouse input", "Track pointer position and press state."),
    DemoDestination(
      PointerIconDemo,
      "Pointer icons",
      "Swap the GLFW cursor for common pointer icons.",
    ),
    DemoDestination(PopupMenuDemo, "Popup menus", "Context menus and dropdown popup menus."),
    DemoDestination(
      DialogDemo,
      "Dialogs and modals",
      "Alert, form, and modal dialog presentations.",
    ),
    DemoDestination(FileDropDemo, "File drop", "Accept files dragged into the window."),
    DemoDestination(FilePickerDemo, "File picker", "Open native file picker dialogs."),
    DemoDestination(
      InputEventsDemo,
      "Input events",
      "Keyboard, scroll, and text input event stream.",
    ),
  )

@Composable
internal fun DemoHomeScreen(
  windowInfo: HostWindowInfo,
  onDestinationSelected: (Any) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    DemoHeader(windowInfo)
    Text("Demos", style = MaterialTheme.typography.titleLarge)
    Text(
      "Pick a demo to open it in its own screen. Use the back button or Esc to return here.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LazyColumn(
      modifier = Modifier.fillMaxWidth().weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      items(demoDestinations, key = { it.title }) { destination ->
        ListItem(
          headlineContent = { Text(destination.title) },
          supportingContent = { Text(destination.description) },
          modifier = Modifier.fillMaxWidth().clickable { onDestinationSelected(destination.route) },
        )
        HorizontalDivider()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DemoDetailScaffold(
  title: String,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(title) },
        navigationIcon = {
          TextButton(onClick = onNavigateBack) { Text("Back") }
        },
      )
    },
  ) { innerPadding ->
    Column(
      Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      content()
    }
  }
}

@Composable
internal fun DemoHeader(windowInfo: HostWindowInfo) {
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

internal fun demoTitle(route: Any): String =
  demoDestinations.firstOrNull { it.route == route }?.title ?: "Demo"

internal fun NavHostController.navigateToDemo(route: Any) {
  when (route) {
    DemoHome -> navigate(DemoHome) { launchSingleTop = true }
    WindowStateDemo -> navigate(WindowStateDemo) { launchSingleTop = true }
    WindowShowcaseDemo -> navigate(WindowShowcaseDemo) { launchSingleTop = true }
    LifecycleDemo -> navigate(LifecycleDemo) { launchSingleTop = true }
    FrameRateDemo -> navigate(FrameRateDemo) { launchSingleTop = true }
    LayoutDirectionDemo -> navigate(LayoutDirectionDemo) { launchSingleTop = true }
    PointerInputDemo -> navigate(PointerInputDemo) { launchSingleTop = true }
    PointerIconDemo -> navigate(PointerIconDemo) { launchSingleTop = true }
    PopupMenuDemo -> navigate(PopupMenuDemo) { launchSingleTop = true }
    DialogDemo -> navigate(DialogDemo) { launchSingleTop = true }
    FileDropDemo -> navigate(FileDropDemo) { launchSingleTop = true }
    FilePickerDemo -> navigate(FilePickerDemo) { launchSingleTop = true }
    InputEventsDemo -> navigate(InputEventsDemo) { launchSingleTop = true }
  }
}
