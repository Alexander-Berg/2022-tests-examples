package ru.yandex.vos2.watching.stages.notifications.substages

import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vos2.BasicsModel.TrustLevel
import ru.yandex.vos2.OfferModel.{InactiveReason, Offer}
import ru.yandex.vos2.UserModel.UserType._
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.realty.model.offer.RealtyOfferGenerator
import ru.yandex.vos2.watching.stages.notifications.EventProcessingState
import ru.yandex.vos2.watching.stages.notifications.NotificationUtils._

import scala.concurrent.duration._

/**
  * @author roose
  */
class FirstAgentOfferEventStageSpec extends WordSpecLike with Matchers with Checkers {

  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 20, maxDiscardedFactor = 25.5)

  val now = System.currentTimeMillis

  val stage = FirstAgentOfferEventStage
  val delay = FirstAgentOfferEventStage.Delay

  val offerGen = for {
    rawOffer <- RealtyOfferGenerator.offerGen()
    removed <- Gen.oneOf(false, true)
    reason <- Gen.oneOf(InactiveReason.values)
    date <- Gen.choose(now - delay - 36.hours.toMillis, now)
  } yield rawOffer.toBuilder.setTimestampCreate(date).build()

  implicit val arbOffer = Arbitrary(offerGen)

  "SoldEventStage" should {
    "schedule offer for revisit if an offer was created recently" in {
      check { o: Offer =>
        ((o.getUser.getUserType == UT_AGENT || o.getUser.getUserType == UT_AGENCY) &&
        now < o.getTimestampCreate + delay - 1.hour.toMillis) ==> {
          val state = stage.process(EventProcessingState(o))
          !isEventGenerated(state)
          state.delay < Duration.Inf
        }
      }
    }

    "generate event if offer is created 1-2 days ago" in {
      check { o: Offer =>
        val user = o.toBuilder.getUserBuilder.setUserType(Gen.oneOf(UT_AGENT, UT_AGENCY).sample.get)
        val offer = o.toBuilder.setUser(user).build()
        (offer.getTimestampCreate + delay < now &&
        now < offer.getTimestampCreate + delay + 23.hours.toMillis) ==> {
          isEventGenerated(stage.process(EventProcessingState(offer)), offer.externalId)
        }
      }
    }

    "not change state if an offer is banned" in {
      check { o: Offer =>
        val offer = o.toBuilder.setFinalTrustLevel(TrustLevel.TL_ZERO).build()
        val state = EventProcessingState(offer)
        state == stage.process(state)
      }
    }
  }
}
