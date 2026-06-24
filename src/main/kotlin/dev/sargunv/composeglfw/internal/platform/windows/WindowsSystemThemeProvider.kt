@file:OptIn(InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform.windows

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.SystemTheme
import dev.sargunv.composeglfw.internal.platform.SystemThemeProvider
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_CHAR
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.invoke.MethodHandle
import kotlin.concurrent.thread
import org.lwjgl.system.MemoryUtil.NULL

internal fun createWindowsSystemThemeProvider(
  onSystemThemeChanged: (SystemTheme) -> Unit
): SystemThemeProvider = RegistrySystemThemeProvider(onSystemThemeChanged)

private class RegistrySystemThemeProvider(private val onSystemThemeChanged: (SystemTheme) -> Unit) :
  SystemThemeProvider {
  private var key: Long = NULL
  private var changeEvent: Long = NULL
  private var closeEvent: Long = NULL
  private var watcherThread: Thread? = null
  @Volatile private var closed = false
  @Volatile private var currentSystemTheme: SystemTheme = SystemTheme.Unknown

  override val systemTheme: SystemTheme
    get() = currentSystemTheme

  init {
    runCatching {
        key = WindowsThemeRegistry.openPersonalizeKey()
        changeEvent = WindowsThemeRegistry.createEvent()
        closeEvent = WindowsThemeRegistry.createEvent()
        currentSystemTheme = WindowsThemeRegistry.readSystemTheme(key)
        watcherThread =
          thread(
            start = true,
            isDaemon = true,
            name = "Compose GLFW Windows system theme watcher",
          ) {
            watchThemeChanges()
          }
      }
      .onFailure { close() }
  }

  override fun close() {
    closed = true
    if (closeEvent != NULL) {
      runCatching { WindowsThemeRegistry.setEvent(closeEvent) }
    }
    runCatching { watcherThread?.join(WatcherShutdownTimeoutMillis) }
    watcherThread = null
    WindowsThemeRegistry.closeHandle(changeEvent)
    WindowsThemeRegistry.closeHandle(closeEvent)
    WindowsThemeRegistry.closeKey(key)
    changeEvent = NULL
    closeEvent = NULL
    key = NULL
  }

  private fun watchThemeChanges() {
    Arena.ofConfined().use { arena ->
      val handles = arena.allocate(ADDRESS, 2)
      handles.setAtIndex(ADDRESS, 0, MemorySegment.ofAddress(changeEvent))
      handles.setAtIndex(ADDRESS, 1, MemorySegment.ofAddress(closeEvent))

      while (!closed) {
        if (!WindowsThemeRegistry.notifyValueChanges(key, changeEvent)) {
          return
        }
        if (closed) {
          return
        }
        updateSystemTheme(WindowsThemeRegistry.readSystemTheme(key))

        when (WindowsThemeRegistry.waitForAny(handles)) {
          WaitObject0 -> {
            if (!closed) {
              updateSystemTheme(WindowsThemeRegistry.readSystemTheme(key))
            }
          }
          WaitObject0 + 1 -> return
          else -> return
        }
      }
    }
  }

  private fun updateSystemTheme(systemTheme: SystemTheme) {
    if (systemTheme != currentSystemTheme) {
      currentSystemTheme = systemTheme
      onSystemThemeChanged(systemTheme)
    }
  }
}

private object WindowsThemeRegistry {
  private val linker: Linker = Linker.nativeLinker()
  private val advapi32: SymbolLookup by lazy {
    SymbolLookup.libraryLookup("Advapi32", Arena.global())
  }
  private val kernel32: SymbolLookup by lazy {
    SymbolLookup.libraryLookup("Kernel32", Arena.global())
  }

  private val regOpenKeyExW: MethodHandle by lazy {
    linker.downcallHandle(
      advapi32.symbol("RegOpenKeyExW"),
      FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS),
    )
  }

  private val regQueryValueExW: MethodHandle by lazy {
    linker.downcallHandle(
      advapi32.symbol("RegQueryValueExW"),
      FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS),
    )
  }

  private val regNotifyChangeKeyValue: MethodHandle by lazy {
    linker.downcallHandle(
      advapi32.symbol("RegNotifyChangeKeyValue"),
      FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT),
    )
  }

  private val regCloseKey: MethodHandle by lazy {
    linker.downcallHandle(
      advapi32.symbol("RegCloseKey"),
      FunctionDescriptor.of(JAVA_INT, ADDRESS),
    )
  }

  private val createEventW: MethodHandle by lazy {
    linker.downcallHandle(
      kernel32.symbol("CreateEventW"),
      FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS),
    )
  }

  private val setEvent: MethodHandle by lazy {
    linker.downcallHandle(
      kernel32.symbol("SetEvent"),
      FunctionDescriptor.of(JAVA_INT, ADDRESS),
    )
  }

  private val waitForMultipleObjects: MethodHandle by lazy {
    linker.downcallHandle(
      kernel32.symbol("WaitForMultipleObjects"),
      FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT),
    )
  }

  private val closeHandle: MethodHandle by lazy {
    linker.downcallHandle(
      kernel32.symbol("CloseHandle"),
      FunctionDescriptor.of(JAVA_INT, ADDRESS),
    )
  }

  fun openPersonalizeKey(): Long =
    Arena.ofConfined().use { arena ->
      val out = arena.allocate(ADDRESS)
      val status =
        regOpenKeyExW.invoke(
          MemorySegment.ofAddress(HkeyCurrentUser),
          arena.wideString(PersonalizeKeyPath),
          0,
          KeyNotify or KeyQueryValue,
          out,
        ) as Int
      check(status == ErrorSuccess) { "RegOpenKeyExW failed with status $status" }
      out.get(ADDRESS, 0).address()
    }

  fun readSystemTheme(key: Long): SystemTheme =
    Arena.ofConfined().use { arena ->
      val type = arena.allocate(JAVA_INT)
      val data = arena.allocate(JAVA_INT)
      val dataSize = arena.allocate(JAVA_INT)
      dataSize.set(JAVA_INT, 0, JAVA_INT.byteSize().toInt())

      val status =
        regQueryValueExW.invoke(
          MemorySegment.ofAddress(key),
          arena.wideString(AppsUseLightThemeValueName),
          MemorySegment.NULL,
          type,
          data,
          dataSize,
        ) as Int

      if (
        status == ErrorSuccess &&
          type.get(JAVA_INT, 0) == RegDword &&
          dataSize.get(JAVA_INT, 0) >= JAVA_INT.byteSize()
      ) {
        if (data.get(JAVA_INT, 0) == 0) SystemTheme.Dark else SystemTheme.Light
      } else {
        SystemTheme.Unknown
      }
    }

  fun createEvent(): Long {
    val event =
      createEventW.invoke(MemorySegment.NULL, False, False, MemorySegment.NULL) as MemorySegment
    check(event.address() != NULL) { "CreateEventW failed" }
    return event.address()
  }

  fun setEvent(event: Long) {
    if (event != NULL) {
      setEvent.invoke(MemorySegment.ofAddress(event))
    }
  }

  fun notifyValueChanges(
    key: Long,
    event: Long,
  ): Boolean {
    if (key == NULL || event == NULL) {
      return false
    }

    val status =
      regNotifyChangeKeyValue.invoke(
        MemorySegment.ofAddress(key),
        False,
        RegNotifyChangeLastSet,
        MemorySegment.ofAddress(event),
        True,
      ) as Int
    return status == ErrorSuccess
  }

  fun waitForAny(handles: MemorySegment): Int =
    waitForMultipleObjects.invoke(2, handles, False, Infinite) as Int

  fun closeKey(key: Long) {
    if (key != NULL) {
      runCatching { regCloseKey.invoke(MemorySegment.ofAddress(key)) }
    }
  }

  fun closeHandle(handle: Long) {
    if (handle != NULL) {
      runCatching { closeHandle.invoke(MemorySegment.ofAddress(handle)) }
    }
  }

  private fun SymbolLookup.symbol(name: String): MemorySegment =
    find(name).orElseThrow { IllegalStateException("Missing native symbol $name") }

  private fun Arena.wideString(value: String): MemorySegment {
    val segment = allocate(JAVA_CHAR.byteSize() * (value.length + 1L))
    value.forEachIndexed { index, char ->
      segment.set(JAVA_CHAR, index * JAVA_CHAR.byteSize(), char)
    }
    segment.set(JAVA_CHAR, value.length * JAVA_CHAR.byteSize(), 0.toChar())
    return segment
  }
}

private const val WatcherShutdownTimeoutMillis = 1_000L
private const val HkeyCurrentUser = 0x80000001L
private const val KeyQueryValue = 0x0001
private const val KeyNotify = 0x0010
private const val RegDword = 4
private const val RegNotifyChangeLastSet = 0x00000004
private const val ErrorSuccess = 0
private const val WaitObject0 = 0x00000000
private const val Infinite = -1
private const val False = 0
private const val True = 1
private const val PersonalizeKeyPath =
  "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
private const val AppsUseLightThemeValueName = "AppsUseLightTheme"
