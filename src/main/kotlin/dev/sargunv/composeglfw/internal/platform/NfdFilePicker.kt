package dev.sargunv.composeglfw.internal.platform

import dev.sargunv.composeglfw.DisplayServer
import dev.sargunv.composeglfw.FileDialogFilter
import dev.sargunv.composeglfw.FilePicker
import dev.sargunv.composeglfw.internal.window.PlatformWindow
import java.nio.file.Path
import org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.util.nfd.NFDFilterItem
import org.lwjgl.util.nfd.NFDOpenDialogArgs
import org.lwjgl.util.nfd.NFDPickFolderArgs
import org.lwjgl.util.nfd.NFDSaveDialogArgs
import org.lwjgl.util.nfd.NFDWindowHandle
import org.lwjgl.util.nfd.NativeFileDialog.NFD_CANCEL
import org.lwjgl.util.nfd.NativeFileDialog.NFD_ERROR
import org.lwjgl.util.nfd.NativeFileDialog.NFD_FreePath
import org.lwjgl.util.nfd.NativeFileDialog.NFD_GetError
import org.lwjgl.util.nfd.NativeFileDialog.NFD_Init
import org.lwjgl.util.nfd.NativeFileDialog.NFD_OKAY
import org.lwjgl.util.nfd.NativeFileDialog.NFD_OpenDialogMultiple_With
import org.lwjgl.util.nfd.NativeFileDialog.NFD_OpenDialog_With
import org.lwjgl.util.nfd.NativeFileDialog.NFD_PathSet_Free
import org.lwjgl.util.nfd.NativeFileDialog.NFD_PathSet_FreePath
import org.lwjgl.util.nfd.NativeFileDialog.NFD_PathSet_GetCount
import org.lwjgl.util.nfd.NativeFileDialog.NFD_PathSet_GetPath
import org.lwjgl.util.nfd.NativeFileDialog.NFD_PickFolderMultiple_With
import org.lwjgl.util.nfd.NativeFileDialog.NFD_PickFolder_With
import org.lwjgl.util.nfd.NativeFileDialog.NFD_Quit
import org.lwjgl.util.nfd.NativeFileDialog.NFD_SaveDialog_With
import org.lwjgl.util.nfd.NativeFileDialog.NFD_WINDOW_HANDLE_TYPE_COCOA
import org.lwjgl.util.nfd.NativeFileDialog.NFD_WINDOW_HANDLE_TYPE_UNSET
import org.lwjgl.util.nfd.NativeFileDialog.NFD_WINDOW_HANDLE_TYPE_WINDOWS
import org.lwjgl.util.nfd.NativeFileDialog.NFD_WINDOW_HANDLE_TYPE_X11

internal class NfdFilePicker(
  private var window: PlatformWindow,
  private val checkThread: (String) -> Unit,
  private val runNativeDialog: (() -> Unit) -> Unit,
) : FilePicker, AutoCloseable {
  private var runtimeLease: AutoCloseable? = null

  fun updateWindow(window: PlatformWindow) {
    this.window = window
  }

  override fun openFile(
    defaultDirectory: Path?,
    filters: List<FileDialogFilter>,
  ): Path? {
    checkThread("show a file open dialog")
    ensureRuntime()
    var result: Path? = null
    runNativeDialog {
      MemoryStack.stackPush().use { stack ->
        val outPath = stack.mallocPointer(1)
        val args =
          NFDOpenDialogArgs.calloc(stack)
            .filterList(filterList(stack, filters))
            .defaultPath(defaultDirectory?.toNativeString(stack))
            .applyParentWindow(stack)
        result =
          whenResult(NFD_OpenDialog_With(outPath, args)) {
            outPath[0].toPathAndFree(::NFD_FreePath)
          }
      }
    }
    return result
  }

  override fun openFiles(
    defaultDirectory: Path?,
    filters: List<FileDialogFilter>,
  ): List<Path> {
    checkThread("show a multiple file open dialog")
    ensureRuntime()
    var result: List<Path> = emptyList()
    runNativeDialog {
      MemoryStack.stackPush().use { stack ->
        val outPathSet = stack.mallocPointer(1)
        val args =
          NFDOpenDialogArgs.calloc(stack)
            .filterList(filterList(stack, filters))
            .defaultPath(defaultDirectory?.toNativeString(stack))
            .applyParentWindow(stack)
        result =
          whenResult(NFD_OpenDialogMultiple_With(outPathSet, args)) {
            outPathSet[0].toPathListAndFree(stack)
          } ?: emptyList()
      }
    }
    return result
  }

  override fun saveFile(
    defaultDirectory: Path?,
    defaultName: String?,
    filters: List<FileDialogFilter>,
  ): Path? {
    checkThread("show a save file dialog")
    ensureRuntime()
    var result: Path? = null
    runNativeDialog {
      MemoryStack.stackPush().use { stack ->
        val outPath = stack.mallocPointer(1)
        val args =
          NFDSaveDialogArgs.calloc(stack)
            .filterList(filterList(stack, filters))
            .defaultPath(defaultDirectory?.toNativeString(stack))
            .defaultName(defaultName?.toNativeString(stack))
            .applyParentWindow(stack)
        result =
          whenResult(NFD_SaveDialog_With(outPath, args)) {
            outPath[0].toPathAndFree(::NFD_FreePath)
          }
      }
    }
    return result
  }

  override fun pickFolder(defaultDirectory: Path?): Path? {
    checkThread("show a folder picker dialog")
    ensureRuntime()
    var result: Path? = null
    runNativeDialog {
      MemoryStack.stackPush().use { stack ->
        val outPath = stack.mallocPointer(1)
        val args =
          NFDPickFolderArgs.calloc(stack)
            .defaultPath(defaultDirectory?.toNativeString(stack))
            .applyParentWindow(stack)
        result =
          whenResult(NFD_PickFolder_With(outPath, args)) {
            outPath[0].toPathAndFree(::NFD_FreePath)
          }
      }
    }
    return result
  }

  override fun pickFolders(defaultDirectory: Path?): List<Path> {
    checkThread("show a multiple folder picker dialog")
    ensureRuntime()
    var result: List<Path> = emptyList()
    runNativeDialog {
      MemoryStack.stackPush().use { stack ->
        val outPathSet = stack.mallocPointer(1)
        val args =
          NFDPickFolderArgs.calloc(stack)
            .defaultPath(defaultDirectory?.toNativeString(stack))
            .applyParentWindow(stack)
        result =
          whenResult(NFD_PickFolderMultiple_With(outPathSet, args)) {
            outPathSet[0].toPathListAndFree(stack)
          } ?: emptyList()
      }
    }
    return result
  }

  override fun close() {
    runtimeLease?.close()
    runtimeLease = null
  }

  private fun ensureRuntime() {
    if (runtimeLease == null) {
      runtimeLease = NfdRuntime.acquire()
    }
  }

  private fun NFDOpenDialogArgs.applyParentWindow(stack: MemoryStack): NFDOpenDialogArgs {
    parentWindow(parentWindowHandle(stack))
    return this
  }

  private fun NFDSaveDialogArgs.applyParentWindow(stack: MemoryStack): NFDSaveDialogArgs {
    parentWindow(parentWindowHandle(stack))
    return this
  }

  private fun NFDPickFolderArgs.applyParentWindow(stack: MemoryStack): NFDPickFolderArgs {
    parentWindow(parentWindowHandle(stack))
    return this
  }

  private fun parentWindowHandle(stack: MemoryStack): NFDWindowHandle {
    val handle = NFDWindowHandle.calloc(stack)
    when (currentDisplayServer()) {
      DisplayServer.WIN32 ->
        handle.set(NFD_WINDOW_HANDLE_TYPE_WINDOWS.toLong(), glfwGetWin32Window(window.handle))
      DisplayServer.COCOA ->
        handle.set(NFD_WINDOW_HANDLE_TYPE_COCOA.toLong(), glfwGetCocoaWindow(window.handle))
      DisplayServer.X11 ->
        handle.set(NFD_WINDOW_HANDLE_TYPE_X11.toLong(), glfwGetX11Window(window.handle))
      DisplayServer.WAYLAND -> handle.set(NFD_WINDOW_HANDLE_TYPE_UNSET.toLong(), NULL)
    }
    return handle
  }
}

