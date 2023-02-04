package ru.yandex.vos2.autoru.dao.offers.diff

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.api.DiffLogModel.OfferChangeEvent
import ru.auto.api.additional.AdditionalOfferInfoOuterClass
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Editor
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService.ServiceType
import ru.yandex.vos2.AutoruModel.AutoruOffer.{CarDocuments, PaidService, Phone, Price}
import ru.yandex.vos2.BasicsModel.Photo.RecognizedNumber
import ru.yandex.vos2.BasicsModel.{Currency, Photo}
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.utils.ApiFormUtils.RichPriceInfoOrBuilder
import ru.yandex.vos2.autoru.utils.PaidServiceUtils._
import ru.yandex.vos2.autoru.utils.converters.offerform.OfferFormConverter
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.services.mds.MdsPhotoUtils
import ru.yandex.vos2.util.CurrencyUtils
import ru.yandex.vos2.{getNow, OfferModel}

import scala.jdk.CollectionConverters._

/**
  * Created by sievmi on 08.09.17.
  */

@RunWith(classOf[JUnitRunner])
class DiffGeneratorTest extends AnyFunSuite with MockitoSupport with InitTestDbs {

  private val mdsPhotoUtils = mock[MdsPhotoUtils]
  when(mdsPhotoUtils.getOrigPhotoUrl(?)).thenReturn(Some("url"))
  private val features = components.featuresManager
  private val feature = features.SendAllOffersToKafka
  private val diffGenerator = new DiffGenerator(mdsPhotoUtils, feature)

  private def setVin(builder: CarDocuments.Builder, vin: String) = {
    builder.setVin(vin)
  }

  private def putFlag(builder: Offer.Builder, optFlag: Option[OfferFlag]): Unit = {
    optFlag match {
      case Some(flag) => builder.putFlag(flag)
      case _ =>
    }
  }

  private def setPriceRub(builder: Offer.Builder, price: Double): Unit = {
    builder.getOfferAutoruBuilder.setPrice(
      Price
        .newBuilder()
        .setPrice(price)
        .setCurrency(Currency.RUB)
        .setCreated(123)
    )
  }

  private def addPhotosByUrl(builder: Offer.Builder, urls: Seq[String], deleted: Boolean = false): Unit = {
    val photos = urls.map(url =>
      Photo.newBuilder().setIsMain(false).setOrder(1).setName(url).setCreated(123).setOrigName(url).setDeleted(deleted)
    )

    photos.foreach(photo => builder.getOfferAutoruBuilder.addPhoto(photo))
  }

  private def addPhonesByNumbers(builder: Offer.Builder, numbers: Seq[String]): Unit = {
    val phones = numbers.map(number => {
      Phone.newBuilder().setNumber(number)
    })

    phones.foreach(phone => builder.getOfferAutoruBuilder.getSellerBuilder.addPhone(phone))
  }

  private def addServicesByTypes(builder: Offer.Builder, serviceTypes: Seq[ServiceType]): Unit = {
    val services = serviceTypes.map(serviceType => {
      PaidService.newBuilder().setCreated(123).setIsActive(true).setServiceType(serviceType)
    })

    services.foreach(service => {
      builder.getOfferAutoruBuilder.addServices(service)
    })
  }

  private def addEditor(builder: Offer.Builder): Unit = {
    val editor = Editor
      .newBuilder()
      .setIp("test-ip")
      .setName("test-name")
      .setPlatform(AutoruEssentials.Platform.DESKTOP)
      .build()
    builder.getOfferAutoruBuilder.setEditor(editor)
  }

  def createNumber(number: String = "", confidence: Double = 0, widthPercent: Double = 0): RecognizedNumber = {
    RecognizedNumber
      .newBuilder()
      .setNumber(number)
      .setConfidence(confidence)
      .setWidthPercent(widthPercent)
      .build()
  }

  def createOffer(now: Long, vin: String = "", flag: Option[OfferFlag] = None): OfferModel.Offer.Builder = {
    val offerBuilder = TestUtils.createOffer(now)
    setVin(offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder, vin)
    putFlag(offerBuilder, flag)

    offerBuilder
  }

