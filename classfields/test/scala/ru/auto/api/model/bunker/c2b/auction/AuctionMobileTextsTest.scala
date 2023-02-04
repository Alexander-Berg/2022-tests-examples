package ru.auto.api.model.bunker.c2b.auction

import auto.c2b.common.MobileTextsOuterClass._
import org.scalatest.funsuite.AnyFunSuite

import java.io.ByteArrayInputStream
import scala.jdk.CollectionConverters._

class AuctionMobileTextsTest extends AnyFunSuite {

  test("parse C2bAuctionMobileTexts from test json") {
    val in = new ByteArrayInputStream(
      """
        |[
        |  {
        |    "fullName": "/auto_ru/c2b_auction/mobile_texts/form",
        |    "content": {
        |      "buyback": {
        |        "title": "Поможем продать ваше авто за",
        |        "description": "Бесплатно и не обязывает продавать авто",
        |        "items": [
        |          {
        |            "title": "Показываете авто 1 раз",
        |            "subtitle": "Специалист Авто.ру приедет, куда скажете",
        |            "icon": "https://yastatic.net/s3/..."
        |          },
        |          {
        |            "title": "Получаете финальную цену после осмотра",
        |            "subtitle": "Торговаться не придется",
        |            "icon": "https://yastatic.net/s3/..."
        |          },
        |          {
        |            "title": "Оформляете сделку в салоне дилера",
        |            "subtitle": "Деньги – сразу, наличными или на карту",
        |            "icon": "https://yastatic.net/s3/..."
        |          }
        |        ]
        |      },
        |      "checkupClaim": {
        |        "description": "Позвоним в течение часа, чтобы договориться о месте и времени осмотра.",
        |        "time": "Работаем с 10:00 до 19:00",
        |        "place": "Проводим осмотры в пределах 20 км от МКАД"
        |      },
        |      "successClaim": {
        |        "title": "Как подготовиться к осмотру",
        |        "description": "Ваше объявление не будет опубликовано на Авто.ру, пока проходит осмотр автомобиля.",
        |        "items": [
        |          {
        |            "title": "Помыть машину — это увеличит финальную цену"
        |          },
        |          {
        |            "title": "Взять оригиналы: паспорт, ПТС, СТС"
        |          },
        |          {
        |            "title": "Найти второй ключ"
        |          },
        |          {
        |            "title": "Подготовить комплект сезонной резины, если есть"
        |          }
        |        ]
        |      }
        |    }
        |  },
        |  {
        |    "fullName": "/auto_ru/c2b_auction/mobile_texts/listing",
        |    "content": {
        |      "mobilePhone": "+7 921 635-85-31"
        |    }
        |  }
        |]
        |""".stripMargin.getBytes("UTF-8")
    )
    val parsedMobileTexts = C2bAuctionMobileTexts.parse(in)

    val mobileForms = {
      val buyback =
        Buyback.newBuilder
          .setTitle("Поможем продать ваше авто за")
          .setDescription("Бесплатно и не обязывает продавать авто")
          .addAllItems(
            Seq(
              BuybackItem
                .newBuilder()
                .setTitle("Показываете авто 1 раз")
                .setSubtitle("Специалист Авто.ру приедет, куда скажете")
                .setIcon("https://yastatic.net/s3/...")
                .build(),
              BuybackItem
                .newBuilder()
                .setTitle("Получаете финальную цену после осмотра")
                .setSubtitle("Торговаться не придется")
                .setIcon("https://yastatic.net/s3/...")
                .build(),
              BuybackItem
                .newBuilder()
                .setTitle("Оформляете сделку в салоне дилера")
                .setSubtitle("Деньги – сразу, наличными или на карту")
                .setIcon("https://yastatic.net/s3/...")
                .build()
            ).asJava
          )
          .build()

      val checkupClaim =
        CheckupClaim.newBuilder
          .setDescription("Позвоним в течение часа, чтобы договориться о месте и времени осмотра.")
          .setTime("Работаем с 10:00 до 19:00")
          .setPlace("Проводим осмотры в пределах 20 км от МКАД")
          .build()

      val successClaim =
        SuccessClaim.newBuilder
          .setTitle("Как подготовиться к осмотру")
          .setDescription("Ваше объявление не будет опубликовано на Авто.ру, пока проходит осмотр автомобиля.")
          .addAllItems(
            Seq(
              SuccessClaimItem.newBuilder().setTitle("Помыть машину — это увеличит финальную цену").build(),
              SuccessClaimItem.newBuilder().setTitle("Взять оригиналы: паспорт, ПТС, СТС").build(),
              SuccessClaimItem.newBuilder().setTitle("Найти второй ключ").build(),
              SuccessClaimItem.newBuilder().setTitle("Подготовить комплект сезонной резины, если есть").build()
            ).asJava
          )
          .build()

      MobileForms
        .newBuilder()
        .setBuyback(buyback)
        .setCheckupClaim(checkupClaim)
        .setSuccessClaim(successClaim)
        .build()
    }

    val mobileTexts = MobileTexts.newBuilder.setForms(mobileForms).build()

    assert(parsedMobileTexts.mobileTexts == mobileTexts)
  }
}
