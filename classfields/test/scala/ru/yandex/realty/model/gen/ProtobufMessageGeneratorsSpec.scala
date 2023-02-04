package ru.yandex.realty.model.gen

import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{Message, Struct}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.response.Response.OfferResponse
import ru.yandex.realty.SpecBase
import ru.yandex.realty.api.ProtoResponse.ErrorResponse
import ru.yandex.vertis.paging.Slicing
import ru.yandex.vertis.protobuf.ProtoInstanceProvider

@RunWith(classOf[JUnitRunner])
class ProtobufMessageGeneratorsSpec extends SpecBase with ProtobufMessageGenerators with ProtoInstanceProvider {

  private def toJson(m: Message): String =
    JsonFormat.printer().print(m)

  "ProtobufMessageGenerators" should {
    "generate simple messages" in {
      val slicing = generate[Slicing]().next
      info("result: " + toJson(slicing))
    }
    "generate complex messages" in {
      val errorResponse = generate[ErrorResponse]().next
      info("result: " + toJson(errorResponse))
    }
    "generate empty structs" in {
      val struct = generate[Struct]().next
      info("result: " + toJson(struct))
    }
    "generate OfferResponse" in {
      val or = generate[OfferResponse]().next
      info("result: " + toJson(or))
    }
    "reuse the same generator" in {
      val gen = generate[ErrorResponse]()

      val first = gen.next
      val second = gen.next

      info("first: " + toJson(first))
      info("second: " + toJson(second))
    }
  }
}
