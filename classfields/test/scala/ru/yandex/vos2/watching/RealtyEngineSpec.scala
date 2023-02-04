package ru.yandex.vos2.watching

import java.lang.Math.abs
import java.lang.System.currentTimeMillis

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.offer.OfferHiddenReason
import ru.yandex.vos2.BasicsModel.TrustLevel.TL_MEDIUM
import ru.yandex.vos2.OfferModel.OfferFlag.OF_EXPIRED
import ru.yandex.vos2.OfferModel._
import ru.yandex.vos2.config.TestRealtySchedulerComponents
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.watching.utils.ProbeIdxClient

import scala.concurrent.duration._

/**
  * @author Ilya Gerasimov (747mmhg@yandex-team.ru)
  */
class RealtyEngineSpec extends WordSpec with Matchers {

  class Toolkit {
    val components = new TestRealtySchedulerComponents
    val idxClient = components.kafkaIdxClient
    val engine = new RealtyEngine(components)
  }

  "Engine" should {
    "send set TL medium to valid offer and send it to Karma and Idx" in {
      val toolkit = new Toolkit
      val offer = createOffer().clearFinalTrustLevel().build()
      val user = offer.getUser.toBuilder.setTrustLevel(TL_MEDIUM)
      val (update, _) = toolkit.engine.process(offer, user.build())
      assert(update.getUpdate.isDefined)
      val processed = update.getUpdate.get
      assert(processed.getFinalTrustLevel == TL_MEDIUM)
      assert(!ProbeIdxClient.hasHiddenReason(toolkit.idxClient.sentRequest))
    }
  }

  "Engine" should {
    "schedule visiting by the moment of expiration" in {
      val toolkit = new Toolkit
      val ttlHours = 1
      val offer = createOffer().clearFinalTrustLevel().setOfferTTLHours(ttlHours).build()
      val user = offer.getUser.toBuilder.setTrustLevel(TL_MEDIUM)
      val (update, _) = toolkit.engine.process(offer, user.build())
      assert(update.getVisitDelay.isDefined)
      assert(abs(update.getVisitDelay.get.toDuration(Duration.Undefined).toSeconds - ttlHours.hours.toSeconds) < 2)
    }
  }

  "Engine" should {
    "properly process expired offer" in {
      val toolkit = new Toolkit
      val ttlHours = 1
      val offer = createOffer()
        .clearFinalTrustLevel()
        .setTimestampTtlStart(currentTimeMillis() - ttlHours.hours.toMillis - 1)
        .setOfferTTLHours(ttlHours)
        .build()
      val user = offer.getUser.toBuilder.setTrustLevel(TL_MEDIUM)
      val (update, _) = toolkit.engine.process(offer, user.build())
      assert(update.getVisitDelay.isDefined)
      assert(!update.getVisitDelay.get.isFinite)
      assert(update.getUpdate.isDefined)
      assert(update.getUpdate.get.hasFlag(OF_EXPIRED))
      assert(!update.getUpdate.get.hasFinalTrustLevel)
      assert(
        toolkit.idxClient.sentRequest.getCreate.getOffer.getContent.getHiddenReason == OfferHiddenReason.OFFER_HIDDEN_REASON_INACTIVE
      )
    }
  }

  "Engine" should {
    "update user in offer" in {
      val toolkit = new Toolkit
      val ttlHours = 1
      val offer = createOffer().clearFinalTrustLevel().setOfferTTLHours(ttlHours).build()
      val user = offer.getUser.toBuilder.setTrustLevel(TL_MEDIUM)
      user.getUserContactsBuilder.setName("tested!")
      val (update, _) = toolkit.engine.process(offer, user.build())
      assert(update.getUpdate.get.getUser == user.build())
    }
  }

  "Engine" should {
    "properly process deleted offer" in {
      val toolkit = new Toolkit
      val offer = createOffer()
        .putFlag(OfferFlag.OF_DELETED)
        .setHashIDX("qfsdf")
        .build()
      val user = offer.getUser.toBuilder.setTrustLevel(TL_MEDIUM)
      val (update, _) = toolkit.engine.process(offer, user.build())
      assert(update.getVisitDelay.isDefined)
      assert(!update.getVisitDelay.get.isFinite)
      assert(toolkit.idxClient.removed)
      assert(!update.getUpdate.get.hasFinalTrustLevel)
    }
  }

  "Engine" should {
    "produce same results if offer does not change" in {
      val toolkit = new Toolkit
      val offer = createOffer().clearFinalTrustLevel().build()
      val user = offer.getUser.toBuilder.setTrustLevel(TL_MEDIUM)
      val (update, _) = toolkit.engine.process(offer, user.build())
      assert(update.getUpdate.isDefined)
      val processed = update.getUpdate.get
      val toolkit2 = new Toolkit
      val (update2, _) = toolkit2.engine.process(processed, user.build())
      assert(update2.getUpdate.isEmpty)
    }
  }

  "Engine" should {
    "mark offer as valid if offer's last check was cancelled" in {
      val toolkit = new Toolkit
      val offer = createOffer().clearFinalTrustLevel().setDescription("http://ya.ru").build()
      val user = offer.getUser.toBuilder.setTrustLevel(TL_MEDIUM).build()
      val (update, _) = toolkit.engine.process(offer, user)
      assert(update.getUpdate.isDefined)
      val processed = update.getUpdate.get
      val updated = processed.toBuilder
      updated
        .getQASCheckBuilder(0)
        .setStatus(QASCheckStatus.RECEIVED)
        .setTimestampReceived(System.currentTimeMillis())
        .getResolutionBuilder
        .setIsCanceled(true)
      val toolkit2 = new Toolkit
      val (update2, _) = toolkit2.engine.process(updated.build(), user)
      assert(update2.getUpdate.isDefined)
      val processed2 = update2.getUpdate.get
      assert(processed2.hasFinalTrustLevel)
      assert(processed2.getFinalTrustLevel == TL_MEDIUM)
      assert(processed2.getQASCheckCount == 1)
    }
  }

  private def createOffer(): Offer.Builder = {
    TestUtils.createOffer().setDescription("")
  }
}
