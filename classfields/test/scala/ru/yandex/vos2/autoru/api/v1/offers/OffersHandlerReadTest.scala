package ru.yandex.vos2.autoru.api.v1.offers

import java.sql.Timestamp
import akka.http.scaladsl.model._
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.functional.syntax._
import play.api.libs.json._
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.ResponseModel.OfferIdsByVinsResponse.OfferIdByVin
import ru.auto.api.ResponseModel.{OfferBelongResponse, OfferIdsByVinsResponse}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.utils.Vos2ApiHandlerResponses._
import ru.yandex.vos2.autoru.utils.Vos2ApiSuite
import ru.yandex.vos2.dao.utils.JdbcTemplateWrapper
import ru.yandex.vos2.{getNow, OfferID, Vin}
import ru.yandex.vos2.autoru.utils.testforms.TestFormParams
import ru.yandex.vos2.util.{ExternalAutoruUserRef, Protobuf}

import scala.jdk.CollectionConverters._
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class OffersHandlerReadTest extends AnyFunSuiteLike with Vos2ApiSuite with BeforeAndAfterAll with OptionValues {

  override def beforeAll(): Unit = {
    initDbs()
  }

  case class OffersListPaginationData(page: Int, page_size: Int, total_offers_count: Int, total_page_count: Int)

  implicit val offersListPaginationDataReader = Json.reads[OffersListPaginationData]

  case class OffersListFilters(include_removed: Boolean, owner: Boolean)

  implicit val offersListFilters = Json.reads[OffersListFilters]

  case class HaveId(id: String)

  implicit val haveIdReader = Json.reads[HaveId]

  case class OffersListData(offers: List[HaveId], pagination: OffersListPaginationData)

  implicit val offersListData = (
    (__ \ "offers").readNullable[List[HaveId]].map(_.getOrElse(List.empty)) and
      (__ \ "pagination").read[OffersListPaginationData]
  )(OffersListData.apply _)

  private val now = getNow

  private val offer1 = getOfferById(1042409964L).toBuilder.setTimestampCreate(now).build()
  private val offer2 = getOfferById(1044159039L).toBuilder.setTimestampCreate(now).build()
  private val offer3 = getOfferById(1043045004L)
  private val offer4 = getOfferById(1044216699L)
  private val offer5 = getOfferById(1043211458L)

  implicit private val t = Traced.empty

  test("offersList") {
    components.getOfferDao().saveMigrated(Seq(offer1, offer2, offer3, offer4, offer5), "test")
    components.offerVosDao.saveMigratedFromYdb(Seq(offer1, offer2, offer3, offer4, offer5))(Traced.empty)

    components.autoruSalesDao.shard.master.jdbc.batchUpdate(
      "update all7.sales set create_date = ? where id = ?",
      Seq(Array(new Timestamp(now), Long.box(1042409964L)), Array(new Timestamp(now), Long.box(1044159039L)))
    )
    // читаем из базы vos

    readTests()

  }

  test("client region") {
    val (_, randomUserRef, _) = testFormGenerator.randomOwnerIds(isDealer = false)
    val randomSalon = testFormGenerator.randomSalon.client.get
    val randomSalonRef = ExternalAutoruUserRef.salonRef(randomSalon.id)
    val region = (checkSuccessJsonValueRequest(Get(s"/api/v1/users/$randomSalonRef/region")) \ "region").as[Long]
    assert(region == randomSalon.yaCityId.get)
    checkErrorRequest(Get(s"/api/v1/users/$randomUserRef/region"), StatusCodes.BadRequest, unallowedUserRef)
  }

  for {
    category <- Seq("cars", "trucks", "moto")
  } test(s"user instead of client with category $category") {
    val (_, randomUserRef, _) = testFormGenerator.randomOwnerIds(isDealer = false)
    val randomSalonRef = testFormGenerator.randomSalon.client.get.id

    val form = testFormGenerator
      .createForm(category, TestFormParams(now = new DateTime(), isDealer = true, optOwnerId = Some(randomSalonRef)))
      .form
      .toBuilder
      .clearBadges()
      .clearServices()
      .build()
    val extDealerId = form.getUserRef
    val offerId = checkSuccessRequest(
      Post(s"/api/v1/offers/$category/$extDealerId?insert_new=1&source=ios")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(form)))
    )
    components.offerVosDao.saveMigratedFromYdb(
      Seq(components.getOfferDao().findById(offerId, includeRemoved = true)(Traced.empty).value)
    )(Traced.empty)

    // легковые
    checkSuccessReadRequest(Get(s"/api/v1/offer/$category/$extDealerId/$offerId"))
    checkErrorRequest(Get(s"/api/v1/offer/$category/$randomUserRef/$offerId"), StatusCodes.NotFound, unknownOfferError)

    {
      val res = checkSuccessListingRequest(Get(s"/api/v1/offers/$category/$extDealerId?include_removed=1"))
      assert(res.getOffersList.asScala.exists(_.getId == offerId))
    }

    {
      val res = checkSuccessListingRequest(Get(s"/api/v1/offers/$category/$randomUserRef?include_removed=1"))
      assert(!res.getOffersList.asScala.exists(_.getId == offerId))
    }
  }

  test("check belonging of offers to users") {
    components.getOfferDao().saveMigrated(Seq(offer1, offer2, offer3), "test")

    def doBelong(req: HttpRequest, expected: Boolean): Unit = {
      val response = checkSuccessProtoFromJsonRequest[OfferBelongResponse](req)
      response.getBelong shouldBe expected
    }

    doBelong(
      Get("/api/v1/offers/dealer:10086/check-belong?offer_id=1042409964-038a&offer_id=1044159039-33be8"),
      expected = true
    )
    doBelong(
      Get("/api/v1/offers/user:10591660/check-belong?offer_id=1043045004-977b3"),
      expected = true
    )
    doBelong(
      Get("/api/v1/offers/user:10591660/check-belong?offer_id=1043045004-977b3&offer_id=1043045004-977b3"),
      expected = true
    )
    doBelong(
      Get("/api/v1/offers/dealer:10086/check-belong?offer_id=1042409964-038a&offer_id=1043045004-977b3"),
      expected = false
    )
    doBelong(
      Get("/api/v1/offers/user:10591660/check-belong?offer_id=1042409964-038a"),
      expected = false
    )
  }

  test("get hashed ids") {
    components.getOfferDao().saveMigrated(Seq(offer1, offer2, offer3), "test")
    components.offerVosDao.saveMigratedFromYdb(Seq(offer1, offer2, offer3))(Traced.empty)

    val request = Put("/api/v1/offers/all/dealer:10086/i-refs/hashed?include_removed=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, "[1042409964, 1043045004]"))
    val hashedIds = checkSuccessJsonRequest[Seq[OfferID]](request)
    hashedIds should contain only "1042409964-038a"
  }

  test("get offers vins") {
    components.getOfferDao().saveMigrated(Seq(offer1, offer2, offer3), "test")
    components.offerVosDao.saveMigratedFromYdb(Seq(offer1, offer2, offer3))(Traced.empty)

    val request = Put("/api/v1/offers/dealer:10086/vins?include_removed=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, """["1044214673-960f", "1044159039-33be8"]"""))
    val result = checkSuccessJsonValueRequest(request)
    (result \ 0 \ "offer_id").get.as[String] shouldBe "1044159039-33be8"
    (result \ 0 \ "vin").get.as[String] shouldBe "VF37L9HECCJ650918"
    (result \ 1).isEmpty shouldBe true
  }

  test("get empty offers vins") {
    val request = Put("/api/v1/offers/dealer:10086/vins?include_removed=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, """[]"""))
    val result = checkSuccessJsonValueRequest(request)
    (result \ 0).isEmpty shouldBe true
  }

  test("get offer ids by vins") {
    components.getOfferDao().saveMigrated(Seq(offer1, offer2, offer3), "test")
    components.offerVosDao.saveMigratedFromYdb(Seq(offer1, offer2, offer3))(Traced.empty)

    val request = Put("/api/v1/offers/dealer:10086/offer_ids/by_vins?include_removed=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, """["VF37L9HECCJ650918", "NOT_EXISTING_VIN"]"""))
    val result = checkSuccessJsonValueRequest(request)
    (result \ "VF37L9HECCJ650918").get.as[String] shouldBe "1044159039-33be8"
    (result \ "NOT_EXISTING_VIN").isEmpty shouldBe true
  }

  test("get empty offer ids by vins") {
    val request = Put("/api/v1/offers/dealer:10086/offer_ids/by_vins?include_removed=1")
      .withEntity(HttpEntity(ContentTypes.`application/json`, """[]"""))
    val result = checkSuccessJsonValueRequest(request)
    (result \ 0).isEmpty shouldBe true
  }

  test("get detailed offers by vins") {
    components.getOfferDao().saveMigrated(getOffersFromJson, "test")
    components.offerVosDao.saveMigratedFromYdb((getOffersFromJson))(Traced.empty)

    def checkDetailedOfferId(request: HttpRequest, expected: mutable.Map[Vin, OfferIdByVin]): Assertion = {
      val response = checkSuccessProtoFromJsonRequest[OfferIdsByVinsResponse](request)
      response.getResultMap.asScala.shouldBe(expected)
    }

    val offersIdByVins = OfferIdByVin
      .newBuilder()
      .setOfferId("1044159039-33be8")
      .setCategory(Category.CARS)
      .setSection(Section.USED)
      .build()

    checkDetailedOfferId(
      Put("/api/v1/offers/dealer:10086/offer-ids-by-vin?include_removed=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, """["VF37L9HECCJ650918", "NOT_EXISTING_VIN"]""")),
      mutable.Map(("VF37L9HECCJ650918" -> offersIdByVins))
    )
    checkDetailedOfferId(
      Put("/api/v1/offers/dealer:10086/offer-ids-by-vin?include_removed=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, """[]""")),
      mutable.Map.empty[Vin, OfferIdByVin]
    )
  }

  //scalastyle:off method.length
  private def readTests(): Unit = {
    // список объявлений пользователя

    // указана невалидная категория
    checkErrorRequest(Get("/api/v1/offers/blabla"), StatusCodes.NotFound, unknownHandler)

    // invalid user ref (blabla интерпретируем как userRef, он не в нужном формате)
    checkErrorRequest(Get("/api/v1/offers/cars/blabla"), StatusCodes.NotFound, unknownHandler)

    checkOffersList(
      "/api/v1/offers/cars/user:12345",
      offersLength = 0,
      page = 1,
      pageSize = 10,
      totalOffersCount = 0,
      totalPageCount = 0,
      includeRemoved = false,
      owner = false
    )

    checkOffersList(
      "/api/v1/offers/cars/dealer:10086",
      offersLength = 1,
      page = 1,
      pageSize = 10,
      totalOffersCount = 1,
      totalPageCount = 1,
      includeRemoved = false,
      owner = false
    )

    checkOffersList(
      "/api/v1/offers/cars/dealer:10086?include_removed=1",
      offersLength = 2,
      page = 1,
      pageSize = 10,
      totalOffersCount = 2,
      totalPageCount = 1,
      includeRemoved = true,
      owner = false
    )

    // указана неверная категория
    checkOffersList(
      "/api/v1/offers/moto/dealer:10086?include_removed=1",
      offersLength = 0,
      page = 1,
      pageSize = 10,
      totalOffersCount = 0,
      totalPageCount = 0,
      includeRemoved = true,
      owner = false
    )

    // указана невалидная категория
    checkErrorRequest(Get("/api/v1/offers/blabla/dealer:10086?include_removed=1"), StatusCodes.NotFound, unknownHandler)

    // универсальная категория all - в старой базе ищем в cars и траксах
    checkOffersList(
      "/api/v1/offers/all/dealer:10086?include_removed=1",
      offersLength = 2,
      page = 1,
      pageSize = 10,
      totalOffersCount = 2,
      totalPageCount = 1,
      includeRemoved = true,
      owner = false
    )

    // проверяем апи количества объявлений
    assert(checkSuccessStringRequest(Get("/api/v1/offers/all/dealer:10086/count?include_removed=1")).toInt == 2)

    // протестируем порядок сортировки в vos
    // пусть у нас три объявления принадлежат пользователю, одно активное, два удаленных
    val jdbc: JdbcTemplateWrapper = components.mySql.shard(0).master.jdbc
    jdbc.update("update t_offers_status set v_value = 0 where k_id='1042409964-038a'")
    jdbc.update("update t_offers set bucket = 'ac_10086' where k_id = '1043045004-977b3'")
    jdbc.update("update t_offers_status set v_value = 3 where k_id='1043045004-977b3'")
    checkOffersList(
      "/api/v1/offers/all/dealer:10086?include_removed=1",
      offersLength = 3,
      page = 1,
      pageSize = 10,
      totalOffersCount = 3,
      totalPageCount = 1,
      includeRemoved = true,
      owner = false,
      offersCheck = offers => {
        // объявления должны идти в такой последовательности: сначала активное, потом два удаленных,
        // удаленные отсортированы по дате создания по убыванию
        assert(offers.map(_.id) == List("1042409964-038a", "1043045004-977b3", "1044159039-33be8"))
      }
    )

    // вернем как было для дальнейших тестов
    jdbc.update("update t_offers_status set v_value = 2 where k_id='1042409964-038a'")
    jdbc.update("update t_offers set bucket = 'a_10591660' where k_id = '1043045004-977b3'")
    jdbc.update("update t_offers_status set v_value = 1 where k_id='1043045004-977b3'")

    checkOffersList(
      "/api/v1/offers/cars/dealer:10086?include_removed=1&page_size=1",
      offersLength = 1,
      page = 1,
      pageSize = 1,
      totalOffersCount = 2,
      totalPageCount = 2,
      includeRemoved = true,
      owner = false
    )

    checkOffersList(
      "/api/v1/offers/cars/dealer:10086?include_removed=1&page_size=1&page=2",
      offersLength = 1,
      page = 2,
      pageSize = 1,
      totalOffersCount = 2,
      totalPageCount = 2,
      includeRemoved = true,
      owner = false
    )

    checkOffersList(
      "/api/v1/offers/cars/dealer:10086?owner=1",
      offersLength = 1,
      page = 1,
      pageSize = 10,
      totalOffersCount = 1,
      totalPageCount = 1,
      includeRemoved = false,
      owner = true
    )

    // Другие критерии поиска

    // Три объявления у одного пользователя - одно со ставкой по аукциону, два без ставки.
    jdbc.update("update t_offers set bucket = 'a_10591660' where k_id = '1042409964-038a'")
    jdbc.update("update t_offers set bucket = 'a_10591660' where k_id = '1044159039-33be8'")

    checkOffersList(
      "/api/v1/offers/all/user:10591660?include_removed=1&has_calls_auction_bid=1",
      offersLength = 1,
      page = 1,
      pageSize = 10,
      totalOffersCount = 1,
      totalPageCount = 1,
      includeRemoved = true,
      owner = false,
      offersCheck = offers => {
        assert(offers.map(_.id).toSet == Set("1043045004-977b3"))
      }
    )

    checkOffersList(
      "/api/v1/offers/all/user:10591660?include_removed=1&has_calls_auction_bid=0",
      offersLength = 2,
      page = 1,
      pageSize = 10,
      totalOffersCount = 2,
      totalPageCount = 1,
      includeRemoved = true,
      owner = false,
      offersCheck = offers => {
        assert(offers.map(_.id).toSet == Set("1042409964-038a", "1044159039-33be8"))
      }
    )

    // Возвращаем как было.
    jdbc.update("update t_offers set bucket = 'ac_10086' where k_id = '1042409964-038a'")
    jdbc.update("update t_offers set bucket = 'ac_10086' where k_id = '1044159039-33be8'")

    // Три объявления у одного пользователя - все с разными ставками.
    jdbc.update("update t_offers set bucket = 'a_10591660' where k_id = '1044216699-0f1a0'")
    jdbc.update("update t_offers set bucket = 'a_10591660' where k_id = '1043211458-fbbd39'")

    //проверяем сортировку по calls_auction_bid-asc
    checkOffersList(
      "/api/v1/offers/all/user:10591660?include_removed=1&sort=calls_auction_bid-asc",
      offersLength = 3,
      page = 1,
      pageSize = 10,
      totalOffersCount = 3,
      totalPageCount = 1,
      includeRemoved = true,
      owner = false,
      offersCheck = offers => {
        assert(offers.map(_.id) == Seq("1044216699-0f1a0", "1043211458-fbbd39", "1043045004-977b3"))
      }
    )

    //проверяем сортировку по calls_auction_bid-desc
    checkOffersList(
      "/api/v1/offers/all/user:10591660?include_removed=1&sort=calls_auction_bid-desc",
      offersLength = 3,
      page = 1,
      pageSize = 10,
      totalOffersCount = 3,
      totalPageCount = 1,
      includeRemoved = true,
      owner = false,
      offersCheck = offers => {
        assert(offers.map(_.id) == Seq("1043045004-977b3", "1043211458-fbbd39", "1044216699-0f1a0"))
      }
    )

    // Возвращаем как было.
    jdbc.update("delete from t_offers where k_id = '1043211458-fbbd39'")
    jdbc.update("delete from t_offers where k_id = '1044216699-0f1a0'")

    // одно объявление пользователя

    // не передана категория
    checkErrorRequest(Get("/api/v1/offers/blabla/1043045004-977b3"), StatusCodes.NotFound, unknownHandler)

    // invalid user ref (blabla интерпретируем как userRef, он не в нужном формате)
    checkErrorRequest(Get("/api/v1/offers/cars/blabla/1043045004-977b3"), StatusCodes.NotFound, unknownHandler)

    // тест sameoffers
    // создадим три копии объявления 1042409964, с датой создания 1, 2 и 61 день назад
    val createdOffers = makeOfferCopiesInDb(offer1)
    val earliestIn60days = createdOffers.filter(_._2 > new DateTime().minusDays(60).getMillis).sortBy(_._2).head
    val earliest = createdOffers.sortBy(_._2).head
    val other = createdOffers.filter(x => x._1 != earliest._1 && x._1 != earliestIn60days._1).head

    sameOffersNonEmpty("1042409964-038a", earliestIn60days._1)

    // дубликат есть, потому что это объявление не самое раннее
    sameOffersNonEmpty(other._1, earliestIn60days._1)

    // самый ранний из дубликатов - дубликатов нет
    sameOffersEmptyJson(earliestIn60days._1)

    // раньше 30 дней - дубликатов нет
    sameOffersEmptyJson(earliest._1)

    // для этого дубликатов не делали, их быть не должно
    sameOffersEmptyJson("1044159039-33be8")

    // другая категория, дубликатов нет
    sameOffersEmptyJson("1042409964-038a", "moto")
  }

  private def sameOffersNonEmpty(offerId: String, earliestIn60days: String): Unit = {
    Get(s"/api/v1/offers/cars/dealer:10086/$offerId/sameoffer") ~> route ~> check {
      status shouldBe StatusCodes.OK
      contentType shouldBe ContentTypes.`application/json`
      val json = responseAs[String]
      val offer = parseAsFormData(json)
      assert(offer.getId == earliestIn60days)
    }
  }

  private def sameOffersEmptyJson(offerId: String, category: String = "cars"): Unit = {
    Get(s"/api/v1/offers/$category/dealer:10086/$offerId/sameoffer") ~> route ~> check {
      status shouldBe StatusCodes.NoContent
      // TODO тест, что response empty
    }
  }

  //scalastyle:off parameter.number
  private def checkOffersList(url: String,
                              offersLength: Int,
                              page: Int,
                              pageSize: Int,
                              totalOffersCount: Int,
                              totalPageCount: Int,
                              includeRemoved: Boolean,
                              owner: Boolean,
                              offersCheck: List[HaveId] => Unit = _ => ()): Unit = {
    Get(url) ~> route ~> check {
      val x = responseAs[String]
      //println(x)
      assert(status == StatusCodes.OK, "status is not OK")
      assert(contentType == ContentTypes.`application/json`, "ContentType is different")
      val offersList = Json.parse(x).as[OffersListData]
      assert(offersList.offers.length == offersLength, "offers length is wrong")
      assert(offersList.pagination.page == page, "page is wrong")
      assert(offersList.pagination.page_size == pageSize, "page size is wrong")
      assert(offersList.pagination.total_offers_count == totalOffersCount, "total offers count is wrong")
      assert(offersList.pagination.total_page_count == totalPageCount, "total page count is wrong")
      if (offersLength >= 1) {
        offersCheck(offersList.offers)
      }
    }
  }

  private def makeOfferCopiesInDb(offer: Offer): Seq[(String, Long)] = {
    // создадим две копии, созданные на день раньше и на два дня раньше, и одну, созданную на 61 день раньше
    val user = offer.getUser
    def newOffer(day: Int): Offer = {
      offer.toBuilder.setTimestampCreate(new DateTime(offer.getTimestampCreate).minusDays(day).getMillis).build()
    }
    val offer1 = components.getOfferDao().create(user, newOffer(1))(Traced.empty)
    val offer2 = components.getOfferDao().create(user, newOffer(2))(Traced.empty)
    val offer3 = components.getOfferDao().create(user, newOffer(61))(Traced.empty)
    components.offerVosDao.saveMigratedFromYdb(Seq(offer1, offer2, offer3))(Traced.empty)

    Seq(offer1, offer2, offer3).map(offer => (offer.getOfferID, offer.getTimestampCreate))
  }

  def parseAsFormData(json: String): ApiOfferModel.Offer = {
    Protobuf.fromJson[ApiOfferModel.Offer](json)
  }
}
