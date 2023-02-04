package vertis.pipeline.convert

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Message
import common.zio.clients.clickhouse.jdbc.ClickhouseJdbcClient.ClickhouseJdbcClient
import common.zio.clients.clickhouse.jdbc.{ClickhouseError, ClickhouseJdbcClient, ClickhouseJdbcClientLive}
import common.zio.clients.clickhouse.testkit.TestTransactor
import common.zio.logging.Logging
import common.zio.ops.prometheus.Prometheus
import common.zio.ops.tracing.testkit.TestTracing
import org.scalacheck.Gen
import vertis.broker.pipeline.ch.batching.TestBatching
import vertis.broker.pipeline.ch.sink.ChWriter
import vertis.broker.pipeline.ch.sink.conf.ChSinkConfig
import vertis.broker.pipeline.ch.sink.converter.{
  PreparedMessage,
  ProtoClickhouseConverter,
  ProtoClickhouseConverterImpl
}
import vertis.broker.pipeline.ch.sink.evolution.ChSchemaEvolutionImpl
import vertis.broker.pipeline.ch.sink.model.{ChProtoSchema, ChTable, ChTargetTable}
import vertis.broker.pipeline.ch.sink.queries.ClickhouseQueries
import vertis.broker.pipeline.ch.testkit.TestProtoClickhouseConverter
import vertis.broker.pipeline.ch.util.ChOffsets
import vertis.clickhouse.model.ChSchema
import vertis.stream.conf.BatchingConf
import vertis.stream.model.TopicPartition
import vertis.zio.BaseEnv
import zio._
import zio.blocking.Blocking
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec}

import java.util.UUID

/**
 * @author kusaeva
 * @author reimai
 */
abstract class ClickhouseTestSpec extends DefaultRunnableSpec with TestBatching {

  protected val converter: ProtoClickhouseConverter = ProtoClickhouseConverterImpl

  override def spec: ZSpec[TestEnvironment, Any] = {
    chSpec.provideCustomLayerShared {
      (Blocking.live ++ TestTracing.noOp >>> TestTransactor.live >>>
        ClickhouseJdbcClientLive.live ++ Prometheus.live ++ Logging.live).orDie
    }
  }

  protected def chSpec: ZSpec[BaseEnv with ClickhouseJdbcClient, Any]

  case class ChTest(
      client: ClickhouseJdbcClient.Service,
      writer: ChWriter,
      descriptor: Descriptor,
      sinkConf: ChSinkConfig,
      schemaEvolution: ChSchemaEvolutionImpl) {

    lazy val getSchema: ChProtoSchema = converter.toSchema(descriptor, sinkConf.timestampField, sinkConf.fields)
    lazy val getTable: ChTable = ChTable.build(sinkConf, getSchema)
    lazy val offsets: ChOffsets = new ChOffsets(client)

    def checkColumns(expectedNames: String*) = {
      val names = schemaEvolution.getActualColumns(sinkConf.id).map(_.map(_.name))
      zio.test.assertM(names)(equalTo(expectedNames))
    }

    def checkOffsets(expected: Map[TopicPartition, Long]) =
      offsets.listAllOffsets(sinkConf.offsetView, expected.keySet).map { offsets =>
        zio.test.assert(offsets)(equalTo(expected))
      }

    def writeRandom(partition: TopicPartition, offsets: Ref[Int]): RIO[BaseEnv, Unit] =
      for {
        n <- zio.random.nextIntBounded(100).map(_ + 1)
        startOffset <- offsets.get
        schema = getSchema
        batch <- createBatch(schema, descriptor, partition, n, startOffset.toLong)
        _ <- writer.write(batch)
        _ <- offsets.set(startOffset + n)
      } yield ()

    val dropAll: IO[ClickhouseError, Unit] =
      client.executeQuery(ClickhouseQueries.drop(sinkConf.id)) *>
        client.executeQuery(ClickhouseQueries.drop(sinkConf.offsetView))
  }

  protected def genPreparedMessage(gen: Gen[Message], tp: TopicPartition, o: Long): Gen[PreparedMessage] =
    gen.map(msg => PreparedMessage(msg = msg, id = UUID.randomUUID().toString, topicPartition = tp, offset = o))

  protected def createResources(
      descriptor: Descriptor,
      client: ClickhouseJdbcClient.Service,
      evolution: ChSchemaEvolutionImpl,
      schema: ChSchema,
      tableName: String): ChTest = {
    val sinkConf = ChSinkConfig(
      target = ChTargetTable("local", getClass.getSimpleName, tableName),
      messageType = descriptor.getFullName,
      primaryKeys = Seq.empty,
      fields = TestProtoClickhouseConverter.allFields(descriptor),
      timestampField = TestProtoClickhouseConverter.firstTsField(descriptor),
      expireInDays = Some(30),
      batching = BatchingConf(maxSize = 5L)
    )
    val writer = new ChWriter(sinkConf, schema, client)
    ChTest(client, writer, descriptor, sinkConf, evolution)
  }

  protected def chTestM(
      descriptor: Descriptor,
      tableName: String = ""): RManaged[BaseEnv with ClickhouseJdbcClient, ChTest] =
    for {
      client <- ZIO.environment[ClickhouseJdbcClient].map(_.get).toManaged_
      evolution <- ChSchemaEvolutionImpl.create(client).toManaged_
      schema = TestProtoClickhouseConverter.toSchema(descriptor)
    } yield createResources(
      descriptor,
      client,
      evolution,
      schema,
      Option(tableName).filter(_.nonEmpty).getOrElse(descriptor.getName)
    )

  protected def samplePartition(idx: Int = 0) = TopicPartition(topic = "sas//test/topic", partition = idx)
}
