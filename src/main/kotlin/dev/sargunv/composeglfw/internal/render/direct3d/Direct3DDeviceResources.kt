package dev.sargunv.composeglfw.internal.render.direct3d

import androidx.compose.ui.unit.IntSize
import dev.sargunv.composeglfw.internal.platform.windows.Com
import dev.sargunv.composeglfw.internal.platform.windows.Guid
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT
import kotlin.text.Charsets.UTF_16LE
import org.lwjgl.system.MemoryUtil.NULL

internal class Direct3DDeviceResources(private val hwnd: Long) : AutoCloseable {
  val factory: Long
  val adapter: Long
  val device: Long
  val commandQueue: Long
  private val composition: DirectCompositionResources
  var swapChain: Long = NULL
    private set

  val compositionDevice: Long
    get() = composition.device

  val compositionTarget: Long
    get() = composition.target

  val compositionVisual: Long
    get() = composition.visual

  init {
    factory = createFactory()
    val adapterAndDevice = createHardwareDevice(factory)
    adapter = adapterAndDevice.adapter
    device = adapterAndDevice.device
    commandQueue = createCommandQueue(device)
    composition = createCompositionResources(hwnd)
  }

  fun createSwapChain(size: IntSize) {
    check(swapChain == NULL) { "Direct3D swap chain is already initialized" }
    swapChain = createSwapChain(factory, commandQueue, size)
    composition.bindSwapChain(swapChain)
  }

  fun resizeSwapChain(size: IntSize) {
    check(swapChain != NULL) { "Direct3D swap chain is not initialized" }
    val hr =
      Com.callHRESULT(
        swapChain,
        Slot.SwapChainResizeBuffers,
        BufferCount,
        size.width,
        size.height,
        BackBufferFormat,
        0,
      )
    checkSucceeded(hr, "IDXGISwapChain.ResizeBuffers")
  }

  fun getBuffer(index: Int): Long =
    Arena.ofConfined().use { arena ->
      val out = arena.allocate(ADDRESS)
      val hr =
        Com.callHRESULT(
          swapChain,
          Slot.SwapChainGetBuffer,
          index,
          Guid.ID3D12Resource.allocate(arena),
          out,
        )
      checkSucceeded(hr, "IDXGISwapChain.GetBuffer")
      out.pointerAddress()
    }

  fun currentBackBufferIndex(): Int =
    Com.callInt(swapChain, Slot.SwapChainGetCurrentBackBufferIndex)

  fun present() {
    val hr = Com.callHRESULT(swapChain, Slot.SwapChainPresent, 1, 0)
    checkSucceeded(hr, "IDXGISwapChain.Present")
  }

  override fun close() {
    composition.close()
    Com.release(swapChain)
    swapChain = NULL
    Com.release(commandQueue)
    Com.release(device)
    Com.release(adapter)
    Com.release(factory)
  }

  private fun createFactory(): Long =
    Arena.ofConfined().use { arena ->
      val out = arena.allocate(ADDRESS)
      val hr =
        NativeLibraries.createDXGIFactory2.invoke(
          0,
          Guid.IDXGIFactory4.allocate(arena),
          out,
        ) as Int
      checkSucceeded(hr, "CreateDXGIFactory2")
      out.pointerAddress()
    }

  private fun createHardwareDevice(factory: Long): AdapterAndDevice =
    Arena.ofConfined().use { arena ->
      val outAdapter = arena.allocate(ADDRESS)
      var index = 0
      while (true) {
        outAdapter.set(ADDRESS, 0, MemorySegment.NULL)
        val enumHr = Com.callHRESULT(factory, Slot.FactoryEnumAdapters1, index, outAdapter)
        if (enumHr == DXGI_ERROR_NOT_FOUND) {
          break
        }
        checkSucceeded(enumHr, "IDXGIFactory.EnumAdapters1")

        val adapter = outAdapter.pointerAddress()
        val adapterName = adapter.description(arena)
        if (
          adapter.isHardwareAdapter(arena) && !adapterName.contains("Parallels", ignoreCase = true)
        ) {
          val device = createDeviceOrNull(adapter, arena)
          if (device != NULL) {
            return@use AdapterAndDevice(adapter, device)
          }
        }
        Com.release(adapter)
        index += 1
      }
      createWarpDevice(factory, arena)
    }

  private fun Long.isHardwareAdapter(arena: Arena): Boolean {
    val desc = arena.allocate(DXGI_ADAPTER_DESC1_SIZE)
    val hr = Com.callHRESULT(this, Slot.AdapterGetDesc1, desc)
    checkSucceeded(hr, "IDXGIAdapter1.GetDesc1")
    val flags = desc.get(JAVA_INT, DXGI_ADAPTER_DESC1_FLAGS_OFFSET)
    return flags and DXGI_ADAPTER_FLAG_SOFTWARE == 0
  }

