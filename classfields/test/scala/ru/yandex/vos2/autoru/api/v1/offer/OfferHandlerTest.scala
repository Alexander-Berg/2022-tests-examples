package ru.yandex.vos2.autoru.api.v1.offer

import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.JsString
import ru.auto.api.{ApiOfferModel, CarsModel}
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName
import ru.auto.api.ApiOfferModel.{DeliveryInfo, DeliveryRegion, Location, OfferStatus}
import ru.auto.api.CommonModel.{GeoPoint, RegionInfo}
import ru.auto.api.ResponseModel.{PhoneCallsCountersResponse, StsPhotoUploadResponse}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferID
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.utils.Vos2ApiHandlerResponses._
import ru.yandex.vos2.autoru.utils.PhotoUtilsApiSuite
import ru.yandex.vos2.autoru.utils.testforms.TestFormParams
import ru.yandex.vos2.util.{ExternalAutoruUserRef, Protobuf, RandomUtil}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vos2.services.mds.{AutoruOrigNamespaceSettings, MdsPhotoData}

import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class OfferHandlerTest
  extends AnyFunSuiteLike
  with Matchers
  with InitTestDbs
  with OptionValues
  with PhotoUtilsApiSuite
  with BeforeAndAfter {

  before {
    initDbs()
    Get("/api/v1/offer/cars/blabla") ~> route ~> check {}
  }

  after {
    components.featureRegistry.updateFeature(components.featuresManager.PhotoAddUrl.name, false)
    Mockito.reset(components.passportClient)
  }

  implicit private val t: Traced = Traced.empty

  test("photoAdd: feature is on, isDealer=false, photos are empty") {
    components.featureRegistry.updateFeature(components.featuresManager.PhotoAddUrl.name, true)
    val photoAddUrl = "https://auto.ru/l/" + RandomUtil.nextHexString(8)
    when(components.passportClient.createPhotoAddUrl(?)(?)).thenReturn(Some(photoAddUrl))
    val category = "cars"
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    val offerId = checkSuccessRequest(req)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    readForm.getAdditionalInfo.getPhotoAddUrl shouldBe photoAddUrl
  }

  test("photoAdd: feature is on, isDealer=false, photos non empty") {
    components.featureRegistry.updateFeature(components.featuresManager.PhotoAddUrl.name, true)
    val userId: Long = testFormGenerator.carTestForms.randomUser.id
    val extUserId: String = ExternalAutoruUserRef.privateRef(userId)

    // создаем черновик
    val req1 = createRequest(Post(s"/api/v1/draft/cars/$extUserId")) { builder =>
      builder.setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
    }
    val draftId = checkSuccessRequest(req1)
    val readDraft = checkSuccessReadRequest(Get(s"/api/v1/draft/cars/$extUserId/$draftId"))
    val userRef = readDraft.getUserRef

    val fullForm = testFormGenerator.carTestForms.generatePrivateForm(TestFormParams(optOwnerId = Some(userId))).form
    checkSuccessRequest(createRequest(Put(s"/api/v1/draft/cars/$userRef/$draftId")) { builder =>
      builder.mergeFrom(fullForm)
    })

    // передали данные загруженной картинки, фото успешно сохраняется
    val orig = MdsPhotoData(AutoruOrigNamespaceSettings.namespace, imageHashGenerator.generateValue)
    val bodyPart =
      BodyPart("data", HttpEntity(s"""{"namespace":"autoru-orig", "groupId":${orig.groupId}, "name":"${orig.id}"}"""))
    val req2 = Post(s"/api/v1/draft/cars/$userRef/$draftId/photo", Multipart.FormData(bodyPart))
    checkSuccessProtoFromJsonRequest[StsPhotoUploadResponse](req2)

    // сохраняем полноценное объявление
    val offerId = checkSuccessRequest(Post(s"/api/v1/draft/cars/$draftId/publish/$extUserId"))
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/cars/$offerId"))

    readForm.getAdditionalInfo.getPhotoAddUrl shouldBe empty
  }

  test("photoAdd: feature is on, isDealer=true") {
    components.featureRegistry.updateFeature(components.featuresManager.PhotoAddUrl.name, true)
    val category = "cars"
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = true))
    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    val offerId = checkSuccessRequest(req)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    readForm.getAdditionalInfo.getPhotoAddUrl shouldBe empty
  }

  test("photoAdd: feature is off") {
    val category = "cars"
    val formInfo = testFormGenerator.createForm(category, TestFormParams(isDealer = false))
    val extUserId: String = formInfo.form.getUserRef
    val req = Post(s"/api/v1/offers/$category/$extUserId?insert_new=1&source=mobile")
      .withEntity(HttpEntity(ContentTypes.`application/json`, formInfo.json))
    val offerId = checkSuccessRequest(req)
    val readForm = checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$offerId"))
    readForm.getAdditionalInfo.getPhotoAddUrl shouldBe empty
  }

  test("offersInfo") {
    val offer = getOfferById(1043045004L)
    components.getOfferDao().saveMigrated(Seq(offer), "test")
    components.offerVosDao.saveMigratedFromYdb(Seq(offer))(Traced.empty)

    Get("/api/v1/offer/cars/blabla") ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
      contentType shouldBe ContentTypes.`application/json`
      responseAs[String] shouldBe
        """{"status":"ERROR","errors":[{"code":"ILLEGAL_ARGUMENT","description":"Offer """ +
          """Id is Invalid"}]}"""
    }

    Get("/api/v1/offer/cars/user:71279827/1043045004-977b3?include_removed=true") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
      contentType shouldBe ContentTypes.`application/json`
      responseAs[String] shouldBe
        """{"status":"ERROR","errors":[{"code":"UNKNOWN_OFFER","description":"Offer Not Found"}]}"""
    }

    Get("/api/v1/offer/moto/blabla") ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
      contentType shouldBe ContentTypes.`application/json`
      responseAs[String] shouldBe
        """{"status":"ERROR","errors":[{"code":"ILLEGAL_ARGUMENT","description":"Offer """ +
          """Id is Invalid"}]}"""
    }

    // передана невалидная категория, offerId не передан
    Get("/api/v1/offer/blabla") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
      contentType shouldBe ContentTypes.`application/json`
      responseAs[String] shouldBe
        """{"status":"ERROR","errors":[{"code":"NOT_FOUND","description":"The """ +
          """requested handler could not be found. Please check method and url of the request."}]}"""
    }

    // передана невалидная категория
    Get("/api/v1/offer/blabla/1043045004-977b3") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
      contentType shouldBe ContentTypes.`application/json`
      responseAs[String] shouldBe
        """{"status":"ERROR","errors":[{"code":"NOT_FOUND","description":"The """ +
          """requested handler could not be found. Please check method and url of the request."}]}"""
    }

    // ничего не нашли, потому что передана не та категория
    Get("/api/v1/offer/moto/1043045004-977b3") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
      contentType shouldBe ContentTypes.`application/json`
    }

    // нашли по offerId через маппинг t_offers_users, категория подходит
    Get("/api/v1/offer/cars/1043045004-977b3") ~> route ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      val offer = parseAsFormData(responseAs[String])
      assert(offer.getDescription.startsWith("VOS"))
    }

    // нашли по offerId через маппинг t_offers_users, категория all подходит всем, но только если из новой базы
    // в старой это будет чтение из разных таблиц, так что для all просто не будем знать, откуда читать
    Get("/api/v1/offer/all/1043045004-977b3") ~> route ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      val offer = parseAsFormData(responseAs[String])
      assert(offer.getDescription.startsWith("VOS"))
    }

    components.mySql.shards.foreach(shard => {
      shard.master.jdbc.update("delete from t_offers_users")
    })

    // тест, что обновил маппинг: в t_offers_users изначально ничего не было, мы пришли, был вызов searchOffer... и
    // табличка обновилась
    Get("/api/v1/offer/cars/1043045004-977b3") ~> route ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      val offer = parseAsFormData(responseAs[String])
      assert(offer.getDescription.startsWith("VOS"))
    }

    // ничего не нашли - не передана категория
    Get("/api/v1/offer/1043045004-977b3") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
      contentType shouldBe ContentTypes.`application/json`
    }

    // ничего не нашли - передана не та категория
    Get("/api/v1/offer/trucks/1043045004-977b3") ~> route ~> check {
      status shouldBe StatusCodes.NotFound // на самом деле тут not found - обработчик не нашли...
      contentType shouldBe ContentTypes.`application/json`
    }

    Get("/api/v1/offer/moto/1043045004-977b3") ~> route ~> check {
      status shouldBe StatusCodes.NotFound // на самом деле тут not found - обработчик не нашли...
      contentType shouldBe ContentTypes.`application/json`
    }

    Get("/api/v1/offer/cars/1043045004-977b3") ~> route ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      val offer = parseAsFormData(responseAs[String])
      assert(offer.getDescription.startsWith("VOS GL350 CDI 4 matic"))
    }

    Get("/api/v1/offer/all/1043045004-977b3") ~> route ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      val offer = parseAsFormData(responseAs[String])
      assert(offer.getDescription.startsWith("VOS GL350 CDI 4 matic"))
    }

    // недокументированный параметр для чтения из vos независимо от зукипера
    Get("/api/v1/offer/all/1043045004-977b3?fromvos=1") ~> route ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      val offer = parseAsFormData(responseAs[String])
      assert(offer.getDescription.startsWith("VOS"))
    }

    // не передан хеш - отдаем 404, хотя с таким айди объявление видим
    Get("/api/v1/offer/cars/1043045004") ~> route ~> check {
      status shouldBe StatusCodes.NotFound
      contentType shouldBe ContentTypes.`application/json`
      responseAs[String] shouldBe
        """{"status":"ERROR","errors":[{"code":"UNKNOWN_OFFER","description":"Offer """ +
          """Not Found"}]}"""
    }

    Get("/api/v1/offer/cars/blabla") ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
      contentType shouldBe ContentTypes.`application/json`
      responseAs[String] shouldBe
        """{"status":"ERROR","errors":[{"code":"ILLEGAL_ARGUMENT","description":"Offer """ +
          """Id is Invalid"}]}"""
    }

    Get("/api/v1/offer/moto/blabla") ~> route ~> check {
      status shouldBe StatusCodes.BadRequest
      contentType shouldBe ContentTypes.`application/json`
      responseAs[String] shouldBe
        """{"status":"ERROR","errors":[{"code":"ILLEGAL_ARGUMENT","description":"Offer """ +
          """Id is Invalid"}]}"""
    }

    Get("/api/v1/offer/cars/1043045004-977b3/callsCounters?from=2017-09-14") ~> route ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      val counters = parseCounters(responseAs[String])
      counters.getDailyCallsCount shouldBe 1
      counters.getDailyCallsOrDefault("2017-09-14", 0) shouldBe 2
    }
  }

  val allBooleanOptions = Seq(None, Some(false), Some(true))

  test("don't get hashed id from both old db and vos without data in old db") {
    initDbs(withData = false)
    for (fromVos <- allBooleanOptions) {
      checkNotFoundHashed(fromVos)
    }
  }

  test("get hashed id only from old db with data in old db and without migration") {
    checkNotFoundHashed(fromVos = Some(true))
    for (fromVos <- Seq(None, Some(false))) {
      checkOkHashed(fromVos)
    }
    checkNotFoundHashed(Some(true))
  }

  test("get hashed id from both old db and vos after migration") {
    components.getOfferDao().saveMigrated(getOffersFromJson, "test")
    components.offerVosDao.saveMigratedFromYdb(getOffersFromJson)(Traced.empty)

    for (fromVos <- allBooleanOptions) {
      checkOkHashed(fromVos)
    }
  }

  test("get hashed id of removed offer too") {
    components.getOfferDao().saveMigrated(getOffersFromJson, "test")
    components.offerVosDao.saveMigratedFromYdb(getOffersFromJson)(Traced.empty)

    checkSimpleSuccessRequest(Delete("/api/v1/offer/cars/1043270830-6b56a"))
    for (fromVos <- allBooleanOptions) {
      checkOkHashed(fromVos)
    }
  }

  private def checkOkHashed(fromVos: Option[Boolean]): Unit = {
    val json = checkSuccessJsonValueRequest(
      Get("/api/v1/offer/cars/user:18318774/i-ref/1043270830/hashed".withParams(fromVos))
    )
    val id = (json \ "id").get
    assert(id == JsString("1043270830-6b56a"))
  }

  private def checkNotFoundHashed(fromVos: Option[Boolean]): Unit = {
    checkErrorRequest(
      Get("/api/v1/offer/cars/user:18318774/i-ref/1043270830/hashed".withParams(fromVos)),
      StatusCodes.NotFound,
      unknownOfferError
    )
  }

  implicit private class RichStringPath(path: String) {

    def withParams(fromVos: Option[Boolean]): Uri =
      Uri(path).withQuery(Query(fromVos.map(b => "fromvos" -> (if (b) "1" else "0")).toMap))
  }

  test("return 404 for unexisting OfferIRef") {
    checkErrorRequest(
      Get("/api/v1/offer/cars/user:18318774/i-ref/987654321/hashed"),
      StatusCodes.NotFound,
      unknownOfferError
    )
  }

  test("return 404 for other category") {
    checkErrorRequest(
      Get("/api/v1/offer/moto/user:18318774/i-ref/1043270830/hashed"),
      StatusCodes.NotFound,
      unknownOfferError
    )
  }

  test("return 404 for absent category segment") {
    checkErrorRequest(
      Get("/api/v1/offer/user:18318774/i-ref/1043270830/hashed"),
      StatusCodes.NotFound,
      unknownHandler
    )
  }

  test("return 404 for absent userref segment") {
    checkErrorRequest(
      Get("/api/v1/offer/cars/i-ref/1043270830/hashed"),
      StatusCodes.NotFound,
      unknownHandler
    )
  }

  test("return 400 for non-long OfferIRef") {
    checkErrorRequest(
      Get("/api/v1/offer/cars/user:18318774/i-ref/1043270830-6b56a/hashed"),
      StatusCodes.BadRequest,
      invalidOfferIRef
    )
  }

  test("return 400 for negative OfferIRef") {
    checkErrorRequest(
      Get("/api/v1/offer/cars/user:18318774/i-ref/-1043270830/hashed"),
      StatusCodes.BadRequest,
      invalidOfferIRef
    )
  }

  test("return 404 for absent hashed segment") {
    checkErrorRequest(
      Get("/api/v1/offer/cars/user:18318774/i-ref/1043270830"),
      StatusCodes.NotFound,
      unknownHandler
    )
  }

  test("update offer delivery info and set delivery tag") {
    val offerId = "1043270830-6b56a"
    components.getOfferDao().saveMigrated(getOffersFromJson, "test")
    offerDeliveryInfoIsEmpty(offerId)
    updateOfferDeliveryInfo(offerId, buildDeliveryInfo())
    offerDeliveryInfoIsNotEmpty(offerId)
  }

  test("enable and disable offer multiposting classified when multiposting is active") {
    val offerId = "1043270830-6b56a"
    components.getOfferDao().saveMigrated(getOffersFromJson, "test")

    offerClassifiedState(offerId, ClassifiedName.AUTORU, isEnabled = true, OfferStatus.STATUS_UNKNOWN)
    offerClassifiedState(offerId, ClassifiedName.AVITO, isEnabled = false, OfferStatus.STATUS_UNKNOWN)

    activateMultiposting(offerId) // classified status will be ACTIVE

    offerClassifiedState(offerId, ClassifiedName.AUTORU, isEnabled = true, OfferStatus.ACTIVE)
    enableMultipostingClassified(offerId, ClassifiedName.AUTORU)
    offerClassifiedState(offerId, ClassifiedName.AUTORU, isEnabled = true, OfferStatus.ACTIVE)
    disableMultipostingClassified(offerId, ClassifiedName.AUTORU)
    offerClassifiedState(offerId, ClassifiedName.AUTORU, isEnabled = false, OfferStatus.INACTIVE)

    offerClassifiedState(offerId, ClassifiedName.AVITO, isEnabled = false, OfferStatus.STATUS_UNKNOWN)
    enableMultipostingClassified(offerId, ClassifiedName.AVITO)
    offerClassifiedState(offerId, ClassifiedName.AVITO, isEnabled = true, OfferStatus.NEED_ACTIVATION)
    disableMultipostingClassified(offerId, ClassifiedName.AVITO)
    offerClassifiedState(offerId, ClassifiedName.AVITO, isEnabled = false, OfferStatus.INACTIVE)
  }

  test("enable and disable offer multiposting classified when multiposting is inactive") {
    val offerId = "1043270830-6b56a"
    components.getOfferDao().saveMigrated(getOffersFromJson, "test")
    deactivateMultiposting(offerId) // classified status will be INACTIVE

    offerClassifiedState(offerId, ClassifiedName.AVITO, isEnabled = false, OfferStatus.INACTIVE)
    enableMultipostingClassified(offerId, ClassifiedName.AVITO)
    offerClassifiedState(offerId, ClassifiedName.AVITO, isEnabled = true, OfferStatus.INACTIVE)
    disableMultipostingClassified(offerId, ClassifiedName.AVITO)
    offerClassifiedState(offerId, ClassifiedName.AVITO, isEnabled = false, OfferStatus.INACTIVE)
  }

  test("reset offer delivery info and remove delivery tag") {
    val offerId = "1043270830-6b56a"
    components.getOfferDao().saveMigrated(getOffersFromJson, "test")
    offerDeliveryInfoIsEmpty(offerId)
    updateOfferDeliveryInfo(offerId, buildDeliveryInfo())
    offerDeliveryInfoIsNotEmpty(offerId)
    updateOfferDeliveryInfo(offerId, DeliveryInfo.getDefaultInstance)
    offerDeliveryInfoIsEmpty(offerId)
  }

  private def offerClassifiedState(offerId: OfferID,
                                   classified: ClassifiedName,
                                   isEnabled: Boolean,
                                   expectedStatus: OfferStatus): Unit = {
    val offer = getOffer(offerId)
    val cf = offer.getMultiposting.getClassifiedsList.asScala.find(_.getName == classified)
    assert(cf.exists(_.getEnabled == isEnabled))
    assert(cf.exists(_.getStatus == expectedStatus))
  }

  private def getOffer(offerId: OfferID): ApiOfferModel.Offer = {
    Get(s"/api/v1/offer/cars/$offerId".withParams(Some(true)).toString()) ~> route ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      val offer = parseAsFormData(responseAs[String])
      offer
    }
  }

  private def offerDeliveryInfoIsNotEmpty(offerId: OfferID): Unit = {
    Get(s"/api/v1/offer/cars/$offerId".withParams(Some(true)).toString()) ~> route ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      val offer = parseAsFormData(responseAs[String])
      assert(buildDeliveryInfo(enrichWithFederalSubjectId = true) == offer.getDeliveryInfo)
      val deliveryTag = "delivery"
      assert(offer.getTagsList.contains(deliveryTag))
    }
  }

  private def offerDeliveryInfoIsEmpty(offerId: OfferID): Unit = {
    Get(s"/api/v1/offer/cars/$offerId".withParams(Some(true)).toString()) ~> route ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      val offer = parseAsFormData(responseAs[String])
      assert(offer.getDeliveryInfo == DeliveryInfo.getDefaultInstance)
      val deliveryTag = "delivery"
      assert(!offer.getTagsList.contains(deliveryTag))
    }
  }

  private def updateOfferDeliveryInfo(offerId: OfferID, deliveryInfo: DeliveryInfo): Unit = {
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/cars/user:18318774/$offerId/delivery", deliveryInfo.toByteArray))
  }

  private def enableMultipostingClassified(offerId: OfferID, classified: ClassifiedName): Unit = {
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/cars/user:18318774/$offerId/multiposting/$classified"))
  }

  private def disableMultipostingClassified(offerId: OfferID, classified: ClassifiedName): Unit = {
    checkSimpleSuccessRequest(Delete(s"/api/v1/offer/cars/user:18318774/$offerId/multiposting/$classified"))
  }

  private def activateMultiposting(offerId: OfferID): Unit = {
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/cars/user:18318774/$offerId/multiposting/status/active"))
  }

  private def deactivateMultiposting(offerId: OfferID): Unit = {
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/cars/user:18318774/$offerId/multiposting/status/inactive"))
  }

  /**
    * @param enrichWithFederalSubjectId - enrich delivery region location with
    *                                   federal_subject_id if {@code true}
    */
  private def buildDeliveryInfo(enrichWithFederalSubjectId: Boolean = false) = {
    DeliveryInfo
      .newBuilder()
      .addDeliveryRegions(
        DeliveryRegion
          .newBuilder()
          .setLocation {
            val b = Location
              .newBuilder()
              .setAddress("asd")
              .setGeobaseId(213)
              .setRegionInfo(
                RegionInfo
                  .newBuilder()
                  .setName("Moscow")
                  .setId(213)
              )
              .setCoord(
                GeoPoint
                  .newBuilder()
                  .setLatitude(1.23)
                  .setLongitude(1.32)
              )
            if (enrichWithFederalSubjectId) {
              b.setFederalSubjectId(1)
            }
            b.build()
          }
      )
      .build()
  }

  private def parseAsFormData(json: String): ApiOfferModel.Offer = {
    Protobuf.fromJson[ApiOfferModel.Offer](json)
  }

  private def parseCounters(json: String): PhoneCallsCountersResponse = {
    Protobuf.fromJson[PhoneCallsCountersResponse](json)
  }
}
