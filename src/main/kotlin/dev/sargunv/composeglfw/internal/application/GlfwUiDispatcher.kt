package dev.sargunv.composeglfw.internal.application

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

internal class GlfwUiDispatcher : CoroutineDispatcher() {
  private val tasks = ConcurrentLinkedQueue<Runnable>()
  private var ownerThread: Thread? = null

  fun bindToCurrentThread() {
    ownerThread = Thread.currentThread()
  }

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    tasks += block
  }

  fun drain() {
    check(Thread.currentThread() == ownerThread) { "GLFW UI dispatcher drained from a non-owner thread" }

    // Compose layout/draw observation is single-threaded per scene, so coroutine work for the
    // scene is allowed to enqueue from any thread but only runs on the GLFW owner thread.
    while (true) {
      val task = tasks.poll() ?: return
      task.run()
    }
  }
}
