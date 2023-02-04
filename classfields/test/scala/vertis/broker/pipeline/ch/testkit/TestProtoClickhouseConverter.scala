package vertis.broker.pipeline.ch.testkit

import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor
import ru.yandex.vertis.proto.util.ProtoTypesUtil
import vertis.broker.pipeline.ch.sink.converter.{
  PreparedMessage,
  ProtoClickhouseConverter,
  ProtoClickhouseConverterImpl
}
import vertis.broker.pipeline.ch.sink.model.ClickhouseRow.Cells
import vertis.broker.pipeline.ch.sink.model.{ChProtoSchema, ClickhouseRow}
import vertis.clickhouse.model.ChSchema
import zio.Task

import scala.jdk.CollectionConverters._

/**
 * Test version of [[ProtoClickhouseConverter]], creates fake ch configs from proto descriptors
 * @author Ratskevich Natalia reimai@yandex-team.ru
 */
object TestProtoClickhouseConverter extends ProtoClickhouseConverter {

  /**
     * Just use all fields and thee first timestamp to find
     */
  def toSchema(descriptor: Descriptors.Descriptor): ChProtoSchema =
    toSchema(descriptor, firstTsField(descriptor), allFields(descriptor))

  def allFields(descriptor: Descriptors.Descriptor): Seq[String] = descriptor.getFields.asScala.map(_.getName).toSeq

  def firstTsField(descriptor: Descriptors.Descriptor): String = descriptor.getFields.asScala
    .find(f => f.getType == FieldDescriptor.Type.MESSAGE && ProtoTypesUtil.isTimestamp(f.getMessageType))
    .getOrElse(throw new IllegalArgumentException(s"No timestamp field in ${descriptor.getFullName}"))
    .getName

  override def toSchema(
      descriptor: Descriptors.Descriptor,
      tsColumnName: String,
      fieldsFilter: Seq[String]): ChProtoSchema =
    ProtoClickhouseConverterImpl.toSchema(descriptor, tsColumnName, fieldsFilter)

  /** return sorted list of columns names */
  override def toColumns(schema: ChProtoSchema): List[String] =
    ProtoClickhouseConverterImpl.toColumns(schema)

  /** return sorted list of columns values */
  override def toRow(input: PreparedMessage, schema: ChProtoSchema): ClickhouseRow =
    ProtoClickhouseConverterImpl.toRow(input, schema)

  /** use null for columns that are not in the current schema */
  override def fixEmptyColumns(rows: Seq[ClickhouseRow], schema: ChSchema): Seq[Cells] =
    ProtoClickhouseConverterImpl.fixEmptyColumns(rows, schema)
}
