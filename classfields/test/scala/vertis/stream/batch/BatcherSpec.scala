package vertis.stream.batch

import com.typesafe.config.ConfigMemorySize
import vertis.stream.model.{OffsetRange, TopicPartition}
import vertis.stream.convert.{ConvertedMessage, Weighted}
import vertis.stream.batch.BatcherSpec._
import vertis.zio.test.ZioSpecBase
import vertis.core.model.DataCenters
import vertis.stream.sink.OffsetRangeMerger
import zio.stream.ZStream

/** @author kusaeva
  */
class BatcherSpec extends ZioSpecBase {

  val batcher = Batcher.byWeight[Inner](ConfigMemorySize.ofBytes(2).toBytes)

  "Batcher" should {
    "batch by weight" in ioTest {
      val messages = Seq(
        Message(Inner(1), 0, tp1, v2),
        Message(Inner(1), 1, tp1, v2),
        Message(Inner(1), 2, tp1, v1), // should not be in batch, but should be in offsets
        Message(Inner(1), 3, tp1, v2),
        Message(Inner(1), 4, tp1, v2)
      )
      val stream = ZStream.fromIterator(messages.iterator)
      val x = stream.aggregateAsync(batcher.transducer)
      x.runCollect.flatMap { chunk =>
        for {
          _ <- check("batches elements:")(chunk.flatMap(_.elements).size shouldBe 5)
          _ <- check("offsets:") {
            val h :: t = chunk.map(_.offsets).toList
            t.foldLeft(h)(OffsetRangeMerger.merge) should contain theSameElementsAs Map(tp1 -> OffsetRange(tp1, 0, 4))
          }
        } yield ()
      }
    }
    "batch multiple partitions" ignore ioTest { // todo: flaky
      val messages = Seq(
        Message(Inner(1), 0, tp1, v2),
        Message(Inner(1), 0, tp2, v2),
        Message(Inner(1), 1, tp1, v2),
        Message(Inner(1), 2, tp1, v2),
        Message(Inner(1), 1, tp2, v2)
      )
      val stream = ZStream.fromIterator(messages.iterator)
      val x = stream.aggregateAsync(batcher.transducer)
      x.runCollect.flatMap { chunk =>
        for {
          _ <- check("batches count:")(chunk.size shouldBe 3)
          _ <- check("offsets:")(
            chunk.map(_.offsets).toSeq should contain theSameElementsAs Seq(
              Map(
                tp1 -> OffsetRange(tp1, 0, 0),
                tp2 -> OffsetRange(tp2, 0, 0)
              ),
              Map(
                tp1 -> OffsetRange(tp1, 1, 2)
              ),
              Map(
                tp2 -> OffsetRange(tp2, 1, 1)
              )
            )
          )
        } yield ()
      }
    }
  }
}

object BatcherSpec {
  final val tp1 = TopicPartition(s"${DataCenters.Vla}/topic", 1)
  final val tp2 = TopicPartition(s"${DataCenters.Vla}/topic", 2)

  final val v1 = "v0.0.1"
  final val v2 = "v0.0.2"

  final case class Inner(weight: Long) extends Weighted

  final case class Message(data: Inner, offset: Long, partition: TopicPartition, schemaVersion: String)
    extends ConvertedMessage[Inner]
}
