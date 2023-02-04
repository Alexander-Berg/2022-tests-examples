package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.apache.commons.codec.digest.DigestUtils
import org.mockito.Mockito.doNothing
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.baker.env.{DefaultEnvProvider, Env}
import ru.yandex.vertis.baker.lifecycle.Application
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Editor
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.ModerationPushWorkerYdb.{getStateStr, StateData}
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag, OfferStatusHistoryItem}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.moderation.OfferToModerationInstant
import ru.yandex.vos2.autoru.services.moderation.AutoruModerationPushApiClient
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.commonfeatures.VosFeatureTypes.WithGeneration
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder
import ru.yandex.vos2.services.mds.{AutoruAllNamespaceSettings, MdsPhotoUtils}

import scala.concurrent.duration.DurationInt

class ModerationPushWorkerYdbTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with BeforeAndAfterAll
  with InitTestDbs {
  implicit val traced: Traced = Traced.empty

  initDbs()

  private lazy val activeSale = getOfferById(1043026846).toBuilder.clearFlag().build()
  private lazy val bannedSale = getOfferById(1044216699).toBuilder.clearFlag().putFlag(OfferFlag.OF_BANNED).build()

  implicit private val mdsPhotoUtils: MdsPhotoUtils = MdsPhotoUtils("", "", AutoruAllNamespaceSettings)

  private val offerToModerationInstant =
    new OfferToModerationInstant(components.regionTree, components.carsCatalog, components.currencyRates)
  private val feature = components.featuresManager.PushToModerationYdb

  private def getClient(pushIsThrown: Boolean = false): AutoruModerationPushApiClient = {
    val client = mock[AutoruModerationPushApiClient]

    if (pushIsThrown) {
      when(client.push(?)(?)).thenThrow(classOf[RuntimeException])
    } else {
      doNothing().when(client).push(?)(?)
    }

    client
  }

  private def getActiveOffer(): Offer = {
    activeSale
  }

  private def getBannedOffer(): Offer = {
    bannedSale
  }

  abstract private class Fixture {

    val mockedApplication = mock[Application]
    when(mockedApplication.env).thenReturn(new Env(DefaultEnvProvider))

    def worker(client: AutoruModerationPushApiClient) =
      new ModerationPushWorkerYdb(
        client,
        mdsPhotoUtils,
        offerToModerationInstant
      ) with YdbWorkerTestImpl {
        override def features: FeaturesManager = components.featuresManager
      }
  }

  ("Offer is draft") in new Fixture {
    val offer = getActiveOffer()

    val offerWithUpdatedFlag = offer.toBuilder
      .putFlag(OfferFlag.OF_DRAFT)
      .build()

    val shouldProcess = worker(getClient()).shouldProcess(offerWithUpdatedFlag, None).shouldProcess

    assert(!shouldProcess)
  }

  ("Offer is not changed") in new Fixture {
    val offer = getActiveOffer()
    val offerInstant = offerToModerationInstant.toInstance(offer)
    val moderationHash = DigestUtils.sha1Hex(offerInstant.toByteArray).take(16)

    val updatedOffer = {
      val b = offer.toBuilder
      // Проверим что editor не влияет на HashModeration
      b.getOfferAutoruBuilder.setEditor(Editor.newBuilder().setName("testName"))
      b.setHashModeration(moderationHash).build()
    }

    val shouldProcess = worker(getClient()).shouldProcess(updatedOffer, None).shouldProcess
    val result = worker(getClient()).process(updatedOffer, Some(getStateStr(StateData(moderationHash, 1))))

    assert(shouldProcess)
    assert(updatedOffer.getHashModeration == moderationHash)
    assert(result.updateOfferFunc.isEmpty)
    assert(result.nextCheck.isEmpty)
  }

  ("Feature Generation is changed") in new Fixture {
    val offer = getActiveOffer()
    components.featureRegistry.updateFeature(feature.name, WithGeneration(feature.value.value, 2))
    try {
      val offerInstant = offerToModerationInstant.toInstance(offer)
      val moderationHash = DigestUtils.sha1Hex(offerInstant.toByteArray).take(16)
      val updatedOffer = {
        val b = offer.toBuilder
        b.setHashModeration(moderationHash).build()
      }

      val shouldProcess = worker(getClient()).shouldProcess(offer, None).shouldProcess
      val resultOffer =
        worker(getClient())
          .process(updatedOffer, Some(getStateStr(StateData(moderationHash, 1))))
          .updateOfferFunc
          .get(updatedOffer)

      assert(shouldProcess)
      assert(resultOffer.getHashModeration != moderationHash)
    } finally {
      components.featureRegistry.updateFeature(feature.name, WithGeneration(feature.value.value, 1))
    }
  }

  ("Push failed") in new Fixture {
    val offer = getActiveOffer()

    val result = worker(getClient(pushIsThrown = true)).process(offer, None)

    assert(!offer.hasHashModeration)
    assert(result.updateOfferFunc.isEmpty)
    assert(result.nextCheck.nonEmpty)
  }

  ("Push success") in new Fixture {
    val offer = getActiveOffer()
    val offerInstant = offerToModerationInstant.toInstance(offer)
    val moderationHash = DigestUtils.sha1Hex(offerInstant.toByteArray).take(16)

    val updatedOffer = {
      val b = offer.toBuilder
      b.getOfferAutoruBuilder.setEditor(Editor.newBuilder().setName("testName"))
      b.build()
    }
    val result = worker(getClient()).process(updatedOffer, None)
    val resultOffer = worker(getClient()).process(updatedOffer, None).updateOfferFunc.get(updatedOffer)

    assert(resultOffer.getHashModeration == moderationHash)
    assert(!resultOffer.getOfferAutoru.hasEditor)
  }

  ("Do not send offer with short history") in new Fixture {

    val offer = {
      val b = getBannedOffer().toBuilder
      b.addStatusHistory(
        OfferStatusHistoryItem
          .newBuilder()
          .setTimestamp(getNow)
      )
      b.build()
    }

    val shouldProcess = worker(getClient()).shouldProcess(offer, None).shouldProcess

    assert(!shouldProcess)
  }

  ("Do not send old offer") in new Fixture {

    val offer = {
      val b = getBannedOffer().toBuilder
      b.addStatusHistory(
        OfferStatusHistoryItem
          .newBuilder()
          .setTimestamp(getNow - 365.days.toMillis)
      )
      b.addStatusHistory(
        OfferStatusHistoryItem
          .newBuilder()
          .setTimestamp(getNow - 350.days.toMillis)
      )
      b.build()
    }

    val shouldProcess = worker(getClient()).shouldProcess(offer, None).shouldProcess

    assert(!shouldProcess)
  }

  ("Send old offer with fresh history") in new Fixture {

    val offer = {
      val b = getBannedOffer().toBuilder
      b.addStatusHistory(
        OfferStatusHistoryItem
          .newBuilder()
          .setTimestamp(getNow - 365.days.toMillis)
      )
      b.addStatusHistory(
        OfferStatusHistoryItem
          .newBuilder()
          .setTimestamp(getNow - 350.days.toMillis)
      )
      b.addStatusHistory(
        OfferStatusHistoryItem
          .newBuilder()
          .setTimestamp(getNow)
      )
      b.build()
    }

    val shouldProcess = worker(getClient()).shouldProcess(offer, None).shouldProcess

    assert(shouldProcess)
  }

}
