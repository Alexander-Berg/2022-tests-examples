package ru.yandex.vos2.watching.stages.notifications.substages

import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vos2.BasicsModel.TrustLevel
import ru.yandex.vos2.OfferModel.OfferFlag._
import ru.yandex.vos2.OfferModel.{InactiveReason, Offer}
import ru.yandex.vos2.UserModel.UserType
import ru.yandex.vos2.realty.model.offer.RealtyOfferGenerator
import ru.yandex.vos2.watching.stages.notifications.EventProcessingState
import ru.yandex.vos2.watching.stages.notifications.NotificationUtils._

import ru.yandex.vos2.model.ModelUtils._

/**
  * @author roose
  */
class ChangeMindEventStageSpec extends WordSpecLike with Matchers with Checkers {

  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 20, maxDiscardedFactor = 25.5)

  val stage = ChangeMindEventStage

  val offerGen = for {
    rawOffer <- RealtyOfferGenerator.offerGen()
    removed <- Gen.oneOf(false, true)
    reason <- Gen.oneOf(InactiveReason.values)
  } yield {
    val builder = rawOffer.toBuilder
      .addFlag(if (removed) OF_DELETED else OF_INACTIVE)
    Gen.frequency((2, None), (8, Some(reason))).sample.get.foreach(builder.setInactiveReason)
    builder.build()
  }

  implicit val arbOffer = Arbitrary(offerGen)

  "ChangeMindEventStage" should {
    "generate event if offer is inactive or deleted because it's not needed" in {
      check { o: Offer =>
        val user = o.toBuilder.getUserBuilder.setUserType(UserType.UT_OWNER)
        val offer = o.toBuilder.setUser(user).build()
        (offer.getInactiveReason == InactiveReason.IR_NO_NEED) ==> {
          isEventGenerated(stage.process(EventProcessingState(offer)), offer.externalId)
        }
      }
    }

    "not change state if user is not owner" in {
      check { o: Offer =>
        (o.getUser.getUserType != UserType.UT_OWNER) ==> {
          val state = EventProcessingState(o)
          state == stage.process(state)
        }
      }
    }

    "not change state if offer is deleted or inactive with other reason" in {
      check { o: Offer =>
        (o.getInactiveReason != InactiveReason.IR_NO_NEED) ==> {
          val state = EventProcessingState(o)
          state == stage.process(state)
        }
      }
    }

    "not change state if offer is banned" in {
      check { o: Offer =>
        val inactiveOffer = o.toBuilder.setFinalTrustLevel(TrustLevel.TL_ZERO).build()
        val state = EventProcessingState(inactiveOffer)
        state == stage.process(state)
      }
    }
  }
}
