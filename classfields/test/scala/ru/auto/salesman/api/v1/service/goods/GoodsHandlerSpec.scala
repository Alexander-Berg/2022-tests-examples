package ru.auto.salesman.api.v1.service.goods

import akka.actor.ActorRefFactory
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes._
import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.api.akkahttp.ApiException.BadRequestException
import ru.auto.salesman.api.v1.service.goods.views.GoodsRecordView
import ru.auto.salesman.model.GoodStatuses.Active
import ru.auto.salesman.model.ProductId.{Add, Fresh, Placement}
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.payment_model.PaymentModel
import ru.auto.salesman.model.{
  CityId,
  Client,
  ClientId,
  ClientStatuses,
  Epoch,
  GoodStatus,
  OfferCategories,
  OfferCategory,
  OfferId,
  ProductId,
  RegionId
}
import ru.auto.salesman.service.GoodsDecider.Action.Deactivate
import ru.auto.salesman.service.GoodsDecider.DeactivateReason.NotEnoughFunds
import ru.auto.salesman.service.GoodsDecider.NoActionReason.HoldError
import ru.auto.salesman.service.goods.DealerGoodsService
import ru.auto.salesman.service.goods.domain._
import ru.auto.salesman.service.placement.validation.DealerGoodsPreparingService
import ru.auto.salesman.service.placement.validation.domain.{
  ActivationValidationResult,
  Allowed,
  TemporaryError
}
import ru.auto.salesman.test.dao.gens._
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import spray.json._

class GoodsHandlerSpec extends RoutingSpec {

  private val since = "2017-09-27T12:34:56.789%2B03:00"

  private def epoch(s: String) = DateTime.parse(s.replace("%2B", "+")).getMillis

  private val goods = Gen.listOf(GoodRecordGen).next

  private val products = Seq(Add, Placement)
  private val goodStatus = Some(Active)

  private val client =
    Client(
      clientId = 23965,
      agencyId = None,
      categorizedClientId = None,
      companyId = None,
      RegionId(1000L),
      CityId(2L),
      ClientStatuses.Active,
      singlePayment = Set(),
      firstModerated = false,
      paidCallsAvailable = false,
      priorityPlacement = true
    )

  val paymentModelFactory = TestPaymentModelFactory.withoutSingleWithCalls()

  private val paymentModel = paymentModelFactory
    .paymentModel(
      Fresh,
      Category.CARS,
      Section.NEW,
      client
    )
    .success
    .value

  private val goodsService = mock[DealerGoodsService]
  private val goodsPreparingService = mock[DealerGoodsPreparingService]
  private val route = new GoodsHandler(goodsService, goodsPreparingService).route

  "GET /recent" should {
    "not operate without operator" in {
      Get(s"/recent?since=$since") ~> seal(route) ~> check {
        status shouldBe BadRequest
      }
    }

    "return bad request due to bad since" in {
      val uri = s"/recent?since=wrong"
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe BadRequest
      }
    }