private object NfdRuntime {
  private var leaseCount = 0

  @Synchronized
  fun acquire(): AutoCloseable {
    if (leaseCount == 0) {
      val result = NFD_Init()
      if (result != NFD_OKAY) {
        error("Native file dialog initialization failed: ${NFD_GetError() ?: "unknown error"}")
      }
    }
    leaseCount += 1
    var closed = false
    return AutoCloseable {
      synchronized(this) {
        if (!closed) {
          closed = true
          leaseCount -= 1
          if (leaseCount == 0) {
            NFD_Quit()
          }
        }
      }
    }
  }
}

private fun filterList(
  stack: MemoryStack,
  filters: List<FileDialogFilter>,
): NFDFilterItem.Buffer? {
  if (filters.isEmpty()) {
    return null
  }
  val buffer = NFDFilterItem.malloc(filters.size, stack)
  filters.forEachIndexed { index, filter ->
    buffer
      .get(index)
      .name(filter.name.toNativeString(stack))
      .spec(filter.extensions.toNativeSpec().toNativeString(stack))
  }
  return buffer
}

private fun List<String>.toNativeSpec(): String = joinToString(",") { it.trim().trimStart('.') }

private fun Path.toNativeString(stack: MemoryStack) = toString().toNativeString(stack)

private fun String.toNativeString(stack: MemoryStack) = stack.UTF8(this)

private inline fun <T> whenResult(result: Int, block: () -> T): T? =
  when (result) {
    NFD_OKAY -> block()
    NFD_CANCEL -> null
    NFD_ERROR -> error("Native file dialog failed: ${NFD_GetError() ?: "unknown error"}")
    else -> error("Native file dialog returned unexpected result $result")
  }

private inline fun Long.toPathAndFree(free: (Long) -> Unit): Path {
  check(this != NULL) { "Native file dialog returned a null path" }
  try {
    return Path.of(memUTF8(this))
  } finally {
    free(this)
  }
}

private fun Long.toPathListAndFree(stack: MemoryStack): List<Path> {
  check(this != NULL) { "Native file dialog returned a null path set" }
  try {
    val count = stack.mallocInt(1)
    checkNfd(NFD_PathSet_GetCount(this, count))
    val outPath = stack.mallocPointer(1)
    return List(count[0]) { index ->
      checkNfd(NFD_PathSet_GetPath(this, index, outPath))
      outPath[0].toPathAndFree(::NFD_PathSet_FreePath)
    }
  } finally {
    NFD_PathSet_Free(this)
  }
}

private fun checkNfd(result: Int) {
  when (result) {
    NFD_OKAY -> return
    NFD_CANCEL -> error("Native file dialog returned cancellation while reading selected paths")
    NFD_ERROR -> error("Native file dialog failed: ${NFD_GetError() ?: "unknown error"}")
    else -> error("Native file dialog returned unexpected result $result")
  }
}
