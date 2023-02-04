package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.mockito.Mockito.verify
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.vin.VinResolutionEnums
import ru.auto.api.vin.VinResolutionEnums.ResolutionPart
import ru.auto.api.vin.VinResolutionModel.{ResolutionEntry, VinIndexResolution}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.{MockitoSupport, NotMockedException}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.worker.YdbWorkerResult
import ru.yandex.vos2.AutoruModel.AutoruOffer.SellerType
import ru.yandex.vos2.BasicsModel
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.catalog.cars.CarsCatalog
import ru.yandex.vos2.autoru.dao.old.{AutoruSalesDao, SettingsDao}
import ru.yandex.vos2.autoru.model.AutoruCatalogModels.Modification
import ru.yandex.vos2.autoru.model.AutoruSale.{Badge, Damage, DiscountPrice, Phone, PhonesRedirect, Video}
import ru.yandex.vos2.autoru.model._
import ru.yandex.vos2.autoru.services.vindecoder.VinDecoderClient
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}

import scala.util.Success

class LicensePlateResolutionWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  private val testVin = "XW8ZZZ61ZEG061733"
  private val testId = "11111-test"
  private val testMark = "AUDI"
  private val testModel = "A4"
  private val testHp = 100
  private val testDisp = 0
  private val testYear = 2012
  private val testOwners = 3
  private val testLicensePlate = "X000XX777"
  private val testPhotoLicensePlate = "X999XX777"

  private val version = 1

  private val featureRegistry = FeatureRegistryFactory.inMemory()
  private val featuresManager = new FeaturesManager(featureRegistry)

  abstract private class Fixture {

    def testOffer = {
      val offer = TestUtils.createOffer()
      offer
        .setOfferID(testId)
        .setTimestampCreate(ru.yandex.vos2.getNow - 10 * 60 * 60 * 1000)
      offer.getOfferAutoruBuilder.getCarInfoBuilder
        .setMark(testMark)
        .setModel(testModel)
        .setHorsePower(testHp)
        .setDisplacement(testDisp)
      offer.getOfferAutoruBuilder.getDocumentsBuilder
        .setLicensePlate(testLicensePlate)
      offer.getOfferAutoruBuilder.getEssentialsBuilder
        .setYear(testYear)
      offer.getOfferAutoruBuilder.getOwnershipBuilder
        .setPtsOwnersCount(testOwners)
      offer.getOfferAutoruBuilder.getVinResolutionBuilder
        .setVersion(version)
        .setTimestamp(ru.yandex.vos2.getNow)
      offer.getOfferAutoruBuilder
        .addPhoto(buildPhoto("1", List(buildNumber(testPhotoLicensePlate, 0.4, 0.5))))
        .addPhoto(buildPhoto("2", List(buildNumber(testPhotoLicensePlate, 0.3, 0.5))))
        .addPhoto(buildPhoto("3", List(buildNumber(testPhotoLicensePlate, 0.5, 0.5))))
        .addPhoto(buildPhoto("4", List(buildNumber("aaa", 0.4, 0.5))))
        .addPhoto(buildPhoto("5", List(buildNumber("aaa", 0.3, 0.5))))
        .addPhoto(buildPhoto("6", List(buildNumber("aaa", 0.5, 0.5))))
      offer
    }

    val client = mockStrict[VinDecoderClient]
    val catalog = mockStrict[CarsCatalog]
    val saleDao = mockStrict[AutoruSalesDao]
    val settingsDao = mockStrict[SettingsDao]

    val worker = new LicensePlateResolutionWorkerYdb(
      client,
      catalog,
      saleDao,
      settingsDao
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }

    def checkProcessedExeption(offer: Offer): YdbWorkerResult = {

      worker.shouldProcess(offer, None).shouldProcess shouldBe true

      var res = YdbWorkerResult(None, None)
      intercept[NotMockedException] {
        // provided client was called
        res = worker.process(offer, None)

      }
      res
    }

    def checkProcessed(offer: Offer): YdbWorkerResult = {

      worker.shouldProcess(offer, None).shouldProcess shouldBe true

      // provided client was called
      worker.process(offer, None)
    }

    def checkIgnored(offer: Offer): Unit = {
      val client = mockStrict[VinDecoderClient]
      val catalog = mockStrict[CarsCatalog]
      val saleDao = mockStrict[AutoruSalesDao]
      val settingsDao = mockStrict[SettingsDao]

      worker.shouldProcess(offer, None).shouldProcess shouldBe false
    }

  }

  private def buildPhoto(name: String, numbers: Iterable[BasicsModel.Photo.RecognizedNumber]): BasicsModel.Photo = {
    val photo = BasicsModel.Photo.newBuilder()
    numbers.foreach(n => photo.addNumbers(n))
    photo.setOrigName(name)
    photo.setIsMain(false)
    photo.setOrder(0)
    photo.setName(name)
    photo.setCreated(1)
    photo.build()
  }

  private def buildNumber(number: String,
                          confidence: Double,
                          withPercent: Double): BasicsModel.Photo.RecognizedNumber = {
    BasicsModel.Photo.RecognizedNumber
      .newBuilder()
      .setNumber(number)
      .setConfidence(confidence)
      .setWidthPercent(withPercent)
      .build()
  }

  //scalastyle:off
  private def createSale = {
    AutoruSale(
      id = 0,
      hash = "G4GLTScd",
      createDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
      anyUpdateTime = new DateTime(2016, 10, 28, 18, 31, 26, 0),
      expireDate = new DateTime(2016, 11, 28, 18, 31, 26, 0),
      freshDate = None,
      status = AutoruSaleStatus.STATUS_CREATED_BY_CLIENT,
      sectionId = 1,
      userId = 18318774,
      poiId = None,
      countryId = 0,
      regionId = 0,
      cityId = 0,
      yaCountryId = 225,
      yaRegionId = 1,
      yaCityId = 21652,
      clientId = 0,
      contactId = 0,
      newClientId = 0,
      salonId = 0,
      _salonContactId = 0,
      year = 2011,
      price = 500000.56,
      currency = "RUR",
      priceRur = 0.0,
      _markId = 109,
      modificationId = 58238,
      _folderId = 30907,
      description =
        "Своевременное обслуживание. Пройдены все ТО. Куплена не в кредит. Непрокуренный салон. Сервисная книжка. Не участвовала в ДТП.",
      settings = None,
      poi = Some(
        AutoruPoi(
          poiId = 0,
          countryId = None,
          regionId = None,
          cityId = None,
          yaCountryId = Some(225),
          yaRegionId = Some(1),
          yaCityId = Some(21652),
          address = Some("Рублёвское шоссе"),
          latitude = Some(43.905251),
          longitude = Some(30.261402)
        )
      ),
      user = Some(
        AutoruUser(
          id = 18318774,
          email = Some("superscalper@yandex.ru"),
          phones = Seq(
            UserPhone(
              id = 32624814,
              userId = Some(18318774),
              number = None,
              phone = 79161793608L,
              status = 1,
              isMain = false,
              code = 0,
              created = new DateTime(2016, 8, 21, 14, 36, 31, 0),
              updated = None
            )
          )
        )
      ),
      phones = Some(
        List(
          Phone(
            saleId = 0,
            phoneId = 32624814,
            phone = Some(
              UserPhone(
                id = 32624814,
                userId = Some(18318774),
                number = None,
                phone = 79161793608L,
                status = 1,
                isMain = false,
                code = 0,
                created = new DateTime(2016, 8, 21, 14, 36, 31, 0),
                updated = None
              )
            ),
            callFrom = 9,
            callTill = 23,
            contactName = "Христофор"
          )
        )
      ),
      salonPoiContacts = None,
      salonPoi = None,
      dealerMarks = None,
      images = Some(
        List(
          AutoruImage(
            id = 0,
            saleId = 0,
            main = true,
            order = 1,
            name = "101404-1e190a2f94f4f29a8eb7dc720d75ec51",
            originalName = None,
            created = new DateTime(2016, 10, 28, 18, 31, 26, 0),
            cvHash = None,
            exifLat = None,
            exifLon = None,
            exifDate = None,
            state = ImageStates.Default
          ),
          AutoruImage(
            id = 0,
            saleId = 0,
            main = false,
            order = 2,
            name = "117946-b6d2fb88b2628038237af5c45ae1299a",
            originalName = None,
            created = new DateTime(2016, 10, 28, 18, 31, 26, 0),
            cvHash = None,
            exifLat = None,
            exifLon = None,
            exifDate = None,
            state = ImageStates.Default
          ),
          AutoruImage(
            id = 0,
            saleId = 0,
            main = false,
            order = 3,
            name = "136387-6df3831aea9cd6df09157c86b8f3d2a0",
            originalName = None,
            created = new DateTime(2016, 10, 28, 18, 31, 26, 0),
            cvHash = None,
            exifLat = None,
            exifLon = None,
            exifDate = None,
            state = ImageStates.Default
          )
        )
      ),
      videos = Some(
        List(
          Video(
            id = 0,
            saleId = 0,
            provider = "Yandex",
            value = "",
            parseValue = "",
            videoId = 123620,
            createDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
            updateDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
            yandexVideo = None
          )
        )
      ),
      modification = Some(
        Modification(
          id = 58238,
          label = None,
          markId = 109,
          folderId = 30907,
          techParamId = Some(6143500),
          configurationId = Some(6143425),
          startYear = 2010,
          endYear = Some(2013),
          properties = Map(
            "turbo_type" -> "1488",
            "drive" -> "1074",
            "body_size" -> "4410",
            "weight" -> "1525",
            "acceleration" -> "10,1",
            "consumption_city" -> "9,1",
            "clearance" -> "170",
            "tank_volume" -> "58",
            "front_suspension" -> "1514",
            "body_type" -> "1358",
            "engine_order" -> "1458",
            "consumption_mixed" -> "7,1",
            "gearbox_type" -> "1414",
            "power_system" -> "1480",
            "cylinders_value" -> "4",
            "front_brake" -> "1548",
            "compression" -> "16",
            "cylinders_order" -> "1446",
            "front_wheel_base" -> "1591",
            "gears_count" -> "6",
            "engine_volume" -> "1995",
            "power_kvt" -> "135",
            "height" -> "1660",
            "engine_type" -> "1260",
            "diametr" -> "84.0x90.0",
            "fuel" -> "1504",
            "engine_power" -> "184",
            "wheel_size" -> "225/60/R17",
            "max_speed" -> "195",
            "boot_volume_max" -> "1436",
            "back_suspension" -> "1534",
            "doors_count" -> "5",
            "moment_rpm" -> "1800",
            "moment" -> "392",
            "back_brake" -> "1558",
            "consumption_hiway" -> "6",
            "back_wheel_base" -> "1592",
            "valvetrain" -> "1466",
            "boot_volume_min" -> "591",
            "power_rpm" -> "4000",
            "seats" -> "134",
            "valves" -> "4",
            "width" -> "1820",
            "full_weight" -> "2140",
            "wheel_base" -> "2640"
          )
        )
      ),
      folders = None,
      services = None,
      badges = Some(
        List(
          Badge(
            id = 0,
            saleId = 0,
            categoryId = 15,
            createDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
            isActivated = false,
            badge = "Парктроник"
          ),
          Badge(
            id = 0,
            saleId = 0,
            categoryId = 15,
            createDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
            isActivated = false,
            badge = "Кожаный салон"
          ),
          Badge(
            id = 0,
            saleId = 0,
            categoryId = 15,
            createDate = new DateTime(2016, 10, 28, 18, 31, 26, 0),
            isActivated = false,
            badge = "Коврики в подарок"
          )
        )
      ),
      damage = Some(
        Damage(
          id = 0,
          saleId = 0,
          value =
            """[{"car_part":"frontbumper","type":["4"],"description":":("},{"car_part":"frontleftdoor","type":["3"],"description":":(("}]"""
        )
      ),
      phonesRedirect =
        Some(PhonesRedirect(id = 0, saleId = 0, active = true, updated = new DateTime(2016, 10, 28, 18, 31, 26, 0))),
      recallReason = None,
      discountPrice = Some(
        DiscountPrice(
          id = 0,
          saleId = 0,
          userId = 18318774,
          clientId = 0,
          price = 100500,
          status = "active"
        )
      )
    )
  }

  private def resolutionNotEmpty = {
    resolutionWithStatus(VinResolutionEnums.Status.OK)
  }

  private def resolutionWithStatus(status: VinResolutionEnums.Status) = {
    VinIndexResolution
      .newBuilder()
      .setVersion(version)
      .setVin(testVin)
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.SUMMARY)
          .setStatus(status)
      )
      .build()
  }

  ("ignore offer with category != CARS") in new Fixture {
    val offer = testOffer
    offer.getOfferAutoruBuilder
      .setCategory(Category.TRUCKS)
    checkIgnored(offer.build())
  }

  ("ignore new offers") in new Fixture {
    val offer = testOffer
    offer.getOfferAutoruBuilder
      .setSection(Section.NEW)
    checkIgnored(offer.build())
  }

  ("ignore commercial offers") in new Fixture {
    val offer = testOffer
    offer.getOfferAutoruBuilder
      .setSellerType(SellerType.COMMERCIAL)
    checkIgnored(offer.build())
  }

  ("ignore offers with vin") in new Fixture {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getDocumentsBuilder.setVin(testVin)
    checkIgnored(offer.build())
  }

  ("ignore offers without licenseplate") in new Fixture {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getDocumentsBuilder.clearLicensePlate()
    offer.getOfferAutoruBuilder.clearPhoto()
    checkIgnored(offer.build())
  }

  ("ignore offer with not enough recognized numbers") in new Fixture {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getDocumentsBuilder.clearLicensePlate()
    offer.getOfferAutoruBuilder.clearPhoto()
    offer.getOfferAutoruBuilder
      .addPhoto(buildPhoto("2", List(buildNumber(testLicensePlate, 0.3, 0.5))))
      .addPhoto(buildPhoto("3", List(buildNumber(testLicensePlate, 0.2, 0.5))))
      .addPhoto(buildPhoto("1", List(buildNumber(testLicensePlate, 0.4, 0.5))))
    checkIgnored(offer.build())
  }

  ("ignore if nothing changed") in new Fixture {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setOfferHash(VinResolutionWorkerYdb.offerHashCode(offer.getOfferAutoru))
      .setVersion(version)
      .setResolution(resolutionNotEmpty)

    checkIgnored(offer.build())
  }

  ("process if version changed") in new Fixture {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setOfferHash(VinResolutionWorkerYdb.offerHashCode(testOffer.getOfferAutoru))
      .setVersion(0)
      .setResolution(resolutionNotEmpty)

    checkProcessedExeption(offer.build())
  }

  ("process if license plate form taxi") in new Fixture {
    val offer = testOffer
    offer.getOfferAutoruBuilder.clearVinResolution()
    offer.getOfferAutoruBuilder.clearPhoto()
    offer.getOfferAutoruBuilder.getDocumentsBuilder.setLicensePlate("AA66677")

    checkProcessedExeption(offer.build())
  }

  ("process if important offer part changed") in new Fixture {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setOfferHash(VinResolutionWorkerYdb.offerHashCode(offer.getOfferAutoru))
      .setVersion(version)
      .setResolution(resolutionNotEmpty)
    offer.getOfferAutoruBuilder.getCarInfoBuilder.setModel("A5")

    checkProcessedExeption(offer.build())
  }

  ("process if force for without vin") in new Fixture {
    val offer = testOffer
    offer.getOfferAutoruBuilder.getDocumentsBuilder
      .clearVin()
      .setLicensePlate("X555XX77")

    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setOfferHash(VinResolutionWorkerYdb.offerHashCode(offer.getOfferAutoru))
      .setResolution(resolutionWithStatus(VinResolutionEnums.Status.UNTRUSTED))
      .setVersion(version)
      .setForceUpdate(true)

    checkProcessedExeption(offer.build())
  }

  ("send vin resolution request to vin-decoder on offer processing and save resolution and vin") in new Fixture {

    when(client.getResolutionByLicensePlate(?, ?)(?)).thenReturn(Success(resolutionNotEmpty))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)
    when(saleDao.saveProperties(?)(?)).thenReturn(Seq.empty)
    when(saleDao.getOffer(?)(?)).thenReturn(Some(createSale))
    when(settingsDao.getCachedSettingMappings).thenReturn(Map("vin" -> 12))

    val offer = testOffer

    val result = checkProcessed(offer.build())
    val resultOffer = result.updateOfferFunc.get(offer.build()).getOfferAutoru

    result.nextCheck.nonEmpty shouldBe true
    resultOffer.getVinResolution.hasResolution shouldBe true
    resultOffer.getVinResolution.getVersion shouldBe version
    resultOffer.getVinResolution.getResolution shouldBe resolutionNotEmpty
    resultOffer.getDocuments.getVin shouldBe testVin

    verify(client)
      .getResolutionByLicensePlate(testLicensePlate, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
    verify(saleDao).saveProperties(?)(?)
  }

  (
    "send resolution request to vin-decoder on offer processing " +
      "and save resolution and vin when license is from photo"
  ) in new Fixture {

    when(client.getResolutionByLicensePlate(?, ?)(?)).thenReturn(Success(resolutionNotEmpty))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)
    when(saleDao.saveProperties(?)(?)).thenReturn(Seq.empty)
    when(saleDao.getOffer(?)(?)).thenReturn(Some(createSale))
    when(settingsDao.getCachedSettingMappings).thenReturn(Map("vin" -> 12))

    val offer = testOffer
    offer.getOfferAutoruBuilder.getDocumentsBuilder.clearLicensePlate()

    val result = checkProcessed(offer.build())
    val resultOffer = result.updateOfferFunc.get(offer.build()).getOfferAutoru

    result.nextCheck.nonEmpty shouldBe true
    resultOffer.getVinResolution.hasResolution shouldBe true
    resultOffer.getVinResolution.getVersion shouldBe version
    resultOffer.getVinResolution.getResolution shouldBe resolutionNotEmpty
    resultOffer.getDocuments.getVin shouldBe testVin

    verify(client).getResolutionByLicensePlate(
      testPhotoLicensePlate,
      VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None)
    )
    verify(saleDao).saveProperties(?)(?)
  }

  (
    "send resolution request to vin-decoder on offer " +
      "processing and drop resolution and vin if result is very bad and license is from photo"
  ) in new Fixture {

    when(client.getResolutionByLicensePlate(?, ?)(?))
      .thenReturn(Success(resolutionWithStatus(VinResolutionEnums.Status.ERROR)))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)
    when(saleDao.saveProperties(?)(?)).thenReturn(Seq.empty)
    when(saleDao.getOffer(?)(?)).thenReturn(Some(createSale))
    when(settingsDao.getCachedSettingMappings).thenReturn(Map("vin" -> 12))

    val offer = testOffer
    offer.getOfferAutoruBuilder.getDocumentsBuilder.clearLicensePlate()

    val result = checkProcessed(offer.build())
    val resultOffer = result.updateOfferFunc.get(offer.build()).getOfferAutoru

    result.nextCheck.nonEmpty shouldBe true
    resultOffer.getVinResolution.hasResolution shouldBe false
    resultOffer.getVinResolution.getVersion shouldBe version
    resultOffer.getDocuments.getVin shouldBe ""

    verify(client).getResolutionByLicensePlate(
      testPhotoLicensePlate,
      VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None)
    )
  }

  ("send vin resolution request to vin-decoder on offer processing and not drop resolution even if result is bad") in new Fixture {

    when(client.getResolutionByLicensePlate(?, ?)(?))
      .thenReturn(Success(resolutionWithStatus(VinResolutionEnums.Status.INVALID)))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)
    when(saleDao.saveProperties(?)(?)).thenReturn(Seq.empty)
    when(saleDao.getOffer(?)(?)).thenReturn(Some(createSale))
    when(settingsDao.getCachedSettingMappings).thenReturn(Map("vin" -> 12))

    val offer = testOffer

    val result = checkProcessed(offer.build())
    val resultOffer = result.updateOfferFunc.get(offer.build()).getOfferAutoru

    result.nextCheck.nonEmpty shouldBe true
    resultOffer.getVinResolution.hasResolution shouldBe true
    resultOffer.getVinResolution.getResolution.getEntriesCount > 0 shouldBe true
    resultOffer.getVinResolution.getVersion shouldBe version
    resultOffer.getDocuments.getVin shouldBe testVin

    verify(client)
      .getResolutionByLicensePlate(testLicensePlate, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }
}
