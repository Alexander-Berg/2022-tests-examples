package vertis.pipeline.lb

import common.yt.YtError
import common.zio.schedule.RetryConf
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.vertis.broker.model.common.PartitionPeriods
import vertis.core.model.{DataCenter, DataCenters}
import vertis.logbroker.client.consumer.model.{LbTopicPartition, Offset, Partition}
import vertis.logbroker.client.model.LogbrokerError
import vertis.pipeline.conf.LbYtStreamConfig
import vertis.pipeline.lb.LbYtOffsetSourceIntSpec.Test
import vertis.stream.lb.model.LbStreamSourceConfig
import vertis.stream.lb.sink.LbTopicConverters.lbTpConverter
import vertis.stream.yt.conf.YtStreamSinkConfig
import vertis.stream.yt.init.YtPipelineInit
import vertis.stream.yt.util.zio.clients.YtOffsets
import vertis.yt.zio.YtZioTest
import vertis.yt.zio.YtZioTest.YtTestResources
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase.TestBody
import zio.ZIO

import java.util.concurrent.ThreadLocalRandom

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class LbYtOffsetSourceIntSpec extends YtZioTest {

  private val lbRetry = LogbrokerError.lbRetry(RetryConf())

  "topic offset source" should {

    "read offsets" in offsetSourceTest { test =>
      import test._
      val streamConf = conf("/vertis/broker/unit_test/my-topic", Set(0, 1, 2, 3))
      val topicOffsetsSource = offsetSource
        .streamOffsetSource(streamConf)
        .createOffsetSource(DataCenters.Sas, streamConf.source.partitions)
      for {
        _ <- yt.tx.withTx("init")(pipeInit.init(streamConf.toYtStreamingConf))
        lastWritten <- topicOffsetsSource.lastWrittenOffsets.retry(lbRetry)
        toRead <- topicOffsetsSource.offsetsToRead(Map.empty)
        _ <- check("initial is -1")(lastWritten shouldBe Map(0 -> -1, 1 -> -1, 2 -> -1, 3 -> -1))
        _ <- check("read from 0")(toRead shouldBe Map(0 -> 0, 1 -> 0, 2 -> 0, 3 -> 0))
      } yield ()
    }

    "filter offsets by topic" in offsetSourceTest { test =>
      import test._
      val streamConf = conf("/vertis/broker/unit_test/some-topic", Set(0, 1, 2))
      val otherStreamConf = conf("/vertis/broker/unit_test/some-other-topic", Set(0, 1, 2, 3, 4))
      val topicOffsetSource = offsetSource
        .streamOffsetSource(streamConf)
        .createOffsetSource(DataCenters.Iva, streamConf.source.partitions)
      for {
        _ <- yt.tx.withTx("init")(pipeInit.init(streamConf.toYtStreamingConf))
        _ <- yt.tx.withTx("init other")(pipeInit.init(otherStreamConf.toYtStreamingConf))
        _ <- setOffsets(test)(streamConf, DataCenters.Iva, 0 -> 10L, 1 -> 20L)
        _ <- setRandomOffsets(test)(otherStreamConf)
        lastWritten <- topicOffsetSource.lastWrittenOffsets.retry(lbRetry)
        toRead <- topicOffsetSource.offsetsToRead(Map.empty)
        _ <- check("got written offsets")(lastWritten shouldBe Map(0 -> 10L, 1 -> 20L, 2 -> -1L))
        _ <- check("read from written +1")(toRead shouldBe Map(0 -> 11L, 1 -> 21L, 2 -> 0L))
      } yield ()
    }
  }

  private def setRandomOffsets(test: Test)(streamConf: LbYtStreamConfig): ZIO[BaseEnv, YtError, Unit] = {
    ZIO
      .foreach(DataCenters.logbrokerDcs) {
        val randomOffsets = streamConf.source.partitions
          .map(p => p -> ThreadLocalRandom.current().nextLong(0L, 100500L))
        dc => setOffsets(test)(streamConf, dc, randomOffsets.toSeq: _*)
      }
      .unit
  }

  private def setOffsets(
      test: Test
    )(streamConf: LbYtStreamConfig,
      dc: DataCenter,
      offsets: (Partition, Offset)*): ZIO[BaseEnv, YtError, Unit] = {
    test.yt.tx
      .withTx("set_offsets") {
        ZIO.foreach(offsets.map { case (p, o) => LbTopicPartition(streamConf.source.topic, dc, p) -> o }) {
          case (lbTp, o) =>
            test.offsets.setOffset(streamConf.sink.tableBase, lbTpConverter.get(lbTp), o)
        }
      }
      .unit
  }

  private def conf(
      topic: String,
      partitions: Set[Int],
      dcs: Set[DataCenter] = DataCenters.logbrokerDcs): LbYtStreamConfig = {
    val name = topic.split('/').last
    LbYtStreamConfig(
      LbStreamSourceConfig(
        topic,
        "test-consumer",
        dcs,
        partitions
      ),
      YtStreamSinkConfig(
        testBasePath.child(name),
        name,
        PartitionPeriods.byDay,
        "some.message.Type",
        None
      )
    )
  }

  private def offsetSourceTest(body: Test => TestBody): Unit = {
    ioTest {
      ytResources.use { case YtTestResources(yt) =>
        val offsets = new YtOffsets(yt.cypress)
        val source = new LbYtOffsetSource(offsets, yt.tx)
        val init = new YtPipelineInit(yt.cypress, offsets)
        body(Test(source, offsets, yt, init))
      }
    }
  }
}

object LbYtOffsetSourceIntSpec {

  case class Test(offsetSource: LbYtOffsetSource, offsets: YtOffsets, yt: YtZio, pipeInit: YtPipelineInit)
}
