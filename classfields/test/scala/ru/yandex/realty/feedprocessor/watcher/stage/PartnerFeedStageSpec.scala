package ru.yandex.realty.feedprocessor.watcher.stage

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.partnerdata.feedloader.common.FeedloaderClient
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.feedprocessor.model.{PartnerFeed, PartnerFeedGenerator, PartnerFeedState, PartnerOffer}
import ru.yandex.realty.feedprocessor.services.PartnerOfferFiller
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.persistence.{OfferIdGenerator, RawOfferHasher}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState

import java.nio.file.{Files, Path}
import java.util

@RunWith(classOf[JUnitRunner])
class PartnerFeedStageSpec extends AsyncSpecBase with PartnerFeedGenerator {

  private val feedloaderClient = mock[FeedloaderClient]
  private val features = new SimpleFeatures
  private val rawOfferHasher = new RawOfferHasher(features.FullReindex)
  private val dir: Path = Files.createTempDirectory("feeds")
  private val partnerFeedStage = new PartnerFeedStage(feedloaderClient, rawOfferHasher, dir.toFile)
  implicit private val traced: Traced = Traced.empty

  "PartnerFeedStage" should {
    "process feeds" in {
      val partnerFeed = partnerFeedGen(1, "partner_1").next
      val partnerOffers = Seq("1", "2", "3", "4")
        .map(buildPartnerOffer(partnerFeed, _))
        .map(o => o.copy(hash = rawOfferHasher.hashOf(o.data).toByteArray))
      val partnerOfferState = PartnerFeedState(partnerFeed, partnerOffers)
      val feedInputStream = getClass.getClassLoader.getResourceAsStream("feed_1.xml.gz")
      toMockFunction1(feedloaderClient.getFeedInputStream(_: String))
        .expects("partner_1")
        .once()
        .returning(feedInputStream)
      val actual = partnerFeedStage.process(ProcessingState(partnerOfferState)).futureValue.entry.partnerOffers
      feedInputStream.close()
      actual.map(_.data.getInternalId).toSet shouldEqual Set("1", "3", "5")
      actual.find(_.data.getInternalId == "1").get.status shouldBe false
      actual.find(_.data.getInternalId == "5").get.status shouldBe true
      actual.find(_.data.getInternalId == "3").get.status shouldBe true
      val old3OfferHash = partnerOffers
        .find(_.data.getInternalId == "3")
        .get
        .hash
      val new3OfferHash = actual.find(_.data.getInternalId == "3").get.hash
      !util.Arrays.equals(new3OfferHash, old3OfferHash) shouldBe true
      actual.map(_.visitTime).forall(_.isDefined) shouldBe true
    }
  }

  private def buildPartnerOffer(partnerFeed: PartnerFeed, internalId: String): PartnerOffer = {
    val rawOffer = new RawOfferImpl()
    rawOffer.setPartnerId(partnerFeed.partnerId)
    rawOffer.setId(OfferIdGenerator.getId(partnerFeed.partnerId, internalId).toString)
    rawOffer.setInternalId(internalId)
    PartnerOffer(
      partnerFeed.partnerId,
      rawOffer.getId,
      Array[Byte](),
      PartnerOfferFiller.fill(rawOffer, partnerFeed),
      true,
      None,
      partnerFeed.partnerId.toInt
    )
  }

}
