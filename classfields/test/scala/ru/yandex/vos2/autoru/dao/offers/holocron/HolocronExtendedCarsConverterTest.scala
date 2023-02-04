package ru.yandex.vos2.autoru.dao.offers.holocron

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import ru.auto.api.ApiOfferModel.{Score, Section}
import ru.auto.api.CommonModel.{RecallReason => ApiRecallReason}
import ru.auto.api.cert.CertModel.BrandCertStatus
import ru.vertis.holocron.autoru.{RecallReason => HoloRecallReason}
import ru.vertis.holocron.common.{Action, HoloOffer}
import ru.yandex.auto.message.CatalogSchema.CatalogCardMessage
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.protobuf.Holocron
import ru.yandex.vertis.validation.MessageValidator
import ru.yandex.vertis.validation.model.Valid
import ru.yandex.vertis.validation.validators.{CompositeMessageValidator, RequiredMessageValidator, SpecialFieldValidator}
import ru.yandex.vos2.AutoruModel.AutoruOffer.CustomHouseStatus
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.CertificationModel.CertInfo.CertStatus
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.catalog.cars.CarsCatalog
import ru.yandex.vos2.autoru.catalog.cars.model.CarCard
import ru.yandex.vos2.autoru.catalog.moto.MotoCatalog
import ru.yandex.vos2.autoru.catalog.trucks.TrucksCatalog
import ru.yandex.vos2.autoru.dao.offers.holocron.converters.cars.HolocronExtendedCarsConverter
import ru.yandex.vos2.autoru.model.TestUtils.createOffer
import ru.yandex.vos2.autoru.model.extdata.BanReasons
import ru.yandex.vos2.autoru.utils.TestDataEngine
import ru.yandex.vos2.autoru.utils.converters.offerform.{OfferFormConverter, SalonConverter}
import ru.yandex.vos2.autoru.utils.currency.CurrencyRates
import ru.yandex.vos2.autoru.utils.geo.Tree
import ru.yandex.vos2.autoru.utils.validators.{OfferValidator, OfferValidatorImpl, ValidResult, ValidationResult}
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.services.mds.{AutoruAllNamespaceSettings, AutoruPanoramaNamespaceSettings, MdsPhotoUtils}
import ru.yandex.vos2.util.Protobuf