  private fun Long.description(arena: Arena): String {
    val desc = arena.allocate(DXGI_ADAPTER_DESC1_SIZE)
    val hr = Com.callHRESULT(this, Slot.AdapterGetDesc1, desc)
    checkSucceeded(hr, "IDXGIAdapter1.GetDesc1")
    val bytes = ByteArray(DXGI_ADAPTER_DESC1_DESCRIPTION_BYTES)
    bytes.indices.forEach { index ->
      bytes[index] = desc.get(JAVA_BYTE, index.toLong())
    }
    return bytes.toString(UTF_16LE).substringBefore('\u0000')
  }

  private fun createWarpDevice(factory: Long, arena: Arena): AdapterAndDevice {
    val outAdapter = arena.allocate(ADDRESS)
    val hr =
      Com.callHRESULT(
        factory,
        Slot.FactoryEnumWarpAdapter,
        Guid.IDXGIAdapter1.allocate(arena),
        outAdapter,
      )
    checkSucceeded(hr, "IDXGIFactory4.EnumWarpAdapter")
    val adapter = outAdapter.pointerAddress()
    val device = createDeviceOrNull(adapter, arena)
    if (device == NULL) {
      Com.release(adapter)
      error("No Direct3D 12 capable hardware or WARP adapter was found")
    }
    return AdapterAndDevice(adapter, device)
  }

  private fun createDeviceOrNull(adapter: Long, arena: Arena): Long {
    val out = arena.allocate(ADDRESS)
    val hr =
      NativeLibraries.d3d12CreateDevice.invoke(
        MemorySegment.ofAddress(adapter),
        D3D_FEATURE_LEVEL_11_0,
        Guid.ID3D12Device.allocate(arena),
        out,
      ) as Int
    return if (hr >= 0) out.pointerAddress() else NULL
  }

  private fun createCommandQueue(device: Long): Long =
    Arena.ofConfined().use { arena ->
      val desc = arena.allocate(D3D12_COMMAND_QUEUE_DESC_SIZE)
      desc.set(JAVA_INT, 0, D3D12_COMMAND_LIST_TYPE_DIRECT)
      desc.set(JAVA_INT, 4, D3D12_COMMAND_QUEUE_PRIORITY_NORMAL)
      desc.set(JAVA_INT, 8, D3D12_COMMAND_QUEUE_FLAG_NONE)
      desc.set(JAVA_INT, 12, 0)

      val out = arena.allocate(ADDRESS)
      val hr =
        Com.callHRESULT(
          device,
          Slot.DeviceCreateCommandQueue,
          desc,
          Guid.ID3D12CommandQueue.allocate(arena),
          out,
        )
      checkSucceeded(hr, "ID3D12Device.CreateCommandQueue")
      out.pointerAddress()
    }

  private fun createSwapChain(
    factory: Long,
    commandQueue: Long,
    size: IntSize,
  ): Long =
    Arena.ofConfined().use { arena ->
      val desc = arena.allocate(DXGI_SWAP_CHAIN_DESC1_SIZE)
      desc.set(JAVA_INT, 0, size.width)
      desc.set(JAVA_INT, 4, size.height)
      desc.set(JAVA_INT, 8, BackBufferFormat)
      desc.set(JAVA_INT, 12, 0)
      desc.set(JAVA_INT, 16, 1)
      desc.set(JAVA_INT, 20, 0)
      desc.set(JAVA_INT, 24, DXGI_USAGE_RENDER_TARGET_OUTPUT)
      desc.set(JAVA_INT, 28, BufferCount)
      desc.set(JAVA_INT, 32, DXGI_SCALING_STRETCH)
      desc.set(JAVA_INT, 36, DXGI_SWAP_EFFECT_FLIP_SEQUENTIAL)
      desc.set(JAVA_INT, 40, DXGI_ALPHA_MODE_PREMULTIPLIED)
      desc.set(JAVA_INT, 44, 0)

      val outSwapChain1 = arena.allocate(ADDRESS)
      val hr =
        Com.callHRESULT(
          factory,
          Slot.FactoryCreateSwapChainForComposition,
          commandQueue,
          desc,
          MemorySegment.NULL,
          outSwapChain1,
        )
      checkSucceeded(hr, "IDXGIFactory2.CreateSwapChainForComposition")

      val swapChain1 = outSwapChain1.pointerAddress()
      try {
        Com.queryInterface(swapChain1, Guid.IDXGISwapChain3, arena)
      } finally {
        Com.release(swapChain1)
      }
    }

  private fun createCompositionResources(hwnd: Long): DirectCompositionResources =
    Arena.ofConfined().use { arena ->
      val outDevice = arena.allocate(ADDRESS)
      val createHr =
        NativeLibraries.dCompositionCreateDevice2.invoke(
          MemorySegment.NULL,
          Guid.IDCompositionDesktopDevice.allocate(arena),
          outDevice,
        ) as Int
      checkSucceeded(createHr, "DCompositionCreateDevice2")
      val compositionDevice = outDevice.pointerAddress()

      val outTarget = arena.allocate(ADDRESS)
      val targetHr =
        Com.callHRESULT(
          compositionDevice,
          Slot.CompositionDesktopDeviceCreateTargetForHwnd,
          hwnd,
          0,
          outTarget,
        )
      checkSucceeded(targetHr, "IDCompositionDesktopDevice.CreateTargetForHwnd")
      val target = outTarget.pointerAddress()

      val outVisual = arena.allocate(ADDRESS)
      val visualHr =
        Com.callHRESULT(
          compositionDevice,
          Slot.CompositionDevice2CreateVisual,
          outVisual,
        )
      checkSucceeded(visualHr, "IDCompositionDevice2.CreateVisual")

      DirectCompositionResources(
        device = compositionDevice,
        target = target,
        visual = outVisual.pointerAddress(),
      )
    }

