package ru.yandex.vertis.shark.client.bank.converter.impl

import ru.yandex.vertis.shark.model.generators.AutoruOfferGen
import zio.test.Assertion.isTrue
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object TinkoffBankCarConverterSpec extends DefaultRunnableSpec with AutoruOfferGen {

  private lazy val converter = new TinkoffBankCarConverterImpl

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("TinkoffBankCarConverterImpl")(
      testM("convert offer data to tinkoff format") {
        val offer = sampleOffer()
        val res = for {
          res <- converter.convert(offer)
        } yield {
          res.nonEmpty
        }
        assertM(res)(isTrue)
      }
    )
}
