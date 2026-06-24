@file:OptIn(InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform.macos

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.SystemTheme
import dev.sargunv.composeglfw.internal.platform.SystemThemeProvider
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.macosx.ObjCRuntime.class_addMethod
import org.lwjgl.system.macosx.ObjCRuntime.objc_allocateClassPair
import org.lwjgl.system.macosx.ObjCRuntime.objc_getClass
import org.lwjgl.system.macosx.ObjCRuntime.objc_registerClassPair

internal fun createMacSystemThemeProvider(
  onSystemThemeChanged: (SystemTheme) -> Unit
): SystemThemeProvider = AppKitSystemThemeProvider(onSystemThemeChanged)

private class AppKitSystemThemeProvider(private val onSystemThemeChanged: (SystemTheme) -> Unit) :
  SystemThemeProvider {
  private var app: Long = NULL
  private var observer: Long = NULL
  private var keyPath: Long = NULL
  private var currentSystemTheme: SystemTheme = SystemTheme.Unknown

  override val systemTheme: SystemTheme
    get() = currentSystemTheme

  init {
    runCatching {
        MacObjectiveC.autoreleasePool().use {
          val nsApp =
            MacObjectiveC.sendPointer(MacObjectiveC.cls("NSApplication"), "sharedApplication")
          val keyPathString = MacObjectiveC.retainedNsString(EffectiveAppearanceKeyPath)
          val observerObject =
            MacObjectiveC.sendPointer(MacObjectiveC.sendPointer(observerClass, "alloc"), "init")

          app = nsApp
          keyPath = keyPathString
          observer = observerObject
          observers[observerObject] = this
          currentSystemTheme = nsApp.readSystemTheme()
          MacObjectiveC.sendVoid(
            nsApp,
            "addObserver:forKeyPath:options:context:",
            observerObject,
            keyPathString,
            0,
            observerContext,
          )
        }
      }
      .onFailure {
        close()
      }
  }

  override fun close() {
    val observerObject = observer
    if (observerObject != NULL) {
      observers.remove(observerObject)
    }
    if (app != NULL && observerObject != NULL && keyPath != NULL) {
      runCatching {
        MacObjectiveC.sendVoid(
          app,
          "removeObserver:forKeyPath:context:",
          observerObject,
          keyPath,
          observerContext,
        )
      }
    }
    MacObjectiveC.release(observerObject)
    MacObjectiveC.release(keyPath)
    app = NULL
    observer = NULL
    keyPath = NULL
  }

  private fun refreshSystemTheme() {
    updateSystemTheme(app.readSystemTheme())
  }

  private fun updateSystemTheme(systemTheme: SystemTheme) {
    if (systemTheme != currentSystemTheme) {
      currentSystemTheme = systemTheme
      onSystemThemeChanged(systemTheme)
    }
  }

  private fun Long.readSystemTheme(): SystemTheme =
    MacObjectiveC.autoreleasePool().use {
      val effectiveAppearance = MacObjectiveC.sendPointer(this, "effectiveAppearance")
      effectiveAppearance.toSystemTheme()
    }

  companion object {
    private val observers = ConcurrentHashMap<Long, AppKitSystemThemeProvider>()

    private val observerContext: Long by lazy { ObserveValueCallback.address }

    private val observerClass: Long by lazy {
      objc_getClass(ObserverClassName).takeIf { it != NULL }
        ?: run {
          val cls = objc_allocateClassPair(MacObjectiveC.cls("NSObject"), ObserverClassName, 0)
          check(cls != NULL) { "Failed to allocate Objective-C class: $ObserverClassName" }
          check(
            class_addMethod(
              cls,
              MacObjectiveC.selector("observeValueForKeyPath:ofObject:change:context:"),
              observerContext,
              "v@:@@@^v",
            )
          ) {
            "Failed to add observeValueForKeyPath:ofObject:change:context: to $ObserverClassName"
          }
          objc_registerClassPair(cls)
          cls
        }
    }

    fun handleObservation(
      self: Long,
      context: Long,
    ) {
      if (context == observerContext) {
        observers[self]?.refreshSystemTheme()
      }
    }
  }
}

private fun Long.toSystemTheme(): SystemTheme {
  if (this == NULL) {
    return SystemTheme.Unknown
  }
  val matchingName =
    MacObjectiveC.sendPointer(
      this,
      "bestMatchFromAppearancesWithNames:",
      MacObjectiveC.nsArray(DarkAquaAppearanceName, AquaAppearanceName),
    )
  return when {
    matchingName == NULL -> SystemTheme.Unknown
    MacObjectiveC.sendBoolean(matchingName, "isEqualToString:", DarkAquaAppearanceName) ->
      SystemTheme.Dark
    MacObjectiveC.sendBoolean(matchingName, "isEqualToString:", AquaAppearanceName) ->
      SystemTheme.Light
    else -> SystemTheme.Unknown
  }
}

private object ObserveValueCallback {
  val address: Long by lazy {
    Linker.nativeLinker()
      .upcallStub(
        MethodHandles.lookup()
          .findStatic(
            ObserveValueCallback::class.java,
            "observe",
            MethodType.methodType(
              Void.TYPE,
              MemorySegment::class.java,
              MemorySegment::class.java,
              MemorySegment::class.java,
              MemorySegment::class.java,
              MemorySegment::class.java,
              MemorySegment::class.java,
            ),
          ),
        FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS),
        Arena.global(),
      )
      .address()
  }

  @JvmStatic
  @Suppress("UNUSED_PARAMETER")
  fun observe(
    self: MemorySegment,
    selector: MemorySegment,
    keyPath: MemorySegment,
    observedObject: MemorySegment,
    change: MemorySegment,
    context: MemorySegment,
  ) {
    runCatching {
      AppKitSystemThemeProvider.handleObservation(self.address(), context.address())
    }
  }
}

private val AquaAppearanceName: Long by lazy {
  MacObjectiveC.frameworkObjectConstant("AppKit", "NSAppearanceNameAqua")
}

private val DarkAquaAppearanceName: Long by lazy {
  MacObjectiveC.frameworkObjectConstant("AppKit", "NSAppearanceNameDarkAqua")
}

private const val EffectiveAppearanceKeyPath = "effectiveAppearance"
private const val ObserverClassName = "ComposeGlfwSystemThemeObserver"
