package ru.yandex.vos2.watching.stages.notifications.substages

import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vos2.OfferModel.InactiveReason._
import ru.yandex.vos2.OfferModel.OfferFlag._
import ru.yandex.vos2.OfferModel.{InactiveReason, Offer}
import ru.yandex.vertis.vos2.model.realty.OfferType
import ru.yandex.vertis.vos2.model.realty.RealtyOffer.RealtyCategory
import ru.yandex.vos2.UserModel.UserType
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder
import ru.yandex.vos2.realty.model.offer.RealtyOfferGenerator
import ru.yandex.vos2.watching.stages.notifications.EventProcessingState
import ru.yandex.vos2.watching.stages.notifications.NotificationUtils._

import ru.yandex.vos2.model.ModelUtils._

/**
  * @author roose
  */
class SoldEventStageSpec extends WordSpecLike with Matchers with Checkers {

  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSuccessful = 20, maxDiscardedFactor = 25.5)

  val now = System.currentTimeMillis

  val stage = SoldEventStage

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

  "SoldEventStage" should {
    "generate event if offer is deleted or inactive and the object was sold" in {
      check { o: Offer =>
        val user = o.toBuilder.getUserBuilder.setUserType(UserType.UT_OWNER)
        val offer = sellApartmentsOffer(o).toBuilder.setUser(user).build()
        (offer.hasInactiveReason && Seq(IR_SOLD_HERE, IR_SOLD_OUTSIDE).contains(offer.getInactiveReason)) ==> {
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

    "not change state if offer is deleted or inactive with other reasons" in {
      check { o: Offer =>
        (o.hasInactiveReason && !Seq(IR_SOLD_HERE, IR_SOLD_OUTSIDE).contains(o.getInactiveReason)) ==> {
          val state = EventProcessingState(o)
          state == stage.process(state)
        }
      }
    }

    "not change state if offer is deleted or inactive without a reason" in {
      check { o: Offer =>
        (!o.hasInactiveReason) ==> {
          val state = EventProcessingState(o)
          state == stage.process(state)
        }
      }
    }

    "not change state if offer is not removed or inactive" in {
      check { o: Offer =>
        val offer = o.toBuilder.clearFlag(OF_INACTIVE).clearFlag(OF_DELETED).build()
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
