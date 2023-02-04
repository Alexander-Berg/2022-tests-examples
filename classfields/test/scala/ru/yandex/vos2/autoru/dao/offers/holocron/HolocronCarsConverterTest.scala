package ru.yandex.vos2.autoru.dao.offers.holocron

import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import ru.auto.api.ApiOfferModel.Section
import ru.auto.api.CommonModel.SteeringWheel
import ru.vertis.holocron.common.{Action, HoloOffer}
import ru.yandex.auto.message.CatalogSchema.CatalogCardMessage
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.AutoruModel.AutoruOffer.{SteeringWheel => VosSteeringWheel}
import ru.yandex.vos2.BasicsModel.{CompositeStatus, Currency}
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.catalog.cars.CarsCatalog
import ru.yandex.vos2.autoru.catalog.cars.model.CarCard
import ru.yandex.vos2.autoru.catalog.moto.MotoCatalog
import ru.yandex.vos2.autoru.catalog.trucks.TrucksCatalog
import ru.yandex.vos2.autoru.dao.offers.holocron.converters.broker.cars.HoloCarOfferConverter
import ru.yandex.vos2.autoru.dao.offers.holocron.converters.cars.HolocronCarsConverter
import ru.yandex.vos2.autoru.model.TestUtils._
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
class HolocronCarsConverterTest extends AnyWordSpec with Matchers with MockitoSupport with OptionValues {

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
    val featuresRegistry: FeatureRegistry = FeatureRegistryFactory.inMemory()
    val featuresManager = new FeaturesManager(featuresRegistry)

    private val salonConverter = mock[SalonConverter]

    private val banReasons = mock[BanReasons]

    private val regionTree = Tree.from(TestDataEngine)

    val carsCatalog: CarsCatalog = mock[CarsCatalog]

    private val trucksCatalog = mock[TrucksCatalog]

    private val motoCatalog = mock[MotoCatalog]

    private val currencyRates = mock[CurrencyRates]

    private val offerFormConverter = new OfferFormConverter(
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

    val holocronConverter = new HolocronCarsConverter(
      mdsPhotoUtils,
      currencyRates,
      offerFormConverter,
      regionTree
    )

    when(carsCatalog.getMarkByCode(?)).thenReturn(None)
    when(carsCatalog.getModelByCode(?, ?)).thenReturn(None)
    when(carsCatalog.getCardByTechParamId(?)).thenReturn(Some(CarCard(CatalogCardMessage.getDefaultInstance)))
    when(currencyRates.convert(?, ?, ?)).thenReturn(Some(BigDecimal(10)))
  }

  "HolocronCarsConverter" should {
    "set timestamp" in new Fixture {
      val offer: Offer.Builder = createOffer()
      val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getTimestamp shouldBe Timestamps.fromMillis(date.getMillis)
    }

    "set corrected timestamp for non-DEACTIVATE action" in new Fixture {
      val lastSentHoloTimestamp = new DateTime(2020, 5, 9, 5, 0, 0, 0)
      val offer: Offer.Builder = createOffer()
      offer.getSimpleHolocronStatusBuilder.setLastSentHoloTimestamp(
        Timestamps.fromMillis(lastSentHoloTimestamp.getMillis)
      )
      val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getTimestamp shouldBe Timestamps.fromMillis(lastSentHoloTimestamp.plusSeconds(1).getMillis)
    }

    "set corrected timestamp for non-DEACTIVATE action from last events ts" in new Fixture {
      val lastSentHoloTs = new DateTime(2020, 5, 9, 5, 0, 0, 0)
      val maxEventHoloTs = new DateTime(2020, 5, 9, 7, 0, 0, 0)
      val offer: Offer.Builder = createOffer()
      offer.getSimpleHolocronStatusBuilder.setLastSentHoloTimestamp(
        Timestamps.fromMillis(lastSentHoloTs.getMillis)
      )
      offer.getSimpleHolocronStatusBuilder
        .addLastEventsBuilder()
        .setHoloTimestamp(
          Timestamps.fromMillis(maxEventHoloTs.getMillis)
        )
      offer.getSimpleHolocronStatusBuilder
        .addLastEventsBuilder()
        .setHoloTimestamp(
          Timestamps.fromMillis(lastSentHoloTs.getMillis)
        )
      val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getTimestamp shouldBe Timestamps.fromMillis(maxEventHoloTs.plusSeconds(1).getMillis)
    }

    "set archived from status history with no elem" in new Fixture {
      val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
      val offer: Offer.Builder = createOffer()
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
      offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getCar.hasArchived shouldBe false
    }

    "set archived from status history with single non-CS_ACTIVE elem" in new Fixture {
      val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
      val offer: Offer.Builder = createOffer()
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
      offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_INACTIVE)
        .setTimestamp(date.minusDays(14).getMillis)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getCar.getArchived shouldBe Timestamps.fromMillis(date.minusDays(14).getMillis)
    }

