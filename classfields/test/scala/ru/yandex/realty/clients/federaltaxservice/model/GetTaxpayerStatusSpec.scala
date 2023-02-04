package ru.yandex.realty.clients.federaltaxservice.model

import play.api.libs.json.Json
import ru.yandex.realty.SpecBase

import java.time.LocalDate

class GetTaxpayerStatusSpec extends SpecBase {
  "GetTaxpayerStatus" when {
    "Request" should {
      "serialise to json" in {
        val obj = GetTaxpayerStatus.Request("123456789", LocalDate.of(2022, 2, 4))
        Json.toJson(obj).toString should be("""{"inn":"123456789","requestDate":"2022-02-04"}""")
      }
    }
    "Response" should {
      "deserialize from json" in {
        val text =
          """{
            |  "status":false,
            |  "message":"123456789 не является плательщиком налога на профессиональный доход"
            |}""".stripMargin
        val res = Json.parse(text).as[GetTaxpayerStatus.Response]
        res should be(
          GetTaxpayerStatus
            .Response(status = false, "123456789 не является плательщиком налога на профессиональный доход")
        )
      }
    }
    "ErrorResponse" should {
      "deserialize from json when message is not empty" in {
        val text =
          """{
            |  "code": "taxpayer.status.service.limited.error",
            |  "message": "Превышено количество запросов к сервису с одного ip-адреса в единицу времени, пожалуйста, попробуйте позднее"
            |}""".stripMargin
        val res = Json.parse(text).as[GetTaxpayerStatus.ErrorResponse]
        res should be(
          GetTaxpayerStatus.ErrorResponse(
            "taxpayer.status.service.limited.error",
            Some(
              "Превышено количество запросов к сервису с одного ip-адреса в единицу времени, пожалуйста, попробуйте позднее"
            )
          )
        )
      }
      "deserialize from json when message is empty" in {
        val text =
          """{
            |  "code": "taxpayer.status.service.limited.error"
            |}""".stripMargin
        val res = Json.parse(text).as[GetTaxpayerStatus.ErrorResponse]
        res should be(
          GetTaxpayerStatus.ErrorResponse("taxpayer.status.service.limited.error", None)
        )
      }
    }
  }
}
