package ru.yandex.vos2.watching.stages

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.model.ModelUtils.{RichOffer, RichOfferBuilder}
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.watching.ProcessingState

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class FeedOfferCleanupStageSpec extends WordSpec with Matchers {

  val interval = 1.hour
  val stage = new FeedOfferCleanupStage(interval)

  "FeedOfferCleanup" should {

    "drop expired deleted feed offers" in {
      val offer = makeOffer(fromFeed = true, deleted = true, System.currentTimeMillis() - interval.toMillis)

      assert(isDropped(stage.process(ProcessingState(offer, offer))))
    }

    "keep not deleted feed offers" in {
      val offer = makeOffer(fromFeed = true, deleted = false, System.currentTimeMillis() - interval.toMillis)

      assert(!isDropped(stage.process(ProcessingState(offer, offer))))
    }

    "keep feed offers deleted recently" in {
      val offer = makeOffer(fromFeed = true, deleted = true, System.currentTimeMillis() - interval.toMillis / 2)

      assert(!isDropped(stage.process(ProcessingState(offer, offer))))
    }

    "schedule recently deleted feed offers to the moment of cleanup" in {
      val offer = makeOffer(fromFeed = true, deleted = true, System.currentTimeMillis() - interval.toMillis / 2)
      val delay = stage.process(ProcessingState(offer, offer)).delay
      assert(delay.isFinite())
      delay.toMillis shouldBe (interval.toMillis / 2 +- 500)
    }

    "keep manual offers" in {
      val offer = makeOffer(fromFeed = false, deleted = true, System.currentTimeMillis() - interval.toMillis)

      assert(!isDropped(stage.process(ProcessingState(offer, offer))))
    }
  }

  def makeOffer(fromFeed: Boolean, deleted: Boolean, updated: Long): Offer = {
    val builder = TestUtils.createOffer()
    builder.getOfferRealtyBuilder.setFromFeed(fromFeed)
    builder
      .alterFlag(OfferFlag.OF_DELETED, deleted)
      .clearFlag(OfferFlag.OF_DROP)
      .setTimestampUpdate(updated)
      .setTimestampAnyUpdate(updated)
    builder.build()
  }

  def isDropped(state: ProcessingState): Boolean = {
    state.offer.hasFlag(OfferFlag.OF_DROP)
  }
}
