package dev.sargunv.composeglfw.internal.application

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.LockSupport
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

internal class UiDispatcher(private val wakeEventLoop: () -> Unit) : CoroutineDispatcher() {
  private val tasks = ConcurrentLinkedQueue<Runnable>()
  private var ownerThread: Thread? = null

  val hasTasks: Boolean
    get() = tasks.isNotEmpty()

  fun bindToCurrentThread() {
    ownerThread = Thread.currentThread()
  }

  fun isOwnerThread(): Boolean = Thread.currentThread() == ownerThread

  fun unparkOwnerThread() {
    ownerThread?.let(LockSupport::unpark)
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
    wakeEventLoop()
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
