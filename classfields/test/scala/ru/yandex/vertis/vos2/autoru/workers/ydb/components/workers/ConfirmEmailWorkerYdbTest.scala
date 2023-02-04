package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.passport.model.api.ApiModel.RequestEmailChangeResult
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.services.passport.PassportClient

class ConfirmEmailWorkerYdbTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with BeforeAndAfterAll
  with InitTestDbs {
  implicit val traced: Traced = Traced.empty

  private val mockPassportClient = mock[PassportClient]
  private val passportClientResponse = RequestEmailChangeResult.getDefaultInstance

  when(mockPassportClient.requestEmailChange(?, ?, ?, ?)(?)).thenReturn(Some(passportClientResponse))
  when(mockPassportClient.findUserByEmail(?)(?)).thenReturn(Seq.empty)

  abstract private class Fixture {

    val worker = new ConfirmEmailWorkerYdb(
      mockPassportClient
    ) with YdbWorkerTestImpl
  }
  private val saleId1 = 1043270830 // объявление от частника с активными услугами
  private val offer1: OfferModel.Offer = getOfferById(saleId1)

  "ok" in new Fixture {

    val offer = {
      val b = offer1.toBuilder
      b.getOfferAutoruBuilder.getSellerBuilder
        .setSentConfirmEmail(false)
        .setUnconfirmedEmail("ConfirmEmailStageTest@fakemail.ru")
      b.build()
    }

    val result = worker.process(offer, None).updateOfferFunc.get(offer1)

    assert(result.getOfferAutoru.getSeller.getSentConfirmEmail)
  }

  ("is call center") in new Fixture {

    val offer = {
      val b = offer1.toBuilder
      val autoru = b.getOfferAutoruBuilder

      autoru.getSellerBuilder
        .setSentConfirmEmail(false)
        .setUnconfirmedEmail("callcentre@auto.ru")

      autoru.getSourceInfoBuilder
        .setIsCallcenter(true)

      b.build()
    }

    val result = worker.process(offer, None).updateOfferFunc.get(offer1)

    assert(result.getOfferAutoru.getSeller.getSentConfirmEmail)
  }
}
