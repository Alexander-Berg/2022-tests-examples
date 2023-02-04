package ru.yandex.vos2.autoru.api.v1.offers

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import ru.auto.api.ApiOfferModel.{Category, Offer, OfferStatus, Section}
import ru.auto.api.CommonModel.{PaidService, RecallReason}
import ru.auto.api.MotoModel.MotoCategory
import ru.auto.api.ResponseModel.{OfferCountResponse, OfferListingResponse, OffersGroupedByBanReasonResponse}
import ru.auto.api.TrucksModel.TruckCategory
import ru.auto.api.{ApiOfferModel, MotoModel}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferIRef
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.model.{AutoruOfferID, AutoruSaleStatus}
import ru.yandex.vos2.autoru.utils.Vos2ApiHandlerResponses._
import ru.yandex.vos2.autoru.utils.Vos2ApiSuite
import ru.yandex.vos2.autoru.utils.testforms.{CarFormInfo, MotoFormInfo, TestFormParams, TruckFormInfo}
import ru.yandex.vos2.dao.offers.OfferUpdate
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.util.Protobuf

import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by andrey on 9/1/17.
  */
@RunWith(classOf[JUnitRunner])
class OffersHandlerListingTest extends AnyFunSuiteLike with Vos2ApiSuite with OptionValues with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    initDbs()
    // делаем дилера без объявлений
    components.oldSalesDatabase.master.jdbc.update("delete from all7.sales where new_client_id = ?", Long.box(dealerId))
  }

  implicit private val t: Traced = Traced.empty

  private val MOSCOW: Long = 213
  private val PROTVINO: Long = 20576
  private val SERPUKHOV: Long = 10754
  private val VIDNOE: Long = 10719
  private val KREMENKI: Long = 37145
  private val OBOLENSK: Long = 21629

  val BanReason1 = "wrong_year"
  val BanReason2 = "BanReason2"

  private val now = DateTime.now()

  private lazy val dealerId = 21029

  private val extDealerId = s"dealer:$dealerId"

  private val userRef = UserRef.refAutoruClient(dealerId)

  // создаем ему тестовый набор объявлений: по два объявления в комтрансе, мото и легковых
  private lazy val offer1 = {
    val formBuilder: Offer.Builder = testFormGenerator.truckTestForms
      .createForm(
        TestFormParams[TruckFormInfo](
          isDealer = true,
          optOwnerId = Some(dealerId),
          optGeobaseId = Some(MOSCOW),
          section = Section.NEW,
          optCreateDate = Some(now.minusDays(6)),
          optCard = components.trucksCatalog.getCardByMarkModel("HINO", "MELPHA")
        )
      )
      .form
      .toBuilder
    formBuilder.clearServices()
    formBuilder.getPriceInfoBuilder.setPrice(100000)
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/trucks/$extDealerId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    )
    val offerId: String = offer.getId
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/trucks/$extDealerId/$offerId/tags/1.1,1.2"))
    addServices(
      offer,
      AddServiceRequest("all_sale_add", active = true, expired = false, absentDays = false),
      AddServiceRequest("all_sale_color", active = true, expired = false, absentDays = false)
    )
    components.offersWriter.recall(
      offerId,
      Some(userRef),
      Some(Category.TRUCKS),
      RecallReason.SOLD_ON_AUTORU,
      manyCalls = false,
      DateTime.now(),
      None,
      "comment",
      None
    )
    migrateOffer(offerId)

    checkSuccessReadRequest(Get(s"/api/v1/offer/trucks/$extDealerId/$offerId"))
  }

  private lazy val offer2 = {
    val formBuilder: Offer.Builder = testFormGenerator.truckTestForms
      .createForm(
        TestFormParams[TruckFormInfo](
          isDealer = true,
          optOwnerId = Some(dealerId),
          optGeobaseId = Some(PROTVINO),
          section = Section.NEW,
          optCreateDate = Some(now.minusDays(5)),
          optCard = components.trucksCatalog.getCardByMarkModel("HANIA", "6X2")
        )
      )
      .form
      .toBuilder
    formBuilder.clearServices()
    formBuilder.getPriceInfoBuilder.setPrice(200000)
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/trucks/$extDealerId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    )
    addServices(
      offer,
      AddServiceRequest("all_sale_fresh", active = true, expired = false, absentDays = false),
      AddServiceRequest("all_sale_special", active = true, expired = false, absentDays = false)
    )

    checkSimpleSuccessRequest(Put(s"/api/v1/offer/trucks/$extDealerId/${offer.getId}/tags/2.1,2.2"))
    migrateOffer(offer.getId)
    checkSuccessReadRequest(Get(s"/api/v1/offer/trucks/$extDealerId/${offer.getId}"))
  }

  private lazy val offer3 = {
    val formBuilder: Offer.Builder = testFormGenerator.motoTestForms
      .createForm(
        TestFormParams[MotoFormInfo](
          isDealer = true,
          optOwnerId = Some(dealerId),
          optGeobaseId = Some(SERPUKHOV),
          section = Section.USED,
          optCreateDate = Some(now.minusDays(4)),
          optCard = components.motoCatalog.getCardByMarkModel("BMW", "F_650_GS")
        )
      )
      .form
      .toBuilder
    formBuilder.clearServices()
    formBuilder.getPriceInfoBuilder.setPrice(300000)
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/moto/$extDealerId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    )
    addServices(
      offer,
      AddServiceRequest("all_sale_top", active = true, expired = false, absentDays = false),
      AddServiceRequest("all_sale_premium", active = true, expired = false, absentDays = false)
    )
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/moto/$extDealerId/${offer.getId}/tags/3.1,3.2,x"))
    banOffer(offer.getId)
    migrateOffer(offer.getId)

    checkSuccessReadRequest(Get(s"/api/v1/offer/moto/$extDealerId/${offer.getId}"))
  }

  private lazy val offer4 = {
    val formBuilder: Offer.Builder = testFormGenerator.motoTestForms
      .createForm(
        TestFormParams[MotoFormInfo](
          isDealer = true,
          optOwnerId = Some(dealerId),
          optGeobaseId = Some(VIDNOE),
          section = Section.USED,
          optCreateDate = Some(now.minusDays(3)),
          optCard = components.motoCatalog.getCardByMarkModel("VESPA", "LX")
        )
      )
      .form
      .toBuilder
    formBuilder.clearServices()
    formBuilder.getPriceInfoBuilder.setPrice(400000)
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/moto/$extDealerId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    )
    addServices(
      offer,
      AddServiceRequest("all_sale_add", active = true, expired = false, absentDays = false),
      AddServiceRequest("all_sale_color", active = true, expired = false, absentDays = false)
    )
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/moto/$extDealerId/${offer.getId}/tags/4.1,4.2,x"))
    components.offersWriter.setArchive(offer.getId, Some(userRef), Some(Category.MOTO), archive = true, "comment", None)
    migrateOffer(offer.getId)
    checkSuccessReadRequest(Get(s"/api/v1/offer/moto/$extDealerId/${offer.getId}?include_removed=1"))
  }

  private lazy val offer5 = {
    val formBuilder: Offer.Builder = testFormGenerator.carTestForms
      .createForm(
        TestFormParams[CarFormInfo](
          isDealer = true,
          optOwnerId = Some(dealerId),
          optGeobaseId = Some(KREMENKI),
          section = Section.USED,
          optCreateDate = Some(now.minusDays(2)),
          optCard = components.carsCatalog.getCardByMarkModel("AUDI", "Q5")
        )
      )
      .form
      .toBuilder
    formBuilder.clearServices()
    formBuilder.getPriceInfoBuilder.setPrice(500000)
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/cars/$extDealerId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    )
    addServices(
      offer,
      AddServiceRequest("all_sale_fresh", active = true, expired = false, absentDays = false),
      AddServiceRequest("all_sale_special", active = true, expired = false, absentDays = false)
    )

    checkSimpleSuccessRequest(Put(s"/api/v1/offer/cars/$extDealerId/${offer.getId}/tags/5.1,5.2"))
    migrateOffer(offer.getId)

    checkSuccessReadRequest(Get(s"/api/v1/offer/cars/$extDealerId/${offer.getId}?include_removed=1"))
  }

  private lazy val offer6 = {
    val formBuilder: Offer.Builder = testFormGenerator.carTestForms
      .createForm(
        TestFormParams[CarFormInfo](
          isDealer = true,
          optOwnerId = Some(dealerId),
          optGeobaseId = Some(OBOLENSK),
          section = Section.USED,
          optCreateDate = Some(now.minusDays(1)),
          optCard = components.carsCatalog.getCardByMarkModel("BMW", "3ER")
        )
      )
      .form
      .toBuilder
    formBuilder.clearServices()
    formBuilder.getPriceInfoBuilder.setPrice(600000)
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/cars/$extDealerId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    )
    val autoruOfferId: AutoruOfferID = AutoruOfferID.parse(offer.getId)
    components.autoruSalesDao.setStatus(autoruOfferId.id, Seq.empty, AutoruSaleStatus.STATUS_EXPIRED)
    components.getOfferDao().useOfferID(autoruOfferId) { offer =>
      OfferUpdate.visitNow(offer.toBuilder.clearFlag().addFlag(OfferFlag.OF_EXPIRED).build())
    }

    checkSimpleSuccessRequest(Put(s"/api/v1/offer/cars/$extDealerId/${offer.getId}/tags/6.1,6.2"))
    migrateOffer(offer.getId)
    checkSuccessReadRequest(Get(s"/api/v1/offer/cars/$extDealerId/${offer.getId}?include_removed=1"))
  }

  private lazy val offer7 = {
    val formBuilder: Offer.Builder = testFormGenerator.truckTestForms
      .createForm(
        TestFormParams[TruckFormInfo](
          isDealer = true,
          optOwnerId = Some(dealerId),
          optGeobaseId = Some(MOSCOW),
          section = Section.NEW,
          optCreateDate = Some(now.minusDays(6)),
          optCard = components.trucksCatalog.getCardByMarkModel("MERCEDES", "817")
        )
      )
      .form
      .toBuilder
    formBuilder.clearServices()
    formBuilder.getPriceInfoBuilder.setPrice(700000)
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/trucks/$extDealerId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    )
    components.getOfferDao().useOfferID(AutoruOfferID.parse(offer.getId)) { offer =>
      OfferUpdate.visitNow(
        offer.toBuilder
          .addReasonsBan(BanReason1)
          .build()
      )
    }

    checkSimpleSuccessRequest(Put(s"/api/v1/offer/trucks/$extDealerId/${offer.getId}/tags/7.1,7.2"))
    migrateOffer(offer.getId)

    checkSuccessReadRequest(Get(s"/api/v1/offer/trucks/$extDealerId/${offer.getId}"))
  }

  private lazy val offer8 = {
    val formBuilder: Offer.Builder = testFormGenerator.motoTestForms
      .createForm(
        TestFormParams[MotoFormInfo](
          isDealer = true,
          optOwnerId = Some(dealerId),
          optGeobaseId = Some(VIDNOE),
          section = Section.USED,
          optCreateDate = Some(now.minusDays(3)),
          optCard = components.motoCatalog.getCardByMarkModel("TRIUMPH", "DAYTONA_600")
        )
      )
      .form
      .toBuilder
    formBuilder.clearServices()
    formBuilder.getPriceInfoBuilder.setPrice(800000)
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/moto/$extDealerId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    )
    components.getOfferDao().useOfferID(AutoruOfferID.parse(offer.getId)) { offer =>
      OfferUpdate.visitNow(
        offer.toBuilder
          .addReasonsBan(BanReason1)
          .addReasonsBan(BanReason2)
          .build()
      )
    }

    checkSimpleSuccessRequest(Put(s"/api/v1/offer/moto/$extDealerId/${offer.getId}/tags/8.1,8.2"))
    migrateOffer(offer.getId)

    checkSuccessReadRequest(Get(s"/api/v1/offer/moto/$extDealerId/${offer.getId}?include_removed=1"))
  }

  private lazy val offer9 = {
    val formBuilder: Offer.Builder = testFormGenerator.carTestForms
      .createForm(
        TestFormParams[CarFormInfo](
          isDealer = true,
          optOwnerId = Some(dealerId),
          optGeobaseId = Some(SERPUKHOV),
          section = Section.USED,
          optCreateDate = Some(now.minusDays(1)),
          optCard = components.carsCatalog.getCardByMarkModel("BMW", "3ER")
        )
      )
      .form
      .toBuilder
    formBuilder.clearServices()
    formBuilder.getPriceInfoBuilder.setPrice(600000)
    val offer = checkSuccessRequestWithOffer(
      Post(s"/api/v1/offers/cars/$extDealerId?insert_new=1")
        .withEntity(HttpEntity(ContentTypes.`application/json`, Protobuf.toJson(formBuilder.build())))
    )
    val autoruOfferId: AutoruOfferID = AutoruOfferID.parse(offer.getId)
    components.autoruSalesDao.setStatus(autoruOfferId.id, Seq.empty, AutoruSaleStatus.STATUS_EXPIRED)
    components.getOfferDao().useOfferID(autoruOfferId) { offer =>
      OfferUpdate.visitNow(offer.toBuilder.clearFlag().addFlag(OfferFlag.OF_EXPIRED).build())
    }
    checkSimpleSuccessRequest(Put(s"/api/v1/offer/cars/$extDealerId/${offer.getId}/tags/9.1,9.2"))
    migrateOffer(offer.getId)
    checkSuccessReadRequest(Get(s"/api/v1/offer/cars/$extDealerId/${offer.getId}?include_removed=1"))
  }

  private def migrateOffer(offerId: String): Unit = {
    val offerFromDb = components.getOfferDao().findById(offerId, includeRemoved = true)(Traced.empty).value
    components.offerVosDao.saveMigratedFromYdb(Seq(offerFromDb))(Traced.empty)
  }

  test("test data correct") {
    assert(offer1.getTruckInfo.getTruckCategory == TruckCategory.BUS)
    assert(offer1.getTruckInfo.getMark == "HINO")
    assert(offer1.getTruckInfo.getModel == "MELPHA")
    assert(offer1.getStatus == OfferStatus.INACTIVE)

    assert(offer2.getTruckInfo.getTruckCategory == TruckCategory.TRUCK)
    assert(offer2.getTruckInfo.getMark == "HANIA")
    assert(offer2.getTruckInfo.getModel == "6X2")
    assert(offer2.getStatus == OfferStatus.NEED_ACTIVATION)

    assert(offer3.getMotoInfo.getMotoCategory == MotoModel.MotoCategory.MOTORCYCLE)
    assert(offer3.getMotoInfo.getMark == "BMW")
    assert(offer3.getMotoInfo.getModel == "F_650_GS")
    assert(offer3.getStatus == OfferStatus.BANNED)

    assert(offer4.getMotoInfo.getMotoCategory == MotoModel.MotoCategory.SCOOTERS)
    assert(offer4.getMotoInfo.getMark == "VESPA")
    assert(offer4.getMotoInfo.getModel == "LX")
    assert(offer4.getStatus == OfferStatus.REMOVED)

    assert(offer5.getCarInfo.getMark == "AUDI")
    assert(offer5.getCarInfo.getModel == "Q5")
    assert(offer5.getStatus == OfferStatus.NEED_ACTIVATION)

    assert(offer6.getCarInfo.getMark == "BMW")
    assert(offer6.getCarInfo.getModel == "3ER")
    assert(offer6.getStatus == OfferStatus.EXPIRED)

    assert(offer7.getTruckInfo.getTruckCategory == TruckCategory.TRUCK)
    assert(offer7.getTruckInfo.getMark == "MERCEDES")
    assert(offer7.getTruckInfo.getModel == "817")
    assert(offer7.getStatus == OfferStatus.NEED_ACTIVATION)

    assert(offer8.getMotoInfo.getMotoCategory == MotoModel.MotoCategory.MOTORCYCLE)
    assert(offer8.getMotoInfo.getMark == "TRIUMPH")
    assert(offer8.getMotoInfo.getModel == "DAYTONA_600")

    assert(offer9.getCarInfo.getMark == "BMW")
    assert(offer9.getCarInfo.getModel == "3ER")
    assert(offer9.getStatus == OfferStatus.EXPIRED)
  }

  //scalastyle:off line.size.limit

  test("default sorting") {
    val allOffersRequest: String = "tag=1.1&tag=2.1&tag=3.1&tag=4.1&tag=5.1&tag=6.1&include_removed=1"

    val result = checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?$allOffersRequest"))
    checkOffers(result, offer5, offer2, offer1, offer6, offer4, offer3)
  }

  test("bad sorting request") {
    checkErrorRequest(
      Get(s"/api/v1/offers/all/$extDealerId?sort=testtest"),
      StatusCodes.BadRequest,
      illegalArgumentError("Unknown sorting param: testtest")
    )
  }

  for {
    pageSize <- 7 to 1 by -1
    availableOffersCount = 6
    availablePageCount = math.ceil(1.0 * availableOffersCount / pageSize).toInt
    page <- 1 to availablePageCount + 1
  } test(s"paging (pageSize = $pageSize, page = $page)") {
    val allOffersRequest: String = "tag=1.1&tag=2.1&tag=3.1&tag=4.1&tag=5.1&tag=6.1&include_removed=1"
    val result = checkSuccessListingRequest(
      Get(s"/api/v1/offers/all/$extDealerId?$allOffersRequest&page=$page&page_size=$pageSize")
    )
    val offersCount: Int =
      if (page > availablePageCount) 0
      else if (page == availablePageCount) availableOffersCount - (pageSize * (page - 1))
      else pageSize

    assert(result.getOffersCount == offersCount)
    assert(result.getPagination.getPage == page)
    assert(result.getPagination.getPageSize == pageSize)
    assert(result.getPagination.getTotalOffersCount == availableOffersCount)
    assert(result.getPagination.getTotalPageCount == availablePageCount)
  }

  test("tags") {
    // возвращаем офферы, где есть хотя бы один тег
    val result = checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?tag=1.1&tag=2.1&tag=3.1"))
    assert(result.getOffersCount == 3)
    assert(result.getOffers(0).getId == offer2.getId)
    assert(result.getOffers(1).getId == offer1.getId)
    assert(result.getOffers(2).getId == offer3.getId)
  }

  test("tags exclude") {
    // возвращаем офферы, в которых нет ни одного указанного тега
    val result1 = checkSuccessListingRequest(
      Get(s"/api/v1/offers/all/$extDealerId?exclude_tag=4.1&exclude_tag=5.1&exclude_tag=6.1&include_removed=1")
    )
    checkOffers(result1, offer7, offer8, offer2, offer1, offer3, offer9)
    // вариант, когда передаем и просто tags
    val result2 = checkSuccessListingRequest(
      Get(s"/api/v1/offers/all/$extDealerId?exclude_tag=4.1&exclude_tag=5.1&exclude_tag=6.1&tag=x&include_removed=1")
    )
    assert(result2.getOffersCount == 1)
    assert(result2.getOffers(0).getId == offer3.getId)
  }

  test("duplication") {
    // возвращаем оффер один раз, даже если он попадает под несколько условий
    val result1 = checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?tag=1.1&tag=1.2"))
    assert(result1.getOffersCount == 1)
    assert(result1.getOffers(0).getId == offer1.getId)

    val result2 = checkSuccessListingRequest(
      Get(s"/api/v1/offers/all/$extDealerId?service=all_sale_add&service=all_sale_color&include_removed=1")
    )
    assert(result2.getOffersCount == 2)
    assert(result2.getOffers(0).getId == offer1.getId)
    assert(result2.getOffers(1).getId == offer4.getId)
  }

  test("groupedByBanReason") {
    Get(s"/api/v1/offers/all/$extDealerId/grouped-by-ban-reason?owner=0&include_removed=1&fromvos=1") ~> route ~> check {
      val response = responseAs[String]
      val resp = Protobuf.fromJson[OffersGroupedByBanReasonResponse](response)
      withClue(resp) {
        status shouldBe StatusCodes.OK
        resp.getEntriesCount shouldBe 2
        val respEntries = resp.getEntriesList.asScala
        respEntries.exists(entry => entry.getCount == 2 && entry.getBanReason == BanReason1) shouldBe true
        respEntries.exists(entry => entry.getCount == 1 && entry.getBanReason == BanReason2.toLowerCase) shouldBe true
      }
    }
  }

  test("services") {
    // фильтр service ищет только активные
    {
      val result = checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?service=all_sale_special"))
      checkOffers(result, offer5, offer2)
    }

    {
      addServices(offer5, AddServiceRequest("all_sale_special", active = false, expired = false, absentDays = false))
      migrateOffer(offer5.getId)

      val result = checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?service=all_sale_special"))
      checkOffers(result, offer2)
    }

    {
      addServices(offer2, AddServiceRequest("all_sale_special", active = true, expired = true, absentDays = false))
      migrateOffer(offer2.getId)

      val result = checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?service=all_sale_special"))
      checkOffers(result, offer2) // активность определяется только флагом is_active = true
      addServices(offer2, AddServiceRequest("all_sale_special", active = false, expired = true, absentDays = false))
      migrateOffer(offer2.getId)

      val result2 = checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?service=all_sale_special"))
      checkOffers(result2)
    }

    // восстанавливаем как было
    addServices(offer5, AddServiceRequest("all_sale_special", active = true, expired = false, absentDays = false))
    addServices(offer2, AddServiceRequest("all_sale_special", active = true, expired = false, absentDays = false))
    migrateOffer(offer2.getId)
    migrateOffer(offer5.getId)

  }

  test("mark_models filter") {
    // можно передавать несколько марок-моделей
    {
      val result = checkSuccessListingRequest(
        Get(
          s"/api/v1/offers/all/$extDealerId?include_removed=1&mark_model=AUDI&mark_model=BMW&mark_model=VESPA%23LX&mark_model=HINO%23MELPHA"
        )
      )
      checkOffers(result, offer5, offer1, offer6, offer4, offer3, offer9)
    }

    {
      val result = checkSuccessListingRequest(
        Get(s"/api/v1/offers/all/$extDealerId?include_removed=1&mark_model=AUDI&mark_model=BMW&mark_model=VESPA%23LX")
      )
      checkOffers(result, offer5, offer6, offer4, offer3, offer9)
      assert(result.getFilters.getMarkModelCount == 3)
      val markModelFilters: mutable.Buffer[String] = result.getFilters.getMarkModelList.asScala
      assert(markModelFilters.contains("AUDI"))
      assert(markModelFilters.contains("BMW"))
      assert(markModelFilters.contains("VESPA#LX"))
    }
  }

  test("mark-models api") {
    val result1 = checkSuccessMarkModelsRequest(
      Get(
        s"/api/v1/offers/all/$extDealerId/markmodels?include_removed=1&mark_model=AUDI&mark_model=BMW&mark_model=VESPA%23LX&mark_model=HINO%23MELPHA"
      )
    )
    assert(result1.getMarkModelsCount == 4)
    val markModels = result1.getMarkModelsList.asScala

    val bmw = markModels.find(p => p.getMark == "BMW")
    assert(bmw.nonEmpty)
    assert(bmw.get.getHumanName == "BMW")
    assert(bmw.get.getModelsCount == 2)

    assert(
      bmw.get.getModelsList.asScala
        .exists(mm => {
          mm.getModel == "3ER" && mm.getCategory == ApiOfferModel.Category.CARS && mm.getOffersCount == 2
        })
    )
    assert(
      bmw.get.getModelsList.asScala
        .exists(mm => {
          mm.getModel == "F_650_GS" && mm.getCategory == ApiOfferModel.Category.MOTO
        })
    )

    val vespa = markModels.find(p => p.getMark == "VESPA")
    assert(vespa.nonEmpty)
    assert(vespa.get.getModelsCount == 1)
    val hino = markModels.find(p => p.getMark == "HINO")
    assert(hino.nonEmpty)
    assert(hino.get.getModelsCount == 1)
    val audi = markModels.find(p => p.getMark == "AUDI")
    assert(audi.nonEmpty)
    assert(audi.get.getModelsCount == 1)

    // BMW конкретная модель
    val result2 = checkSuccessMarkModelsRequest(
      Get(s"/api/v1/offers/all/$extDealerId/markmodels?include_removed=1&mark_model=BMW%233ER")
    )
    assert(result2.getMarkModelsCount == 1)
    assert(result2.getMarkModels(0).getMark == "BMW")
    assert(result2.getMarkModels(0).getModelsCount == 1)
    assert(result2.getMarkModels(0).getModels(0).getModel == "3ER")
    assert(result2.getMarkModels(0).getModels(0).getCategory == ApiOfferModel.Category.CARS)
    // исключения тегов
    val result3 = checkSuccessMarkModelsRequest(
      Get(
        s"/api/v1/offers/all/$extDealerId/markmodels?exclude_tag=4.1&exclude_tag=5.1&exclude_tag=6.1&tag=x&include_removed=1"
      )
    )
    assert(result3.getMarkModelsCount == 1)
    assert(result3.getMarkModels(0).getMark == "BMW")
    assert(result3.getMarkModels(0).getModelsCount == 1)
    assert(result3.getMarkModels(0).getModels(0).getModel == "F_650_GS")
    assert(result3.getMarkModels(0).getModels(0).getCategory == ApiOfferModel.Category.MOTO)
  }

  test("offers count as protobuf") {
    val allOffersRequest: String = "tag=1.1&tag=2.1&tag=3.1&tag=4.1&tag=5.1&tag=6.1&include_removed=1"
    val req: HttpRequest = Get(s"/api/v1/offers/all/$extDealerId/count?$allOffersRequest")
    val result0 = checkSuccessStringRequest(req)
    assert(result0 == "6")
    val result1 =
      checkSuccessStringRequest(req.withHeaders(Accept(MediaRange.One(MediaTypes.`application/json`, 1.0f))))
    assert(result1 == "6")
    val result2 = checkSuccessProtobufRequest[OfferCountResponse](
      req.withHeaders(Accept(MediaRange.One(ru.yandex.vertis.util.akka.http.protobuf.Protobuf.mediaType, 1.0f)))
    )
    assert(result2.getCount == 6)
  }

  test("noActiveServices") {
    val result1 =
      checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?no_active_services=1&include_removed=1"))
    assert(result1.getFilters.getNoActiveServices)
    assert(result1.getOffersCount == 4)
    checkOffers(result1, offer8, offer7, offer6, offer9)
    addServices(offer1, AddServiceRequest("all_sale_color", active = false, expired = false, absentDays = false))
    migrateOffer(offer1.getId)

    val result2 =
      checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?no_active_services=1&include_removed=1"))
    checkOffers(result2, offer8, offer7, offer1, offer6, offer9)
    addServices(
      offer2,
      AddServiceRequest("all_sale_fresh", active = false, expired = false, absentDays = false),
      AddServiceRequest("all_sale_special", active = false, expired = true, absentDays = false)
    )
    migrateOffer(offer2.getId)

    val result3 =
      checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?no_active_services=1&include_removed=1"))
    checkOffers(result3, offer8, offer7, offer2, offer1, offer6, offer9)
    addServices(offer2, AddServiceRequest("all_sale_activate", active = true, expired = true, absentDays = false))
    migrateOffer(offer2.getId)

    val result4: OfferListingResponse =
      checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?no_active_services=1&include_removed=1"))
    checkOffers(result4, offer8, offer7, offer2, offer1, offer6, offer9)
    val result5 =
      checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?no_active_services=0&include_removed=1"))
    assert(result5.getOffersCount == 9)
    // восстанавливаем как было
    addServices(offer1, AddServiceRequest("all_sale_color", active = true, expired = false, absentDays = false))
    addServices(
      offer2,
      AddServiceRequest("all_sale_fresh", active = true, expired = false, absentDays = false),
      AddServiceRequest("all_sale_special", active = true, expired = false, absentDays = false),
      AddServiceRequest("all_sale_activate", active = false, expired = true, absentDays = false)
    )
    migrateOffer(offer2.getId)
    migrateOffer(offer1.getId)

  }

  test("indexInfo") {
    val result1 = checkSuccessMarkModelsRequest(
      Get(
        s"/api/v1/offers/all/$extDealerId/index-info?index=mark_models&include_removed=1&mark_model=AUDI&mark_model=BMW&mark_model=VESPA%23LX&mark_model=HINO%23MELPHA"
      )
    )
    assert(result1.getMarkModelsCount == 4)
    val markModels = result1.getMarkModelsList.asScala
    val bmw = markModels.find(_.getMark == "BMW")
    assert(bmw.nonEmpty)
    assert(bmw.get.getHumanName == "BMW")
    assert(bmw.get.getModelsCount == 2)

    assert(
      bmw.get.getModelsList.asScala
        .exists(mm => {
          mm.getModel == "3ER"
        })
    )
    assert(
      bmw.get.getModelsList.asScala
        .exists(mm => {
          mm.getModel == "F_650_GS"
        })
    )

    val vespa = markModels.find(p => p.getMark == "VESPA")
    assert(vespa.nonEmpty)
    assert(vespa.get.getModelsCount == 1)
    val hino = markModels.find(p => p.getMark == "HINO")
    assert(hino.nonEmpty)
    assert(hino.get.getModelsCount == 1)
    val audi = markModels.find(p => p.getMark == "AUDI")
    assert(audi.nonEmpty)
    assert(audi.get.getModelsCount == 1)

    val allOffersRequest: String = "tag=1.1&tag=2.1&tag=3.1&tag=4.1&tag=5.1&tag=6.1&tag=7.1&tag=8.1&include_removed=1"
    val result2 = checkSuccessTruckCategoriesRequest(
      Get(s"/api/v1/offers/all/$extDealerId/index-info?index=truck_categories&$allOffersRequest")
    )
    assert(result2.getTruckCategoriesCount == 2)
    val trucksCategories = result2.getTruckCategoriesList.asScala
    val truck = trucksCategories.find(_.getTruckCategory == TruckCategory.TRUCK)
    assert(truck.nonEmpty)
    assert(truck.get.getOffersCount == 2)
    val bus = trucksCategories.find(_.getTruckCategory == TruckCategory.BUS)
    assert(bus.nonEmpty)
    assert(bus.get.getOffersCount == 1)

    val result3 = checkSuccessMotoCategoriesRequest(
      Get(s"/api/v1/offers/all/$extDealerId/index-info?index=moto_categories&$allOffersRequest")
    )
    assert(result3.getMotoCategoriesCount == 2)
    val motoCategories = result3.getMotoCategoriesList.asScala
    val scooters = motoCategories.find(_.getMotoCategory == MotoCategory.SCOOTERS)
    assert(scooters.nonEmpty)
    assert(scooters.get.getOffersCount == 1)
    val motorcycle = motoCategories.find(_.getMotoCategory == MotoCategory.MOTORCYCLE)
    assert(motorcycle.nonEmpty)
    assert(motorcycle.get.getOffersCount == 2)

    checkErrorRequest(
      Get(s"/api/v1/offers/all/$extDealerId/index-info?index=pewpew&$allOffersRequest"),
      StatusCodes.BadRequest,
      illegalArgumentError("unexpected index param pewpew")
    )
    checkErrorRequest(
      Get(s"/api/v1/offers/all/$extDealerId/index-info?$allOffersRequest"),
      StatusCodes.BadRequest,
      illegalArgumentError("Missed required query parameter index")
    )
  }

  //todo флапает при выкладке, локально не воспроизводится. нужно разабраться
  ignore(s"simple listing (invos = false)") {
    val result = checkSuccessListingRequest(Get(s"/api/v1/offers/all/$extDealerId?include_removed=1"))
    assert(result.getOffersCount == 9)
  }

  test(s"find offers by offerIRef") {
    def getIRef(offer: Offer): OfferIRef = AutoruOfferID.parse(offer.getId).id
    val offer8IRef = getIRef(offer8)
    val offer7IRef = getIRef(offer7)
    val result = checkSuccessListingRequest(
      Get(s"/api/v1/offers/all/$extDealerId?offer_i_ref=$offer8IRef&offer_i_ref=$offer7IRef")
    )
    checkOffers(result, offer8, offer7)
  }

  /*private def checkOffer(actualOffer: ApiOfferModel.Offer, wantedOffer: ApiOfferModel.Offer): Unit = {
    assert(actualOffer.getId == wantedOffer.getId, s"wrong offer here: wanted: ${offerNumById(wantedOffer.getId)}, " +
      s"actual: ${offerNumById(actualOffer.getId)}")
  }*/

  private def checkOffers(result: OfferListingResponse, wantedOffers: ApiOfferModel.Offer*): Unit = {
    val resultOfferNums = result.getOffersList.asScala.toSeq.map(_.getId).map(offerNumById)
    val wantedOfferNums = wantedOffers.map(_.getId).map(offerNumById)
    val notInWanted: Seq[String] = resultOfferNums.diff(wantedOfferNums)
    val notInResult: Seq[String] = wantedOfferNums.diff(resultOfferNums)
    val message = new ArrayBuffer[String]
    if (notInWanted.nonEmpty) message += s"unexpected offers found: ${notInWanted.mkString(", ")}"
    if (notInResult.nonEmpty) message += s"wanted offers not found: ${notInResult.mkString(", ")}"
    assert(notInWanted.isEmpty && notInResult.isEmpty, message.mkString("\n"))
  }

  private def addServices(offer: ApiOfferModel.Offer, addServiceRequest: AddServiceRequest*): Unit = {
    val services: mutable.Buffer[PaidService] = createMultipleAddServicesRequests(addServiceRequest: _*).getServicesList.asScala
    components.offersWriter.addServices(
      offer.getId,
      Some(userRef),
      Some(offer.getCategory),
      None,
      services.toSeq,
      allowDealers = true
    )(t)
  }

  //scalastyle:off cyclomatic.complexity

  private def offerNumById(id: String): String = {
    id match {
      case x if x == offer1.getId => "offer1"
      case x if x == offer2.getId => "offer2"
      case x if x == offer3.getId => "offer3"
      case x if x == offer4.getId => "offer4"
      case x if x == offer5.getId => "offer5"
      case x if x == offer6.getId => "offer6"
      case x if x == offer7.getId => "offer7"
      case x if x == offer8.getId => "offer8"
      case x if x == offer9.getId => "offer9"
      case _ => "N/A"
    }
  }
}
