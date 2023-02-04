package vertis.proto.converter

import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.Timestamps
import ru.vertis.holocron.common.holo_car_offer.HoloCarOffer
import ru.yandex.inside.yt.kosher.impl.common.http.EmptyCompressor
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import vertis.proto.converter.ProtoYsonConvertAndWriteYtIntSpec._
import vertis.proto.converter.test._
import vertis.yt.model.{YtSchema, YtTable}
import vertis.yt.zio.YtZioTest
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.BaseEnv
import zio._

import java.time.Instant

/** @author kusaeva
  */
class ProtoYsonConvertAndWriteYtIntSpec extends YtZioTest with YtTableTestHelper {

  private val basePath = testBasePath

  "ProtoYsonConvertAndWriteYtIntSpec" should {
    descriptors.foreach { descriptor =>
      descriptor.getName in {
        ioTest {
          ytZio.use { yt =>
            val emptyMessage = DynamicMessage.newBuilder(descriptor).build
            val randomMessages = RandomProtobufGenerator
              .genFor(DynamicMessage.getDefaultInstance(descriptor), maxRep = repeatedCount)
              .next(messagesCount)
            val rows = randomMessages ++ Seq(emptyMessage)
            createTable(yt, tableName = descriptor.getName, descriptor, basePath, rows = rows)
          }
        }
      }
    }

    "trim long timestamps" in ioTest {
      checkFailed[BaseEnv, Throwable, IllegalArgumentException] {
        ytZio.use { yt =>
          testTimestampTrimmed(yt, "2222-02-03T12:42:12Z")
        }
      }
    }

    "trim prehistoric timestamps" in ioTest {
      checkFailed[BaseEnv, Throwable, IllegalArgumentException] {
        ytZio.use { yt =>
          testTimestampTrimmed(yt, "1222-02-03T12:42:12Z")
        }
      }
    }

    "create table with schema" in {
      ioTest {
        ytZio.use { yt =>
          val descriptor = HoloCarOffer.javaDescriptor
          val tableName = descriptor.getName
          val schema = converter.toSchema(descriptor)
          val table = YtTable(tableName, basePath.child(tableName), YtSchema(schema))
          for {
            _ <- yt.cypress.createTable(None, table)
            schema2 <- yt.tables.getSchemaWithHints(None, table.path)
            newTable = basePath.child("temp")
            _ <- yt.cypress.createTable(None, YtTable(newTable.name, newTable, schema2))
          } yield ()
        }
      }
    }
  }

  /** Verify a successful append to yt table
    */
  private def testTimestampTrimmed(yt: YtZio, timestamp: String): RIO[BaseEnv, Unit] = {
    val descriptor = WithTimestamp.javaDescriptor
    val ts = Instant.parse(timestamp).toEpochMilli
    val msg = WithTimestamp.defaultInstance.copy(ts =
      Some(com.google.protobuf.timestamp.Timestamp.fromJavaProto(Timestamps.fromMillis(ts)))
    )
    val tableName = "ts_test_table"
    val schema = converter.toSchema(descriptor)
    val table = YtTable(tableName, basePath.child(tableName), YtSchema(schema))
    for {
      messages <- Task(Seq(msg).map(m => converter.toRow(DynamicMessage.parseFrom(descriptor, m.toByteString))))
      entries <- ZIO.collectAll(messages)
      _ <- yt.cypress.createTable(None, table)
      _ <- logger.info(s"Created table $tableName")
      _ <- yt.tables.appendToTable(None, YTableEntryTypes.YSON)(
        basePath.child(tableName),
        entries.iterator,
        new EmptyCompressor()
      )
      _ <- logger.info(s"Appended ${entries.size} entries to table $tableName")
    } yield ()
  }
}

object ProtoYsonConvertAndWriteYtIntSpec {
  private val messagesCount = 5
  private val repeatedCount = 3

  private lazy val descriptors = Seq(
    SimpleMessage.javaDescriptor,
    WithMessage.javaDescriptor,
    WithEnum.javaDescriptor,
    WithEnumInProto.javaDescriptor,
    WithTimestamp.javaDescriptor,
    WithWrapped.javaDescriptor,
    WithAllWrapped.javaDescriptor,
    WithOneOf.javaDescriptor,
    WithOneOfMessage.javaDescriptor,
    WithRepeatedMessage.javaDescriptor,
    WithRepeatedPrimitive.javaDescriptor,
    WithRepeatedWrappedPrimitive.javaDescriptor,
    WithRepeatedEnum.javaDescriptor,
    WithRepeatedEnumMessage.javaDescriptor,
    WithMap.javaDescriptor,
    WithMapOfEnum.javaDescriptor,
    WithMapOfEnumMessage.javaDescriptor,
    WithMapOfWrapped.javaDescriptor,
    WithMapOfMessage.javaDescriptor,
    TypesMessage.javaDescriptor,
    TestMessage.javaDescriptor,
    BytesMessage.javaDescriptor
  )
}
