package ru.yandex.vos2.autoru.model.extdata

import java.io.StringReader

import org.scalatest.funsuite.AnyFunSuite
import org.testcontainers.shaded.org.apache.commons.io.input.ReaderInputStream
import ru.yandex.vos2.autoru.utils.TestDataEngine
import ru.yandex.vos2.autoru.utils.geo.Tree

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 10/10/2019
  */
class LicensePlateValidationDictionaryTest extends AnyFunSuite {

  val data =
    """[{
               |    "name": "grz_validation",
               |    "fullName": "/vertis-moderation/autoru/grz_validation",
               |    "flushDate": "2019-09-09T14:29:05.244Z",
               |    "version": 12,
               |    "mime": "application/json; charset=utf-8",
               |    "content": {
               |   "geo_ids":{
               |      "1":{
               |         "text_waiting_for_photo":"Ой, на фотографиях в объявлении {offer_name} не видно госномера 😕 Без него мы не можем сравнить фото с описанием, всё проверить и с чистым сердцем отпустить объявление на сайт.\n\nНо это легко исправить. Сделайте снимок, на котором будут хорошо видны авто и его госномер, и добавьте в объявление. Номер скроется от чужих глаз нашим лого, не переживайте.\n\nНо можно ещё проще — пришлите этот снимок сюда, в чат. Тогда в объявлении оно не появится и его увидит только служба поддержки.\n\nВсё это нужно для того, чтобы на Авто.ру попадали только достоверные и качественные предложения. Надеемся, вы нас понимаете ❤️",
               |         "text_check_ok":"Спасибо за фотографию! Мы проверили фото, и всё в порядке. Вы — супер 🎉",
               |         "text_check_failed":"Мы проверили фото, но что-то не так 🌚 Давайте попробуем ещё раз — добавьте снимок в объявление или пришлите сюда. Может, сейчас повезёт.",
               |         "text_reminder":"Не забыли про фото? 🧐 Мы ещё ждём снимок автомобиля с госномером, чтобы проверить объявление. Пришлите его в чат или загрузите в объявление.",
               |         "sms_reminder":"Не забыли про фото? Мы ещё ждём снимок автомобиля с госномером, чтобы проверить объявление. Пришлите его в чат с техподдержкой или загрузите в объявление.",
               |         "sending_active":true,
               |         "hiding_vin_report_active":false
               |      },
               |      "default":{
               |         "text_waiting_for_photo":"Ой, на фотографиях в объявлении {offer_name} не видно госномера 😕 Без него мы не можем сравнить фото с описанием, всё проверить и с чистым сердцем отпустить объявление на сайт.\n\nНо это легко исправить. Сделайте снимок, на котором будут хорошо видны авто и его госномер, и добавьте в объявление. Номер скроется от чужих глаз нашим лого, не переживайте.\n\nНо можно ещё проще — пришлите этот снимок сюда, в чат. Тогда в объявлении оно не появится и его увидит только служба поддержки.\n\nВсё это нужно для того, чтобы на Авто.ру попадали только достоверные и качественные предложения. Надеемся, вы нас понимаете ❤️",
               |         "text_check_ok":"Спасибо за фотографию! Мы проверили фото, и всё в порядке. Вы — супер 🎉",
               |         "text_check_failed":"Мы проверили фото, но что-то не так 🌚 Давайте попробуем ещё раз — добавьте снимок в объявление или пришлите сюда. Может, сейчас повезёт.",
               |         "text_reminder":"Не забыли про фото? 🧐 Мы ещё ждём снимок автомобиля с госномером, чтобы проверить объявление. Пришлите его в чат или загрузите в объявление.",
               |         "sms_reminder":"Не забыли про фото? Мы ещё ждём снимок автомобиля с госномером, чтобы проверить объявление. Пришлите его в чат с техподдержкой или загрузите в объявление.",
               |         "sending_active":true,
               |         "hiding_vin_report_active":false
               |      }
               |   },
               |   "vin_report_error_text":"Мы не смогли проверить, что VIN в объявлении принадлежит автомобилю на фотографиях, поэтому отчёта нет. Будьте внимательны."
               |}
               |}]""".stripMargin
  test("parse json from bunker") {
    LicensePlateValidationDictionary.parse(new ReaderInputStream(new StringReader(data), "utf-8"))
  }

  test("get settings by region") {
    val dict = LicensePlateValidationDictionary.parse(new ReaderInputStream(new StringReader(data), "utf-8"))
    assert(dict.getSettings(1, Tree.from(TestDataEngine)).sendingActive)
  }
}
