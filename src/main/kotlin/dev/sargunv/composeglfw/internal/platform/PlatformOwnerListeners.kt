@file:OptIn(InternalComposeUiApi::class)

package dev.sargunv.composeglfw.internal.platform

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformRootForTest
import androidx.compose.ui.semantics.SemanticsOwner

internal class RootForTestRegistry : PlatformContext.RootForTestListener {
  private val mutableRoots = linkedSetOf<PlatformRootForTest>()
  private var listener: PlatformContext.RootForTestListener? = null

  val roots: Set<PlatformRootForTest>
    get() = mutableRoots

  fun setListener(listener: PlatformContext.RootForTestListener?) {
    this.listener = listener
    mutableRoots.forEach { root ->
      listener?.onRootForTestCreated(root)
    }
  }

  override fun onRootForTestCreated(root: PlatformRootForTest) {
    mutableRoots += root
    listener?.onRootForTestCreated(root)
  }

  override fun onRootForTestDisposed(root: PlatformRootForTest) {
    mutableRoots -= root
    listener?.onRootForTestDisposed(root)
  }
}

internal class SemanticsOwnerRegistry : PlatformContext.SemanticsOwnerListener {
  private val mutableOwners = linkedSetOf<SemanticsOwner>()
  private var listener: PlatformContext.SemanticsOwnerListener? = null

  val owners: Set<SemanticsOwner>
    get() = mutableOwners

  fun setListener(listener: PlatformContext.SemanticsOwnerListener?) {
    this.listener = listener
    mutableOwners.forEach { owner ->
      listener?.onSemanticsOwnerAppended(owner)
    }
  }

  override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
    mutableOwners += semanticsOwner
    listener?.onSemanticsOwnerAppended(semanticsOwner)
  }

  override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
    mutableOwners -= semanticsOwner
    listener?.onSemanticsOwnerRemoved(semanticsOwner)
  }

  override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
    listener?.onSemanticsChange(semanticsOwner)
  }

  override fun onLayoutChange(
    semanticsOwner: SemanticsOwner,
    semanticsNodeId: Int,
  ) {
    listener?.onLayoutChange(semanticsOwner, semanticsNodeId)
  }
}
