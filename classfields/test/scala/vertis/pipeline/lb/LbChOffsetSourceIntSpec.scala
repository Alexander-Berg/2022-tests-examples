package vertis.pipeline.lb

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.DynamicMessage
import common.zio.clients.clickhouse.jdbc.ClickhouseJdbcClient.ClickhouseJdbcClient
import common.zio.schedule.RetryConf
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import vertis.broker.pipeline.ch.sink.conf.ChSinkConfig
import vertis.broker.pipeline.ch.sink.converter.{Event, EventCustomTs, EventTs}
import vertis.broker.pipeline.ch.sink.queries.ClickhouseQueries
import vertis.core.model.{DataCenter, DataCenters}
import vertis.logbroker.client.consumer.model.{LbTopicPartition, Offset}
import vertis.logbroker.client.model.LogbrokerError
import vertis.pipeline.conf.LbChStreamConfig
import vertis.pipeline.convert.ClickhouseTestSpec
import vertis.stream.lb.model.LbStreamSourceConfig
import vertis.stream.lb.sink.LbTopicConverters.lbTpConverter
import vertis.stream.model.Partition
import vertis.zio.BaseEnv
import zio._
import zio.test.Assertion._
import zio.test._

import java.util.concurrent.ThreadLocalRandom

/** @author kusaeva
  */
object LbChOffsetSourceIntSpec extends ClickhouseTestSpec {

  case class OffsetsTest(chTest: ChTest) {
    lazy val offsetSource: LbChOffsetSource = new LbChOffsetSource(chTest.offsets)
  }

  override protected def chSpec: ZSpec[BaseEnv with ClickhouseJdbcClient, Any] =
    suite("LbChOffsetSource")(readOffsets, filterOffsetsByTopic, returnMaxOffsetByTopic)

  private def offsetSourceTest(descriptor: Descriptor) =
    chTestM(descriptor).map(OffsetsTest)

  private def conf(
      topic: String,
      sinkConf: ChSinkConfig,
      partitions: Set[Int],
      dcs: Set[DataCenter] = DataCenters.logbrokerDcs): LbChStreamConfig =
    LbChStreamConfig(
      LbStreamSourceConfig(
        topic,
        "test-consumer",
        dcs,
        partitions
      ),
      sinkConf,
      "v0.0.5000"
    )
  private val lbRetry = LogbrokerError.lbRetry(RetryConf())

  private val readOffsets = testM("read offsets") {
    offsetSourceTest(Event.getDescriptor).use { offsetTest =>
      import offsetTest._
      import chTest._
      val streamConf = conf("/vertis/broker/unit_test/my-topic", sinkConf, Set(0, 1, 2, 3))
      val topicOffsetsSource = offsetSource
        .streamOffsetSource(streamConf)
        .createOffsetSource(DataCenters.Sas, streamConf.source.partitions)
      for {
        _ <- UIO(getTable).tap(schemaEvolution.update)
        initialOffsets <- assertM(topicOffsetsSource.lastWrittenOffsets.retry(lbRetry))(
          equalTo(Map(0 -> -1L, 1 -> -1L, 2 -> -1L, 3 -> -1L))
        )
        readFromZero <- assertM(topicOffsetsSource.offsetsToRead(Map.empty))(
          equalTo(Map(0 -> 0L, 1 -> 0L, 2 -> 0L, 3 -> 0L))
        )
      } yield initialOffsets && readFromZero
    }
  }