  val converter: OfferFormConverter = new OfferFormConverter(
    components.mdsPhotoUtils,
    components.regionTree,
    components.mdsPanoramasUtils,
    components.offerValidator,
    components.salonConverter,
    components.currencyRates,
    components.featuresManager,
    components.banReasons,
    components.carsCatalog,
    components.trucksCatalog,
    components.motoCatalog
  )

  private def genDiff(builder1: Offer.Builder, builder2: Offer.Builder): Option[OfferChangeEvent] = {
    diffGenerator.generateDiff(Some(builder1.build()), builder2.build(), converter, "test", "test")(Traced.empty)
  }

  test("offers have equal fields") {
    val offer1 = createOffer(getNow, "vin").build()
    val offer2 = createOffer(getNow, "vin").build()

    val diff = diffGenerator.generateDiff(Some(offer1), offer2, converter, "test", "test")(Traced.empty)

    assert(diff.isEmpty)
  }

  test("offers have different vin") {
    val offer1 = createOffer(now = getNow, vin = "vin1").build()
    val offer2 = createOffer(getNow, vin = "vin2").build()

    val optDiff = diffGenerator.generateDiff(Some(offer1), offer2, converter, "test", "test")(Traced.empty)

    assert(optDiff.nonEmpty)
    optDiff.foreach(diff => {
      assert(diff.getDiff.hasVin)
      assert(diff.getDiff.getVin.getOldValue == "VIN1")
      assert(diff.getDiff.getVin.getNewValue == "VIN2")
    })

  }

  test("offers have different statuses") {
    val offer1 = createOffer(getNow, flag = Some(OfferFlag.OF_BANNED)).build()
    val offer2 = createOffer(getNow, flag = Some(OfferFlag.OF_NEED_ACTIVATION)).build()

    val optDiff = diffGenerator.generateDiff(Some(offer1), offer2, converter, "test", "test")(Traced.empty)

    assert(optDiff.nonEmpty)
    optDiff.foreach(diff => {
      assert(diff.getDiff.hasStatus)
      assert(diff.getDiff.getStatus.getOldValue == OfferStatus.BANNED)
      assert(diff.getDiff.getStatus.getNewValue == OfferStatus.NEED_ACTIVATION)
    })
  }

  test("offer1 is None") {
    val offer2 = createOffer(getNow, "vin", Some(OfferFlag.OF_DELETED)).build()

    val optDiff = diffGenerator.generateDiff(None, offer2, converter, "test", "test")(Traced.empty)

    assert(optDiff.nonEmpty)
    optDiff.foreach(diff => {
      assert(diff.getDiff.hasVin)
      assert(diff.getDiff.getVin.getOldValue == "")
      assert(diff.getDiff.getVin.getNewValue == "VIN")

      assert(diff.getDiff.hasStatus)
      assert(diff.getDiff.getStatus.getOldValue == OfferStatus.STATUS_UNKNOWN)
      assert(diff.getDiff.getStatus.getNewValue == OfferStatus.REMOVED)
    })
  }

  test("offers have different services") {
    val offerBuilder1 = createOffer(getNow)
    addServicesByTypes(offerBuilder1, Seq(ServiceType.FRESH, ServiceType.TURBO))
    val offerBuilder2 = createOffer(getNow)
    addServicesByTypes(offerBuilder2, Seq(ServiceType.FRESH, ServiceType.TOP, ServiceType.PROMO))

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    val diff = optDiff.get.getDiff
    assert(diff.hasServices)

    assert(diff.getServices.getAddedCount == 2)
    assert(diff.getServices.getAddedList.asScala.exists(_.getService == stringByService(ServiceType.TOP).get))
    assert(diff.getServices.getAddedList.asScala.exists(_.getService == stringByService(ServiceType.PROMO).get))

    assert(diff.getServices.getDeletedCount == 1)
    assert(diff.getServices.getDeletedList.asScala.exists(_.getService == stringByService(ServiceType.TURBO).get))
  }

