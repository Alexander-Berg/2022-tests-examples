package ru.yandex.vos2.watching.stages

import java.time.LocalDate

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.UserModel.User
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.user.whitelist.AccessListService
import ru.yandex.vos2.util.RandomUtil
import ru.yandex.vos2.watching.ProcessingState

import scala.concurrent.duration._

/**
  * Created by Vsevolod Levin on 03.07.2018.
  */
@RunWith(classOf[JUnitRunner])
class FeedOfferBlacklistedStageSpec extends WordSpec with Matchers {

  private val blacklistedUid = 42L

  private val interval = 24.hours
  private val accessListService: AccessListService = new AccessListService {
    override def isAllowed(uid: Long): Boolean = uid != blacklistedUid
  }

  val stage = new FeedOfferBlacklistedStage(accessListService, interval)

  "FeedOfferBlacklistedStage" should {
    "do nothing to non-feed offers" in {
      val offer = makeOffer(
        fromFeed = false
      )

      val state = ProcessingState(offer, offer)

      stage.process(state) should be(state)
    }

    "do nothing to non-feed offers of blacklisted user" in {
      val offer = makeOffer(
        fromFeed = false,
        uid = blacklistedUid
      )

      val state = ProcessingState(offer, offer)

      stage.process(state) should be(state)
    }

    "do nothing to feed offers of non-blacklisted user" in {
      val offer = makeOffer(
        fromFeed = true
      )

      val state = ProcessingState(offer, offer)

      stage.process(state) should be(state)
    }

    "do nothing to deleted feed offers of blacklisted user" in {
      val offer = makeOffer(
        fromFeed = true,
        uid = blacklistedUid,
        deleted = true
      )

      val state = ProcessingState(offer, offer)

      stage.process(state) should be(state)
    }

    "delete feed offers of blacklisted user if day of week is suitable" in {
      val dayOfWeek = LocalDate.now().getDayOfWeek.ordinal()
      val offer = makeOffer(
        fromFeed = true,
        uid = blacklistedUid,
        offerExtId = 7 * RandomUtil.nextInt(1, 1000) + dayOfWeek
      )

      val state = stage.process(ProcessingState(offer, offer))

      state.offer.isRemoved should be(true)
      state.delay.toMillis should be >= interval.toMillis
      state.delay.toMillis should be <= (interval.toMillis * 2)
    }

    "schedule feed offers of blacklisted user if day of week is not suitable" in {
      val dayOfWeek = LocalDate.now().getDayOfWeek.ordinal()
      val offer = makeOffer(
        fromFeed = true,
        uid = blacklistedUid,
        offerExtId = 7 * RandomUtil.nextInt(1, 1000) + dayOfWeek + 1
      )

      val state = stage.process(ProcessingState(offer, offer))

      state.offer.isRemoved should be(false)
      state.delay.toMillis should be >= 12.hours.toMillis
      state.delay.toMillis should be <= 24.hours.toMillis
    }
  }

  def makeOffer(fromFeed: Boolean, uid: Long = 1, deleted: Boolean = false, offerExtId: Long = 0): Offer = {
    val builder = TestUtils.createOffer()
    builder.getOfferRealtyBuilder.setFromFeed(fromFeed)
    builder.alterFlag(OfferFlag.OF_DELETED, deleted)
    builder.getUserBuilder.setAlternativeIds(User.AlternativeIds.newBuilder().setExternal(uid))

    if (offerExtId != 0) {
      builder.setExternalId(offerExtId.toString)
    }

    builder.build()
  }
}