  private val filterOffsetsByTopic = testM("filter offsets by topic") {
    offsetSourceTest(EventTs.getDescriptor).use { offsetsTest =>
      import offsetsTest._
      import chTest._
      val streamConf = conf("/vertis/broker/unit_test/some-topic", sinkConf, Set(0, 1, 2))
      val otherStreamConf = conf("/vertis/broker/unit_test/some-other-topic", sinkConf, Set(0, 1, 2, 3, 4))
      val topicOffsetSource = offsetSource
        .streamOffsetSource(streamConf)
        .createOffsetSource(DataCenters.Iva, streamConf.source.partitions)
      for {
        _ <- UIO(getTable).tap(schemaEvolution.update)
        _ <- setOffsets(chTest)(streamConf, DataCenters.Iva, 0 -> 10L, 1 -> 20L)
        _ <- setRandomOffsets(chTest)(otherStreamConf)
        gotWrittenOffsets <- assertM(topicOffsetSource.lastWrittenOffsets.retry(lbRetry))(
          equalTo(Map(0 -> 10L, 1 -> 20L, 2 -> -1L))
        )
        readFromWrittenPlus1 <- assertM(topicOffsetSource.offsetsToRead(Map.empty))(
          equalTo(Map(0 -> 11L, 1 -> 21L, 2 -> 0L))
        )
      } yield gotWrittenOffsets && readFromWrittenPlus1
    }
  }

  private val returnMaxOffsetByTopic = testM("return max offset by topic") {
    offsetSourceTest(EventCustomTs.getDescriptor).use { offsetsTest =>
      import offsetsTest._
      import chTest._
      val streamConf = conf("/vertis/broker/unit_test/some-topic", sinkConf, Set(0, 1, 2))
      val topicOffsetSource = offsetSource
        .streamOffsetSource(streamConf)
        .createOffsetSource(DataCenters.Iva, streamConf.source.partitions)
      for {
        _ <- UIO(getTable).tap(schemaEvolution.update)
        _ <- setOffsets(chTest)(streamConf, DataCenters.Iva, 0 -> 10L, 1 -> 20L)
        gotSettedOffsets <- assertM(topicOffsetSource.lastWrittenOffsets.retry(lbRetry))(
          equalTo(Map(0 -> 10L, 1 -> 20L, 2 -> -1L))
        )
        _ <- setOffsets(chTest)(streamConf, DataCenters.Iva, 0 -> 15L, 1 -> 41L, 2 -> 11L)
        gotLastWritten <- assertM(topicOffsetSource.lastWrittenOffsets.retry(lbRetry))(
          equalTo(Map(0 -> 15L, 1 -> 41L, 2 -> 11L))
        )
        gotToReadFromWrittenPlus1 <- assertM(topicOffsetSource.offsetsToRead(Map.empty))(
          equalTo(Map(0 -> 16L, 1 -> 42L, 2 -> 12L))
        )
      } yield gotSettedOffsets
    }
  }

  private def setRandomOffsets(test: ChTest)(streamConf: LbChStreamConfig) = {
    ZIO
      .foreach(DataCenters.logbrokerDcs) {
        val randomOffsets = streamConf.source.partitions
          .map(p => p -> ThreadLocalRandom.current().nextLong(0L, 100500L))
        dc => setOffsets(test)(streamConf, dc, randomOffsets.toSeq: _*)
      }
      .unit
  }

  private def setOffsets(
      test: ChTest
    )(streamConf: LbChStreamConfig,
      dc: DataCenter,
      offsets: (Partition, Offset)*): ZIO[Any, Throwable, Unit] = {
    val schema = test.getSchema
    val columns = converter.toColumns(schema)
    val gen = RandomProtobufGenerator
      .genFor(DynamicMessage.getDefaultInstance(test.descriptor), maxRep = 1)
    ZIO
      .foreach_(offsets.map { case (p, o) => LbTopicPartition(streamConf.source.topic, dc, p) -> o }) {
        case (lbTp, o) =>
          val input = genPreparedMessage(gen, lbTpConverter.get(lbTp), o).next
          val rows = converter.fixEmptyColumns(Seq(converter.toRow(input, schema)), schema)
          val insertQuery = ClickhouseQueries.insertBatch(streamConf.sink.id, columns, rows)
          test.client
            .executeQuery(insertQuery)
            .unit
      }
  }
}
