package vertis.broker.pipeline.ch.batching

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.DynamicMessage
import org.scalacheck.Gen
import ru.yandex.vertis.broker.model.internal.{Envelope, SystemInfo}
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import vertis.broker.pipeline.ch.sink.converter.{PreparedMessage, ProtoClickhouseConverterImpl}
import vertis.broker.pipeline.ch.sink.model.{ChProtoSchema, ClickhouseRow}
import vertis.pipeline.convert.ChMessage
import vertis.stream.batch.{Batcher, ConvertedBatch}
import vertis.stream.model._
import zio._
import zio.stream.ZStream

import java.util.UUID

/** @author kusaeva
  */
trait TestBatching {

  case class Meta(descriptor: Descriptor, offset: Long, tp: TopicPartition, schemaVersion: String)

  protected val schemaVersions = Seq("v0.0.1", "v0.1.0", "v1.0.0", "v2.0.0")

  private val converter = ProtoClickhouseConverterImpl

  protected def genMessages(descriptor: Descriptor, count: Int): Seq[DynamicMessage] = {
    val instance = DynamicMessage.getDefaultInstance(descriptor)
    RandomProtobufGenerator
      .genFor(instance, maxRep = 3)
      .next(count)
      .toSeq
  }

  protected def getPMessages(
      messages: Seq[DynamicMessage],
      tp: TopicPartition,
      offsetFrom: Long,
      schemaVersion: Option[String] = None): Seq[ProtoMessage[Meta]] =
    messages.zipWithIndex
      .map { case (m, idx) => toPMessage(m, offsetFrom + idx, tp, schemaVersion) }

  protected def genPMessages(
      descriptor: Descriptor,
      count: Int,
      tp: TopicPartition,
      offsetFrom: Long): Seq[ProtoMessage[Meta]] =
    getPMessages(genMessages(descriptor, count), tp, offsetFrom)

  protected def toPMessage(
      msg: DynamicMessage,
      offset: Long,
      tp: TopicPartition,
      schemaVersion: Option[String] = None): ProtoMessage[Meta] = {
    val version = schemaVersion.getOrElse(Gen.oneOf(schemaVersions).next)
    ProtoMessage(
      envelope = enveloped(msg, schemaVersion = version.toString),
      sourceMeta = Meta(msg.getDescriptorForType, offset, tp, version)
    )
  }

  protected def toChMessage(schema: ChProtoSchema)(p: ProtoMessage[Meta]): Task[ChMessage] = {
    import p.sourceMeta._
    for {
      m <- PreparedMessage.create(p, descriptor, tp, offset)
      rowSchema = converter.toSchema(descriptor, schema.timestampColumnName, schema.columns.map(_.name))
      row = converter.toRow(m, rowSchema)
    } yield ChMessage(row, tp, offset, schemaVersion)
  }

  private def enveloped(msg: DynamicMessage, schemaVersion: String): Envelope =
    Envelope(
      schemaVersion = schemaVersion,
      messageType = msg.getDescriptorForType.getFullName,
      id = UUID.randomUUID().toString,
      data = msg.toByteString,
      system = SystemInfo(
        createTimeMs = System.currentTimeMillis(),
        sourceId = "me"
      )
    )

  def createBatchFromPMessages(
      schema: ChProtoSchema,
      messages: Seq[ProtoMessage[Meta]]): Task[ConvertedBatch[ClickhouseRow]] = {
    val batcher = Batcher.byCount[ClickhouseRow](messages.size.toLong)
    ZIO.foreach(messages)(toChMessage(schema)) >>= { chMessages =>
      ZStream
        .fromIterator(chMessages.iterator)
        .aggregate(batcher.transducer)
        .runHead
        .map(_.get)
    }
  }

  def createBatch(
      schema: ChProtoSchema,
      descriptor: Descriptor,
      partition: TopicPartition,
      batchSize: Int,
      offsetsFrom: Long = 0): Task[ConvertedBatch[ClickhouseRow]] =
    createBatchFromPMessages(schema, getPMessages(genMessages(descriptor, batchSize), partition, offsetsFrom))
}
