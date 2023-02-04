package tests.strict

import strict.*
import zio.test.Gen.*
import zio.test.*

package object types {
  val anyInt8: Gen[Any, Int8]       = byte.map(Int8(_))
  val anyUint8: Gen[Any, Uint8]     = byte.map(Uint8(_))
  val anyInt16: Gen[Any, Int16]     = short.map(Int16(_))
  val anyUint16: Gen[Any, Uint16]   = char.map(Uint16(_))
  val anyInt32: Gen[Any, Int32]     = int.map(Int32(_))
  val anyUint32: Gen[Any, Uint32]   = int.map(Uint32(_))
  val anyInt64: Gen[Any, Int64]     = long.map(Int64(_))
  val anyUint64: Gen[Any, Uint64]   = long.map(Uint64(_))
  val anyFloat32: Gen[Any, Float32] = float.map(Float32(_))
  val anyFloat64: Gen[Any, Float64] = double.map(Float64(_))

  val anyTimestamp: Gen[Any, Timestamp] = long(0L, Long.MaxValue)
    .map(i => Timestamp(i))

  val anyBytes: Gen[Any & Sized, Bytes] = chunkOf(byte)
    .filter(_.nonEmpty)
    .map(_.toArray)
    .map(Bytes.apply)

  val anyUtf8: Gen[Any & Sized, Utf8] = asciiString.map(Utf8(_))
  val anyBool: Gen[Any, Bool]         = boolean.map(Bool(_))

  def anyBytes(n: Int): Gen[Any, Bytes] =
    chunkOfN(n)(byte).filter(_.nonEmpty).map(_.toArray).map(Bytes.apply)

}
