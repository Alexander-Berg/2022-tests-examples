package ru.yandex.vos2.autoru.model.extdata

import java.io.StringReader

import org.scalatest.funsuite.AnyFunSuite
import org.testcontainers.shaded.org.apache.commons.io.input.ReaderInputStream

class BannedRevalidationDictionaryTest extends AnyFunSuite {

  private val data: String =
    """|[
       |  {
       |    "name": "banned_revalidation",
       |    "fullName": "/vertis-moderation/autoru/banned_revalidation",
       |    "flushDate": "2022-02-02T11:59:44.314Z",
       |    "version": 4,
       |    "mime": "application/json; charset=utf-8",
       |    "content": {
       |      "default": {
       |        "text_chat_complete_check_failed": "Модераторы проверили ваше объявление о продаже МАРКА / МОДЕЛЬ, но в нём всё ещё есть ошибки 😔 Пожалуйста, исправьте их в соответствии с <a href='https://yandex.ru/support/autoru/car-rules-ads.html'>правилами</a>.\n\nЕсли возникли вопросы, или что-то осталось непонятным, пожалуйста, напишите в этот чат, мы поможем.️",
       |        "text_sms_complete_check_failed": "Мы проверили ваше объявление о продаже МАРКА / МОДЕЛЬ, но в нем остались ошибки. Подробности в личном кабинете: auth.auto.ru/login",
       |        "sender_template_complete_check_failed": "moderation.banned_revalidation_unsuccess",
       |        "sending_active": true
       |      }
       |    }
       |  }
       |]""".stripMargin
  test("parse json from bunker") {
    val expected = BannedRevalidationDictionary(
      textChatCompleteCheckFailed = Some(
        "Модераторы проверили ваше объявление о продаже МАРКА / МОДЕЛЬ, но в нём всё ещё есть ошибки 😔 Пожалуйста, исправьте их в соответствии с <a href='https://yandex.ru/support/autoru/car-rules-ads.html'>правилами</a>.\n\nЕсли возникли вопросы, или что-то осталось непонятным, пожалуйста, напишите в этот чат, мы поможем.️"
      ),
      textSmsCompleteCheckFailed = Some(
        "Мы проверили ваше объявление о продаже МАРКА / МОДЕЛЬ, но в нем остались ошибки. Подробности в личном кабинете: auth.auto.ru/login"
      ),
      senderTemplateCompleteCheckFailed = Some("moderation.banned_revalidation_unsuccess"),
      sendingActive = true
    )
    val actual =
      BannedRevalidationDictionary.parse(new ReaderInputStream(new StringReader(data), "utf-8"))
    assert(actual == expected)
  }
}
