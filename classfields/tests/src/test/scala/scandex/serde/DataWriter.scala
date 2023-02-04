package scandex.serde

import alien.memory.Values
import strict.{Bool, Bytes, Int32, Int64, Int8, Utf8}

import java.io.OutputStream
import java.nio.ByteBuffer

trait DataWriter[T] {
  def saveData(data: Seq[T], out: OutputStream): Unit
}

object DataWriter {

  implicit class RichOutputStream(out: OutputStream) {

    def saveData[T](data: Seq[T])(
      implicit
      wr: DataWriter[T],
    ): Unit = {
      wr.saveData(data, out)
    }

  }

  implicit def BoolDataWriter: DataWriter[Bool] =
    (data: Seq[Bool], out: OutputStream) => {
      out.write(
        data
          .map(d =>
            if (d.value)
              1
            else
              0,
          )
          .map(_.toByte)
          .toArray,
      )
    }

  implicit def Int64DataWriter: DataWriter[Int64] =
    (data: Seq[Int64], out: OutputStream) => {
      val b = ByteBuffer
        .allocate(data.size * Values.Long.bits.toInt / 8)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)

      data.foreach(l => b.putLong(l.value))
      out.write(b.array())
    }

  implicit def LongDataWriter: DataWriter[Long] =
    (data: Seq[Long], out: OutputStream) =>
      Int64DataWriter.saveData(data.map(Int64(_)), out)

  implicit def Int32DataWriter: DataWriter[Int32] =
    (data: Seq[Int32], out: OutputStream) => {
      val b = ByteBuffer
        .allocate(data.size * Values.Int.bits.toInt / 8)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)

      data.foreach(i => b.putInt(i.value))
      out.write(b.array())
    }

  implicit def Utf8DataWriter: DataWriter[Utf8] =
    (data: Seq[Utf8], out: OutputStream) => {
      val o = ByteBuffer
        .allocate((data.size + 1) * Values.Long.bits.toInt / 8)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
      val offsets =
        data.foldLeft(List(0L))((acc, x) => acc :+ (acc.last + x.length))
      offsets.foreach(o.putLong)
      out.write(o.array())
      Int8DataWriter.saveData(data.flatMap(x => x.asBytes.map(Int8(_))), out)
    }

  implicit def Int8DataWriter: DataWriter[Int8] =
    (data: Seq[Int8], out: OutputStream) => out.write(data.map(_.value).toArray)

  implicit def BytesDataWriter: DataWriter[Bytes] =
    (data: Seq[Bytes], out: OutputStream) => {
      val o = ByteBuffer
        .allocate((data.size + 1) * Values.Long.bits.toInt / 8)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)

      val offsets =
        data.foldLeft(List(0L))((acc, x) => acc :+ (acc.last + x.length))

      offsets.foreach(o.putLong)
      out.write(o.array())

      data.foreach(bytes => out.write(bytes.toArray()))
    }

}
