package ru.yandex.vertis.parsing.api.managers

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.CatalogModel.TechInfo
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.api.managers.offers.OffersManagerImpl
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.components.TestParsingComponents
import ru.yandex.vertis.parsing.auto.components.vehiclename.{CarVehicleNameAware, NopVehicleNameAware, TruckVehicleNameAware}
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.parsers.CommonAutoParser
import ru.yandex.vertis.parsing.auto.util.TestDataUtils
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.common.UrlOrHashOrId
import ru.yandex.vertis.parsing.util.TestUtils
import ru.yandex.vertis.parsing.util.api.exceptions.{IncorrectParamsException, OfferNotFoundException}
import ru.yandex.vertis.parsing.util.api.{Request, RequestImpl}
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class OffersManagerImplTest extends FunSuite with MockitoSupport with ScalaFutures {
  private val components = TestParsingComponents

  import components._

  private val offersManager = new OffersManagerImpl
    with components.DaoAwareImpl
    with components.CatalogsAwareImpl
    with components.ConvertersAwareImpl
    with components.DiffAnalyzerFactoryAwareImpl
    with components.FeaturesAwareImpl
    with components.ClientsAwareImpl
    with components.ExecutionContextAwareImpl
    with components.HolocronConverterAwareImpl
    with NopVehicleNameAware
    with TruckVehicleNameAware
    with CarVehicleNameAware

  test("getInfo for offer in OPENED status") {
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getTruckInfoBuilder.setMark("MERCEDES").setModel("814")
    offer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone("79291112233")
    val row = testRow(testAvitoTrucksUrl, offer).copy(status = CommonModel.Status.OPENED)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))

    type T1 = Seq[String]
    type R = Map[String, Boolean]

    stub.apply[T1, Traced, R](phonesDao.checkPhonesRegistered(_: T1)(_: Traced)) {
      case (phones, _) =>
        phones.map(p => (p, false)).toMap
    }

    val res = offersManager.getInfo(UrlOrHashOrId.Url(row.url), doNotSetOpened = true).futureValue
    assert(res.getCanPublish.getValue)
  }

  test("getInfo for offer in OPENED status by id") {
    reset(phonesDao)
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getTruckInfoBuilder.setMark("MERCEDES").setModel("814")
    offer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone("79291112233")
    val row: ParsedRow = testRow(testAvitoTrucksUrl, offer).copy(status = CommonModel.Status.OPENED)
    val id = row.data.getOffer.getAdditionalInfo.getRemoteId
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))

    type T1 = Seq[String]
    type R = Map[String, Boolean]

    stub.apply[T1, Traced, R](phonesDao.checkPhonesRegistered(_: T1)(_: Traced)) {
      case (phones, _) =>
        phones.map(p => (p, false)).toMap
    }

    val res = offersManager.getInfo(UrlOrHashOrId.Id(id), doNotSetOpened = true).futureValue
    assert(res.getCanPublish.getValue)
    verify(parsedOffersDao).getParsedOffers(eq(Seq(row.hash)), eq(false), eq(false))(?)
  }

  test("get photos: incorrect params") {
    implicit val request: Request = new RequestImpl
    val url = "http://avito.ru"
    intercept[IncorrectParamsException] {
      TestUtils.cause(offersManager.getPhotos(UrlOrHashOrId.Url(url), None, Seq.empty).futureValue)
    }
  }

  test("get photos: offer not found") {
    implicit val request: Request = new RequestImpl
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq.empty)
    val url = TestDataUtils.testAvitoCarsUrl
    intercept[OfferNotFoundException] {
      TestUtils.cause(
        offersManager.getPhotos(UrlOrHashOrId.Url(url), None, Seq.empty).futureValue(Timeout(1500.millis))
      )
    }
  }

  test("getInfo for cars") {
    reset(phonesDao)
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getCarInfoBuilder.setTechParamId(500)
    offer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone("79291112233")
    val url = testAvitoCarsUrl
    val remoteId = CommonAutoParser.remoteId(url)
    val row = testRow(url, offer, category = Category.CARS).copy(status = CommonModel.Status.SENT)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))
    val techInfo = TechInfo.newBuilder()
    techInfo.getMarkInfoBuilder.setName("MERCEDES")
    techInfo.getModelInfoBuilder.setName("814")
    when(searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(Some(techInfo.build())))

    type T1 = Seq[String]
    type R = Map[String, Boolean]

    stub.apply[T1, Traced, R](phonesDao.checkPhonesRegistered(_: T1)(_: Traced)) {
      case (phones, _) =>
        phones.map(p => (p, false)).toMap
    }

    val res = offersManager.getInfo(UrlOrHashOrId.Url(row.url), doNotSetOpened = true).futureValue
    assert(res.getCanPublish.getValue)
    assert(res.getRemoteId == remoteId)
    assert(res.getRemoteUrl == url)
  }

  test("don't allow to publish trucks without phones") {
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getTruckInfoBuilder.setMark("MERCEDES").setModel("814")
    val row = testRow(testAvitoTrucksUrl, offer).copy(status = CommonModel.Status.OPENED)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))

    val res = offersManager.getInfo(UrlOrHashOrId.Url(row.url), doNotSetOpened = true).futureValue
    assert(!res.getCanPublish.getValue)
  }

  test("allow to publish cars without phones") {
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getCarInfoBuilder.setTechParamId(500)
    val row = testRow(testAvitoCarsUrl, offer, category = Category.CARS).copy(status = CommonModel.Status.SENT)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))
    val techInfo = TechInfo.newBuilder()
    techInfo.getMarkInfoBuilder.setName("MERCEDES")
    techInfo.getModelInfoBuilder.setName("814")
    when(searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(Some(techInfo.build())))

    val res = offersManager.getInfo(UrlOrHashOrId.Url(row.url), doNotSetOpened = true).futureValue
    assert(res.getCanPublish.getValue)
  }

  test("allow to publish cars for registered users") {
    reset(phonesDao)
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getCarInfoBuilder.setTechParamId(500)
    offer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone("79291112233")
    val row = testRow(testAvitoCarsUrl, offer, category = Category.CARS).copy(status = CommonModel.Status.SENT)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))
    val techInfo = TechInfo.newBuilder()
    techInfo.getMarkInfoBuilder.setName("MERCEDES")
    techInfo.getModelInfoBuilder.setName("814")
    when(searcherClient.carsCatalogData(?)(?)).thenReturn(Future.successful(Some(techInfo.build())))

    type T1 = Seq[String]
    type R = Map[String, Boolean]

    stub.apply[T1, Traced, R](phonesDao.checkPhonesRegistered(_: T1)(_: Traced)) {
      case (phones, _) =>
        phones.map(p => (p, true)).toMap
    }

    val res = offersManager.getInfo(UrlOrHashOrId.Url(row.url), doNotSetOpened = true).futureValue
    assert(res.getCanPublish.getValue)
  }

  test("fix drive") {
    val hash = "hash"
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getCarInfoBuilder.setDrive("FRONT")
    val row = testRow(testAvitoCarsUrl, offer, category = Category.CARS).copy(status = CommonModel.Status.SENT)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))
    val result = offersManager.getOffer(UrlOrHashOrId.Hash(hash), None).futureValue
    assert(result.getCarInfo.getDrive == "FORWARD_CONTROL")
  }

  test("fix transmission") {
    val hash = "hash"
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getCarInfoBuilder.setTransmission("ROBOT_2CLUTCH")
    val row = testRow(testAvitoCarsUrl, offer, category = Category.CARS).copy(status = CommonModel.Status.SENT)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))
    val result = offersManager.getOffer(UrlOrHashOrId.Hash(hash), None).futureValue
    assert(result.getCarInfo.getTransmission == "ROBOT")
  }

  test("getOffer without vin") {
    val hash = "hash"
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getDocumentsBuilder.setVin("0****30")
    val row = testRow(testAvitoCarsUrl, offer, category = Category.CARS).copy(status = CommonModel.Status.SENT)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))
    val result = offersManager.getOffer(UrlOrHashOrId.Hash(hash), None).futureValue
    assert(result.getDocuments.getVin.isEmpty)
  }

  test("getOffer with only phone") {
    val hash = "hash"
    val phone1 = "phone1"
    val phone2 = "phone2"
    val phone3 = "phone3"
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone2)
    offer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone3)
    val row = testRow(testAvitoCarsUrl, offer, category = Category.CARS).copy(status = CommonModel.Status.SENT)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))
    val result = offersManager.getOffer(UrlOrHashOrId.Hash(hash), Some(phone1)).futureValue
    assert(result.getSeller.getPhonesCount == 1)
    assert(result.getSeller.getPhones(0).getPhone == phone1)
  }

  test("getOffer with only phone 2") {
    val hash = "hash"
    val phone2 = "phone2"
    val phone3 = "phone3"
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone2)
    offer.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone3)
    val row = testRow(testAvitoCarsUrl, offer, category = Category.CARS).copy(status = CommonModel.Status.SENT)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))
    val result = offersManager.getOffer(UrlOrHashOrId.Hash(hash), None).futureValue
    assert(result.getSeller.getPhonesCount == 2)
    assert(result.getSeller.getPhones(0).getPhone == phone2)
    assert(result.getSeller.getPhones(1).getPhone == phone3)
  }

  test("getFull: don't clear vin") {
    val hash = "hash"
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.getOfferBuilder.getDocumentsBuilder.setVin("0****30")
    val row = testRow(testAvitoCarsUrl, offer, category = Category.CARS).copy(status = CommonModel.Status.SENT)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))
    val result = offersManager.getFullData(UrlOrHashOrId.Hash(hash)).futureValue
    assert(result.getOffer.getDocuments.getVin.nonEmpty)
  }

  test("replace avito photo sizes: feature disabled: don't replace") {
    val hash = "hash"
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.addPhoto("https://84.img.avito.st/640x480/3852083484.jpg")
    offer.addPhoto("https://12.img.avito.st/640x480/3852090112.jpg")
    val row = testRow(testAvitoCarsUrl, offer, category = Category.CARS).copy(status = CommonModel.Status.SENT)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))
    val result = offersManager.getPhotos(UrlOrHashOrId.Hash(hash), None, Seq.empty).futureValue
    assert(result.getPhotoCount == 2)
    assert(result.getPhoto(0) == "https://84.img.avito.st/640x480/3852083484.jpg")
    assert(result.getPhoto(1) == "https://12.img.avito.st/640x480/3852090112.jpg")
  }

  test("replace avito photo sizes: feature enabled: replace") {
    features.ReplaceAvitoPhotoSizes.setEnabled(true)
    val hash = "hash"
    implicit val request: Request = new RequestImpl
    val offer = ParsedOffer.newBuilder()
    offer.addPhoto("https://84.img.avito.st/640x480/3852083484.jpg")
    offer.addPhoto("https://12.img.avito.st/640x480/3852090112.jpg")
    val row = testRow(testAvitoCarsUrl, offer, category = Category.CARS).copy(status = CommonModel.Status.SENT)
    when(parsedOffersDao.getParsedOffers(?, ?, ?)(?)).thenReturn(Seq(row))
    val result = offersManager.getPhotos(UrlOrHashOrId.Hash(hash), None, Seq.empty).futureValue
    assert(result.getPhotoCount == 2)
    assert(result.getPhoto(0) == "https://84.img.avito.st/1280x960/3852083484.jpg")
    assert(result.getPhoto(1) == "https://12.img.avito.st/1280x960/3852090112.jpg")
    features.ReplaceAvitoPhotoSizes.setEnabled(false)
  }

  test("setPublished: provide published phones") {
    val hash = "hash"
    val offerId = "100500-hash"
    val phone = "79291112233"
    implicit val request: Request = new RequestImpl
    when(parsedOffersDao.setPublished(?, ?, ?)(?)).thenReturn(Some(true))
    offersManager.setPublished(UrlOrHashOrId.Hash(hash), offerId, Seq(phone)).futureValue
    verify(parsedOffersDao).setPublished(eq(hash), eq(offerId), eq(Seq(phone)))(any())
  }
}
