package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.vin.VinResolutionEnums
import ru.auto.api.vin.VinResolutionEnums.ResolutionPart
import ru.auto.api.vin.VinResolutionModel.{ResolutionEntry, VinIndexResolution}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.tag_processor.substages.SafeDealTag
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.tag_processor.substages.SafeDealTag.Tag
import ru.yandex.vertis.ydb.skypper.YdbWrapper
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.{Condition, CustomHouseStatus, SellerType}
import ru.yandex.vos2.BasicsModel
import ru.yandex.vos2.OfferModel.{Offer, OfferService}
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.autoru.utils.geo.Tree
import ru.yandex.vos2.commonfeatures.FeaturesManager

import scala.jdk.CollectionConverters._
import scala.jdk.CollectionConverters._

class SafeDealTagTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  private val testTag: String = "test-tag"

  abstract private class Fixture {
    val mockedFeatureManager = mock[FeaturesManager]
    private val mockFeature = mock[Feature[Boolean]]
    when(mockFeature.value).thenReturn(true)
    val daoMocked = mock[AutoruOfferDao]
    val ydbMocked = mock[YdbWrapper]
    val treeMocked = mock[Tree]
    when(treeMocked.isInside(?, ?)).thenReturn(true)

    val sourceOffer: Offer = {
      val offerBuilder = Offer.newBuilder()
      offerBuilder.setTimestampUpdate(0L)
      offerBuilder.setOfferService(OfferService.OFFER_AUTO)
      offerBuilder.setUserRef("test-user")
      val b = AutoruOffer.newBuilder()
      b.setSellerType(SellerType.PRIVATE)
      b.setCategory(Category.CARS)
      b.setSection(Section.USED)
      b.getEssentialsBuilder.setYear(2018)
      b.getStateBuilder.setCondition(Condition.EXCELLENT)
      b.getDocumentsBuilder.setCustomHouseState(CustomHouseStatus.CLEARED)
      b.getDocumentsBuilder.setVin("test-vin")
      val res = VinIndexResolution.newBuilder().setVersion(1)
      res
        .addAllEntries(
          Seq(
            ResolutionEntry
              .newBuilder()
              .setPart(ResolutionPart.RP_RESTRICTED)
              .setStatus(VinResolutionEnums.Status.OK)
              .build(),
            ResolutionEntry
              .newBuilder()
              .setPart(ResolutionPart.RP_MARK_MODEL)
              .setStatus(VinResolutionEnums.Status.OK)
              .build()
          ).asJava
        )
      b.getVinResolutionBuilder.setResolution(res).setVersion(1)
      b.getPriceBuilder
        .setPriceRub(300000)
        .setPrice(300000)
        .setCreated(0L)
        .setCurrency(BasicsModel.Currency.RUB)
      b.setVersion(10)
      b.getSellerBuilder.getPlaceBuilder.setGeobaseId(213L)
      offerBuilder.setOfferAutoru(b)
      offerBuilder.addTag(testTag)
      offerBuilder.build()
    }

    val worker = new SafeDealTag(
      treeMocked
    ) {

      override def features: FeaturesManager = mockedFeatureManager
    }
  }

  ("process valid offer") in new Fixture {
    assertContainsTags(sourceOffer, Set(testTag, Tag), worker)
  }

  ("not process if not cars") in new Fixture {
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.setCategory(Category.TRUCKS)
    assert(!worker.shouldProcess(b.build(), None))
  }

  ("not process if not used") in new Fixture {
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.setSection(Section.NEW)
    assert(!worker.shouldProcess(b.build(), None))
  }

  ("not process if dealer") in new Fixture {
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.setSellerType(SellerType.COMMERCIAL)
    val result = worker.process(b.build(), None)
    assert(result.updateOfferFunc.isEmpty && b.getTagList.size == Set(testTag).size)
  }

  ("process if bad car condition") in new Fixture {
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.getStateBuilder.setCondition(Condition.TO_PARTS)
    assertContainsTags(b.build(), Set(testTag, Tag), worker)
  }

  ("not process if bad vin resolution") in new Fixture {
    val b = sourceOffer.toBuilder
    val res = VinIndexResolution.newBuilder().setVersion(1)
    res
      .addEntriesBuilder()
      .setPart(ResolutionPart.RP_RESTRICTED)
      .setStatus(VinResolutionEnums.Status.INVALID)
      .build()
    b.getOfferAutoruBuilder.getVinResolutionBuilder.setResolution(res).setVersion(1)
    val result = worker.process(b.build(), None)
    assert(result.updateOfferFunc.isEmpty && b.getTagList.size == Set(testTag).size)
  }

  private def assertContainsTags(offer: Offer, tags: Set[String], worker: SafeDealTag): Unit = {
    val result = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(result.getTagList.size == tags.size)
    assert(result.getTagList.asScala.forall(tags.contains))
  }
}
