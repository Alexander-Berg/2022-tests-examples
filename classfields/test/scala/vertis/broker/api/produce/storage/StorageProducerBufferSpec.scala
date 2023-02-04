package vertis.broker.api.produce.storage

import ru.yandex.vertis.generators.ProducerProvider
import vertis.broker.api.model.StorageError.BufferIsFull
import vertis.broker.model.ModelGenerators
import vertis.zio.managed.ManagedUtils._
import vertis.zio.test.{ZioEventually, ZioSpecBase}
import zio.ZIO

/** @author zvez
  */
class StorageProducerBufferSpec extends ZioSpecBase with ProducerProvider with ZioEventually {

  "StorageProducerBuffer" should {
    "pass calls to underline producer" in ioTest {
      val messages = ModelGenerators.producerMessage.next(100)
      for {
        producer <- DummyStorageProducer.make
        buffer <- StorageProducerBuffer.wrapM(producer).acquire.map(_.r)
        promises <- ZIO.foreach(messages)(m => buffer.write(m).fork)

        received <- ZIO.foreach(1 to messages.size)(_ => producer.q.take)
        _ <- ZIO.foreach_(received)(_._2.complete(ZIO.unit))
        _ <- ZIO.foreach_(promises)(_.join)
        _ <- buffer.close
        wasClosed <- producer.closedRef.get
        _ <- check {
          received.map(_._1) should contain theSameElementsAs messages
          wasClosed shouldBe true
        }
      } yield ()
    }

    "limit in-flight and in buffer" in ioTest {
      val messageTemplate = ModelGenerators.producerMessage.next
      val messages = (1 to 25).map(_ => messageTemplate)
      val batch1 = messages.take(10)
      val batch2 = messages.slice(10, 20)
      val batch3 = messages.slice(20, 25)
      for {
        producer <- DummyStorageProducer.make
        buffer <- StorageProducerBuffer
          .wrapM(
            producer,
            maxInFlightBytes = 20 * messageTemplate.size
          )
          .acquire
          .map(_.r)

        write1Promises <- ZIO.foreach(batch1)(v => buffer.write(v).fork)
        write2Promises <- ZIO.foreach(batch2)(v => buffer.write(v).fork)

        _ <- logger.info("Check buffer limit")
        write3 <- ZIO.foreach(batch3)(v => buffer.write(v).either)

        _ <- check {
          write3.size shouldBe 5
          val errors = write3.collect { case Left(BufferIsFull) => () }
          errors.size shouldBe 5
        }

        _ <- logger.info("Acknowledge first batch")
        _ <- ZIO.foreach_(0 until 10) { _ =>
          producer.q.take.flatMap { case (_, p) =>
            p.succeed(())
          }
        }

        write1 <- ZIO.foreach(write1Promises)(_.join.either)
        _ = {
          val errors = write1.collect { case Left(_) => () }
          errors shouldBe empty
        }

        _ <- logger.info("Acknowledge second batch")
        _ <- ZIO.foreach_(0 until 10) { _ =>
          producer.q.take.flatMap { case (_, p) =>
            p.succeed(())
          }
        }

        write2 <- ZIO.foreach(write2Promises)(_.join.either)
        _ <- check {
          val errors = write2.collect { case Left(_) => () }
          errors shouldBe empty
        }

        _ <- checkEventually {
          buffer.statistics.flatMap { stats =>
            check {
              stats shouldBe StorageProducerStatistics.Empty
            }
          }

        }
        _ <- buffer.close

      } yield ()
    }
  }

}
