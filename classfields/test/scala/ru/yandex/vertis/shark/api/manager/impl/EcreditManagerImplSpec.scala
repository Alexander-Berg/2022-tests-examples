package ru.yandex.vertis.shark.api.manager.impl

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.auto.api.api_offer_model.{Category, Section}
import ru.yandex.vertis.shark.model.Api.ByParams
import ru.yandex.vertis.shark.model.Tag
import ru.yandex.vertis.shark.model.UserRef.AutoruDealer
import ru.yandex.vertis.shark.model.ecredit.Configurations.Configuration
import ru.yandex.vertis.shark.model.generators.AutoruOfferGen
import zio.test.environment.TestEnvironment
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}

object EcreditManagerImplSpec extends DefaultRunnableSpec with AutoruOfferGen {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("EcreditManager")(
      test("offerToByParams") {
        val offer = sampleOffer()
        val result = EcreditManagerImpl.offerToByParams(offer)

        val expected =
          ByParams(
            userRef = AutoruDealer(123123L),
            category = Category.CARS,
            section = Section.NEW,
            priceRub = 10000000L.taggedWith[Tag.MoneyRub]
          ).some
        assertTrue(result == expected)
      },
      test("extractEfficientConfiguration") {
        val configs = Map(
          "12" -> Map(
            "1" -> Configuration(
              id = 1,
              interestRate = 123d,
              payment = 100d,
              initialFeeMin = 123,
              periodMin = 12.some,
              periodMax = 24.some,
              amountMin = 100L,
              amountMax = 200L
            ),
            "2" -> Configuration(
              id = 2,
              interestRate = 123d,
              payment = 50d,
              initialFeeMin = 123,
              periodMin = 12.some,
              periodMax = 12.some,
              amountMin = 100L,
              amountMax = 100L
            ),
            "3" -> Configuration(
              id = 3,
              interestRate = 123d,
              payment = 200d,
              initialFeeMin = 123,
              periodMin = 12.some,
              periodMax = 24.some,
              amountMin = 100L,
              amountMax = 200L
            )
          ),
          "24" -> Map(
            "4" -> Configuration(
              id = 4,
              interestRate = 123d,
              payment = 1000d,
              initialFeeMin = 123,
              periodMin = 12.some,
              periodMax = 48.some,
              amountMin = 90L,
              amountMax = 250L
            )
          )
        )
        val result = EcreditManagerImpl.extractEfficientConfiguration(configs)
        val expected = Configuration(
          id = 2,
          interestRate = 123d,
          payment = 50d,
          initialFeeMin = 123,
          periodMin = 12.some,
          periodMax = 48.some,
          amountMin = 90L,
          amountMax = 250L
        ).some
        assertTrue(result == expected)
      }
    )
}
