package dev.sargunv.composeglfw.internal.platform.macos

import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.system.macosx.DynamicLinkLoader.RTLD_LOCAL
import org.lwjgl.system.macosx.DynamicLinkLoader.RTLD_NOW
import org.lwjgl.system.macosx.DynamicLinkLoader.dlerror
import org.lwjgl.system.macosx.DynamicLinkLoader.dlopen
import org.lwjgl.system.macosx.DynamicLinkLoader.dlsym
import org.lwjgl.system.macosx.ObjCRuntime.class_getMethodImplementation
import org.lwjgl.system.macosx.ObjCRuntime.objc_getClass
import org.lwjgl.system.macosx.ObjCRuntime.object_getClass
import org.lwjgl.system.macosx.ObjCRuntime.sel_getName
import org.lwjgl.system.macosx.ObjCRuntime.sel_registerName

internal object MacObjectiveC {
  private val selectors = mutableMapOf<String, Long>()
  private val classes = mutableMapOf<String, Long>()
  private val frameworks = mutableMapOf<String, Long>()

  fun allocInit(className: String): Long = sendPointer(sendPointer(cls(className), "alloc"), "init")

  fun retain(obj: Long): Long = if (obj == NULL) NULL else sendPointer(obj, "retain")

  fun release(obj: Long) {
    if (obj != NULL) {
      sendVoid(obj, "release")
    }
  }

  fun autoreleasePool(): Pool = Pool(allocInit("NSAutoreleasePool"))

  fun isMainThread(): Boolean = sendBoolean(cls("NSThread"), "isMainThread")

  fun metalSystemDefaultDevice(): Long {
    loadFramework("Metal")
    return JNI.invokeP(function("Metal", "MTLCreateSystemDefaultDevice"))
  }

  fun sendPointer(
    receiver: Long,
    selectorName: String,
  ): Long {
    val selector = selector(selectorName)
    return JNI.invokePPP(receiver, selector, implementation(receiver, selector))
  }

  fun sendPointer(
    receiver: Long,
    selectorName: String,
    argument: Long,
  ): Long {
    val selector = selector(selectorName)
    return JNI.invokePPPP(receiver, selector, argument, implementation(receiver, selector))
  }

  fun sendBoolean(
    receiver: Long,
    selectorName: String,
  ): Boolean {
    val selector = selector(selectorName)
    return JNI.invokePPI(receiver, selector, implementation(receiver, selector)) != 0
  }

  fun sendVoid(
    receiver: Long,
    selectorName: String,
  ) {
    val selector = selector(selectorName)
    JNI.invokePPV(receiver, selector, implementation(receiver, selector))
  }

  fun sendVoid(
    receiver: Long,
    selectorName: String,
    argument: Boolean,
  ) {
    val selector = selector(selectorName)
    JNI.invokePPV(receiver, selector, argument, implementation(receiver, selector))
  }

  fun sendVoid(
    receiver: Long,
    selectorName: String,
    argument: Int,
  ) {
    val selector = selector(selectorName)
    JNI.invokePPV(receiver, selector, argument, implementation(receiver, selector))
  }

  fun sendVoid(
    receiver: Long,
    selectorName: String,
    argument: Long,
  ) {
    val selector = selector(selectorName)
    JNI.invokePPPV(receiver, selector, argument, implementation(receiver, selector))
  }

  fun sendVoid(
    receiver: Long,
    selectorName: String,
    argument: Long,
    index: Int,
  ) {
    val selector = selector(selectorName)
    JNI.invokePPPV(receiver, selector, argument, index, implementation(receiver, selector))
  }

  fun sendDouble(
    receiver: Long,
    selectorName: String,
    value: Double,
  ) {
    val selector = selector(selectorName)
    autoreleasePool().use {
      MemoryStack.stackPush().use { stack ->
        val signature = sendPointer(receiver, "methodSignatureForSelector:", selector)
        check(signature != NULL) { "Method signature not found: $selectorName" }
        val invocation =
          sendPointer(cls("NSInvocation"), "invocationWithMethodSignature:", signature)
        val argument = stack.mallocDouble(1)
        argument.put(0, value)
        sendVoid(invocation, "setTarget:", receiver)
        sendVoid(invocation, "setSelector:", selector)
        sendVoid(invocation, "setArgument:atIndex:", memAddress(argument), 2)
        sendVoid(invocation, "invoke")
      }
    }
  }

  fun sendSize(
    receiver: Long,
    selectorName: String,
    width: Double,
    height: Double,
  ) {
    val selector = selector(selectorName)
    autoreleasePool().use {
      MemoryStack.stackPush().use { stack ->
        val signature = sendPointer(receiver, "methodSignatureForSelector:", selector)
        check(signature != NULL) { "Method signature not found: $selectorName" }
        val invocation =
          sendPointer(cls("NSInvocation"), "invocationWithMethodSignature:", signature)
        val size = stack.mallocDouble(2)
        size.put(0, width)
        size.put(1, height)
        sendVoid(invocation, "setTarget:", receiver)
        sendVoid(invocation, "setSelector:", selector)
        sendVoid(invocation, "setArgument:atIndex:", memAddress(size), 2)
        sendVoid(invocation, "invoke")
      }
    }
  }

  private fun cls(name: String): Long =
    classes.getOrPut(name) {
      loadFrameworkForClass(name)
      val cls = objc_getClass(name)
      check(cls != NULL) { "Objective-C class not found: $name" }
      cls
    }

  private fun loadFrameworkForClass(className: String) {
    when {
      className.startsWith("CA") -> loadFramework("QuartzCore")
      className.startsWith("MTL") -> loadFramework("Metal")
      else -> loadFramework("Foundation")
    }
  }

  private fun selector(name: String): Long =
    selectors.getOrPut(name) {
      val selector = sel_registerName(name)
      check(selector != NULL) { "Objective-C selector not found: $name" }
      selector
    }

  private fun implementation(
    receiver: Long,
    selector: Long,
  ): Long {
    check(receiver != NULL) { "Objective-C receiver is null" }
    val implementation = class_getMethodImplementation(object_getClass(receiver), selector)
    check(implementation != NULL) { "Objective-C method not found: ${sel_getName(selector)}" }
    return implementation
  }

  private fun function(
    framework: String,
    symbol: String,
  ): Long {
    val address = dlsym(loadFramework(framework), symbol)
    check(address != NULL) { "Native function not found: $framework/$symbol" }
    return address
  }

  private fun loadFramework(name: String): Long =
    frameworks.getOrPut(name) {
      val path = "/System/Library/Frameworks/$name.framework/$name"
      val handle = dlopen(path, RTLD_NOW or RTLD_LOCAL)
      check(handle != NULL) { "Failed to load $path: ${dlerror()}" }
      handle
    }

  internal class Pool(private var handle: Long) : AutoCloseable {
    override fun close() {
      if (handle != NULL) {
        sendVoid(handle, "drain")
        handle = NULL
      }
    }
  }
}
