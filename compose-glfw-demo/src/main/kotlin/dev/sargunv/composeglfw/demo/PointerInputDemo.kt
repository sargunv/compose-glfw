package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun PointerInputCard(modifier: Modifier = Modifier) {
  var pointer by remember { mutableStateOf(Offset.Zero) }
  var pressed by remember { mutableStateOf(false) }
  var events by remember { mutableIntStateOf(0) }
  val colorScheme = MaterialTheme.colorScheme

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Mouse input", style = MaterialTheme.typography.titleMedium)
      Canvas(
        Modifier.fillMaxWidth()
          .height(280.dp)
          .background(MaterialTheme.colorScheme.surfaceContainerHighest)
          .pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull()
                if (change != null) {
                  pointer = change.position
                  pressed = change.pressed
                  events++
                }
              }
            }
          }
      ) {
        drawRect(colorScheme.surfaceContainerHighest)

        val gridColor = colorScheme.outlineVariant
        val grid = 32f
        var x = 0f
        while (x <= size.width) {
          drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
          x += grid
        }
        var y = 0f
        while (y <= size.height) {
          drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
          y += grid
        }

        val target = Offset(pointer.x.coerceIn(0f, size.width), pointer.y.coerceIn(0f, size.height))
        val targetColor = if (pressed) colorScheme.tertiary else colorScheme.primary
        drawLine(targetColor, Offset(target.x, 0f), Offset(target.x, size.height), strokeWidth = 2f)
        drawLine(targetColor, Offset(0f, target.y), Offset(size.width, target.y), strokeWidth = 2f)
        drawCircle(targetColor.copy(alpha = 0.18f), radius = 34f, center = target)
        drawCircle(targetColor, radius = if (pressed) 12f else 9f, center = target)
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PointerMetric("x", pointer.x.roundToInt().toString(), Modifier.weight(1f))
        PointerMetric("y", pointer.y.roundToInt().toString(), Modifier.weight(1f))
        PointerMetric("button", if (pressed) "down" else "up", Modifier.weight(1f))
        PointerMetric("events", events.toString(), Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun PointerMetric(label: String, value: String, modifier: Modifier = Modifier) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodyMedium)
  }
}