  private data class AdapterAndDevice(
    val adapter: Long,
    val device: Long,
  )

  internal companion object {
    const val BufferCount = 2
    const val BackBufferFormat = 87
  }

  private class DirectCompositionResources(
    val device: Long,
    val target: Long,
    val visual: Long,
  ) : AutoCloseable {
    fun bindSwapChain(swapChain: Long) {
      val contentHr = Com.callHRESULT(visual, Slot.CompositionVisualSetContent, swapChain)
      checkSucceeded(contentHr, "IDCompositionVisual.SetContent")
      val rootHr = Com.callHRESULT(target, Slot.CompositionTargetSetRoot, visual)
      checkSucceeded(rootHr, "IDCompositionTarget.SetRoot")
      commit()
    }

    private fun commit() {
      val hr = Com.callHRESULT(device, Slot.CompositionDeviceCommit)
      checkSucceeded(hr, "IDCompositionDevice.Commit")
    }

    override fun close() {
      if (visual != NULL) {
        Com.callHRESULT(visual, Slot.CompositionVisualSetContent, MemorySegment.NULL)
      }
      if (target != NULL) {
        Com.callHRESULT(target, Slot.CompositionTargetSetRoot, MemorySegment.NULL)
      }
      if (device != NULL) {
        runCatching { commit() }
      }
      Com.release(visual)
      Com.release(target)
      Com.release(device)
    }
  }
}

private object NativeLibraries {
  private val arena = Arena.global()
  private val lookupDxgi = SymbolLookup.libraryLookup("dxgi.dll", arena)
  private val lookupD3d12 = SymbolLookup.libraryLookup("d3d12.dll", arena)
  private val lookupDcomp = SymbolLookup.libraryLookup("dcomp.dll", arena)

  val createDXGIFactory2 =
    Com.linker.downcallHandle(
      lookupDxgi.find("CreateDXGIFactory2").orElseThrow(),
      FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS),
    )

  val d3d12CreateDevice =
    Com.linker.downcallHandle(
      lookupD3d12.find("D3D12CreateDevice").orElseThrow(),
      FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS),
    )

  val dCompositionCreateDevice2 =
    Com.linker.downcallHandle(
      lookupDcomp.find("DCompositionCreateDevice2").orElseThrow(),
      FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS),
    )
}

private fun MemorySegment.pointerAddress(): Long = get(ADDRESS, 0).address()

private fun checkSucceeded(hr: Int, operation: String) {
  check(hr >= 0) { "$operation failed with HRESULT 0x${hr.toUInt().toString(16)}" }
}

private const val D3D_FEATURE_LEVEL_11_0 = 0xb000
private const val D3D12_COMMAND_LIST_TYPE_DIRECT = 0
private const val D3D12_COMMAND_QUEUE_PRIORITY_NORMAL = 0
private const val D3D12_COMMAND_QUEUE_FLAG_NONE = 0
private const val D3D12_COMMAND_QUEUE_DESC_SIZE = 16L
private const val DXGI_ADAPTER_FLAG_SOFTWARE = 2
private const val DXGI_ADAPTER_DESC1_DESCRIPTION_BYTES = 256
private const val DXGI_ADAPTER_DESC1_SIZE = 320L
private const val DXGI_ADAPTER_DESC1_FLAGS_OFFSET = 304L
private const val DXGI_ALPHA_MODE_PREMULTIPLIED = 1
private const val DXGI_ERROR_NOT_FOUND = -2005270526
private const val DXGI_SCALING_STRETCH = 0
private const val DXGI_SWAP_CHAIN_DESC1_SIZE = 48L
private const val DXGI_SWAP_EFFECT_FLIP_SEQUENTIAL = 3
private const val DXGI_USAGE_RENDER_TARGET_OUTPUT = 0x20

private object Slot {
  const val AdapterGetDesc1 = 10
  const val FactoryEnumAdapters1 = 12
  const val FactoryCreateSwapChainForComposition = 24
  const val FactoryEnumWarpAdapter = 27
  const val DeviceCreateCommandQueue = 8
  const val SwapChainPresent = 8
  const val SwapChainGetBuffer = 9
  const val SwapChainResizeBuffers = 13
  const val SwapChainGetCurrentBackBufferIndex = 36
  const val CompositionDeviceCommit = 3
  const val CompositionDevice2CreateVisual = 6
  const val CompositionDesktopDeviceCreateTargetForHwnd = 24
  const val CompositionTargetSetRoot = 3
  const val CompositionVisualSetContent = 15
}
