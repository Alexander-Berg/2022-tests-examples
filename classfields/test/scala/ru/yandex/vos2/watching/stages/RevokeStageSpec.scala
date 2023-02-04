package ru.yandex.vos2.watching.stages

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.context.v2.NoRevokedOffersPartners
import ru.yandex.vos2.BasicsModel.TrustLevel
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.watching.ProcessingState
import ru.yandex.vos2.watching.stages.RevokeStage.{MOSCOW, SPB}

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class RevokeStageSpec extends WordSpec with Matchers {

  private val noRevokePartner = 123L
  private val noRevokePartnersProvider = ProviderAdapter.create(NoRevokedOffersPartners(Set(noRevokePartner)))
  private val stage = new RevokeStage(noRevokePartnersProvider)

  "RevokeStage" should {

    "process with same 2 days delay fow all regions" in {
      val offer = makeOffer()
      val state = stage.process(ProcessingState(offer, offer))
      state.delay shouldBe 2.days
      state.offer.getOfferRealty.hasTimestampRevoke shouldBe true
    }

    "not process banned offer" in {
      val offer = makeOffer(MOSCOW, banned = true)
      val state = stage.process(ProcessingState(offer, offer))
      stage.process(state) shouldBe state
    }

    "not process offers from no revoke partners" in {
      val offer = makeOffer(partnerId = noRevokePartner)
      val state = stage.process(ProcessingState(offer, offer))
      stage.process(state) shouldBe state
    }

    "process only deleted offer" in {
      val offer = makeOffer(MOSCOW, deleted = false)
      val state = stage.process(ProcessingState(offer, offer))
      stage.process(state) shouldBe state
    }

    "process already revoked offer and set right delay" in {
      val passedInterval = 1.day.toMillis
      val revokeTime = System.currentTimeMillis - passedInterval
      val offer = makeOffer(MOSCOW, revokeTime = revokeTime)
      val state = stage.process(ProcessingState(offer, offer))
      val beWithinTolerance = be <= 1.days.toMillis and be >= 1.days.toMillis - 1.minute.toMillis
      state.delay.toMillis should beWithinTolerance
    }

  }

  def makeOffer(
    rgid: Int = -1,
    banned: Boolean = false,
    deleted: Boolean = true,
    revokeTime: Long = -1,
    partnerId: Long = 42
  ): OfferModel.Offer = {
    val builder = TestUtils.createOffer()
    builder.getOfferRealtyBuilder.getUnifiedAddressBuilder.setRgid(rgid)
    builder.getOfferRealtyBuilder.setPartnerId(partnerId)
    if (deleted) {
      builder.addFlag(OfferFlag.OF_DELETED)
    }
    if (banned) {
      builder.setFinalTrustLevel(TrustLevel.TL_ZERO)
    }
    if (revokeTime > 0) {
      builder.getOfferRealtyBuilder.setTimestampRevoke(revokeTime)
    }
    builder.build()
  }
}
