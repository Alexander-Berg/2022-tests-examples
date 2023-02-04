package vertis.broker.pipeline.lb.sink

import ru.yandex.kikimr.persqueue.compression.CompressionCodec
import vertis.broker.pipeline.lb.sink.LbSeqKeeper.{MessageOrigin, OriginatedMessage}
import vertis.broker.pipeline.lb.sink.LbSinkSpec._
import vertis.broker.pipeline.lb.sink.metrics.{ConvertionMetrics, NopConvertionMetrics}
import vertis.core.model.DataCenters
import vertis.logbroker.client.producer.config.LbProducerSessionConfig
import vertis.logbroker.client.test.unit.UnitTestingLogbrokerNativeFacade
import vertis.stream.OffsetCommit
import vertis.stream.model.TopicPartition
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.test.{ZioEventually, ZioSpecBase}
import zio.stream.ZStream
import zio.{Managed, Task, UIO}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class LbSinkSpec extends ZioSpecBase with ZioEventually {

  "LbSink" should {
    // emulates a prod incident - target stopped replying and the source has died
    "stop on the main q shutdown" in sinkTest { test =>
      import test._
      for {
        q <- zio.Queue.unbounded[TestMsg]
        stream = ZStream.fromQueueWithShutdown(q, 1)
        consumerFiber <- sink
          .consume(stream, defaultConf, OffsetCommit.noCommits, q.shutdown, q.awaitShutdown)
          .use_(UIO.unit)
          .tapError(e => logger.error("Error", e))
          .forkDaemon
        _ <- q.offerAll(Iterator.from(0).map(msg).take(100).toSeq)
        _ <- checkEventually(q.size.map(s => s shouldBe 98))
        _ <- q.shutdown
        _ <- consumerFiber.join.absorb.ignore
      } yield ()
    }
  }

  private def sinkTest(body: Test => TestBody): Unit =
    ioTest {
      val facade =
        new UnitTestingLogbrokerNativeFacade(makeProducer = (_, _) => UIO(DevNullProducerSession))
      val sink = new LbSink(facade, IdConverter, 1)
      body(Test(sink))
    }
}

object LbSinkSpec {
  case class TestMsg(origin: MessageOrigin)
  private val defaultTp = TopicPartition("test-topic", 0)
  private val defaultConf = LbProducerSessionConfig(defaultTp.topic, "some-source-id", dc = Some(DataCenters.Sas))

  private def msg(i: Int): TestMsg =
    TestMsg(MessageOrigin(defaultTp, i.toLong))

  object IdConverter extends LbMessageConverter[TestMsg] {

    override def toMessages(
        input: TestMsg,
        metrics: ConvertionMetrics = NopConvertionMetrics): Task[List[OriginatedMessage]] = {
      val data = "payload".getBytes()
      UIO(List(OriginatedMessage(data, data.length, input.origin, CompressionCodec.RAW)))
    }
  }
  case class Test(sink: LbSink[TestMsg])
}
