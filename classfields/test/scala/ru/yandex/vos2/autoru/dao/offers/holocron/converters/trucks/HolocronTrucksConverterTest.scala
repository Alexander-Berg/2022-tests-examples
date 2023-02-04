package ru.yandex.vos2.autoru.dao.offers.holocron.converters.trucks

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.vertis.holocron.common.{Action, HoloOffer}
import ru.yandex.auto.message.TrucksCatalogSchema.TrucksCatalogCardMessage
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.BasicsModel.{CompositeStatus, Currency}
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.catalog.cars.CarsCatalog
import ru.yandex.vos2.autoru.catalog.moto.MotoCatalog
import ru.yandex.vos2.autoru.catalog.trucks.TrucksCatalog
import ru.yandex.vos2.autoru.catalog.trucks.model.TruckCard
import ru.yandex.vos2.autoru.model.TestUtils.createOffer
import ru.yandex.vos2.autoru.model.extdata.BanReasons
import ru.yandex.vos2.autoru.utils.TestDataEngine
import ru.yandex.vos2.autoru.utils.converters.offerform.{OfferFormConverter, SalonConverter}
import ru.yandex.vos2.autoru.utils.currency.CurrencyRates
import ru.yandex.vos2.autoru.utils.geo.Tree
import ru.yandex.vos2.autoru.utils.validators.{OfferValidator, OfferValidatorImpl, ValidResult, ValidationResult}
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.services.mds.{AutoruAllNamespaceSettings, AutoruPanoramaNamespaceSettings, MdsPhotoUtils}

@RunWith(classOf[JUnitRunner])
class HolocronTrucksConverterTest extends AnyWordSpec with MockitoSupport with OptionValues with Matchers {

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

    private val carsCatalog = mock[CarsCatalog]

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

    val holocronConverter = new HolocronTrucksConverter(
      mdsPhotoUtils,
      currencyRates,
      offerFormConverter,
      regionTree
    )

    when(trucksCatalog.getCardByMarkModel(?, ?))
      .thenReturn(Some(TruckCard(TrucksCatalogCardMessage.getDefaultInstance)))
    when(trucksCatalog.markNameByCode(?)).thenReturn(Some("model"))
    when(currencyRates.convert(?, ?, ?)).thenReturn(Some(BigDecimal(10)))
  }

  "HolocronTrucksConverter" should {
    "not send deleted photos" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
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
      assert(holoOffer.getTruck.getPhotosCount == 1)
      assert(holoOffer.getTruck.getPhotos(0) == "//avatars.mds.yandex.net/get-autoru-all/100500/image2/1200x900")
    }

    "set action ACTIVATE" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_INACTIVE)
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getAction == Action.ACTIVATE)
    }

    "set action DEACTIVATE" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
      offer.addFlag(OfferModel.OfferFlag.OF_INACTIVE)
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getAction == Action.DEACTIVATE)
    }

    "set action UPDATE" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getHolocronStatusBuilder.setLastSentOfferStatus(CompositeStatus.CS_ACTIVE)
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getAction == Action.UPDATE)
    }

    "set action ACTIVATE if previous status not set" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getAction == Action.ACTIVATE)
    }

    "set url for used car" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.setSection(Section.USED)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getTruck.getUrl ==
          "https://auto.ru/drags/used/sale/100500-hash"
      )
    }

    "set url for new car" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.setSection(Section.NEW)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getTruck.getUrl ==
          "https://auto.ru/drags/new/sale/100500-hash"
      )
    }

    "set color" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.setSection(Section.NEW)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.setColorHex("0000ff")
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getColorHex == "0000CC")
    }

    "set price from price_rub" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getPriceBuilder
        .setPriceRub(100)
        .setPrice(1000)
        .setCurrency(Currency.USD)
        .setCreated(123456L)
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getTruck.getPrice == 100)
    }

    "set price without currency rates if price_rub is 0 but currency is BasicsModel.Curreny.RUB" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getPriceBuilder.setPrice(1000).setCurrency(Currency.RUB).setCreated(123456L)
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getTruck.getPrice == 1000)
    }

    "set price using currency rates if price_rub is 0" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      offer.getOfferAutoruBuilder.getPriceBuilder.setPrice(1000).setCurrency(Currency.USD).setCreated(123456L)
      assert(holocronConverter.convert(offer.build(), DateTime.now()).getTruck.getPrice == 10)
    }

    "set displacement from truck_info.engine_volume" in new Fixture {
      val offer: Offer.Builder = createOffer(category = Category.TRUCKS)
      offer.setOfferID("100500-hash")
      offer.getOfferAutoruBuilder.getTruckInfoBuilder.setEngineVolume(1998)
      offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
      assert(
        holocronConverter.convert(offer.build(), DateTime.now()).getRawAuto.getTruckInfo.getDisplacement == 1998
      )
    }
  }
}
