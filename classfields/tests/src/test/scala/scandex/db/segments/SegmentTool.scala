package scandex.db.segments

import scandex.db.segments.serde.SerializableSegment
import zio.stream.{ZSink, ZStream}
import zio.ZIO

object SegmentTool {

  def serializeDeserialize(
    source: SerializableSegment,
    target: SerializableSegment,
  ): ZIO[Any, Throwable, Long] = deserialize(source.serializeToStream, target)

  def deserialize(
    stream: ZStream[Any, Throwable, Byte],
    target: SerializableSegment,
  ): ZIO[Any, Throwable, Long] =
    stream.run(
      ZSink.foldLeftChunksZIO(0L) { case (offset, chunk) =>
        for {
          _ <- ZIO.fromTry(target.readFrom(offset, chunk.toArray))
        } yield offset + chunk.length.toLong
      },
    )

}
