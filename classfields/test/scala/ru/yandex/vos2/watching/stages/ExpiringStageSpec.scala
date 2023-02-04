package ru.yandex.vos2.watching.stages

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.util.IOUtils
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.proto.offer.IndexerErrors
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.OfferModel.IDXStatus.NoteType
import ru.yandex.vos2.OfferModel.{IDXStatus, Offer, OfferFlag}
import ru.yandex.vos2.model.ModelUtils.RichOffer
import ru.yandex.vos2.realty.model.{ShowDurationSelector, TestUtils}
import ru.yandex.vos2.watching.ProcessingState

import scala.concurrent.duration._

/**
  * @author Ilya Gerasimov (747mmhg@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class ExpiringStageSpec extends WordSpec with Matchers with MockitoSupport {

  val regionGraph: RegionGraph =
    RegionGraphProtoConverter.deserialize(
      IOUtils.gunzip(
        getClass.getClassLoader.getResourceAsStream("region_graph-8-2.data")
      )
    )
  private val regionGraphProvider = ProviderAdapter.create(regionGraph)
  private val showDurationSelector = new ShowDurationSelector(regionGraphProvider)
  private val expiringStage = new ExpiringStage(showDurationSelector)

  private val expiredOffer = {
    val builder = TestUtils.createOffer()
    val now = System.currentTimeMillis()
    val ttlHours = showDurationSelector.select(builder).toHours.toInt
    val ttl = ttlHours.hours.toMillis
    builder
      .setTimestampTtlStart(now - ttl - 1)
      .setOfferTTLHours(ttlHours)
      .build()
  }

  private val notExpiredOffer = expiredOffer.toBuilder.setOfferTTLHours(214453).build()

  private def makeExpiredByIdx(offer: Offer): Offer = {
    val builder = offer.toBuilder
    builder.getIDXStatusBuilder
      .addNote(
        IDXStatus.Note
          .newBuilder()
          .setNoteType(NoteType.ERROR)
          .setNoteCode(IndexerErrors.OFFER_TOO_OLD_VALUE)
      )
      .addNote(
        IDXStatus.Note
          .newBuilder()
          .setNoteType(NoteType.ERROR)
          .setNoteCode(IndexerErrors.OFFER_TOO_OLD_VALUE + 1)
      )
    builder.build()
  }

  private def markFromFeed(offer: Offer): Offer = {
    val builder = offer.toBuilder
    builder.getOfferRealtyBuilder.setFromFeed(true)
    builder.build()
  }

  "ExpiringStage" should {

    "mark expired manual offers" in {
      val result = expiringStage.process(ProcessingState(expiredOffer, expiredOffer))
      assert(result.offer.isExpired)
    }

    "do not touch not expired manual offers with indexing error" in {
      val result =
        expiringStage.process(ProcessingState(makeExpiredByIdx(notExpiredOffer), makeExpiredByIdx(notExpiredOffer)))
      assert(!result.offer.isExpired)
      assert(result.delay < Duration.Inf)
    }

    "schedule an offer inspection if it is not yet expired for manual offers" in {
      val result = expiringStage.process(ProcessingState(notExpiredOffer, notExpiredOffer))
      assert(!result.offer.isExpired)
      assert(result.delay < Duration.Inf)
    }

    "not change state if an offer already has OF_EXPIRED" in {
      val offer = expiredOffer.toBuilder.addFlag(OfferFlag.OF_EXPIRED).build()
      val state = ProcessingState(offer, offer)
      assert(state == expiringStage.process(state))
    }

    "not change state if an offer already is removed" in {
      val offer = expiredOffer.toBuilder.addFlag(OfferFlag.OF_DELETED).build()
      val state = ProcessingState(offer, offer)
      assert(state == expiringStage.process(state))
    }

    "not change state if an offer already is inactive" in {
      val offer = expiredOffer.toBuilder.addFlag(OfferFlag.OF_INACTIVE).build()
      val state = ProcessingState(offer, offer)
      assert(state == expiringStage.process(state))
    }

    "delete expired drafts" in {
      val offer = expiredOffer.toBuilder.addFlag(OfferFlag.OF_DRAFT).build()
      val processed = expiringStage.process(ProcessingState(offer, offer))
      val processedAgain = expiringStage.process(processed)
      assert(processedAgain.offer.isRemoved)
    }

    "do not touch feed offers with indexing error" in {
      val offer = markFromFeed(makeExpiredByIdx(notExpiredOffer))
      val result = expiringStage.process(ProcessingState(offer, offer))
      assert(result.offer == offer)
    }

    "do not touch feed offers without indexing error" in {
      val offer = markFromFeed(notExpiredOffer)
      val result = expiringStage.process(ProcessingState(offer, offer))
      assert(result.offer == offer)
    }

    "update offer when ttl is wrong" in {
      val offer = expiredOffer.toBuilder.setOfferTTLHours(1).build()
      val result = expiringStage.process(ProcessingState(offer, offer))
      assert(!result.offer.isExpired)
    }
  }
}
