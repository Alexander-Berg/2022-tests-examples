package ru.yandex.vertis.stream.yt.init

import com.google.protobuf.DynamicMessage
import common.yt.schema.YtSortOrders
import ru.yandex.inside.yt.kosher.impl.common.http.EmptyCompressor
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import vertis.proto.converter.ProtoYsonConverterImpl
import vertis.proto.converter.test.SimpleMessage
import vertis.yt.model.{YtColumn, YtSchema, YtTable}
import vertis.yt.zio.YtZioTest
import vertis.zio.BTask
import zio.ZIO
import zio.interop.catz._

/** Test for [[vertis.yt.zio.wrappers.YtZioTables]].
  * It lives here because it is easier to test it using ProtoYsonConverter
  */
class YtZioTablesIntSpec extends YtZioTest {

  private val converter = new ProtoYsonConverterImpl[BTask]()

  private def createTable(tableName: String, schema: Seq[YtColumn]): YtTable =
    YtTable(tableName, testBasePath.child(tableName), YtSchema(schema))

  "YtZioTables.removeSorted" should {
    "make table not sorted" in ioTest {
      ytZio.use { yt =>
        val descriptor = SimpleMessage.javaDescriptor
        val tableName = descriptor.getName
        for {
          messages <- ZIO.foreach {
            RandomProtobufGenerator
              .genForScala[SimpleMessage]
              .next(10)
              .map(msg => DynamicMessage.parseFrom(descriptor, msg.toByteArray))
          }(converter.toRow)
          schema = converter.toSchema(descriptor)
          // create table with sorted fields
          modifiedSchema = schema.map(_.copy(sortOrder = Some(YtSortOrders.ascending)))
          table = createTable(tableName, modifiedSchema)
          _ <- yt.cypress.createTable(None, table)
          _ <- yt.tables.makeNotSorted(None, table.path)
          _ <- yt.tables.appendToTable(None, YTableEntryTypes.YSON)(
            table.path,
            messages.iterator,
            new EmptyCompressor()
          )
        } yield ()
      }
    }
  }
}
