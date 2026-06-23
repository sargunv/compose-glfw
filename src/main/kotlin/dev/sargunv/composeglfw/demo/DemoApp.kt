package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sargunv.composeglfw.GlfwPlatform
import dev.sargunv.composeglfw.GlfwRenderBackend
import dev.sargunv.composeglfw.GlfwWindowInfo
import kotlin.math.sin

@Composable
internal fun ComposeGlfwApp(windowInfo: GlfwWindowInfo) {
  var clicks by remember { mutableIntStateOf(0) }
  var selected by remember { mutableStateOf("Wayland") }
  val accent = if (selected == "Wayland") Color(0xFF0B6B61) else Color(0xFF7C3AED)

  Box(
    Modifier.fillMaxSize()
      .background(Brush.linearGradient(listOf(Color(0xFFF8FAFC), Color(0xFFE7EEF7))))
      .padding(24.dp)
  ) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
      Column {
        BasicText(
          "Compose on GLFW",
          style =
            TextStyle(
              color = Color(0xFF172033),
              fontSize = 32.sp,
              fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
          "A ComposeScene rendered into a Skia OpenGL surface owned by an LWJGL GLFW window.",
          style = TextStyle(color = Color(0xFF46546A), fontSize = 15.sp),
        )
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatusPanel(
          title = "GLFW platform",
          value = windowInfo.platform.displayName,
          detail = "Display: ${windowInfo.displayName ?: "<unset>"}",
          modifier = Modifier.weight(1f),
        )
        StatusPanel(
          title = "Windowing",
          value = "No AWT host",
          detail = "Rendering path: GLFW + Skia ${windowInfo.renderBackend.displayName}",
          modifier = Modifier.weight(1f),
        )
      }

      Canvas(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp))) {
        drawRect(Color(0xFF0F172A))
        val points = 96
        val path = Path()
        repeat(points) { index ->
          val progress = index / (points - 1f)
          val x = progress * size.width
          val wave = sin(progress * 7.2f + clicks * 0.5f) * 34f
          val y = size.height / 2f + wave
          if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color(0xFF38BDF8), style = Stroke(width = 5f, cap = StrokeCap.Round))
        drawCircle(accent, radius = 34f, center = Offset(size.width * 0.78f, size.height / 2f))
        drawArc(
          Color(0xFFFACC15),
          startAngle = clicks * 18f,
          sweepAngle = 240f,
          useCenter = false,
          topLeft = Offset(size.width * 0.78f - 52f, size.height / 2f - 52f),
          size = Size(104f, 104f),
          style = Stroke(width = 8f, cap = StrokeCap.Round),
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        ChoiceChip("Wayland", selected, accent) { selected = it }
        Spacer(Modifier.width(10.dp))
        ChoiceChip("Compose", selected, accent) { selected = it }
        Spacer(Modifier.weight(1f))
        Box(
          Modifier.clip(RoundedCornerShape(6.dp))
            .background(accent)
            .clickable { clicks++ }
            .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
          BasicText(
            "Click count: $clicks",
            style =
              TextStyle(
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
              ),
          )
        }
      }
    }
  }
}

private val GlfwPlatform.displayName: String
  get() =
    when (this) {
      GlfwPlatform.WAYLAND -> "Wayland"
    }

private val GlfwRenderBackend.displayName: String
  get() =
    when (this) {
      GlfwRenderBackend.OPENGL -> "OpenGL"
    }

@Composable
private fun StatusPanel(title: String, value: String, detail: String, modifier: Modifier = Modifier) {
  Column(
    modifier
      .clip(RoundedCornerShape(8.dp))
      .background(Color.White.copy(alpha = 0.82f))
      .border(1.dp, Color(0xFFD8E0EA), RoundedCornerShape(8.dp))
      .padding(16.dp)
  ) {
    BasicText(title, style = TextStyle(color = Color(0xFF64748B), fontSize = 12.sp))
    Spacer(Modifier.height(6.dp))
    BasicText(
      value,
      style =
        TextStyle(
          color = Color(0xFF0F172A),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
        ),
    )
    Spacer(Modifier.height(4.dp))
    BasicText(detail, style = TextStyle(color = Color(0xFF475569), fontSize = 13.sp))
  }
}

@Composable
private fun ChoiceChip(label: String, selected: String, accent: Color, onSelect: (String) -> Unit) {
  val active = label == selected
  val bg = if (active) accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.72f)
  val border = if (active) accent else Color(0xFFD8E0EA)
  Box(
    Modifier.size(width = 112.dp, height = 42.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(bg)
      .border(1.dp, border, RoundedCornerShape(6.dp))
      .clickable { onSelect(label) },
    contentAlignment = Alignment.Center,
  ) {
    BasicText(
      label,
      style =
        TextStyle(
          color = if (active) accent else Color(0xFF334155),
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
        ),
    )
  }
}