  test("paid service order test") {
    val offerBuilder1 = createOffer(getNow)
    addServicesByTypes(offerBuilder1, Seq(ServiceType.FRESH, ServiceType.TURBO))
    val offerBuilder2 = createOffer(getNow)
    addServicesByTypes(offerBuilder2, Seq(ServiceType.TURBO, ServiceType.FRESH))

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.isEmpty)
  }

  test("paid service order test with feature on") {
    val offerBuilder1 = createOffer(getNow)
    addServicesByTypes(offerBuilder1, Seq(ServiceType.FRESH, ServiceType.TURBO))
    val offerBuilder2 = createOffer(getNow)
    addServicesByTypes(offerBuilder2, Seq(ServiceType.TURBO, ServiceType.FRESH))

    components.featureRegistry.updateFeature(feature.name, true)

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    components.featureRegistry.updateFeature(feature.name, false)
    assert(optDiff.isDefined)

  }

  test("offers have different price") {
    val offerBuilder1 = createOffer(getNow)
    setPriceRub(offerBuilder1, 100)
    val offerBuilder2 = createOffer(getNow)
    setPriceRub(offerBuilder2, 200)

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    val diff = optDiff.get.getDiff
    assert(diff.hasPrice)

    assert(diff.getPrice.getOldValue.selectPrice == 100)
    assert(diff.getPrice.getNewValue.selectPrice == 200)
  }

  test("offers have different price, change currency") {
    val offerBuilder1 = createOffer(getNow)
    setPriceRub(offerBuilder1, 200)
    val offerBuilder2 = createOffer(getNow)
    setPriceRub(offerBuilder2, 200)
    offerBuilder2.getOfferAutoruBuilder.getPriceBuilder.setCurrency(Currency.EUR)

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    val diff = optDiff.get.getDiff
    assert(diff.hasPrice)

    assert(diff.getPrice.getOldValue.selectPrice == 200)
    assert(diff.getPrice.getOldValue.getCurrency == CurrencyUtils.fromCurrency(Currency.RUB))

    assert(diff.getPrice.getNewValue.selectPrice == 200)
    assert(diff.getPrice.getNewValue.getCurrency == CurrencyUtils.fromCurrency(Currency.EUR))
  }

  test("offers have different description") {
    val offerBuilder1 = createOffer(getNow)
    offerBuilder1.setDescription("description 1")
    val offerBuilder2 = createOffer(getNow)
    offerBuilder2.setDescription("description 2")

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    val diff = optDiff.get.getDiff

    assert(diff.hasDescription)
    assert(diff.getDescription.getOldValue == "description 1")
    assert(diff.getDescription.getNewValue == "description 2")
  }

  test("offers have different phones") {
    val offerBuilder1 = createOffer(getNow)
    addPhonesByNumbers(offerBuilder1, Seq("0", "1", "2"))
    val offerBuilder2 = createOffer(getNow)
    addPhonesByNumbers(offerBuilder2, Seq("1", "3", "4"))

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    val diff = optDiff.get.getDiff

    assert(diff.hasPhone)
    assert(diff.getPhone.getAddedCount == 2)
    assert(diff.getPhone.getAddedList.asScala.exists(x => x.getPhone == "3"))
    assert(diff.getPhone.getAddedList.asScala.exists(x => x.getPhone == "4"))

    assert(diff.getPhone.getDeletedCount == 2)
    assert(diff.getPhone.getDeletedList.asScala.exists(x => x.getPhone == "0"))
    assert(diff.getPhone.getDeletedList.asScala.exists(x => x.getPhone == "2"))
  }

  test("phone order test") {
    val offerBuilder1 = createOffer(getNow)
    addPhonesByNumbers(offerBuilder1, Seq("0", "1", "2"))
    val offerBuilder2 = createOffer(getNow)
    addPhonesByNumbers(offerBuilder2, Seq("2", "0", "1"))

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.isEmpty)
  }

  test("offers have different photos") {
    val offerBuilder1 = createOffer(getNow)
    addPhotosByUrl(offerBuilder1, Seq("url1-1", "url2-2"))
    val offerBuilder2 = createOffer(getNow)
    addPhotosByUrl(offerBuilder2, Seq("url1-1"))

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    val diff = optDiff.get.getDiff

    assert(diff.hasPhoto)
    assert(diff.getPhoto.getAddedCount == 0)
    assert(diff.getPhoto.getDeletedCount == 1)
    assert(diff.getPhoto.getDeletedList.asScala.exists(x => x.getName == "autoru-all:url2-2"))
  }

  test("offers have deleted photos") {
    val offerBuilder1 = createOffer(getNow)
    addPhotosByUrl(offerBuilder1, Seq("url1-1", "url2-2"))
    val offerBuilder2 = createOffer(getNow)
    addPhotosByUrl(offerBuilder2, Seq("url1-1", "url2-2"), deleted = true)

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    val diff = optDiff.get.getDiff

    assert(diff.hasPhoto)
    assert(diff.getPhoto.getAddedCount == 2)
    assert(diff.getPhoto.getDeletedCount == 2)
    assert(diff.getPhoto.getDeletedList.asScala.exists(x => x.getName == "autoru-all:url2-2"))
  }

  test("photo order test") {
    val offerBuilder1 = createOffer(getNow)
    addPhotosByUrl(offerBuilder1, Seq("url1", "url2"))
    val offerBuilder2 = createOffer(getNow)
    addPhotosByUrl(offerBuilder2, Seq("url2", "url1"))

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.isEmpty)
  }

  test("offers have different sts") {
    val offerBuilder1 = createOffer(getNow)
    offerBuilder1.getOfferAutoruBuilder.getDocumentsBuilder.setStsCode("sts1")
    val offerBuilder2 = createOffer(getNow)
    offerBuilder2.getOfferAutoruBuilder.getDocumentsBuilder.setStsCode("sts2")

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    val diff = optDiff.get.getDiff

    assert(diff.hasSts)
    assert(diff.getSts.getOldValue == "sts1")
    assert(diff.getSts.getNewValue == "sts2")
  }

  test("offers have different license number lists. (recognize same number on new photo and delete old photo)") {
    val offerBuilder1 = createOffer(getNow)
    addPhotosByUrl(offerBuilder1, Seq("url1", "url2"))
    offerBuilder1.getOfferAutoruBuilder.getPhotoBuilderList
      .get(0)
      .addNumbers(createNumber("123", 1, 1))
    val offerBuilder2 = createOffer(getNow)
    addPhotosByUrl(offerBuilder2, Seq("url1", "url2"))
    offerBuilder2.getOfferAutoruBuilder.getPhotoBuilderList
      .get(1)
      .addNumbers(createNumber("123", 1, 1))

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    val diff = optDiff.get.getDiff
    assert(diff.hasLicenseNumbers)

    assert(diff.getLicenseNumbers.getAdded(0).getPhoto == "url2")
    assert(diff.getLicenseNumbers.getAdded(0).getNumbersList.size() == 1)
    assert(diff.getLicenseNumbers.getAdded(0).getNumbers(0).getNumber == "123")

    assert(diff.getLicenseNumbers.getDeleted(0).getPhoto == "url1")
    assert(diff.getLicenseNumbers.getDeleted(0).getNumbersList.size() == 1)
    assert(diff.getLicenseNumbers.getDeleted(0).getNumbers(0).getNumber == "123")
  }

  test("offers have different license number lists. (add 1 photo)") {
    def initNumbers(offerBuilder: Offer.Builder): Unit = {
      addPhotosByUrl(offerBuilder, Seq("url1", "url2", "url3"))

      offerBuilder.getOfferAutoruBuilder.getPhotoBuilderList
        .get(0)
        .addNumbers(createNumber("123", 0.5, 0.5))
      offerBuilder.getOfferAutoruBuilder.getPhotoBuilderList
        .get(0)
        .addNumbers(createNumber("1234", 0.5, 0.5))
      offerBuilder.getOfferAutoruBuilder.getPhotoBuilderList
        .get(1)
        .addNumbers(createNumber())
    }

    val offerBuilder1 = createOffer(getNow)
    initNumbers(offerBuilder1)
    val offerBuilder2 = createOffer(getNow)
    initNumbers(offerBuilder2)
    offerBuilder2.getOfferAutoruBuilder.getPhotoBuilderList
      .get(2)
      .addNumbers(createNumber("12345", 0.8, 0.33))

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    val diff = optDiff.get.getDiff
    assert(diff.hasLicenseNumbers)

    assert(diff.getLicenseNumbers.getDeletedCount == 0)

    assert(diff.getLicenseNumbers.getAdded(0).getPhoto == "url3")
    assert(diff.getLicenseNumbers.getAdded(0).getNumbersList.size() == 1)
    assert(diff.getLicenseNumbers.getAdded(0).getNumbers(0).getNumber == "12345")

    assert(diff.getLicenseNumbers.getCurrentCount == 3)
    assert(
      diff.getLicenseNumbers.getCurrentList.asScala
        .exists(n => n.getNumbers(0).getNumber == "123" && n.getPhoto == "url1")
    )
    assert(
      diff.getLicenseNumbers.getCurrentList.asScala
        .exists(n => n.getNumbers(0).getNumber == "" && n.getPhoto == "url2")
    )
    assert(
      diff.getLicenseNumbers.getCurrentList.asScala
        .exists(n => n.getNumbers(0).getNumber == "12345" && n.getPhoto == "url3")
    )
  }

  test("offers have equal number lists") {
    val offerBuilder1 = createOffer(getNow)

    addPhotosByUrl(offerBuilder1, Seq("url1", "url2"))
    offerBuilder1.getOfferAutoruBuilder.getPhotoBuilderList
      .get(0)
      .addNumbers(createNumber("123", 1, 1))

    val offerBuilder2 = createOffer(getNow)
    addPhotosByUrl(offerBuilder2, Seq("url1", "url2"))
    offerBuilder2.getOfferAutoruBuilder.getPhotoBuilderList
      .get(0)
      .addNumbers(createNumber("123", 1, 1))

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.isEmpty)
  }

  test("license number order test") {
    val offerBuilder1 = createOffer(getNow)

    addPhotosByUrl(offerBuilder1, Seq("url1-1", "url2-2"))
    offerBuilder1.getOfferAutoruBuilder.getPhotoBuilderList
      .get(0)
      .addNumbers(createNumber("123", 1, 1))
    offerBuilder1.getOfferAutoruBuilder.getPhotoBuilderList
      .get(1)
      .addNumbers(createNumber("1234", 1, 1))

    val offerBuilder2 = createOffer(getNow)
    addPhotosByUrl(offerBuilder2, Seq("url2-2", "url1-1", "url3-3"))
    offerBuilder2.getOfferAutoruBuilder.getPhotoBuilderList
      .get(1)
      .addNumbers(createNumber("123", 1, 1))
    offerBuilder2.getOfferAutoruBuilder.getPhotoBuilderList
      .get(0)
      .addNumbers(createNumber("1234", 1, 1))

    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    assert(optDiff.get.hasDiff)
    assert(optDiff.get.getDiff.hasPhoto)
    assert(!optDiff.get.getDiff.hasLicenseNumbers)
  }

  test("don't send drafts") {
    val offer1 = createOffer(now = getNow, vin = "vin1").addFlag(OfferFlag.OF_DRAFT).build()
    val offer2 = createOffer(getNow, vin = "vin2").addFlag(OfferFlag.OF_DRAFT).build()

    val optDiff = diffGenerator.generateDiff(Some(offer1), offer2, converter, "test", "test")(Traced.empty)

    assert(optDiff.isEmpty)
  }

  test("fill event edit info") {
    val offerBuilder1 = createOffer(getNow)
    offerBuilder1.getOfferAutoruBuilder.getDocumentsBuilder.setStsCode("sts1")
    val offerBuilder2 = createOffer(getNow)
    offerBuilder2.getOfferAutoruBuilder.getDocumentsBuilder.setStsCode("sts2")
    addEditor(offerBuilder2)
    val optDiff = genDiff(offerBuilder1, offerBuilder2)

    assert(optDiff.nonEmpty)
    val diff = optDiff.get
    val editor = diff.getAdditionalInfo.getEditor

    assert(editor.getPlatform == AdditionalOfferInfoOuterClass.Platform.DESKTOP)
  }

}
