package ru.yandex.vertis.shark.api.validator.impl

import cats.implicits.{catsSyntaxValidatedId, catsSyntaxValidatedIdBinCompat0}
import com.softwaremill.quicklens._
import com.softwaremill.tagging.Tagger
import ru.auto.api.api_offer_model.{Category, OfferStatus, Section}
import ru.yandex.vertis.shark.Mock.VosAutoruClientMock
import ru.yandex.vertis.shark.api.validator.FormValidator
import ru.yandex.vertis.shark.model.AutoruCreditApplication
import ru.yandex.vertis.shark.model.CreditApplicationSource.{AutoruPayload, Payload}
import ru.yandex.vertis.shark.model.generators.AutoruOfferGen
import ru.yandex.vertis.zio_baker.model.{OfferId, Tag}
import ru.yandex.vertis.zio_baker.scalapb_utils.Validation.Error.InvalidValue
import ru.yandex.vertis.zio_baker.scalapb_utils.Validation.Result
import ru.yandex.vertis.zio_baker.zio.httpclient.HttpClientUtils.HttpCodeException
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation.{failure, value}
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.{Has, ULayer}

object PayloadValidatorSpec extends DefaultRunnableSpec with AutoruOfferGen {

  private val sampleActiveOfferId: OfferId = "1114061752-f3abe210".taggedWith[Tag.OfferId]
  private val sampleInactiveOfferId: OfferId = "1514665756-605ef78b".taggedWith[Tag.OfferId]
  private val sampleNonExistedOfferId: OfferId = "1514665756-605af78b".taggedWith[Tag.OfferId]

  private val sampleActiveOffer = sampleOffer()
    .modify(_.status)
    .setTo(OfferStatus.ACTIVE)
    .modify(_.id)
    .setTo(sampleActiveOfferId)

  private val sampleInactiveOffer = sampleOffer()
    .modify(_.status)
    .setTo(OfferStatus.INACTIVE)
    .modify(_.id)
    .setTo(sampleInactiveOfferId)

  private val vosAutoruMock =
    VosAutoruClientMock.Offer(equalTo((Category.CARS, sampleActiveOfferId, false)), value(sampleActiveOffer)) ||
      VosAutoruClientMock.Offer(equalTo((Category.CARS, sampleInactiveOfferId, false)), value(sampleInactiveOffer)) ||
      VosAutoruClientMock.Offer(
        equalTo((Category.CARS, sampleNonExistedOfferId, false)),
        failure(HttpCodeException(404, "not found offer"))
      )

  private val validatorLayer: ULayer[Has[FormValidator.Service[Payload]]] =
    vosAutoruMock >>> PayloadValidator.live

  private case class ValidatorTestCase(
      description: String,
      payload: Payload,
      expectedResult: Result[Payload])

  private val validatorTestCases = Seq(
    {
      val payload = AutoruPayload(
        Seq(AutoruCreditApplication.Offer(Category.CARS, Section.USED, sampleActiveOfferId, None, None)),
        None
      )
      ValidatorTestCase(
        description = "valid active offer",
        payload = payload,
        expectedResult = payload.valid
      )
    },
    ValidatorTestCase(
      description = "invalid inactive offer",
      payload = AutoruPayload(
        Seq(AutoruCreditApplication.Offer(Category.CARS, Section.USED, sampleInactiveOfferId, None, None)),
        None
      ),
      expectedResult = InvalidValue("offers", sampleInactiveOfferId, "offer not active").invalidNec
    ),
    ValidatorTestCase(
      description = "invalid not found offer",
      payload = AutoruPayload(
        Seq(AutoruCreditApplication.Offer(Category.CARS, Section.USED, sampleNonExistedOfferId, None, None)),
        None
      ),
      expectedResult = InvalidValue("offers", sampleNonExistedOfferId, "offer not found").invalidNec
    )
  )

  private val validatorTests = validatorTestCases.map { tc =>
    testM(tc.description)(
      assertM(FormValidator.validate(tc.payload))(equalTo(tc.expectedResult))
    ).provideLayer(validatorLayer)
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("PayloadValidator")(
      suite("validate")(validatorTests: _*)
    )
}
