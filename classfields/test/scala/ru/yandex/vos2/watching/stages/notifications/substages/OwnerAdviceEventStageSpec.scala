package ru.yandex.vos2.watching.stages.notifications.substages

import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vos2.BasicsModel.TrustLevel
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vertis.vos2.model.realty.OfferType
import ru.yandex.vertis.vos2.model.realty.RealtyOffer.RealtyCategory
import ru.yandex.vos2.UserModel.UserType
import ru.yandex.vos2.realty.model.offer.RealtyOfferGenerator
import ru.yandex.vos2.watching.stages.notifications.EventProcessingState
import ru.yandex.vos2.watching.stages.notifications.NotificationUtils._

import scala.concurrent.duration._

import ru.yandex.vos2.model.ModelUtils._

/**
  * @author roose
  */
class OwnerAdviceEventStageSpec extends WordSpecLike with Matchers with Checkers {

  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 20, maxDiscardedFactor = 25.5)

  val now = System.currentTimeMillis

  val stage = OwnerAdviceEventStage
  val delay = OwnerAdviceEventStage.Delay

  val offerGen = for {
    rawOffer ← RealtyOfferGenerator.offerGen()
    date ← Gen.choose(now - delay - 36.hours.toMillis, now)
  } yield rawOffer.toBuilder.setTimestampCreate(date).build()

  implicit val arbOffer = Arbitrary(offerGen)

  "SoldEventStage" should {
    "schedule offer for revisit if an offer was created recently" in {
      check { offer: Offer ⇒
        val o = sellApartmentsOffer(offer)
        (o.getUser.getUserType == UserType.UT_OWNER &&
        now < o.getTimestampCreate + delay - 1.hour.toMillis) ==> {
          val state = stage.process(EventProcessingState(o))
          !isEventGenerated(state)
          state.delay < Duration.Inf
        }
      }
    }

    "generate event if offer is created 3-4 days ago" in {
      check { o: Offer ⇒
        val user = o.toBuilder.getUserBuilder.setUserType(UserType.UT_OWNER)
        val offer = sellApartmentsOffer(o).toBuilder.setUser(user).build()
        (offer.getTimestampCreate + delay < now &&
        now < offer.getTimestampCreate + delay + 23.hours.toMillis) ==> {
          isEventGenerated(stage.process(EventProcessingState(offer)), offer.externalId)
        }
      }
    }

    "not change state if an offer is banned" in {
      check { o: Offer ⇒
        val offer = o.toBuilder.setFinalTrustLevel(TrustLevel.TL_ZERO).build()
        val state = EventProcessingState(offer)
        state == stage.process(state)
      }
    }

    "not change state if an offer is inactive" in {
      check { o: Offer ⇒
        val offer = o.toBuilder.addFlag(OfferFlag.OF_INACTIVE).build()
        val state = EventProcessingState(offer)
        state == stage.process(state)
      }
    }

    "not change state if an offer is removed" in {
      check { o: Offer ⇒
        val offer = o.toBuilder.addFlag(OfferFlag.OF_DELETED).build()
        val state = EventProcessingState(offer)
        state == stage.process(state)
      }
    }
  }

  private def sellApartmentsOffer(offer: Offer): Offer = {
    val ob = offer.toBuilder
    ob.getOfferRealtyBuilder.setOfferType(OfferType.SELL).setCategory(RealtyCategory.CAT_APARTMENT)
    ob.build()
  }
}
