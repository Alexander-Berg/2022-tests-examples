package ru.yandex.vos2.watching.stages.notifications.substages

import org.scalacheck.Arbitrary
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.realty.model.offer.RealtyOfferGenerator
import ru.yandex.vos2.watching.stages.notifications.EventProcessingState
import ru.yandex.vos2.watching.stages.notifications.NotificationUtils._

class ExpiredEventStageSpec extends WordSpecLike with Matchers with Checkers {

  val now = System.currentTimeMillis
  val stage = ExpiredEventStage
  implicit val arbOffer = Arbitrary(RealtyOfferGenerator.offerGen().map { offer =>
    offer.toBuilder
      .clearFlag()
      .build()
  })

  "ExpiredEventStage" should {
    "generate event if offer expired" in {
      check { o: Offer =>
        val offer = {
          val builder = o.toBuilder.putFlag(OfferFlag.OF_EXPIRED)
          builder.addStatusHistoryBuilder().setOfferStatus(CompositeStatus.CS_ACTIVE)
          builder.addStatusHistoryBuilder().setOfferStatus(CompositeStatus.CS_MODERATION)
          builder.build()
        }
        val state = stage.process(EventProcessingState(offer))
        isEventGenerated(state, offer.externalId)
      }
    }

    "not generate event if offer has not expired" in {
      check { o: Offer =>
        val offer = o.toBuilder
          .clearFlag(OfferFlag.OF_EXPIRED)
          .build()
        !isEventGenerated(stage.process(EventProcessingState(offer)), offer.externalId)
      }
    }

    "not generate event if offer is expired but has been published" in {
      check { o: Offer =>
        val offer = {
          val builder = o.toBuilder.putFlag(OfferFlag.OF_EXPIRED)
          builder.addStatusHistoryBuilder().setOfferStatus(CompositeStatus.CS_BANNED)
          builder.build()
        }
        val state = stage.process(EventProcessingState(offer))
        !isEventGenerated(state, offer.externalId)
      }
    }
  }
}
