package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.OwnerExpensesModel.{InsuranceCompany, OsagoInsurance, TransportTax}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.SellerType
import ru.yandex.vos2.OfferModel.{Offer, OfferService}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.commonfeatures.FeaturesManager

import scala.jdk.CollectionConverters._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OsagoInsuranceWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with InitTestDbs {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val featuresMocked = mock[FeaturesManager]
    val mockFeature: Feature[Boolean] = mock[Feature[Boolean]]

    val worker = new OsagoInsuranceWorkerYdb(
      components.regionTree
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresMocked
    }

    val sourceOffer: Offer = {
      val offerBuilder = Offer.newBuilder()
      offerBuilder.setTimestampUpdate(0L)
      offerBuilder.setOfferService(OfferService.OFFER_AUTO)
      offerBuilder.setUserRef("test-user")
      val b = AutoruOffer.newBuilder()
      b.getSellerBuilder.getPlaceBuilder.setGeobaseId(121522)
      b.getCarInfoBuilder.setHorsePower(100)
      b.setSellerType(SellerType.PRIVATE)
      b.setCategory(Category.CARS)
      b.setSection(Section.USED)
      b.setVersion(10)
      offerBuilder.setOfferAutoru(b)
      offerBuilder.build()
    }
  }

  ("process valid offer") in new Fixture {
    when(mockFeature.value).thenReturn(true)
    when(featuresMocked.OsagoInsurance).thenReturn(mockFeature)

    assert(worker.shouldProcess(sourceOffer, None).shouldProcess)
    val result = worker.process(sourceOffer, None).updateOfferFunc.get(sourceOffer)
    val resultOsago = result.getOfferAutoru.getOwnerExpenses.getOsagoInsuranceList.asScala
    assert(resultOsago.head.getInsuranceCompany == InsuranceCompany.TINKOFF)
    assert(resultOsago.head.getPrice == 1952)
    assert(resultOsago.size == 1)

  }

  ("process valid offer with tax") in new Fixture {
    when(mockFeature.value).thenReturn(true)
    when(featuresMocked.OsagoInsurance).thenReturn(mockFeature)

    val tax =
      TransportTax
        .newBuilder()
        .setTaxByYear(999)
        .setYear(2019)
        .build()

    val offerBuilder = sourceOffer.toBuilder
    offerBuilder.getOfferAutoruBuilder.getOwnerExpensesBuilder
      .setTransportTax(tax)

    val offer = offerBuilder.build()

    assert(worker.shouldProcess(offer, None).shouldProcess)
    val result = worker.process(offer, None).updateOfferFunc.get(offer)
    val ownerExpenses = result.getOfferAutoru.getOwnerExpenses
    val osago = ownerExpenses.getOsagoInsuranceList.asScala
    assert(osago.head.getInsuranceCompany == InsuranceCompany.TINKOFF)
    assert(osago.head.getPrice == 1952)
    assert(osago.size == 1)
    assert(ownerExpenses.getTransportTax == tax)

  }

  ("process erevan offer") in new Fixture {
    when(mockFeature.value).thenReturn(true)
    when(featuresMocked.OsagoInsurance).thenReturn(mockFeature)

    val tax =
      TransportTax
        .newBuilder()
        .setTaxByYear(999)
        .setYear(2019)
        .build()

    val osago = OsagoInsurance
      .newBuilder()
      .setInsuranceCompany(InsuranceCompany.TINKOFF)
      .setPrice(1000)
      .build()

    val offerBuilder = sourceOffer.toBuilder
    offerBuilder.getOfferAutoruBuilder.getOwnerExpensesBuilder
      .setTransportTax(tax)
      .addOsagoInsurance(osago)

    offerBuilder.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder
      .setGeobaseId(10262)

    val offer = offerBuilder.build()

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val result = worker.process(offer, None).updateOfferFunc.get(offer)

    val ownerExpenses = result.getOfferAutoru.getOwnerExpenses
    val osagoList = ownerExpenses.getOsagoInsuranceList.asScala
    assert(osagoList.isEmpty)
    assert(ownerExpenses.getTransportTax == tax)
  }

  ("clear osago when feature is disabled") in new Fixture {
    when(mockFeature.value).thenReturn(false)
    when(featuresMocked.OsagoInsurance).thenReturn(mockFeature)

    val tax =
      TransportTax
        .newBuilder()
        .setTaxByYear(999)
        .setYear(2019)
        .build()

    val osago = OsagoInsurance
      .newBuilder()
      .setInsuranceCompany(InsuranceCompany.TINKOFF)
      .setPrice(1000)
      .build()

    val offerBuilder = sourceOffer.toBuilder
    offerBuilder.getOfferAutoruBuilder.getOwnerExpensesBuilder
      .setTransportTax(tax)
      .addOsagoInsurance(osago)

    val offer = offerBuilder.build()

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val result = worker.process(offer, None).updateOfferFunc.get(offer)
    val ownerExpenses = result.getOfferAutoru.getOwnerExpenses
    val osagoList = ownerExpenses.getOsagoInsuranceList.asScala
    assert(osagoList.isEmpty)
    assert(ownerExpenses.getTransportTax == tax)

  }

}
