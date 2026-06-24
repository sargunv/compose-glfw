package dev.sargunv.composeglfw.internal.platform.windows

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT
import org.lwjgl.system.MemoryUtil.NULL

internal object Com {
  val linker: Linker = Linker.nativeLinker()

  fun queryInterface(
    pointer: Long,
    iid: Guid,
    arena: Arena,
  ): Long {
    val out = arena.allocate(ADDRESS)
    val hr = callHRESULT(pointer, QueryInterfaceSlot, iid.allocate(arena), out)
    check(hr >= 0) { "IUnknown.QueryInterface failed with HRESULT 0x${hr.toUInt().toString(16)}" }
    return out.get(ADDRESS, 0).address()
  }

  fun release(pointer: Long) {
    if (pointer != NULL) {
      callInt(pointer, ReleaseSlot)
    }
  }

  fun callInt(pointer: Long, slot: Int): Int =
    linker
      .downcallHandle(vtableFunction(pointer, slot), FunctionDescriptor.of(JAVA_INT, ADDRESS))
      .invoke(MemorySegment.ofAddress(pointer)) as Int

  fun callHRESULT(pointer: Long, slot: Int, vararg args: Any): Int {
    val descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, *args.map(::layoutFor).toTypedArray())
    val handle = linker.downcallHandle(vtableFunction(pointer, slot), descriptor)
    return handle.invokeWithArguments(
      listOf(MemorySegment.ofAddress(pointer)) + args.map(::nativeArg)
    ) as Int
  }

  private fun vtableFunction(pointer: Long, slot: Int): MemorySegment {
    val instance = MemorySegment.ofAddress(pointer).reinterpret(ADDRESS.byteSize())
    val vtable = instance.get(ADDRESS, 0).reinterpret(ADDRESS.byteSize() * (slot + 1L))
    return vtable.get(ADDRESS, ADDRESS.byteSize() * slot)
  }

  private fun layoutFor(arg: Any) =
    when (arg) {
      is Int -> JAVA_INT
      is Long -> ADDRESS
      is MemorySegment -> ADDRESS
      else -> error("Unsupported native argument type ${arg::class.qualifiedName}")
    }

  private fun nativeArg(arg: Any): Any =
    when (arg) {
      is Long -> MemorySegment.ofAddress(arg)
      else -> arg
    }

  private const val QueryInterfaceSlot = 0
  private const val ReleaseSlot = 2
}

internal enum class Guid(private val bytes: ByteArray) {
  IDXGIFactory4(
    byteArrayOf(
      0x02,
      0xea.toByte(),
      0xc6.toByte(),
      0x1b,
      0x36,
      0xef.toByte(),
      0x4f,
      0x46,
      0xbf.toByte(),
      0x0c,
      0x21,
      0xca.toByte(),
      0x39,
      0xe5.toByte(),
      0x16,
      0x8a.toByte(),
    )
  ),
  IDXGIAdapter1(
    byteArrayOf(
      0x61,
      0x8f.toByte(),
      0x03,
      0x29,
      0x39,
      0x38,
      0x26,
      0x46,
      0x91.toByte(),
      0xfd.toByte(),
      0x08,
      0x68,
      0x79,
      0x01,
      0x1a,
      0x05,
    )
  ),
  IDXGIFactory2(
    byteArrayOf(
      0x1c,
      0x3a,
      0xc8.toByte(),
      0x50,
      0x72,
      0xe0.toByte(),
      0x48,
      0x4c,
      0x87.toByte(),
      0xb0.toByte(),
      0x36,
      0x30,
      0xfa.toByte(),
      0x36,
      0xa6.toByte(),
      0xd0.toByte(),
    )
  ),
  IDXGISwapChain3(
    byteArrayOf(
      0xdb.toByte(),
      0x9b.toByte(),
      0xd9.toByte(),
      0x94.toByte(),
      0xf8.toByte(),
      0xf1.toByte(),
      0xb0.toByte(),
      0x4a,
      0xb2.toByte(),
      0x36,
      0x7d,
      0xa0.toByte(),
      0x17,
      0x0e,
      0xda.toByte(),
      0xb1.toByte(),
    )
  ),
  ID3D12Device(
    byteArrayOf(
      0xf1.toByte(),
      0x19,
      0x98.toByte(),
      0x18,
      0xb6.toByte(),
      0x1d,
      0x57,
      0x4b,
      0xbe.toByte(),
      0x54,
      0x18,
      0x21,
      0x33,
      0x9b.toByte(),
      0x85.toByte(),
      0xf7.toByte(),
    )
  ),
  ID3D12CommandQueue(
    byteArrayOf(
      0xa6.toByte(),
      0x70,
      0xc8.toByte(),
      0x0e,
      0x7e,
      0x5d,
      0x22,
      0x4c,
      0x8c.toByte(),
      0xfc.toByte(),
      0x5b,
      0xaa.toByte(),
      0xe0.toByte(),
      0x76,
      0x16,
      0xed.toByte(),
    )
  ),
  ID3D12Resource(
    byteArrayOf(
      0xbe.toByte(),
      0x42,
      0x64,
      0x69,
      0x2e,
      0xa7.toByte(),
      0x59,
      0x40,
      0xbc.toByte(),
      0x79,
      0x5b,
      0x5c,
      0x98.toByte(),
      0x04,
      0x0f,
      0xad.toByte(),
    )
  ),
  IDCompositionDesktopDevice(
    byteArrayOf(
      0xfe.toByte(),
      0x33,
      0x46,
      0x5f,
      0x08,
      0x1e,
      0xb8.toByte(),
      0x4c,
      0x8c.toByte(),
      0x75,
      0xce.toByte(),
      0x24,
      0x33,
      0x3f,
      0x56,
      0x02,
    )
  );

  fun allocate(arena: Arena): MemorySegment {
    val segment = arena.allocate(bytes.size.toLong())
    bytes.forEachIndexed { index, byte -> segment.set(JAVA_BYTE, index.toLong(), byte) }
    return segment
  }
}
