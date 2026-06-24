package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.sargunv.composeglfw.HostWindowInfo
import dev.sargunv.composeglfw.WindowState

@Composable
internal fun ComposeGlfwApp(
  windowInfo: HostWindowInfo,
  windowState: WindowState,
  showcaseState: WindowShowcaseState,
  showcaseActions: WindowShowcaseActions,
) {
  val navController = rememberNavController()

  DemoTheme {
    Surface(Modifier.fillMaxSize()) {
      NavHost(navController = navController, startDestination = DemoHome) {
        composable<DemoHome> {
          DemoHomeScreen(
            windowInfo = windowInfo,
            onDestinationSelected = navController::navigateToDemo,
          )
        }
        composable<WindowStateDemo> {
          DemoDetailScaffold(
            title = demoTitle(WindowStateDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            WindowStateCard(
              windowInfo,
              windowState,
              darkTheme = isSystemInDarkTheme(),
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
        composable<WindowShowcaseDemo> {
          DemoDetailScaffold(
            title = demoTitle(WindowShowcaseDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            WindowShowcaseCard(showcaseState, showcaseActions, Modifier.fillMaxSize())
          }
        }
        composable<LifecycleDemo> {
          DemoDetailScaffold(
            title = demoTitle(LifecycleDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            LifecycleCard(Modifier.fillMaxSize())
          }
        }
        composable<FrameRateDemo> {
          DemoDetailScaffold(
            title = demoTitle(FrameRateDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            FrameRateCard(Modifier.fillMaxSize())
          }
        }
        composable<LayoutDirectionDemo> {
          DemoDetailScaffold(
            title = demoTitle(LayoutDirectionDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            LayoutDirectionCard(Modifier.fillMaxSize())
          }
        }
        composable<PointerInputDemo> {
          DemoDetailScaffold(
            title = demoTitle(PointerInputDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            PointerInputCard(Modifier.fillMaxSize())
          }
        }
        composable<PointerIconDemo> {
          DemoDetailScaffold(
            title = demoTitle(PointerIconDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            PointerIconCard(Modifier.fillMaxSize())
          }
        }
        composable<PopupMenuDemo> {
          DemoDetailScaffold(
            title = demoTitle(PopupMenuDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            PopupMenuCard(Modifier.fillMaxSize())
          }
        }
        composable<DialogDemo> {
          DemoDetailScaffold(
            title = demoTitle(DialogDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            DialogCard(Modifier.fillMaxSize())
          }
        }
        composable<FileDropDemo> {
          DemoDetailScaffold(
            title = demoTitle(FileDropDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            FileDropCard(Modifier.fillMaxSize())
          }
        }
        composable<FilePickerDemo> {
          DemoDetailScaffold(
            title = demoTitle(FilePickerDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            FilePickerCard(Modifier.fillMaxSize())
          }
        }
        composable<InputEventsDemo> {
          DemoDetailScaffold(
            title = demoTitle(InputEventsDemo),
            onNavigateBack = navController::navigateUp,
          ) {
            InputEventsCard(Modifier.fillMaxSize())
          }
        }
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
