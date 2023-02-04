package vertis.pipeline.lb

import common.zio.logging.Logging
import com.google.protobuf.DynamicMessage
import common.clients.sraas.Version
import common.sraas.Sraas.SraasDescriptor
import common.sraas.TestSraas.DescriptorKey
import common.sraas.{Sraas, TestSraas}
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.vertis.broker.model.common.PartitionPeriods
import ru.yandex.vertis.broker.model.internal.Envelope
import ru.yandex.vertis.proto.util.ProtoHelp
import vertis.pipeline.lb.YtDataSinksIntSpec.{newVersion, oldVersion, protoYsonConverter, FakeCommittableBatch}
import vertis.proto.converter.{ProtoYsonConverter, ProtoYsonConverterImpl, ToYsonConverter}
import vertis.stream.OffsetCommit.noCommits
import vertis.stream.conf.BatchingConf
import vertis.stream.metrics.NopSinkMetrics
import vertis.stream.model._
import vertis.stream.yt.conf.YtStreamSinkConfig
import vertis.stream.yt.convert.SystemProtoYsonConverter
import vertis.stream.yt.sink.YtDataSinks
import vertis.yt.model.YtColumn
import vertis.yt.test.external_offer._
import vertis.yt.util.support.YsonSupport
import vertis.yt.util.support.YsonSupport.emptyMapNode
import vertis.yt.zio.YtZioTest
import vertis.zio.{BTask, BaseEnv}
import zio._
import zio.duration.durationInt
import zio.stream.ZStream
import zio.interop.catz._
import ru.yandex.vertis.proto.util.RandomProtobufGenerator

import scala.jdk.CollectionConverters.IterableHasAsScala

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class YtDataSinksIntSpec extends YtZioTest with YsonSupport {

  "YtSink" should {
    "write a batch with different schema versions" in ioTest {
      withSinks.use { sinks =>
        val oldMsgs = RandomProtobufGenerator
          .genForScala[Before]
          .sample
          .toSeq
          .map(p => ProtoHelp.parseDynamic(Before.javaDescriptor, p.toByteArray))
        val newMsgs = RandomProtobufGenerator
          .genForScala[After]
          .sample
          .toSeq
          .map(p => ProtoHelp.parseDynamic(After.javaDescriptor, p.toByteArray))
        val stream = ZStream.fromIterable[DynamicMessage](newMsgs ++ oldMsgs)
        val conf = YtStreamSinkConfig(
          testBasePath.child("test"),
          "test-name",
          PartitionPeriods.byDay,
          "TestMessage",
          None,
          BatchingConf()
        )
        for {
          ytSink <- sinks.consumer(conf)
          ytBatch <- sinks.ytBatcher(conf)
          batched = stream
            .aggregateAsyncWithin(
              ytBatch.sink,
              Schedule.fixed(10.seconds)
            )
          _ <- logger.info(s"Initiated ${conf.tableName}, starting streaming")
          consumed <- batched
            .run(ytSink.sink(noCommits))
        } yield consumed
      }
    }
  }

  private def withSinks: ZManaged[BaseEnv, Throwable, YtDataSinks[ProtoMessage[Unit], DynamicMessage]] =
    (for {
      _ <- TestSraas.setJavaDescriptor { case DescriptorKey(_, version) =>
        version match {
          case v if v == oldVersion => UIO(SraasDescriptor(Before.javaDescriptor, "before", version.toString))
          case v if v == newVersion => UIO(SraasDescriptor(After.javaDescriptor, "after", version.toString))
          case _ => ZIO.fail(new IllegalArgumentException(s"Unexpected version $version"))
        }
      }.toManaged_
      logging <- ZManaged.service[Logging.Service]
      sraas <- ZManaged.service[Sraas.Service]
      resources <- ytResources
    } yield new YtDataSinks[ProtoMessage[Unit], DynamicMessage](
      resources.yt,
      sraas,
      protoYsonConverter(sraas),
      FakeCommittableBatch,
      logging,
      NopSinkMetrics
    )).provideLayer(env ++ TestSraas.layer)

}

object YtDataSinksIntSpec {

  val oldVersion = Version("v0.0.3000")
  val newVersion = Version("v0.0.4000")

  object FakeCommittableBatch extends CommittableBatch[DynamicMessage, ProtoMessage[Unit]] {
    private val tp = TopicPartition("topic", 0)

    override def offsets(batch: DynamicMessage): Map[TopicPartition, OffsetRange] = Map.empty

    override def payload(batch: DynamicMessage): Iterable[ProtoMessage[Unit]] = {
      val isNew = batch.getAllFields.keySet().asScala.exists(_.getName == "favorites_delta")
      val schemaVersion = if (isNew) newVersion else oldVersion
      val messageType = "my.fake.Type"
      val id = "1"
      val day = 100
      val data = batch.toByteString
      Seq(ProtoMessage(Envelope(schemaVersion.toString, messageType, id, day, data), ()))
    }

    override def payloadWithOffset(batch: DynamicMessage): Iterable[PayloadOffset[ProtoMessage[Unit]]] =
      payload(batch).zipWithIndex.map { case (b, i) => PayloadOffset(b, TopicPartitionOffset(tp, i.toLong)) }

    override def weight(batch: DynamicMessage): Long = 1

    override def size(batch: DynamicMessage): Long = 1

    override def split(batch: DynamicMessage): Chunk[DynamicMessage] = Chunk(batch)
  }

  private val nopeToYsonConverter: ToYsonConverter[UIO, Unit, Unit] = new ToYsonConverter[UIO, Unit, Unit] {
    override def toSchema(meta: Unit): Seq[YtColumn] = Seq.empty

    override def toRow(msg: Unit): UIO[YTreeMapNode] = UIO(emptyMapNode)
  }

  def protoYsonConverter(sraas: Sraas.Service): ProtoYsonConverter[BTask, ProtoMessage[Unit]] =
    new SystemProtoYsonConverter[Unit, ProtoMessage[Unit]](
      sraas,
      new ProtoYsonConverterImpl[BTask](),
      nopeToYsonConverter
    )
}
