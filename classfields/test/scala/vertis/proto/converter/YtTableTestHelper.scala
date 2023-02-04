package vertis.proto.converter

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.util.Timestamps
import com.google.protobuf.{DynamicMessage, Message}
import common.yt.schema.YtSortOrders
import org.scalacheck.Gen
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.common.http.EmptyCompressor
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.vertis.proto.util.scalaPb.ScalaPbHelp
import ru.yandex.vertis.proto.util.scalaPb.ScalaPbHelp.ScalaPbMessage
import ru.yandex.vertis.proto.util.{ProtoTypesUtil, RandomProtobufGenerator}
import scalapb.GeneratedMessageCompanion
import vertis.yt.model.attributes.YtAttribute
import vertis.yt.model.{YtSchema, YtTable}
import vertis.yt.zio.YtZioTest
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.{BTask, BaseEnv}
import zio.interop.catz._
import zio.{RIO, Task, ZIO}

import java.time.Instant
import scala.reflect.runtime.universe

/**  Get your simple yt table alive in no time
  */
trait YtTableTestHelper { self: YtZioTest =>

  protected val converter: ProtoYsonConverter[BTask, Message] = new ProtoYsonConverterImpl[BTask]()

  protected def messagesGen[T <: ScalaPbMessage[T]: GeneratedMessageCompanion: universe.TypeTag]: Gen[T] =
    RandomProtobufGenerator.genForScala[T]

  protected val tmpPath: YPath = testBasePath

  protected def createTable(
      yt: YtZio,
      tableName: String,
      descriptor: Descriptor,
      basePath: YPath = tmpPath,
      rows: Iterable[Message] = Nil,
      attrs: Seq[YtAttribute] = Nil,
      sortBy: Seq[String] = Nil): BTask[YtTable] = {
    createTable(yt, basePath.child(tableName), rows, descriptor, attrs, sortBy)
  }

  protected def createTable(
      yt: YtZio,
      path: YPath,
      rows: Iterable[Message],
      descriptor: Descriptor,
      attrs: Seq[YtAttribute],
      sortBy: Seq[String]): BTask[YtTable] = {
    val schema = converter.toSchema(descriptor).map { column =>
      if (sortBy.contains(column.name)) {
        column.copy(sortOrder = Some(YtSortOrders.ascending))
      } else {
        column
      }
    }
    val tableDef = YtTable("test", path, YtSchema(schema), attributes = attrs)
    yt.cypress.createTable(None, tableDef) *>
      ZIO
        .when(rows.nonEmpty) {
          appendToTable(yt, rows, path, descriptor)
        }
        .as(tableDef)
  }

  protected def appendToTable(
      yt: YtZio,
      rows: Iterable[Message],
      path: YPath,
      descriptor: Descriptor): RIO[BaseEnv, Unit] =
    for {
      rows <- ZIO.foreach(
        rows.toSeq.map(msg => DynamicMessage.parseFrom(descriptor, msg.toByteArray))
      )(converter.toRow)
      _ <- yt.tables.appendToTable(None, YTableEntryTypes.YSON)(
        path,
        rows.iterator,
        new EmptyCompressor()
      )
    } yield ()

  protected def readTable[T <: ScalaPbMessage[T]: GeneratedMessageCompanion: universe.TypeTag](
      yt: YtZio,
      path: YPath): BTask[Seq[T]] =
    yt.tables.readAllToYson(None, path).flatMap { rows =>
      ZIO.foreach(rows)(v => Task(parseYsonToProto[T](v)))
    }

  private def parseYsonToProto[T <: ScalaPbMessage[T]: GeneratedMessageCompanion: universe.TypeTag](
      el: YTreeMapNode): T = {
    val builder = ScalaPbHelp.toDynamic(ScalaPbHelp.defaultInstance[T]).toBuilder
    ScalaPbHelp.descriptorForScalaMessage[T].getFields.forEach { fd =>
      val fieldName = fd.getName
      if (el.getFilterNull(fieldName).isPresent) {
        fd.getJavaType match {
          case JavaType.STRING =>
            builder.setField(fd, el.getString(fieldName))
          case JavaType.ENUM =>
            val enumValue = fd.getEnumType.findValueByName(el.getString(fieldName))
            builder.setField(fd, enumValue)
          case JavaType.MESSAGE if ProtoTypesUtil.isTimestamp(fd.getMessageType) =>
            val ms = el.getLong(fieldName) / 1000
            builder.setField(fd, Timestamps.fromMillis(ms))
          case _ =>
        }
        ()
      }
    }
    ScalaPbHelp.fromDynamic(builder.build())
  }

  protected def toTimestamp(t: Instant): Timestamp =
    Timestamp.fromJavaProto(Timestamps.fromMillis(t.toEpochMilli))
}
