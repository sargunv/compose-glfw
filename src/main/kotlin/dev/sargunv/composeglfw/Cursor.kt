package dev.sargunv.composeglfw

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.unit.IntOffset

/**
 * Creates a pointer icon from an image.
 *
 * The image must have a positive width and height. The hotspot is in source image pixels and is
 * clamped to the image bounds.
 *
 * @param image image to use for the cursor.
 * @param imageScale source image pixels per displayed logical cursor unit.
 * @param hotSpot pixel in the source image that points at the target location.
 */
public fun cursorImagePointerIcon(
  image: ImageBitmap,
  imageScale: Float,
  hotSpot: IntOffset = IntOffset.Zero,
): PointerIcon {
  require(image.width > 0) { "Cursor image width must be positive" }
  require(image.height > 0) { "Cursor image height must be positive" }
  require(java.lang.Float.isFinite(imageScale) && imageScale > 0f) {
    "Cursor image scale must be a positive finite value"
  }
  return CursorImagePointerIcon(
    image = image,
    hotSpot =
      IntOffset(
        hotSpot.x.coerceIn(0, image.width - 1),
        hotSpot.y.coerceIn(0, image.height - 1),
      ),
    imageScale = imageScale,
  )
}

internal data class CursorImagePointerIcon(
  val image: ImageBitmap,
  val hotSpot: IntOffset,
  val imageScale: Float,
) : PointerIcon
