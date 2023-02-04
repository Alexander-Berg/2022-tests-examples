package auto.dealers.multiposting.model.test

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}

import io.circe.parser._
import auto.dealers.multiposting.model._
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object ClassifiedStateParsingSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ClassifiedState")(
      test("parse state without product") {
        val json =
          """{"timestamp":1614846804,
            |"autoru_client_id":40551,
            |"source":"avito",
            |"vin":"X7LASRA1966966091",
            |"offer_id":"2053515973",
            |"url":"https:\/\/www.avito.ru\/mytischi\/avtomobili\/renault_kaptur_2021_2053515973",
            |"finish_time":"2021-04-01T08:16:30+03:00",
            |"status":"active",
            |"status_detail": "error_blocked"}
            |""".stripMargin

        val dt = OffsetDateTime.of(2021, 4, 1, 8, 16, 30, 0, ZoneOffset.ofHours(3))
        val expected = ClassifiedState(
          classified = Classified("avito"),
          clientId = ClientId(40551),
          vin = Vin("X7LASRA1966966091"),
          offerId = Some(OfferId("2053515973")),
          url = Some("https://www.avito.ru/mytischi/avtomobili/renault_kaptur_2021_2053515973"),
          startDate = None,
          expireDate = Some(dt),
          status = "active",
          services = None,
          statusDetailed = Some("error_blocked"),
          warnings = None,
          errors = None,
          servicePrices = None,
          timestamp = 1614846804L
        )

        assert(decode[ClassifiedState](json))(isRight(equalTo(expected)))
      },
      test("parse state with product") {
        val json =
          """{"timestamp":1614846810,
            |"autoru_client_id":49332,
            |"source":"avito",
            |"vin":"X7LRJC4BX66811308",
            |"offer_id":"2082198213",
            |"url":"https:\/\/www.avito.ru\/syzran\/avtomobili\/renault_arkana_2021_2082198213",
            |"finish_time":"2021-03-09T12:04:12+03:00",
            |"status":"active",
            |"start_time":"2021-02-02T09:45:17+00:00",
            |"warnings": [
            |      {
            |        "title": "Для объявления не найдено соответствия с существующим объявлением на Авито.",
            |        "description": "Для аккаунта включен режим привязки объявлений. В данном случае создастся новое объявление."
            |      }
            |    ],
            |"errors": [
            |  {
            |    "title": "по причине «Объявление не исправлено».",
            |    "description": "Объявление не исправлено : После нескольких попыток объявление больше нельзя отредактировать — пожалуйста, создайте новое. Если вам нужна помощь, напишите в поддержку, мы подскажем. Уточните возможность решения проблемы в Службе поддержки: supportautoload@avito.ru 8 800 600-00-01, 8 800 511-01-11",
            |    "manual": "link to ads"
            |  }
            |],
            |"service_prices":[{"product":"highlight","price":239}],
            |"vas":[{"product":"xl","finish_time":"2021-03-09T12:04:12"}]}""".stripMargin

        val startDt = OffsetDateTime.of(LocalDateTime.of(2021, 2, 2, 9, 45, 17), ZoneOffset.ofHours(0))
        val expireDt = OffsetDateTime.of(2021, 3, 9, 12, 4, 12, 0, ZoneOffset.ofHours(3))
        val productExpireDt = OffsetDateTime.of(LocalDateTime.of(2021, 3, 9, 12, 4, 12, 0), ZoneOffset.ofHours(0))
        val expected = ClassifiedState(
          classified = Classified("avito"),
          clientId = ClientId(49332),
          vin = Vin("X7LRJC4BX66811308"),
          offerId = Some(OfferId("2082198213")),
          url = Some("https://www.avito.ru/syzran/avtomobili/renault_arkana_2021_2082198213"),
          startDate = Some(startDt),
          expireDate = Some(expireDt),
          status = "active",
          services = Some(List(ClassifiedService("xl", None, Some(productExpireDt)))),
          statusDetailed = None,
          warnings = Some(
            Seq(
              DetailedInformation(
                title = "Для объявления не найдено соответствия с существующим объявлением на Авито.",
                description =
                  "Для аккаунта включен режим привязки объявлений. В данном случае создастся новое объявление.",
                manual = None
              )
            )
          ),
          errors = Some(
            Seq(
              DetailedInformation(
                title = "по причине «Объявление не исправлено».",
                description =
                  "Объявление не исправлено : После нескольких попыток объявление больше нельзя отредактировать — пожалуйста, создайте новое. Если вам нужна помощь, напишите в поддержку, мы подскажем. Уточните возможность решения проблемы в Службе поддержки: supportautoload@avito.ru 8 800 600-00-01, 8 800 511-01-11",
                manual = Some("link to ads")
              )
            )
          ),
          servicePrices = Some(
            Seq(
              ServicePrice(
                service = "highlight",
                price = 239
              )
            )
          ),
          timestamp = 1614846810
        )

        assert(decode[ClassifiedState](json))(isRight(equalTo(expected)))
      }
    ) @@ sequential
}
