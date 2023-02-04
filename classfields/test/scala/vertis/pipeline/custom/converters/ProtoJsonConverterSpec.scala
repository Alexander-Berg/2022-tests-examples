package vertis.pipeline.custom.converters

import common.sraas.Sraas.SraasDescriptor
import common.sraas.{Sraas, TestSraas}
import io.circe.jawn.parseByteArray
import io.circe.parser.parse
import ru.yandex.kikimr.persqueue.compression.CompressionCodec
import ru.yandex.vertis.broker.model.internal.{Envelope, SystemInfo}
import vertis.logbroker.client.consumer.model.in.LbMessageData
import vertis.pipeline.test.Person
import vertis.stream.lb.convert.message.ProtoMessageJsonConverter
import vertis.zio.test.ZioSpecBase
import zio.{Task, UIO, ZIO}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class ProtoJsonConverterSpec extends ZioSpecBase {

  "proto json converter" should {
    "convert proto to json" in ioTest {
      val rei = Person.newBuilder().setId("p_0").setName("rei").build()
      (for {
        _ <- TestSraas.setJavaDescriptor(_ =>
          UIO(SraasDescriptor(Person.getDescriptor, Person.getDescriptor.getFullName, "v0.0.0"))
        )
        converter <- ZIO
          .service[Sraas.Service]
          .map(new ProtoMessageJsonConverter(_, CompressionCodec.RAW))
        jsonData <- converter.convert(toMessage(enveloped(rei)))
        json <- Task.fromEither(parseByteArray(jsonData.data))
        expected <- Task.fromEither {
          parse("""{
                  |"id": "p_0",
                  |"name":"rei",
                  |"friends": "0"}""".stripMargin)
        }
        _ <- check("json is valid")(
          json shouldBe expected
        )
        _ <- check("size is valid")(
          jsonData.uncompressedDataSize shouldBe expected.noSpaces.length
        )
      } yield ()).provideLayer(env ++ TestSraas.layer)
    }
  }

  private def toMessage(envelope: Envelope): LbMessageData =
    LbMessageData(
      envelope.toByteArray,
      0L,
      CompressionCodec.RAW
    )

  private def enveloped(person: Person): Envelope = Envelope(
    schemaVersion = "v0.0.0",
    messageType = Person.getDescriptor.getFullName,
    id = person.getId,
    data = person.toByteString,
    system = SystemInfo(
      createTimeMs = System.currentTimeMillis(),
      sourceId = "me"
    )
  )
}
