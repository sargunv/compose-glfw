package dev.sargunv.composeglfw

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.unit.IntOffset

public fun cursorImagePointerIcon(
  image: ImageBitmap,
  hotSpot: IntOffset = IntOffset.Zero,
): PointerIcon {
  require(image.width > 0) { "Cursor image width must be positive" }
  require(image.height > 0) { "Cursor image height must be positive" }
  return CursorImagePointerIcon(
    image = image,
    hotSpot =
      IntOffset(
        hotSpot.x.coerceIn(0, image.width - 1),
        hotSpot.y.coerceIn(0, image.height - 1),
      ),
  )
}

internal data class CursorImagePointerIcon(
  val image: ImageBitmap,
  val hotSpot: IntOffset,
) : PointerIcon
