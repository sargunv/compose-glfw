package dev.sargunv.composeglfw.internal.application

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

internal class UiDispatcher : CoroutineDispatcher() {
  private val tasks = ConcurrentLinkedQueue<Runnable>()
  private var ownerThread: Thread? = null

  fun bindToCurrentThread() {
    ownerThread = Thread.currentThread()
  }

  fun checkOwnerThread(operation: String) {
    val owner = ownerThread
    check(owner != null) { "GLFW UI dispatcher has not been bound to a thread" }
    check(Thread.currentThread() == owner) {
      "$operation must run on the GLFW UI thread; owner=${owner.name}, current=${Thread.currentThread().name}"
    }
  }

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    tasks += block
  }

  fun drain() {
    checkOwnerThread("GLFW UI dispatcher drain")

    // Compose layout/draw observation is single-threaded per scene, so coroutine work for the
    // scene is allowed to enqueue from any thread but only runs on the GLFW owner thread.
    while (true) {
      val task = tasks.poll() ?: return
      task.run()
    }
  }
}