    "set archived from status history with single CS_ACTIVE elem" in new Fixture {
      val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
      val offer: Offer.Builder = createOffer()
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
      offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_ACTIVE)
        .setTimestamp(date.minusDays(14).getMillis)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getCar.hasArchived shouldBe false
    }

    "set archived from status history" in new Fixture {
      val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
      val offer: Offer.Builder = createOffer()
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
      offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_DRAFT)
        .setTimestamp(date.minusDays(14).getMillis)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_NEED_ACTIVATION)
        .setTimestamp(date.minusDays(13).getMillis)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_ACTIVE)
        .setTimestamp(date.minusDays(13).getMillis)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_INACTIVE)
        .setTimestamp(date.minusDays(12).getMillis)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_ACTIVE)
        .setTimestamp(date.minusDays(11).getMillis)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_EXPIRED)
        .setTimestamp(date.minusDays(10).getMillis)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_NEED_ACTIVATION)
        .setTimestamp(date.minusDays(9).getMillis)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_ACTIVE)
        .setTimestamp(date.minusDays(8).getMillis)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_INACTIVE)
        .setTimestamp(date.minusDays(7).getMillis)
      offer
        .addStatusHistoryBuilder()
        .setOfferStatus(CompositeStatus.CS_REMOVED)
        .setTimestamp(date.minusDays(6).getMillis)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getCar.getArchived shouldBe Timestamps.fromMillis(date.minusDays(7).getMillis)
    }

    "set timestamp for DEACTIVATE action if status history moment exists" in new Fixture {
      val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
      val offer: Offer.Builder = createOffer()
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
      offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
      setStatusHistory(date, offer)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getAction shouldBe Action.DEACTIVATE
      holoOffer.getTimestamp shouldBe Timestamps.fromMillis(date.minusDays(8).getMillis)
    }

    "set timestamp for DEACTIVATE action if status history exists but last sent holo ts is bigger" in new Fixture {
      val lastSentHoloTimestamp = new DateTime(2020, 5, 9, 5, 0, 0, 0)
      val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
      val offer: Offer.Builder = createOffer()
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
      offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
      offer.getSimpleHolocronStatusBuilder.setLastSentHoloTimestamp(
        Timestamps.fromMillis(lastSentHoloTimestamp.getMillis)
      )
      setStatusHistory(date, offer)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getAction shouldBe Action.DEACTIVATE
      holoOffer.getTimestamp shouldBe Timestamps.fromMillis(lastSentHoloTimestamp.plusSeconds(1).getMillis)
    }

    "set timestamp for DEACTIVATE action, status history exists, last sent holo ts and last events exist" in new Fixture {
      val lastSentHoloTs = new DateTime(2020, 5, 9, 5, 0, 0, 0)
      val maxEventHoloTs = new DateTime(2020, 5, 9, 7, 0, 0, 0)
      val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
      val offer: Offer.Builder = createOffer()
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
      offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
      offer.getSimpleHolocronStatusBuilder.setLastSentHoloTimestamp(
        Timestamps.fromMillis(lastSentHoloTs.getMillis)
      )
      offer.getSimpleHolocronStatusBuilder
        .addLastEventsBuilder()
        .setHoloTimestamp(
          Timestamps.fromMillis(maxEventHoloTs.getMillis)
        )
      offer.getSimpleHolocronStatusBuilder
        .addLastEventsBuilder()
        .setHoloTimestamp(
          Timestamps.fromMillis(lastSentHoloTs.getMillis)
        )
      setStatusHistory(date, offer)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getAction shouldBe Action.DEACTIVATE
      holoOffer.getTimestamp shouldBe Timestamps.fromMillis(maxEventHoloTs.plusSeconds(1).getMillis)
    }

    "set timestamp for DEACTIVATE action if status history moment not exists" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
      offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
      val date = new DateTime(2020, 5, 9, 0, 0, 0, 0)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getAction shouldBe Action.DEACTIVATE
      holoOffer.getTimestamp shouldBe Timestamps.fromMillis(date.getMillis)
    }

    "set timestamp for DEACTIVATE action if status history has no ACTIVE moments" in new Fixture {
      val offer: Offer.Builder = createOffer()
      val date = new DateTime(2020, 11, 19, 0, 0, 0, 0)
      val date1: DateTime = makeDate(2017, 7, 21)
      addStatusHistory(offer, CompositeStatus.CS_BANNED, date1)
      addStatusHistory(offer, CompositeStatus.CS_NEED_ACTIVATION, makeDate(2017, 11, 29))
      addStatusHistory(offer, CompositeStatus.CS_REMOVED, makeDate(2018, 7, 12))
      offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), date)
      holoOffer.getAction shouldBe Action.DEACTIVATE
      holoOffer.getTimestamp shouldBe Timestamps.fromMillis(date1.getMillis)
    }

    "get change version from simple last event" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getSimpleHolocronStatusBuilder
        .setLastSentChangeVersion(40)
        .addLastEventsBuilder()
        .setChangeVersion(50)
      offer.getHolocronStatusBuilder
        .setLastSentChangeVersion(20)
        .addLastEventsBuilder()
        .setChangeVersion(30)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), DateTime.now())
      holoOffer.getChangeVersion shouldBe 51
    }

    "get simple last sent change version" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getSimpleHolocronStatusBuilder
        .setLastSentChangeVersion(40)
      offer.getHolocronStatusBuilder
        .setLastSentChangeVersion(20)
        .addLastEventsBuilder()
        .setChangeVersion(30)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), DateTime.now())
      holoOffer.getChangeVersion shouldBe 41
    }

    "get change version from extended last event" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getHolocronStatusBuilder
        .setLastSentChangeVersion(20)
        .addLastEventsBuilder()
        .setChangeVersion(30)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), DateTime.now())
      holoOffer.getChangeVersion shouldBe 31
    }

    "get extended last sent change version" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getHolocronStatusBuilder
        .setLastSentChangeVersion(20)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), DateTime.now())
      holoOffer.getChangeVersion shouldBe 21
    }

    "should not send deleted photos" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
      offer.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("100500-image1")
        .setDeleted(true)
        .setIsMain(true)
        .setCreated(1)
        .setOrder(1)
      offer.getOfferAutoruBuilder
        .addPhotoBuilder()
        .setName("100500-image2")
        .setDeleted(false)
        .setIsMain(false)
        .setCreated(2)
        .setOrder(2)
      val holoOffer: HoloOffer = holocronConverter.convert(offer.build(), DateTime.now())
      assert(holoOffer.getCar.getPhotosCount == 1)
      assert(holoOffer.getCar.getPhotos(0) == "//avatars.mds.yandex.net/get-autoru-all/100500/image2/1200x900")
    }

    "set action ACTIVATE" when {
      "previous state is INACTIVE and current is ACTIVE" in new Fixture {
        val offer: Offer.Builder = createOffer()
        offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
        offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
        assert(holocronConverter.convert(offer.build(), DateTime.now()).getAction == Action.ACTIVATE)
      }

      "simple previous state is INACTIVE and current is ACTIVE" in new Fixture {
        val offer: Offer.Builder = createOffer()
        offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
        offer.getSimpleHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
        offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
        assert(holocronConverter.convert(offer.build(), DateTime.now()).getAction == Action.ACTIVATE)
      }

      "previous status not set" in new Fixture {
        val offer: Offer.Builder = createOffer()
        offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
        assert(holocronConverter.convert(offer.build(), DateTime.now()).getAction == Action.ACTIVATE)
      }
    }

    "set action DEACTIVATE" when {
      "previous and current states both are inactive and simple last event not exists" in new Fixture {
        val offer: Offer.Builder = createOffer()
        offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
        offer.getSimpleHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
        holocronConverter.convert(offer.build(), DateTime.now()).getAction shouldBe Action.DEACTIVATE
      }

      "simple previous and current states both are inactive and simple last event not exists" in new Fixture {
        val offer: Offer.Builder = createOffer()
        offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
        offer.getSimpleHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
        offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE).addLastEventsBuilder()
        holocronConverter.convert(offer.build(), DateTime.now()).getAction shouldBe Action.DEACTIVATE
      }

      "previous status is Active and current is non-Active" in new Fixture {
        val offer: Offer.Builder = createOffer()
        offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
        offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
        offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
        assert(holocronConverter.convert(offer.build(), DateTime.now()).getAction == Action.DEACTIVATE)
      }
    }

    "should set action UPDATE" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getAction == Action.UPDATE)
    }

    "should set url for used car" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.setSection(Section.USED)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getCar.getUrl ==
          "https://auto.ru/cars/used/sale/100500-hash"
      )
    }

    "should set url for new car" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.setSection(Section.NEW)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getCar.getUrl ==
          "https://auto.ru/cars/new/sale/100500-hash"
      )
    }

    "set color" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.setSection(Section.NEW)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.setColorHex("0000ff")
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getColorHex == "0000CC")
    }

    "should set price from price_rub" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getPriceBuilder
        .setPriceRub(100)
        .setPrice(1000)
        .setCurrency(Currency.USD)
        .setCreated(123456L)
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getCar.getPrice == 100)
    }

    "should set price without currency rates if price_rub is 0 but currency is BasicsModel.Curreny.RUB" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getPriceBuilder.setPrice(1000).setCurrency(Currency.RUB).setCreated(123456L)
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getCar.getPrice == 1000)
    }

    "should set price using currency rates if price_rub is 0" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getPriceBuilder.setPrice(1000).setCurrency(Currency.USD).setCreated(123456L)
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getCar.getPrice == 10)
    }

    "should set configuration id from car_info.configuration_id" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getCarInfoBuilder.setConfigurationId(500)
      val catalogCardMessage: CatalogCardMessage.Builder = CatalogCardMessage.newBuilder()
      catalogCardMessage.setVersion(1)
      catalogCardMessage.getConfigurationBuilder.setId(123)
      catalogCardMessage.getConfigurationBuilder.setVersion(1)
      when(carsCatalog.getCardByTechParamId(?)).thenReturn(Some(CarCard(catalogCardMessage.build())))
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getConfigurationId ==
          500
      )
    }

    "should set configuration id from catalog if car_info.configuration_id = 0" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getCarInfoBuilder.setConfigurationId(123)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      val catalogCardMessage: CatalogCardMessage.Builder = CatalogCardMessage.newBuilder()
      catalogCardMessage.setVersion(1)
      catalogCardMessage.getConfigurationBuilder.setId(123)
      catalogCardMessage.getConfigurationBuilder.setVersion(1)
      when(carsCatalog.getCardByTechParamId(?)).thenReturn(Some(CarCard(catalogCardMessage.build())))
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getConfigurationId ==
          123
      )
    }

    "should set displacement from car_info.displacement" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getCarInfoBuilder.setDisplacement(1998)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      assert(
        holocronConverter
          .convert(offer.build(), DateTime.now())
          .getRawAuto
          .getCarInfo
          .getTechParam
          .getDisplacement == 1998
      )
    }

    "should set steering wheel from car_info.steering_wheel" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getCarInfoBuilder.setSteeringWheel(VosSteeringWheel.RIGHT)
      val catalogCardMessage: CatalogCardMessage.Builder = CatalogCardMessage.newBuilder()
      catalogCardMessage.setVersion(1)
      catalogCardMessage.getConfigurationBuilder.setVersion(1)
      catalogCardMessage.getConfigurationBuilder.setSteeringWheel("LEFT")
      when(carsCatalog.getCardByTechParamId(?)).thenReturn(Some(CarCard(catalogCardMessage.build())))
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getSteeringWheel ==
          SteeringWheel.RIGHT
      )
    }

    "should set steering wheel from catalog if car_info.steering_wheel is absent" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getCarInfoBuilder.setSteeringWheel(VosSteeringWheel.LEFT)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      val catalogCardMessage: CatalogCardMessage.Builder = CatalogCardMessage.newBuilder()
      catalogCardMessage.setVersion(1)
      catalogCardMessage.getConfigurationBuilder.setVersion(1)
      catalogCardMessage.getConfigurationBuilder.setSteeringWheel("LEFT")
      when(carsCatalog.getCardByTechParamId(?)).thenReturn(Some(CarCard(catalogCardMessage.build())))
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getSteeringWheel ==
          SteeringWheel.LEFT
      )
    }

    "should set wheelDrive FORWARD_CONTROL for gear_types FORWARD_CONTROL and FRONT" in new Fixture {
      val offer: Offer.Builder = createOffer()
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")

      offer.getOfferAutoruBuilder.getCarInfoBuilder.setGearType("FRONT")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getDrive ==
          "FORWARD_CONTROL"
      )

      offer.getOfferAutoruBuilder.getCarInfoBuilder.setGearType("REAR")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getDrive ==
          "REAR_DRIVE"
      )

      offer.getOfferAutoruBuilder.getCarInfoBuilder.setGearType("ALL")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getDrive ==
          "ALL_WHEEL_DRIVE"
      )

      offer.getOfferAutoruBuilder.getCarInfoBuilder.setGearType("ALL_PART")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getDrive ==
          "ALL_WHEEL_DRIVE"
      )

      offer.getOfferAutoruBuilder.getCarInfoBuilder.setGearType("ALL_FULL")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getDrive ==
          "ALL_WHEEL_DRIVE"
      )

      offer.getOfferAutoruBuilder.getCarInfoBuilder.setGearType("FORWARD_CONTROL")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getDrive ==
          "FORWARD_CONTROL"
      )

      offer.getOfferAutoruBuilder.getCarInfoBuilder.setGearType("REAR_DRIVE")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getDrive ==
          "REAR_DRIVE"
      )

      offer.getOfferAutoruBuilder.getCarInfoBuilder.setGearType("ALL_WHEEL_DRIVE")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getCarInfo.getDrive ==
          "ALL_WHEEL_DRIVE"
      )
    }

    "convert real offer" in new Fixture {
      val source: BufferedSource =
        scala.io.Source.fromURL(getClass.getResource("/offerForHolocronConvert.json"), "UTF-8")
      try {
        val json = source.getLines.mkString("\n")
        val offer = Protobuf.fromJson[Offer](json)
        val holoOffer = holocronConverter.convert(offer, DateTime.now())
        val brokerModel = HoloCarOfferConverter.convert(holoOffer)
        assert(brokerModel.getPhotosCount == 8)
      }
    }
  }

  private def makeDate(year: Int, month: Int, day: Int) = {
    new DateTime(year, month, day, 0, 0, 0, 0)
  }

  private def addStatusHistory(offer: Offer.Builder, status: CompositeStatus, date: DateTime) = {
    offer
      .addStatusHistoryBuilder()
      .setOfferStatus(status)
      .setTimestamp(date.getMillis)
  }

  private def setStatusHistory(date: DateTime, offer: Offer.Builder) = {
    addStatusHistory(offer, CompositeStatus.CS_DRAFT, date.minusDays(14))
    addStatusHistory(offer, CompositeStatus.CS_NEED_ACTIVATION, date.minusDays(13))
    addStatusHistory(offer, CompositeStatus.CS_ACTIVE, date.minusDays(13))
    addStatusHistory(offer, CompositeStatus.CS_INACTIVE, date.minusDays(12))
    addStatusHistory(offer, CompositeStatus.CS_ACTIVE, date.minusDays(11))
    addStatusHistory(offer, CompositeStatus.CS_EXPIRED, date.minusDays(10))
    addStatusHistory(offer, CompositeStatus.CS_ACTIVE, date.minusDays(9))
    addStatusHistory(offer, CompositeStatus.CS_INACTIVE, date.minusDays(8))
    addStatusHistory(offer, CompositeStatus.CS_REMOVED, date.minusDays(7))
  }
}
