package ru.yandex.vertis.chat.service.support.client

import org.scalatest.{FunSuite, Matchers, OptionValues}
import play.api.libs.json.Json
import ru.yandex.vertis.chat.common.techsupport.TechSupport

/**
  * TODO
  *
  * @author aborunov
  */
class TechSupportSpec extends FunSuite with Matchers with OptionValues {
  test("jivosite messages") {
    val json1 =
      """{"recipient":{"id":"user:19437983"},"sender":{"name":"Алина"},"message":{"type":"text",""" +
        """"text":"яху","id":"111"}}"""
    val req1 = Json.parse(json1).as[TechSupport.Request]
    req1.sender should be(defined)
    req1.message.flatMap(_.id).value shouldBe "111"

    val json2 = """{"recipient":{"id":"user:16577469"},"message":null}"""
    val req2 = Json.parse(json2).as[TechSupport.Request]
    req2.recipient should be(defined)
    req2.message should be(empty)
    req2.sender should be(empty)

    val json3 =
      """{"sender":{"id":"user:19437983", "name":"Никанор (user:19437983)", "phone":"79293334455"},""" +
        """ "message":{"type":"text", "text":"Дратути"}}"""
    val req3 = Json.parse(json3).as[TechSupport.Request]
    req3.recipient should be(empty)
    req3.message should be(defined)
    req3.message.flatMap(_.id) should be(empty)
    req3.sender should be(defined)

    val json4 =
      """{"sender":{"id":"user:19437983", "name":"Никанор (user:19437983)", "phone":"79293334455"},""" +
        """ "message":{"type":"text", "text":"Дратути", "id":null}}"""
    val req4 = Json.parse(json4).as[TechSupport.Request]
    req4.recipient should be(empty)
    req4.message should be(defined)
    req4.message.flatMap(_.id) should be(empty)
    req4.sender should be(defined)

    val json5 =
      """{"sender":{"id":"user:19437983", "name":"Никанор (user:19437983)", "phone":"79293334455"},""" +
        """ "message":{"type":"typeout"}}"""
    val req5 = Json.parse(json5).as[TechSupport.Request]
    req5.recipient should be(empty)
    req5.message should be(defined)
    req5.message.flatMap(_.id) should be(empty)
    req5.sender should be(defined)
  }

  test("jivosite subjects") {
    val req1 = techSupportRequestFromJson("""|{
         |  "recipient":{"id":"user:19437983"},
         |  "sender":{"name":"Александра","email":"blazhievskaya@yandex-team.ru"},
         |  "message":{"type":"text","text":"яху","id":"111"}
         |}""".stripMargin)
    req1.hasSubjects shouldBe false

    val req2 = techSupportRequestFromJson(
      """|{
         |  "recipient":{"id":"user:19437983"},
         |  "sender":{"name":"Александра","email":"blazhievskaya@yandex-team.ru"},
         |  "message":{"type":"text","text":"яху ###Деньги:другой_вопрос ###Деньги:Возврат","id":"111"}
         |}""".stripMargin
    )
    req2.hasSubjects shouldBe true

    req2.asSubjects should be(defined)
  }

  test("empty jivosite subjects") {
    val req1 = techSupportRequestFromJson(
      """|{
         |  "recipient":{"id":"user:19437983"},
         |  "sender":{"name":"Александра","email":"blazhievskaya@yandex-team.ru"},
         |  "message":{"type":"text","text":"яху ###Деньги:другой_вопрос ###Деньги:Возврат ###","id":"111"}
         |}""".stripMargin
    )
    req1.hasSubjects shouldBe true
    req1.asSubjects should be(defined)
    req1.asSubjects.value.subjects.length shouldBe 2

    val req2 = techSupportRequestFromJson("""|{
         |  "recipient":{"id":"user:19437983"},
         |  "sender":{"name":"Александра","email":"blazhievskaya@yandex-team.ru"},
         |  "message":{"type":"text","text":"яху Деньги:Возврат ###","id":"111"}
         |}""".stripMargin)
    req2.hasSubjects shouldBe true
    req2.asSubjects should be(empty)
  }

  test("no spaces between subjects") {
    val req1 =
      techSupportRequestFromJson("""|{
         |  "recipient":{"id":"user:19437983"},
         |  "sender":{"name":"Александра","email":"blazhievskaya@yandex-team.ru"},
         |  "message":{
         |    "type":"text",
         |    "text":"яху пыщь ###Деньги:другой_вопрос###Деньги:Возврат 100500###Пиупиу 11",
         |    "id":"111"}
         |}""".stripMargin)
    req1.hasSubjects shouldBe true
    req1.asSubjects should be(defined)
    req1.asSubjects.value.subjects.length shouldBe 3
    req1.asSubjects.value.subjects should contain("Деньги:другой_вопрос")
    req1.asSubjects.value.subjects should contain("Деньги:Возврат")
    req1.asSubjects.value.subjects should contain("Пиупиу")
  }

  test("\n in subjects") {
    val req1 = techSupportRequestFromJson("""|{
         |  "recipient":{"id":"user:19437983"},
         |  "sender":{"name":"Александра","email":"blazhievskaya@yandex-team.ru"},
         |  "message":{
         |    "type":"text",
         |    "text":"###Блокировки:другой_вопрос\nЗдравствуйте",
         |    "id":"111"}
         |}""".stripMargin)
    req1.hasSubjects shouldBe true
    req1.asSubjects should be(defined)
    req1.asSubjects.value.subjects.length shouldBe 1
    req1.asSubjects.value.subjects should contain("Блокировки:другой_вопрос")
  }

  private def techSupportRequestFromJson(json: String): TechSupport.Request = {
    Json.parse(json).as[TechSupport.Request]
  }
}
