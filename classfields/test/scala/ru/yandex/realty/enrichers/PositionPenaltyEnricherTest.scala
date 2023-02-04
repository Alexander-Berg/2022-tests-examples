package ru.yandex.realty.enrichers

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.bunker.BunkerResources
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.storage.{Partner, TuzCampaignStorage}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.realty.vos.model.diff.FieldEnumNamespace.FieldEnum

@RunWith(classOf[JUnitRunner])
class PositionPenaltyEnricherTest extends AsyncSpecBase with Matchers with MockFactory {

  implicit val trace: Traced = Traced.empty

  "PositionPenaltyEnricherTest" should {

    val bunkerResources = BunkerResources(
      errorToDescription = Map.empty,
      notEditableCodes = Set.empty,
      uidsForNewLk = Set.empty,
      excludeUidsForNewLk = Set.empty,
      uidsForArchiveDeals = Set.empty,
      usersWithPenalty = Set("bunker_penalty_uid"),
      rolesByUser = Map.empty,
      relevancePenaltyValue = 1
    )

    val tuzCampaignStorage = new TuzCampaignStorage(
      activePartners = Seq(Partner(Some("111111111"), Some(1))),
      penaltyPartners = Seq(Partner(Some("tuz_campaign_storage_penalty_uid"), Some(2)))
    )

    val bunkerResourcesProvider = new Provider[BunkerResources] {
      override def get(): BunkerResources = bunkerResources
    }

    val tuzCampaignStorageProvider = new Provider[TuzCampaignStorage] {
      override def get(): TuzCampaignStorage = tuzCampaignStorage
    }

    val positionPenaltyEnricher =
      new PositionPenaltyEnricher(bunkerResourcesProvider, tuzCampaignStorageProvider)

    "setPenalty during processing offer with uid under Penalty in TuzCampaignStorage" in {
      val ow = mock[OfferWrapper]
      val offer = mock[Offer]
      (offer.getUid _).expects().returns("tuz_campaign_storage_penalty_uid").anyNumberOfTimes()
      (offer.getPartnerId _).expects().returns(2).anyNumberOfTimes()
      (offer.hasVas _).expects().returns(false)
      (ow.getOffer _).expects().returns(offer).once()
      (offer.setPositionPenaltyOn _).expects(true).once()
      positionPenaltyEnricher.process(ow).futureValue
    }

    "setPenalty during processing offer with uid under Penalty in Bunkder" in {
      val ow = mock[OfferWrapper]
      val offer = mock[Offer]
      (offer.getUid _).expects().returns("bunker_penalty_uid").anyNumberOfTimes()
      (offer.getPartnerId _).expects().returns(3).anyNumberOfTimes()
      (offer.hasVas _).expects().returns(false)
      (ow.getOffer _).expects().returns(offer).once()
      (offer.setPositionPenaltyOn _).expects(true).once()
      positionPenaltyEnricher.process(ow).futureValue
    }

    "do not setPenalty during processing offer with uid active" in {
      val ow = mock[OfferWrapper]
      val offer = mock[Offer]
      (offer.getUid _).expects().returns("111111111").anyNumberOfTimes()
      (offer.getPartnerId _).expects().returns(1).anyNumberOfTimes()
      (offer.hasVas _).expects().returns(false).never()
      (ow.getOffer _).expects().returns(offer).once()
      (offer.setPositionPenaltyOn _).expects(true).never()
      positionPenaltyEnricher.process(ow).futureValue
    }

  }
}