    "return bad request due to wrong excluded product" in {
      val uri =
        s"/recent?since=$since&excludeProduct=$Placement&excludeProduct=wrong"
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe BadRequest
      }
    }

    "return bad request due to wrong status" in {
      val uri = s"/recent?since=$since&status=wrong"
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe BadRequest
      }
    }

    "return goods" in {
      val uri =
        s"/recent?since=$since&excludeProduct=$Placement&excludeProduct=$Add&status=active"
      (goodsService
        .getRecent(
          _: Epoch,
          _: Seq[ProductId],
          _: Option[GoodStatus]
        ))
        .expects(epoch(since), products, goodStatus)
        .returningZ(goods)
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        val response = responseAs[Seq[GoodsRecordView]]
        response should contain theSameElementsAs goods.map(
          GoodsRecordView.asView
        )
      }
    }

    "return cars good" in {
      val uri =
        s"/recent?since=$since&excludeProduct=$Placement&excludeProduct=$Add&status=active"
      val good = GoodRecordGen.next.copy(category = OfferCategories.Cars)
      (goodsService
        .getRecent(
          _: Epoch,
          _: Seq[ProductId],
          _: Option[GoodStatus]
        ))
        .expects(
          epoch(since),
          products,
          goodStatus
        )
        .returningZ(List(good))
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        val Vector(goodJson) =
          responseAs[JsArray].elements.map(_.asJsObject.fields)
        goodJson("offer").convertTo[Long] shouldBe good.offerId
        goodJson("category").convertTo[String] shouldBe "cars"
      }
    }

    "return commercial good" in {
      val uri =
        s"/recent?since=$since&excludeProduct=$Placement&excludeProduct=$Add&status=active"
      val good = GoodRecordGen.next.copy(category = OfferCategories.Commercial)
      (goodsService
        .getRecent(
          _: Epoch,
          _: Seq[ProductId],
          _: Option[GoodStatus]
        ))
        .expects(
          epoch(since),
          products,
          goodStatus
        )
        .returningZ(List(good))
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        val Vector(goodJson) =
          responseAs[JsArray].elements.map(_.asJsObject.fields)
        goodJson("offer").convertTo[Long] shouldBe good.offerId
        goodJson("category").convertTo[String] shouldBe "commercial"
      }
    }

    "return commercial child good" in {
      val uri =
        s"/recent?since=$since&excludeProduct=$Placement&excludeProduct=$Add&status=active"
      val good = GoodRecordGen.next.copy(category = OfferCategories.Bus)
      (goodsService
        .getRecent(
          _: Epoch,
          _: Seq[ProductId],
          _: Option[GoodStatus]
        ))
        .expects(
          epoch(since),
          products,
          goodStatus
        )
        .returningZ(List(good))
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        val Vector(goodJson) =
          responseAs[JsArray].elements.map(_.asJsObject.fields)
        goodJson("offer").convertTo[Long] shouldBe good.offerId
        goodJson("category").convertTo[String] shouldBe "bus"
      }
    }

    "return moto good" in {
      val uri =
        s"/recent?since=$since&excludeProduct=$Placement&excludeProduct=$Add&status=active"
      val good = GoodRecordGen.next.copy(category = OfferCategories.Moto)
      (goodsService
        .getRecent(
          _: Epoch,
          _: Seq[ProductId],
          _: Option[GoodStatus]
        ))
        .expects(
          epoch(since),
          products,
          goodStatus
        )
        .returningZ(List(good))
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        val Vector(goodJson) =
          responseAs[JsArray].elements.map(_.asJsObject.fields)
        goodJson("offer").convertTo[Long] shouldBe good.offerId
        goodJson("category").convertTo[String] shouldBe "moto"
      }
    }

    "return moto child good" in {
      val uri =
        s"/recent?since=$since&excludeProduct=$Placement&excludeProduct=$Add&status=active"
      val good = GoodRecordGen.next.copy(category = OfferCategories.Atv)
      (goodsService
        .getRecent(
          _: Epoch,
          _: Seq[ProductId],
          _: Option[GoodStatus]
        ))
        .expects(
          epoch(since),
          products,
          goodStatus
        )
        .returningZ(List(good))
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe OK
        val Vector(goodJson) =
          responseAs[JsArray].elements.map(_.asJsObject.fields)
        goodJson("offer").convertTo[Long] shouldBe good.offerId
        goodJson("category").convertTo[String] shouldBe "atv"
      }
    }
  }

  "POST /client/$id" should {

    val clientId = 20101L
    val offerId = "105322885-f1ea3b"
    val autoruOfferId = AutoruOfferId(offerId)
    val product = ProductId.Fresh
    val createDateStr = "2018-09-17T22:22:00.000+03:00"
    val createDate = DateTime.parse(createDateStr)

    "add used cars good" in {
      val category = OfferCategories.Cars
      val section = Section.USED
      val good = GoodsRequest(autoruOfferId, category, section, product, Set.empty)
      val details = GoodsAddingResult(
        autoruOfferId.id,
        category = category,
        product = product,
        createDate = createDate,
        expireDate = None,
        badge = None,
        offerHash = autoruOfferId.hash,
        epoch = None,
        paymentModel = paymentModel,
        addedGoodId = Some(-1)
      )
      val requestBody = JsArray(
        JsObject(
          "offerId" -> JsString(offerId),
          "category" -> JsString("cars"),
          "section" -> JsString("used"),
          "product" -> JsString("all_sale_fresh")
        )
      ).prettyPrint
      val entity = HttpEntity(`application/json`, requestBody)
      (goodsService
        .add(_: ClientId, _: List[GoodsRequest]))
        .expects(clientId, List(good))
        .returningZ(List(details))
      Post("/client/20101", entity).withHeaders(RequestIdentityHeaders) ~> seal(
        route
      ) ~> check {
        status shouldBe OK
        responseAs[JsArray] shouldBe JsArray(
          JsObject(
            "offer" -> JsNumber(105322885),
            "category" -> JsString("cars"),
            "product" -> JsString("all_sale_fresh"),
            "from" -> JsString(createDateStr),
            "offerHash" -> JsString("f1ea3b"),
            "paymentModel" -> JsString("single")
          )
        )
      }
    }

    "add new atv good" in {
      val category = OfferCategories.Atv
      val section = Section.NEW
      val good = GoodsRequest(autoruOfferId, category, section, product, Set.empty)
      val details = GoodsAddingResult(
        autoruOfferId.id,
        category = category,
        product = product,
        createDate = createDate,
        expireDate = None,
        badge = None,
        offerHash = autoruOfferId.hash,
        epoch = None,
        paymentModel = paymentModel,
        addedGoodId = Some(-1)
      )
      val requestBody = JsArray(
        JsObject(
          "offerId" -> JsString(offerId),
          "category" -> JsString("atv"),
          "section" -> JsString("new"),
          "product" -> JsString("all_sale_fresh")
        )
      ).prettyPrint
      val entity = HttpEntity(`application/json`, requestBody)
      (goodsService
        .add(_: ClientId, _: List[GoodsRequest]))
        .expects(clientId, List(good))
        .returningZ(List(details))
      Post("/client/20101", entity).withHeaders(RequestIdentityHeaders) ~> seal(
        route
      ) ~> check {
        status shouldBe OK
        responseAs[JsArray] shouldBe JsArray(
          JsObject(
            "offer" -> JsNumber(105322885),
            "category" -> JsString("atv"),
            "product" -> JsString("all_sale_fresh"),
            "from" -> JsString(createDateStr),
            "offerHash" -> JsString("f1ea3b"),
            "paymentModel" -> JsString("single")
          )
        )
      }
    }
  }

  "POST /client/$id/rt" should {
    val clientId = 20101L
    val offerId = "105322885-f1ea3b"
    val autoruOfferId = AutoruOfferId(offerId)
    val product = ProductId.Fresh
    val createDateStr = "2018-09-17T22:22:00.000+03:00"
    val createDate = DateTime.parse(createDateStr)
    val category = OfferCategories.Cars
    val section = Section.USED

    "return ok on adding single good without money hold" in {

      val good = GoodsRequest(autoruOfferId, category, section, product, Set.empty)
      val details = GoodsAddingResult(
        offerId = autoruOfferId.id,
        category = category,
        product = product,
        createDate = createDate,
        expireDate = None,
        badge = None,
        offerHash = autoruOfferId.hash,
        epoch = None,
        paymentModel = paymentModel,
        addedGoodId = Some(-1)
      )
      val requestBody =
        JsObject(
          "offerId" -> JsString(offerId),
          "category" -> JsString("cars"),
          "section" -> JsString("used"),
          "product" -> JsString("all_sale_fresh")
        ).prettyPrint
      val entity = HttpEntity(`application/json`, requestBody)
      (goodsService
        .add(_: ClientId, _: List[GoodsRequest]))
        .expects(clientId, List(good))
        .returningZ(List(details))
      Post("/client/20101/rt?with_money_check=false", entity).withHeaders(
        RequestIdentityHeaders
      ) ~> seal(
        route
      ) ~> check {
        status shouldBe OK
      }
    }

    "return OK on adding single good with money hold" in {

      val requestBody =
        JsObject(
          "offerId" -> JsString(offerId),
          "category" -> JsString("cars"),
          "section" -> JsString("used"),
          "product" -> JsString("all_sale_fresh")
        ).prettyPrint
      val entity = HttpEntity(`application/json`, requestBody)
      (goodsPreparingService
        .prepare(_: AutoruOfferId, _: ProductId, _: ClientId))
        .expects(*, *, *)
        .returningZ(Allowed)

      Post("/client/20101/rt?with_money_check=true", entity).withHeaders(
        RequestIdentityHeaders
      ) ~> seal(
        route
      ) ~> check {
        status shouldBe OK
      }
    }

    "return BadRequest on adding single good as array" in {
      val requestBody = JsArray(
        JsObject(
          "offerId" -> JsString(offerId),
          "category" -> JsString("cars"),
          "section" -> JsString("used"),
          "product" -> JsString("all_sale_fresh")
        )
      ).prettyPrint
      val entity = HttpEntity(`application/json`, requestBody)

      Post("/client/20101/rt?with_money_check=false", entity).withHeaders(
        RequestIdentityHeaders
      ) ~> seal(
        route
      ) ~> check {
        status shouldBe BadRequest
      }
    }

    "return decider's deactivation reason while adding single good" in {
      val requestBody =
        JsObject(
          "offerId" -> JsString(offerId),
          "category" -> JsString("cars"),
          "section" -> JsString("used"),
          "product" -> JsString("all_sale_fresh")
        ).prettyPrint
      val entity = HttpEntity(`application/json`, requestBody)
      (goodsPreparingService
        .prepare(_: AutoruOfferId, _: ProductId, _: ClientId))
        .expects(*, *, *)
        .returningZ(
          ActivationValidationResult(
            Deactivate(NotEnoughFunds, None)
          )
        )

      Post("/client/20101/rt?with_money_check=true", entity).withHeaders(
        RequestIdentityHeaders
      ) ~> seal(
        route
      ) ~> check {
        status shouldBe PaymentRequired

      }
    }

    "return goodsService's bad request reason while adding single good" in {
      val requestBody =
        JsObject(
          "offerId" -> JsString(offerId),
          "category" -> JsString("cars"),
          "section" -> JsString("used"),
          "product" -> JsString("all_sale_fresh")
        ).prettyPrint
      val entity = HttpEntity(`application/json`, requestBody)

      val exc = "Validation exception"
      (goodsService
        .add(_: ClientId, _: List[GoodsRequest]))
        .expects(*, *)
        .throwingZ(BadRequestException(exc))

      Post("/client/20101/rt?with_money_check=false", entity).withHeaders(
        RequestIdentityHeaders
      ) ~> seal(
        route
      ) ~> check {
        status shouldBe BadRequest

        responseAs[String] shouldBe ("\"" + exc + "\"")
      }
    }

    "return illegal product while adding single good" in {
      val requestBody =
        JsObject(
          "offerId" -> JsString(offerId),
          "category" -> JsString("cars"),
          "section" -> JsString("used"),
          "product" -> JsString("all_sale_fresh")
        ).prettyPrint
      val entity = HttpEntity(`application/json`, requestBody)
      val productId = ProductId.Premium
      val paymentModel = PaymentModel.Quota
      val exc = s"Product $productId is illegal for payment model $paymentModel"
      (goodsService
        .add(_: ClientId, _: List[GoodsRequest]))
        .expects(*, *)
        .throwingZ(IllegalProductForPaymentModel(productId, paymentModel))

      Post("/client/20101/rt?with_money_check=false", entity).withHeaders(
        RequestIdentityHeaders
      ) ~> seal(
        route
      ) ~> check {
        status shouldBe BadRequest

        responseAs[String] shouldBe ("\"" + exc + "\"")
      }
    }

    "return internal service problems on decider's temporary error" in {
      val requestBody =
        JsObject(
          "offerId" -> JsString(offerId),
          "category" -> JsString("cars"),
          "section" -> JsString("used"),
          "product" -> JsString("all_sale_fresh")
        ).prettyPrint
      val entity = HttpEntity(`application/json`, requestBody)
      (goodsPreparingService
        .prepare(_: AutoruOfferId, _: ProductId, _: ClientId))
        .expects(*, *, *)
        .returningZ(
          TemporaryError(HoldError(new Exception("test")))
        )

      Post("/client/20101/rt?with_money_check=true", entity).withHeaders(
        RequestIdentityHeaders
      ) ~> seal(
        route
      ) ~> check {
        status shouldBe InternalServerError
        responseAs[
          String
        ] shouldBe "{\"message\":\"HoldError(java.lang.Exception: test)\"}"

      }
    }

  }

  "GET /category/$category" should {

    val offerId = "105322885-f1ea3b"
    val autoruOfferId = AutoruOfferId(offerId)
    val product = ProductId.Fresh
    val createDateStr = "2018-09-17T22:22:00.000+03:00"
    val createDate = DateTime.parse(createDateStr)

    "get cars goods" in {
      val category = OfferCategories.Cars
      val details = GoodsDetails(
        offerId = autoruOfferId.id,
        category = category,
        product = product,
        createDate = createDate,
        expireDate = None,
        badge = None,
        offerHash = autoruOfferId.hash
      )
      (goodsService
        .get(_: List[OfferId], _: OfferCategory))
        .expects(List(105322885L), category)
        .returningZ(List(details))
      Get("/category/cars?offer=105322885").withHeaders(
        RequestIdentityHeaders
      ) ~> seal(route) ~> check {
        status shouldBe OK
        responseAs[JsArray] shouldBe JsArray(
          JsObject(
            "offer" -> JsNumber(105322885),
            "category" -> JsString("cars"),
            "product" -> JsString("all_sale_fresh"),
            "from" -> JsString(createDateStr),
            "offerHash" -> JsString("f1ea3b")
          )
        )
      }
    }

    "get atv goods" in {
      val category = OfferCategories.Atv
      val details = GoodsDetails(
        offerId = autoruOfferId.id,
        category = category,
        product = product,
        createDate = createDate,
        expireDate = None,
        badge = None,
        offerHash = autoruOfferId.hash
      )
      (goodsService
        .get(_: List[OfferId], _: OfferCategory))
        .expects(List(105322885L), category)
        .returningZ(List(details))
      Get("/category/atv?offer=105322885").withHeaders(
        RequestIdentityHeaders
      ) ~> seal(route) ~> check {
        status shouldBe OK
        responseAs[JsArray] shouldBe JsArray(
          JsObject(
            "offer" -> JsNumber(105322885),
            "category" -> JsString("atv"),
            "product" -> JsString("all_sale_fresh"),
            "from" -> JsString(createDateStr),
            "offerHash" -> JsString("f1ea3b")
          )
        )
      }
    }
  }

  "POST /apply-test" should {
    "return not found" in {
      val entity = HttpEntity(Array[Byte]())

      Post("/apply-test", entity).withHeaders(RequestIdentityHeaders) ~>
        seal(route) ~> check {
          status shouldBe NotFound
        }
    }
  }

  def actorRefFactory: ActorRefFactory = system
}
