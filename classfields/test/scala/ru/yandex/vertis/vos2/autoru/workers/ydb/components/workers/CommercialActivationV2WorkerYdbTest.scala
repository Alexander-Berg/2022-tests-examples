package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.CommercialActivationV2WorkerYdbTest.RichOfferBuilderTest
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.AutoruModelUtils.AutoruModelRichOffer
import ru.yandex.vos2.autoru.model.{AutoruSale, AutoruSaleStatus, TestUtils}
import ru.yandex.vos2.autoru.services.cabinet.CabinetClient
import ru.yandex.vos2.autoru.services.salesman.SalesmanClient
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.util.http.MockHttpClientHelper
import ru.yandex.vos2.{getNow, OfferIRef, OfferModel}

import scala.util.{Failure, Try}

class CommercialActivationV2WorkerYdbTest
  extends AnyWordSpec
  with MockitoSupport
  with InitTestDbs
  with Matchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with MockHttpClientHelper {
  implicit val traced: Traced = Traced.empty
  initDbs()

  val featureRegistry = FeatureRegistryFactory.inMemory()
  val featuresManager = new FeaturesManager(featureRegistry)

  featureRegistry.updateFeature(featuresManager.CommercialActivationV1Ydb.name, true)
  featureRegistry.updateFeature(featuresManager.CommercialActivationV2Ydb.name, true)
  featureRegistry.updateFeature(featuresManager.CommercialActivationV2UseCabinetApi.name, false)

  val offersWriter = components.offersWriter
  val salesmanClient = mock[SalesmanClient]
  val cabinetClient = mock[CabinetClient]

  val TestOfferId = 1044159039
  val TestOfferRef = s"$TestOfferId-33be8"

  override protected def beforeEach(): Unit = {
    featureRegistry.updateFeature(featuresManager.CommercialActivationV2UseCabinetApi.name, false)

    components.autoruSalesDao.setStatus(
      id = TestOfferId,
      expectedStatuses = Seq(),
      newStatus = AutoruSaleStatus.STATUS_WAITING_ACTIVATION
    )
  }

  abstract private class Fixture {
    val offer: Offer

    val featureRegistry = FeatureRegistryFactory.inMemory()
    val featuresManager = new FeaturesManager(featureRegistry)

    val worker = new CommercialActivationV2WorkerYdb(
      offersWriter,
      salesmanClient,
      cabinetClient
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }
  }

  "Commercial Activation Stage" should {
    "activate commercial offer" in new Fixture {
      override val offer: Offer = TestUtils.createOffer(getNow, dealer = true).build()
      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Try(true))
      val result = worker.process(offer, None)

      val clientId: OfferIRef = offer.getUserRef.filter(_.isDigit).toLong
      verify(salesmanClient).addGoods(clientId, offer.getOfferID, offer.oldCategoryId, offer.getOfferAutoru.getSection)
    }

    "skip private user offer activation" in new Fixture {
      override val offer: Offer = TestUtils.createOffer(getNow, dealer = false).build()
      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Try(true))
      val result = worker.process(offer, None)

      verifyNoMoreInteractions(salesmanClient)
    }

    "reschedule stage of request error" in new Fixture {

      override val offer: Offer = TestUtils.createOffer(getNow, dealer = true).build()
      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Failure(new IllegalArgumentException))
      val result = worker.process(offer, None)

      val clientId: OfferIRef = offer.getUserRef.filter(_.isDigit).toLong
      verify(salesmanClient).addGoods(clientId, offer.getOfferID, offer.oldCategoryId, offer.getOfferAutoru.getSection)

      result.nextCheck.nonEmpty shouldBe true
    }

    "activate commercial offer without multiposting" in new Fixture {
      featureRegistry.updateFeature(featuresManager.CommercialActivationV2UseCabinetApi.name, false)

      override val offer: Offer = TestUtils
        .createOffer(getNow, dealer = true)
        .clearFlag()
        .addFlag(OfferModel.OfferFlag.OF_NEED_ACTIVATION)
        .clearMultiposting()
        .build()
      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Try(true))
      val result = worker.process(offer, None)

      val clientId: OfferIRef = offer.getUserRef.filter(_.isDigit).toLong
      verify(salesmanClient).addGoods(clientId, offer.getOfferID, offer.oldCategoryId, offer.getOfferAutoru.getSection)
    }

    "activate commercial offer without multiposting [multiposting_enabled = false]" in new Fixture {
      featureRegistry.updateFeature(featuresManager.CommercialActivationV2UseCabinetApi.name, true)

      override val offer: Offer = TestUtils
        .createOffer(getNow, dealer = true)
        .clearFlag()
        .addFlag(OfferModel.OfferFlag.OF_NEED_ACTIVATION)
        .withMultiposting(
          multipostingStatus = CompositeStatus.CS_INACTIVE,
          autoruStatus = CompositeStatus.CS_INACTIVE,
          autoruEnabled = true
        )
        .build()
      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Try(true))
      when(cabinetClient.isMultipostingEnabled(?)(?)).thenReturn(Try(false))
      val result = worker.process(offer, None)

      val clientId: OfferIRef = offer.getUserRef.filter(_.isDigit).toLong
      verify(salesmanClient).addGoods(clientId, offer.getOfferID, offer.oldCategoryId, offer.getOfferAutoru.getSection)
    }

    "activate commercial multiposting offer with active auto.ru classified" in new Fixture {
      override val offer: Offer = TestUtils
        .createOffer(getNow, dealer = true)
        .clearFlag()
        .addFlag(OfferModel.OfferFlag.OF_NEED_ACTIVATION)
        .withMultiposting(
          multipostingStatus = CompositeStatus.CS_ACTIVE,
          autoruStatus = CompositeStatus.CS_ACTIVE,
          autoruEnabled = true
        )
        .build()

      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Try(true))
      val result = worker.process(offer, None)

      val clientId: OfferIRef = offer.getUserRef.filter(_.isDigit).toLong
      verify(salesmanClient).addGoods(clientId, offer.getOfferID, offer.oldCategoryId, offer.getOfferAutoru.getSection)
    }

    "activate commercial multiposting offer with active auto.ru classified [multiposting_enabled = true]" in new Fixture {
      override val offer: Offer = TestUtils
        .createOffer(getNow, dealer = true)
        .clearFlag()
        .addFlag(OfferModel.OfferFlag.OF_NEED_ACTIVATION)
        .withMultiposting(
          multipostingStatus = CompositeStatus.CS_ACTIVE,
          autoruStatus = CompositeStatus.CS_ACTIVE,
          autoruEnabled = true
        )
        .build()

      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Try(true))
      when(cabinetClient.isMultipostingEnabled(?)(?)).thenReturn(Try(true))
      val result = worker.process(offer, None)

      val clientId: OfferIRef = offer.getUserRef.filter(_.isDigit).toLong
      verify(salesmanClient).addGoods(clientId, offer.getOfferID, offer.oldCategoryId, offer.getOfferAutoru.getSection)
    }

    "activate commercial multiposting offer with enabled auto.ru classified [multiposting_enabled = true]" in new Fixture {
      override val offer: Offer = TestUtils
        .createOffer(getNow, dealer = true)
        .clearFlag()
        .addFlag(OfferModel.OfferFlag.OF_NEED_ACTIVATION)
        .withMultiposting(
          multipostingStatus = CompositeStatus.CS_ACTIVE,
          autoruStatus = CompositeStatus.CS_UNKNOWN,
          autoruEnabled = true
        )
        .build()

      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Try(true))
      when(cabinetClient.isMultipostingEnabled(?)(?)).thenReturn(Try(true))
      val result = worker.process(offer, None)

      val clientId: OfferIRef = offer.getUserRef.filter(_.isDigit).toLong
      verify(salesmanClient).addGoods(clientId, offer.getOfferID, offer.oldCategoryId, offer.getOfferAutoru.getSection)
    }

    "deactivate commercial multiposting offer with not enabled auto.ru classified" in new Fixture {
      override val offer: Offer = TestUtils
        .createOffer(getNow, dealer = true)
        .withOfferId(id = TestOfferId, ref = TestOfferRef)
        .clearFlag()
        .addFlag(OfferModel.OfferFlag.OF_NEED_ACTIVATION)
        .withMultiposting(
          multipostingStatus = CompositeStatus.CS_ACTIVE,
          autoruStatus = CompositeStatus.CS_ACTIVE,
          autoruEnabled = false
        )
        .build()

      val result = worker.process(offer, None)
      val newOffer = result.updateOfferFunc.get(offer)

      newOffer.getFlagList.contains(OfferFlag.OF_NEED_ACTIVATION) shouldBe false
      newOffer.getFlagList.contains(OfferFlag.OF_INACTIVE) shouldBe true

      val oldSale: AutoruSale = components.autoruSalesDao.getOffer(offer.getOfferIRef).value
      oldSale.status shouldBe AutoruSaleStatus.STATUS_HIDDEN
    }

    "dont deactivate commercial multiposting offer with not enabled auto.ru classified and moto category" in new Fixture {
      override val offer: Offer = TestUtils
        .createOffer(getNow, dealer = true, category = Category.MOTO)
        .clearFlag()
        .addFlag(OfferModel.OfferFlag.OF_NEED_ACTIVATION)
        .clearMultiposting()
        .build()

      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Try(true))
      when(cabinetClient.isMultipostingEnabled(?)(?)).thenReturn(Try(true))
      val result = worker.process(offer, None)

      val clientId: OfferIRef = offer.getUserRef.filter(_.isDigit).toLong
      verify(salesmanClient).addGoods(clientId, offer.getOfferID, offer.oldCategoryId, offer.getOfferAutoru.getSection)
    }

    "dont deactivate commercial multiposting offer with not enabled auto.ru classified and trucks category" in new Fixture {
      override val offer: Offer = TestUtils
        .createOffer(getNow, dealer = true, category = Category.TRUCKS)
        .clearFlag()
        .addFlag(OfferModel.OfferFlag.OF_NEED_ACTIVATION)
        .clearMultiposting()
        .build()

      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Try(true))
      when(cabinetClient.isMultipostingEnabled(?)(?)).thenReturn(Try(true))
      val result = worker.process(offer, None)

      val clientId: OfferIRef = offer.getUserRef.filter(_.isDigit).toLong
      verify(salesmanClient).addGoods(clientId, offer.getOfferID, offer.oldCategoryId, offer.getOfferAutoru.getSection)
    }

    "deactivate commercial multiposting offer with not active auto.ru classified" in new Fixture {
      override val offer: Offer = TestUtils
        .createOffer(getNow, dealer = true)
        .withOfferId(id = TestOfferId, ref = TestOfferRef)
        .clearFlag()
        .addFlag(OfferModel.OfferFlag.OF_NEED_ACTIVATION)
        .withMultiposting(
          multipostingStatus = CompositeStatus.CS_ACTIVE,
          autoruStatus = CompositeStatus.CS_INACTIVE,
          autoruEnabled = false
        )
        .build()

      val result = worker.process(offer, None)

      val newOffer = result.updateOfferFunc.get(offer)
      newOffer.getFlagList.contains(OfferFlag.OF_NEED_ACTIVATION) shouldBe false
      newOffer.getFlagList.contains(OfferFlag.OF_INACTIVE) shouldBe true

      val oldSale: AutoruSale = components.autoruSalesDao.getOffer(offer.getOfferIRef).value
      oldSale.status shouldBe AutoruSaleStatus.STATUS_HIDDEN
    }

    "deactivate commercial multiposting offer with inactive multiposting status" in new Fixture {
      override val offer: Offer = TestUtils
        .createOffer(getNow, dealer = true)
        .withOfferId(id = TestOfferId, ref = TestOfferRef)
        .clearFlag()
        .addFlag(OfferModel.OfferFlag.OF_NEED_ACTIVATION)
        .withMultiposting(
          multipostingStatus = CompositeStatus.CS_INACTIVE,
          autoruStatus = CompositeStatus.CS_ACTIVE,
          autoruEnabled = true
        )
        .build()

      val result = worker.process(offer, None)

      val newOffer = result.updateOfferFunc.get(offer)
      newOffer.getFlagList.contains(OfferFlag.OF_NEED_ACTIVATION) shouldBe false
      newOffer.getFlagList.contains(OfferFlag.OF_INACTIVE) shouldBe true

      val oldSale: AutoruSale = components.autoruSalesDao.getOffer(offer.getOfferIRef).value
      oldSale.status shouldBe AutoruSaleStatus.STATUS_HIDDEN
    }

    "deactivate commercial multiposting offer with inactive multiposting status [multiposting_enabled = true]" in new Fixture {
      featureRegistry.updateFeature(featuresManager.CommercialActivationV2UseCabinetApi.name, true)

      override val offer: Offer = TestUtils
        .createOffer(getNow, dealer = true)
        .withOfferId(id = TestOfferId, ref = TestOfferRef)
        .clearFlag()
        .addFlag(OfferModel.OfferFlag.OF_NEED_ACTIVATION)
        .withMultiposting(
          multipostingStatus = CompositeStatus.CS_INACTIVE,
          autoruStatus = CompositeStatus.CS_ACTIVE,
          autoruEnabled = true
        )
        .build()

      when(cabinetClient.isMultipostingEnabled(?)(?)).thenReturn(Try(true))

      val result = worker.process(offer, None)

      val newOffer = result.updateOfferFunc.get(offer)
      newOffer.getFlagList.contains(OfferFlag.OF_NEED_ACTIVATION) shouldBe false
      newOffer.getFlagList.contains(OfferFlag.OF_INACTIVE) shouldBe true

      val oldSale: AutoruSale = components.autoruSalesDao.getOffer(offer.getOfferIRef).value
      oldSale.status shouldBe AutoruSaleStatus.STATUS_HIDDEN
    }
  }

}

object CommercialActivationV2WorkerYdbTest {

  implicit class RichOfferBuilderTest(builder: Offer.Builder) {

    def withOfferId(id: Long, ref: String) = {
      builder
        .setOfferIRef(id)
        .setOfferID(ref)
    }

    def withMultiposting(multipostingStatus: CompositeStatus, autoruStatus: CompositeStatus, autoruEnabled: Boolean) = {
      builder.setMultiposting {
        OfferModel.Multiposting
          .newBuilder()
          .setStatus(multipostingStatus)
          .addClassifieds {
            OfferModel.Multiposting.Classified
              .newBuilder()
              .setStatus(autoruStatus)
              .setEnabled(autoruEnabled)
              .setName(OfferModel.Multiposting.Classified.ClassifiedName.AUTORU)
          }
          .build()
      }
    }
  }

}
