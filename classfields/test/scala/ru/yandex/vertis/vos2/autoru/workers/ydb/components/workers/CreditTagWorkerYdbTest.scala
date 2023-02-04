package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.verify
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.vin.VinResolutionEnums
import ru.auto.api.vin.VinResolutionEnums.ResolutionPart
import ru.auto.api.vin.VinResolutionModel.VinIndexResolution
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.shark.proto.Api.{PreconditionsRequest, PreconditionsResponse}
import ru.yandex.vertis.shark.proto.Api.PreconditionsResponse.Precondition
import ru.yandex.vertis.shark.proto.Api.PreconditionsResponse.Precondition.ProductPrecondition
import ru.yandex.vertis.shark.proto.AutoSellerType
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.CreditTagWorkerYdb.AllowedForCreditTag
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.{Condition, CustomHouseStatus, SellerType, SteeringWheel}
import ru.yandex.vos2.BasicsModel
import ru.yandex.vos2.OfferModel.{Offer, OfferService}
import ru.yandex.vos2.autoru.services.shark.SharkClient
import ru.yandex.vos2.commonfeatures.FeaturesManager

import scala.jdk.CollectionConverters._
import scala.util.Success
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CreditTagWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  private val testTag: String = "test-tag"

  abstract private class Fixture {
    val mockedFeatureManager: FeaturesManager = mock[FeaturesManager]
    val mockFeatureIncreasedCreditAllowedPrice: Feature[Boolean] = mock[Feature[Boolean]]
    val mockFeatureCreditTagYdbForSectionNew: Feature[Boolean] = mock[Feature[Boolean]]
    val mockSharkClient: SharkClient = mock[SharkClient]
    when(mockFeatureIncreasedCreditAllowedPrice.value).thenReturn(true)
    when(mockFeatureCreditTagYdbForSectionNew.value).thenReturn(false)
    when(mockedFeatureManager.IncreasedCreditAllowedPrice).thenReturn(mockFeatureIncreasedCreditAllowedPrice)
    when(mockedFeatureManager.CreditTagYdbForSectionNew).thenReturn(mockFeatureCreditTagYdbForSectionNew)

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
        .addEntriesBuilder()
        .setPart(ResolutionPart.RP_LEGAL_GROUP)
        .setStatus(VinResolutionEnums.Status.OK)
        .build()
      b.getVinResolutionBuilder.setResolution(res).setVersion(1)
      b.getPriceBuilder
        .setPriceRub(300000)
        .setPrice(300000)
        .setCreated(0L)
        .setCurrency(BasicsModel.Currency.RUB)
      b.setVersion(10)
      offerBuilder.setOfferAutoru(b)
      offerBuilder.addTag(testTag)
      offerBuilder.build()
    }

    val productPrecondition: ProductPrecondition = ProductPrecondition
      .newBuilder()
      .setProductId("alfabank-1")
      .setInitialFee(100000)
      .setTermsMonths(36)
      .setInterestRate(5f)
      .setMonthlyPayment(26973)
      .build()

    val precondition: Precondition = PreconditionsResponse.Precondition
      .newBuilder()
      .setObjectId(sourceOffer.getOfferID)
      .addProductPreconditions(productPrecondition)
      .build()

    val preconditionsResponse: PreconditionsResponse = PreconditionsResponse
      .newBuilder()
      .addPreconditions(
        precondition
      )
      .build()

    val emptyPreconditionsResponse: PreconditionsResponse = PreconditionsResponse
      .newBuilder()
      .build()

    when(mockSharkClient.getPreconditions(?)(?)).thenReturn(Success(emptyPreconditionsResponse))

    val worker = new CreditTagWorkerYdb(
      mockSharkClient
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = mockedFeatureManager
    }
  }

  ("process valid offer") in new Fixture {
    assertContainsTags(sourceOffer, Set(testTag, AllowedForCreditTag), worker)
  }

  ("not process if not cars") in new Fixture {
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.setCategory(Category.TRUCKS)
    assert(!worker.shouldProcess(b.build(), None).shouldProcess)
  }

  ("not process if not used") in new Fixture {
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.setSection(Section.NEW)
    assert(!worker.shouldProcess(b.build(), None).shouldProcess)
  }

  ("process if dealer") in new Fixture {
    val offerBuilder = sourceOffer.toBuilder
    offerBuilder.getOfferAutoruBuilder.setSellerType(SellerType.COMMERCIAL)
    assert(worker.shouldProcess(offerBuilder.build(), None).shouldProcess)
  }

  ("process if bad car condition") in new Fixture {
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.getStateBuilder.setCondition(Condition.TO_PARTS)
    assertContainsTags(b.build(), Set(testTag, AllowedForCreditTag), worker)
  }

  ("not process if not cleared") in new Fixture {
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.getDocumentsBuilder.setCustomHouseState(CustomHouseStatus.NOT_CLEARED)
    val result = worker.process(b.build(), None)
    assert(result.updateOfferFunc.isEmpty && b.getTagList.size == Set(testTag).size)
  }

  ("not process if bad vin resolution") in new Fixture {
    val b = sourceOffer.toBuilder
    val res = VinIndexResolution.newBuilder().setVersion(1)
    res
      .addEntriesBuilder()
      .setPart(ResolutionPart.RP_WANDED)
      .setStatus(VinResolutionEnums.Status.INVALID)
      .build()
    b.getOfferAutoruBuilder.getVinResolutionBuilder.setResolution(res).setVersion(1)
    val result = worker.process(b.build(), None)
    assert(result.updateOfferFunc.isEmpty && b.getTagList.size == Set(testTag).size)
  }

  ("not process if right steering wheel") in new Fixture {
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.getCarInfoBuilder.setSteeringWheel(SteeringWheel.RIGHT)
    val result = worker.process(b.build(), None)
    assert(result.updateOfferFunc.isEmpty && b.getTagList.size == Set(testTag).size)
  }

  ("process if section new") in new Fixture {
    when(mockFeatureCreditTagYdbForSectionNew.value).thenReturn(true)
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.setSection(Section.NEW)
    assert(worker.shouldProcess(b.build(), None).shouldProcess)
  }

  ("not process if section used without vin resolution") in new Fixture {
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.setSection(Section.USED).clearVinResolution()
    b.clearTag()
    val offer = b.build()
    val result = worker.process(offer, None)
    val updateOffer = result.updateOfferFunc.map(_.apply(offer))
    assert(updateOffer.isEmpty)
  }

  ("process if section new without vin resolution") in new Fixture {
    when(mockFeatureCreditTagYdbForSectionNew.value).thenReturn(true)
    when(mockSharkClient.getPreconditions(?)(?)).thenReturn(Success(preconditionsResponse))
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.setSection(Section.NEW).clearVinResolution()
    b.clearTag()
    val offer = b.build()
    val result = worker.process(offer, None)
    val updateOffer = result.updateOfferFunc.map(_.apply(offer))
    assert(updateOffer.nonEmpty)
    assert(updateOffer.get.getTagList.asScala.contains(CreditTagWorkerYdb.AllowedForCreditTag))
    assert(updateOffer.get.getSharkMonthlyPaymentRub == productPrecondition.getMonthlyPayment)
  }

  ("keep others tags if no update") in new Fixture {
    when(mockFeatureCreditTagYdbForSectionNew.value).thenReturn(true)
    when(mockSharkClient.getPreconditions(?)(?)).thenReturn(Success(preconditionsResponse))
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder.setSection(Section.NEW).clearVinResolution()
    b.clearTag.addAllTag(Seq(CreditTagWorkerYdb.AllowedForCreditTag, "other_tag").asJava)
    val offer = b.build()
    val result = worker.process(offer, None)
    val updateOffer = result.updateOfferFunc.map(_.apply(offer))
    assert(updateOffer.nonEmpty)
    assert(updateOffer.get.getTagList.asScala.contains(CreditTagWorkerYdb.AllowedForCreditTag))
    assert(updateOffer.get.getTagList.asScala.contains("other_tag"))
    assert(updateOffer.get.getSharkMonthlyPaymentRub == productPrecondition.getMonthlyPayment)
  }

  ("set offer seller type in the request") in new Fixture {
    when(mockSharkClient.getPreconditions(?)(?)).thenReturn(Success(preconditionsResponse))
    val b = sourceOffer.toBuilder
    b.getOfferAutoruBuilder
      .setSection(Section.NEW)
      .setSellerType(SellerType.COMMERCIAL)
    b.clearTag()
    val offer = b.build()
    worker.process(offer, None)
    verify(mockSharkClient).getPreconditions(
      argThat[PreconditionsRequest](
        _.getObjectInfo(0).getObjectPayload().getAuto().getOfferSellerType() == AutoSellerType.COMMERCIAL
      )
    )(?)
  }

  private def assertContainsTags(offer: Offer, tags: Set[String], worker: CreditTagWorkerYdb): Unit = {
    val result = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(result.getTagList.size == tags.size)
    assert(result.getTagList.asScala.forall(tags.contains))
  }
}
