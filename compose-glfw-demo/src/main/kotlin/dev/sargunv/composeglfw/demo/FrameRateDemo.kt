package dev.sargunv.composeglfw.demo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
internal fun FrameRateCard(modifier: Modifier = Modifier) {
  var requestedFrameRate by remember { mutableFloatStateOf(60f) }
  val frameCounter = remember { DrawFrameCounter() }
  var observedFrameRate by remember { mutableIntStateOf(0) }
  val targetFrameRate = requestedFrameRate.roundToInt()

  LaunchedEffect(frameCounter) {
    while (true) {
      delay(1_000)
      observedFrameRate = frameCounter.takeCount()
    }
  }

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Frame rate voting", style = MaterialTheme.typography.titleMedium)
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricRow("Requested", "$targetFrameRate fps", Modifier.weight(1f))
        MetricRow("Observed draw", "$observedFrameRate fps", Modifier.weight(1f))
      }
      Slider(
        value = requestedFrameRate,
        onValueChange = { requestedFrameRate = it },
        valueRange = 15f..120f,
        steps = 104,
        modifier = Modifier.fillMaxWidth(),
      )
      FrameRateAnimation(
        progress = frameRateAnimationProgress(),
        modifier =
          Modifier.fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .preferredFrameRate(targetFrameRate.toFloat())
            .countDrawFrames(frameCounter),
      )
    }
  }
}

@Composable
private fun frameRateAnimationProgress(): Float {
  val transition = rememberInfiniteTransition(label = "frame-rate-demo")
  val progress by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1_600, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "progress",
    )
  return progress
}

@Composable
private fun FrameRateAnimation(
  progress: Float,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  Canvas(modifier) {
    val trackY = size.height * 0.5f
    val horizontalPadding = 28.dp.toPx()
    val start = Offset(horizontalPadding, trackY)
    val end = Offset(size.width - horizontalPadding, trackY)
    val x = start.x + (end.x - start.x) * progress
    val radius = 20.dp.toPx()

    drawLine(
      color = colorScheme.outlineVariant,
      start = start,
      end = end,
      strokeWidth = 5.dp.toPx(),
      cap = StrokeCap.Round,
    )
    drawCircle(
      color = colorScheme.primary.copy(alpha = 0.16f),
      radius = radius * 1.8f,
      center = Offset(x, trackY),
    )
    drawCircle(color = colorScheme.primary, radius = radius, center = Offset(x, trackY))
    drawCircle(
      color = colorScheme.onPrimary,
      radius = radius * 0.34f,
      center = Offset(x, trackY),
    )
  }
}

private fun Modifier.countDrawFrames(counter: DrawFrameCounter): Modifier = drawWithContent {
  counter.recordFrame()
  drawContent()
}

private class DrawFrameCounter {
  private val count = AtomicInteger()

  fun recordFrame() {
    count.incrementAndGet()
  }

  fun takeCount(): Int = count.getAndSet(0)
}
