package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val PanLimitX = 520f
private const val PanLimitY = 360f
private const val PanPixelsPerScrollUnit = 14f

@Composable
internal fun ScrollPanCard(modifier: Modifier = Modifier) {
  var pan by remember { mutableStateOf(Offset.Zero) }
  val colorScheme = MaterialTheme.colorScheme

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("2D scroll pan", style = MaterialTheme.typography.titleMedium)
        Text(
          "Use a wheel or trackpad over the surface. Horizontal deltas pan sideways.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Canvas(
        Modifier.fillMaxWidth()
          .height(320.dp)
          .clipToBounds()
          .background(colorScheme.surfaceContainerHighest)
          .pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                  val change = event.changes.firstOrNull()
                  if (change != null) {
                    val next = (pan + Offset(change.scrollDelta.x, -change.scrollDelta.y) * PanPixelsPerScrollUnit).bounded()
                    if (next != pan) {
                      pan = next
                      change.consume()
                    }
                  }
                }
              }
            }
          }
      ) {
        drawRect(colorScheme.surfaceContainerHighest)
        drawTranslatedGrid(pan)
        drawMarkers(pan)
      }
    }
  }
}

private fun Offset.bounded(): Offset =
  Offset(
    x.coerceIn(-PanLimitX, PanLimitX),
    y.coerceIn(-PanLimitY, PanLimitY),
  )

private fun DrawScope.drawTranslatedGrid(pan: Offset) {
  val spacing = 56f
  val minorColor = Color(0xFFCAD2DC)
  val majorColor = Color(0xFF8FA1B4)
  val origin = Offset(size.width / 2f + pan.x, size.height / 2f + pan.y)
  var x = origin.x.mod(spacing)
  var column = ((x - origin.x) / spacing).roundToInt()
  while (x <= size.width) {
    val major = column % 4 == 0
    drawLine(
      if (major) majorColor else minorColor,
      Offset(x, 0f),
      Offset(x, size.height),
      strokeWidth = if (major) 2f else 1f,
    )
    x += spacing
    column++
  }

  var y = origin.y.mod(spacing)
  var row = ((y - origin.y) / spacing).roundToInt()
  while (y <= size.height) {
    val major = row % 4 == 0
    drawLine(
      if (major) majorColor else minorColor,
      Offset(0f, y),
      Offset(size.width, y),
      strokeWidth = if (major) 2f else 1f,
    )
    y += spacing
    row++
  }
}

private fun DrawScope.drawMarkers(pan: Offset) {
  val origin = Offset(size.width / 2f + pan.x, size.height / 2f + pan.y)
  val markers =
    listOf(
      Offset(0f, 0f),
      Offset(192f, -144f),
      Offset(-240f, 96f),
      Offset(336f, 240f),
      Offset(-384f, -240f),
    )
  val colors =
    listOf(
      Color(0xFF6750A4),
      Color(0xFF006E1C),
      Color(0xFFBA1A1A),
      Color(0xFF006A6A),
      Color(0xFF7D5700),
    )

  markers.forEachIndexed { index, marker ->
    val center = origin + marker
    drawCircle(colors[index], radius = if (index == 0) 11f else 8f, center = center)
    drawCircle(colors[index].copy(alpha = 0.18f), radius = 28f, center = center)
  }
}
