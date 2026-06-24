package dev.sargunv.composeglfw.internal.window

import dev.sargunv.composeglfw.CursorImagePointerIcon
import dev.sargunv.composeglfw.DisplayServer
import java.nio.ByteBuffer
import kotlin.math.floor
import kotlin.math.roundToInt
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree

internal data class ImageCursorKey(
  val pointerIcon: CursorImagePointerIcon,
  val imagePixelScale: Float,
)

internal fun CursorImagePointerIcon.imageCursorKey(
  displayServer: DisplayServer,
  contentScale: Float,
): ImageCursorKey =
  ImageCursorKey(
    pointerIcon = this,
    imagePixelScale = cursorImagePixelScale(displayServer, contentScale),
  )

private fun CursorImagePointerIcon.cursorImagePixelScale(
  displayServer: DisplayServer,
  contentScale: Float,
): Float {
  val nativeScale =
    when (displayServer) {
      DisplayServer.X11 -> contentScale
      DisplayServer.WAYLAND,
      DisplayServer.COCOA -> 1f
    }
  return nativeScale / imageScale
}

internal class NativeCursorImage(
  val width: Int,
  val height: Int,
  val xhot: Int,
  val yhot: Int,
  val rgbaPixels: ByteBuffer,
) : AutoCloseable {
  override fun close() {
    memFree(rgbaPixels)
  }
}

internal fun ImageCursorKey.toNativeCursorImage(): NativeCursorImage {
  val image = pointerIcon.image
  val argbPixels = IntArray(image.width * image.height)
  image.readPixels(argbPixels)

  val cursorWidth = (image.width * imagePixelScale).roundToInt().coerceAtLeast(1)
  val cursorHeight = (image.height * imagePixelScale).roundToInt().coerceAtLeast(1)
  val xhot = (pointerIcon.hotSpot.x * imagePixelScale).roundToInt().coerceIn(0, cursorWidth - 1)
  val yhot = (pointerIcon.hotSpot.y * imagePixelScale).roundToInt().coerceIn(0, cursorHeight - 1)
  val rgbaPixels = memAlloc(cursorWidth * cursorHeight * 4)

  try {
    writeScaledRgba(argbPixels, image.width, image.height, cursorWidth, cursorHeight, rgbaPixels)
    return NativeCursorImage(
      width = cursorWidth,
      height = cursorHeight,
      xhot = xhot,
      yhot = yhot,
      rgbaPixels = rgbaPixels,
    )
  } catch (throwable: Throwable) {
    memFree(rgbaPixels)
    throw throwable
  }
}

private fun writeScaledRgba(
  source: IntArray,
  sourceWidth: Int,
  sourceHeight: Int,
  targetWidth: Int,
  targetHeight: Int,
  target: ByteBuffer,
) {
  for (y in 0 until targetHeight) {
    val sourceY =
      ((y + 0.5) * sourceHeight / targetHeight - 0.5).coerceIn(
        minimumValue = 0.0,
        maximumValue = (sourceHeight - 1).toDouble(),
      )
    val y0 = floor(sourceY).toInt()
    val y1 = (y0 + 1).coerceAtMost(sourceHeight - 1)
    val yWeight = sourceY - y0

    for (x in 0 until targetWidth) {
      val sourceX =
        ((x + 0.5) * sourceWidth / targetWidth - 0.5).coerceIn(
          minimumValue = 0.0,
          maximumValue = (sourceWidth - 1).toDouble(),
        )
      val x0 = floor(sourceX).toInt()
      val x1 = (x0 + 1).coerceAtMost(sourceWidth - 1)
      val xWeight = sourceX - x0

      val argb =
        interpolateArgb(
          topLeft = source[y0 * sourceWidth + x0],
          topRight = source[y0 * sourceWidth + x1],
          bottomLeft = source[y1 * sourceWidth + x0],
          bottomRight = source[y1 * sourceWidth + x1],
          xWeight = xWeight,
          yWeight = yWeight,
        )
      val offset = (y * targetWidth + x) * 4
      target.put(offset, ((argb shr 16) and 0xff).toByte())
      target.put(offset + 1, ((argb shr 8) and 0xff).toByte())
      target.put(offset + 2, (argb and 0xff).toByte())
      target.put(offset + 3, ((argb ushr 24) and 0xff).toByte())
    }
  }
}

private fun interpolateArgb(
  topLeft: Int,
  topRight: Int,
  bottomLeft: Int,
  bottomRight: Int,
  xWeight: Double,
  yWeight: Double,
): Int {
  val top = interpolatePremultipliedArgb(topLeft, topRight, xWeight)
  val bottom = interpolatePremultipliedArgb(bottomLeft, bottomRight, xWeight)
  return interpolatePremultipliedArgb(top, bottom, yWeight)
}

private fun interpolatePremultipliedArgb(start: Int, end: Int, weight: Double): Int {
  val startAlpha = (start ushr 24) and 0xff
  val endAlpha = (end ushr 24) and 0xff
  val alpha = interpolate(startAlpha, endAlpha, weight)
  if (alpha == 0) {
    return 0
  }

  val red = interpolatePremultipliedChannel(start, end, weight, startAlpha, endAlpha, 16, alpha)
  val green = interpolatePremultipliedChannel(start, end, weight, startAlpha, endAlpha, 8, alpha)
  val blue = interpolatePremultipliedChannel(start, end, weight, startAlpha, endAlpha, 0, alpha)
  return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

private fun interpolatePremultipliedChannel(
  start: Int,
  end: Int,
  weight: Double,
  startAlpha: Int,
  endAlpha: Int,
  shift: Int,
  alpha: Int,
): Int {
  val startValue = ((start shr shift) and 0xff) * startAlpha / 255.0
  val endValue = ((end shr shift) and 0xff) * endAlpha / 255.0
  return (interpolate(startValue, endValue, weight) * 255.0 / alpha).roundToInt().coerceIn(0, 255)
}

private fun interpolate(start: Int, end: Int, weight: Double): Int =
  interpolate(start.toDouble(), end.toDouble(), weight).roundToInt().coerceIn(0, 255)

private fun interpolate(start: Double, end: Double, weight: Double): Double =
  start + (end - start) * weight