import scala.io.BufferedSource

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class HolocronExtendedCarsConverterTest extends AnyWordSpec with Matchers with MockitoSupport with OptionValues {

  private val mdsPhotoUtils: MdsPhotoUtils =
    MdsPhotoUtils("http://avatars-int.mds.yandex.net:13000", "//avatars.mds.yandex.net", AutoruAllNamespaceSettings)

  private val mdsPanoramasUtils: MdsPhotoUtils =
    MdsPhotoUtils(
      "http://avatars-int.mds.yandex.net:13000",
      "//avatars.mds.yandex.net",
      AutoruPanoramaNamespaceSettings
    )

  private val offerValidator = new OfferValidator {
    override def validate(req: OfferValidatorImpl.ValidationRequest): ValidationResult = ValidResult(Nil, req.formData)
  }

  private class Fixture {

    val salonConverter: SalonConverter = mock[SalonConverter]

    val featuresRegistry: FeatureRegistry = FeatureRegistryFactory.inMemory()
    val featuresManager = new FeaturesManager(featuresRegistry)

    val banReasons: BanReasons = mock[BanReasons]

    val regionTree: Tree = Tree.from(TestDataEngine)

    val carsCatalog: CarsCatalog = mock[CarsCatalog]

    val trucksCatalog: TrucksCatalog = mock[TrucksCatalog]

    val motoCatalog: MotoCatalog = mock[MotoCatalog]

    val currencyRates: CurrencyRates = mock[CurrencyRates]

    val offerFormConverter = new OfferFormConverter(
      mdsPhotoUtils,
      regionTree,
      mdsPanoramasUtils,
      offerValidator,
      salonConverter,
      currencyRates,
      featuresManager,
      banReasons,
      carsCatalog,
      trucksCatalog,
      motoCatalog
    )

    val holocronExtendedConverter = new HolocronExtendedCarsConverter(
      carsCatalog,
      currencyRates,
      offerFormConverter,
      regionTree
    )

    when(carsCatalog.getMarkByCode(?)).thenReturn(None)
    when(carsCatalog.getModelByCode(?, ?)).thenReturn(None)

    {
      val catalogCardMessageBuilder = CatalogCardMessage.newBuilder()
      catalogCardMessageBuilder.setVersion(1)
      catalogCardMessageBuilder.getSuperGenerationBuilder.setSegment("Segment")
      catalogCardMessageBuilder.getSuperGenerationBuilder.setGroup("Group")
      catalogCardMessageBuilder.getSuperGenerationBuilder.setVersion(1)
      when(carsCatalog.getCardByTechParamId(?)).thenReturn(Some(CarCard(catalogCardMessageBuilder.build())))
    }

    when(currencyRates.convert(?, ?, ?)).thenReturn(Some(BigDecimal(10)))
  }

  "HolocronExtendedCarsConverter" should {
    "get change version from last event" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getSimpleHolocronStatusBuilder
        .setLastSentChangeVersion(40)
        .addLastEventsBuilder()
        .setChangeVersion(50)
      offer.getHolocronStatusBuilder
        .setLastSentChangeVersion(20)
        .addLastEventsBuilder()
        .setChangeVersion(30)
      val holoOffer: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
      holoOffer.getChangeVersion shouldBe 31
    }

    "get last sent change version" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getSimpleHolocronStatusBuilder
        .setLastSentChangeVersion(40)
      offer.getHolocronStatusBuilder
        .setLastSentChangeVersion(20)
      val holoOffer: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
      holoOffer.getChangeVersion shouldBe 21
    }

    "should set action DEACTIVATE" when {
      "previous and current states both are inactive and last event not exists" in new Fixture {
        val offer: Offer.Builder = createOffer()
        offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
        offer.getSimpleHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE).addLastEventsBuilder()
        offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
        holocronExtendedConverter.convert(offer.build(), DateTime.now()).getAction shouldBe Action.DEACTIVATE
      }
    }

    "set action ACTIVATE" when {
      "previous state is INACTIVE and current is ACTIVE" in new Fixture {
        val offer: Offer.Builder = createOffer()
        offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
        offer.getSimpleHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
        offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
        assert(holocronExtendedConverter.convert(offer.build(), DateTime.now()).getAction == Action.ACTIVATE)
      }

      "previous status not set" in new Fixture {
        val offer: Offer.Builder = createOffer()
        offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
        offer.getSimpleHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
        assert(holocronExtendedConverter.convert(offer.build(), DateTime.now()).getAction == Action.ACTIVATE)
      }
    }

    "set gear type: ALL" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.setSection(Section.NEW)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getCarInfoBuilder.setGearType("ALL")
      assert(
        holocronExtendedConverter.convert(offer.build(), DateTime.now()).getVosCar.getWheelDrive ==
          "ALL_WHEEL_DRIVE"
      )
    }

    "set gear type: FRONT" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.setSection(Section.NEW)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getCarInfoBuilder.setGearType("FRONT")
      assert(
        holocronExtendedConverter.convert(offer.build(), DateTime.now()).getVosCar.getWheelDrive ==
          "FORWARD_CONTROL"
      )
    }

    "set custom status: cleared" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.setSection(Section.NEW)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getDocumentsBuilder.setCustomHouseState(CustomHouseStatus.CLEARED)
      assert(holocronExtendedConverter.convert(offer.build(), DateTime.now()).getVosCar.getIsCustom)
    }

    "set custom status: not cleared" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.setSection(Section.NEW)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getDocumentsBuilder.setCustomHouseState(CustomHouseStatus.NOT_CLEARED)
      assert(!holocronExtendedConverter.convert(offer.build(), DateTime.now()).getVosCar.getIsCustom)
    }

    "set photos" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.setSection(Section.NEW)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setIsMain(true)
        .setOrder(1)
        .setName("2076279-eda99c4a7fec758ee27b353b3e2de4a1")
        .setCreated(1575449627120L)
        .setOrigName("2158563-8b96876f8621956586d43dd66d0192d1")
      assert(holocronExtendedConverter.convert(offer.build(), DateTime.now()).getVosCar.getPhotosCount == 1)
      assert(
        holocronExtendedConverter.convert(offer.build(), DateTime.now()).getVosCar.getPhotos(0).getName ==
          "2076279-eda99c4a7fec758ee27b353b3e2de4a1"
      )
    }

    "set active cert" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertId(1)
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setHash("hash")
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setVin("vin")
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setService("service")
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setInspected(1)
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setNumber("number")
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setCreated(1)
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setUpdated(1)
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertStatus(CertStatus.CERT_ACTIVE)
      val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
      holo.getVosCar.getCertification shouldBe "autoru"
    }

    "not set inactive cert" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertId(1)
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setHash("hash")
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setVin("vin")
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setService("service")
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setInspected(1)
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setNumber("number")
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setCreated(1)
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setUpdated(1)
      offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertStatus(CertStatus.CERT_INCORRECT)
      val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
      holo.getVosCar.getCertification shouldBe ""
    }

    "set active brand cert" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getOfferAutoruBuilder.getBrandCertInfoBuilder.setProgramAlias("bmw")
      offer.getOfferAutoruBuilder.getBrandCertInfoBuilder.setVin("vin")
      offer.getOfferAutoruBuilder.getBrandCertInfoBuilder.setCreated(1)
      offer.getOfferAutoruBuilder.getBrandCertInfoBuilder.setCertStatus(BrandCertStatus.BRAND_CERT_ACTIVE)
      val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
      holo.getVosCar.getCertification shouldBe "bmw"
    }

    "not set inactive brand cert" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getOfferAutoruBuilder.getBrandCertInfoBuilder.setProgramAlias("bmw")
      offer.getOfferAutoruBuilder.getBrandCertInfoBuilder.setVin("vin")
      offer.getOfferAutoruBuilder.getBrandCertInfoBuilder.setCreated(1)
      offer.getOfferAutoruBuilder.getBrandCertInfoBuilder.setCertStatus(BrandCertStatus.BRAND_CERT_INACTIVE)
      val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
      holo.getVosCar.getCertification shouldBe ""
    }

    "set new recall reason" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getOfferAutoruBuilder.getRecallInfoBuilder.setReason(ApiRecallReason.SOLD_ON_AVITO)
      offer.getOfferAutoruBuilder.getRecallInfoBuilder.setRecallTimestamp(1)
      val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
      holo.getVosCar.getRecall.getReason shouldBe HoloRecallReason.SOLD_ON_AVITO
    }

    "set empty scoring" in new Fixture {
      val offer: Offer.Builder = createOffer()
      val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
      holo.getVosCar.hasScore shouldBe true
      holo.getVosCar.getScore shouldBe Score.getDefaultInstance
    }

    "holoOffer with empty scoring must be valid" in new Fixture {
      val source: BufferedSource =
        scala.io.Source.fromURL(getClass.getResource("/offerForHolocronConvert.json"), "UTF-8")
      val offer =
        try {
          val json = source.getLines.mkString("\n")
          Protobuf.fromJson[Offer](json).toBuilder
        } finally {
          source.close()
        }
      val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())

      val holocronValidator: MessageValidator = {
        val validators = Seq(
          new SpecialFieldValidator(Seq(Holocron.isClassified)),
          new RequiredMessageValidator
        )

        new CompositeMessageValidator(validators)
      }

      holo.getVosCar.hasScore shouldBe true
      holo.getVosCar.getScore shouldBe Score.getDefaultInstance

      holocronValidator.validate(holo) shouldBe Valid
    }

    "set non-empty scoring" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getScoringBuilder.getCurrentScoringBuilder.getHealthScoringBuilder.setScoringValue(1)
      offer.getScoringBuilder.getCurrentScoringBuilder.getTransparencyScoringBuilder.setScoringValue(2)
      offer.getScoringBuilder.getCurrentScoringBuilder.getPriceScoringBuilder.setScoringValue(3)
      val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
      holo.getVosCar.hasScore shouldBe true
      private val scoreBuilder: Score.Builder = Score.newBuilder()
      scoreBuilder.getHealthBuilder.setValue(1)
      scoreBuilder.getTransparencyBuilder.setValue(2)
      scoreBuilder.getPriceBuilder.setValue(3)
      holo.getVosCar.getScore shouldBe scoreBuilder.build()
    }
  }

  "get status update comment empty" in new Fixture {
    val offer: Offer.Builder = createOffer()
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertId(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setHash("hash")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setVin("vin")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setService("service")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setInspected(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setNumber("number")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCreated(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setUpdated(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertStatus(CertStatus.CERT_ACTIVE)
    val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
    holo.getVosCar.getStatusUpdateComment shouldBe ""
  }

  "get status update comment not empty" in new Fixture {
    val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
    val offer: Offer.Builder = createOffer()
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertId(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setHash("hash")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setVin("vin")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setService("service")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setInspected(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setNumber("number")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCreated(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setUpdated(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertStatus(CertStatus.CERT_ACTIVE)
    setStatusHistory(date, offer)
    val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
    holo.getVosCar.getStatusUpdateComment shouldBe "Test comment 9"
  }

  "when reseller value false" in new Fixture {
    val offer: Offer.Builder = createOffer()
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertId(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setHash("hash")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setVin("vin")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setService("service")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setInspected(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setNumber("number")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCreated(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setUpdated(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertStatus(CertStatus.CERT_ACTIVE)
    val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
    holo.getVosCar.getIsReseller shouldBe false
  }

  "when reseller value true" in new Fixture {
    val offer: Offer.Builder = createOffer()
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertId(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setHash("hash")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setVin("vin")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setService("service")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setInspected(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setNumber("number")
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCreated(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setUpdated(1)
    offer.getOfferAutoruBuilder.getCertInfoBuilder.setCertStatus(CertStatus.CERT_ACTIVE)
    offer.getOfferAutoruBuilder.setReseller(true)
    val holo: HoloOffer = holocronExtendedConverter.convert(offer.build(), DateTime.now())
    holo.getVosCar.getIsReseller shouldBe true
  }

  private def addStatusHistory(offer: Offer.Builder, status: CompositeStatus, date: DateTime, comment: String) = {
    offer
      .addStatusHistoryBuilder()
      .setOfferStatus(status)
      .setTimestamp(date.getMillis)
      .setComment(comment)
  }

  private def setStatusHistory(date: DateTime, offer: Offer.Builder) = {
    addStatusHistory(offer, CompositeStatus.CS_DRAFT, date.minusDays(14), "Test comment 1")
    addStatusHistory(offer, CompositeStatus.CS_NEED_ACTIVATION, date.minusDays(13), "Test comment 2")
    addStatusHistory(offer, CompositeStatus.CS_ACTIVE, date.minusDays(13), "Test comment 3")
    addStatusHistory(offer, CompositeStatus.CS_INACTIVE, date.minusDays(12), "Test comment 4")
    addStatusHistory(offer, CompositeStatus.CS_ACTIVE, date.minusDays(11), "Test comment 5")
    addStatusHistory(offer, CompositeStatus.CS_EXPIRED, date.minusDays(10), "Test comment 6")
    addStatusHistory(offer, CompositeStatus.CS_ACTIVE, date.minusDays(9), "Test comment 7")
    addStatusHistory(offer, CompositeStatus.CS_INACTIVE, date.minusDays(8), "Test comment 8")
    addStatusHistory(offer, CompositeStatus.CS_REMOVED, date.minusDays(7), "Test comment 9")
  }
}
