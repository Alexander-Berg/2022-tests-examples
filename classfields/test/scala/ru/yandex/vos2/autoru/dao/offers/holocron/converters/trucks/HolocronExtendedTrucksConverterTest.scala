package ru.yandex.vos2.autoru.dao.offers.holocron.converters.trucks

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.yandex.auto.message.TrucksCatalogSchema.TrucksCatalogCardMessage
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.AutoruModel.AutoruOffer.TruckInfo.TruckCategory
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
class HolocronExtendedTrucksConverterTest extends AnyFunSuite with MockitoSupport with OptionValues {

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

  private val salonConverter = mock[SalonConverter]

  private val featuresRegistry = FeatureRegistryFactory.inMemory()
  private val featuresManager = new FeaturesManager(featuresRegistry)

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

  private val holocronExtendedConverter =
    new HolocronExtendedTrucksConverter(currencyRates, offerFormConverter, regionTree)

  when(currencyRates.convert(?, ?, ?)).thenReturn(Some(BigDecimal(10)))
  when(trucksCatalog.markNameByCode(?)).thenReturn(Some("model"))
  when(trucksCatalog.getCardByMarkModel(?, ?)).thenReturn(Some(TruckCard(TrucksCatalogCardMessage.getDefaultInstance)))

  test("truck category conversion") {
    val offer = createOffer(category = Category.TRUCKS)
    offer.setOfferID("100500-hash")
    offer.getOfferAutoruBuilder.setSection(Section.NEW)
    offer.getOfferAutoruBuilder.getSellerBuilder.setUserRef("a_12345")
    offer.getOfferAutoruBuilder.getTruckInfoBuilder.setAutoCategory(TruckCategory.TRUCK_CAT_AGRICULTURAL)

    assert(
      holocronExtendedConverter.convert(offer.build(), DateTime.now()).getVosTruck.getTruckCategory ==
        "AGRICULTURAL"
    )
  }
}
