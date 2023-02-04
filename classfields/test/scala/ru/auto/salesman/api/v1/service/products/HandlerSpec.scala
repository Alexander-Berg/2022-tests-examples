package ru.auto.salesman.api.v1.service.products

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import akka.http.scaladsl.model.MediaType.{Binary, Compressible}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.util.ByteString
import com.google.protobuf.util.JsonFormat
import org.mockito.Mockito.{reset, verify}
import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Category.CARS
import ru.auto.api.ApiOfferModel.{OffersList, Section}
import ru.auto.salesman.api.v1.DeprecatedMockitoHandlerBaseSpec
import ru.auto.salesman.api.v1.SalesmanApiUtils.SalesmanHttpRequest
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.ApiModel
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods
import ru.auto.salesman.model.{AutoruUser, OfferCategories}
import ru.auto.salesman.service.async.{AsyncProductService}
import ru.auto.salesman.service.user.PriceService
import ru.auto.salesman.service.user.UserProductService.{
  ActiveOffersProductsRequests,
  ProductResponses
}
import ru.auto.salesman.test.IntegrationPropertyCheckConfig
import ru.auto.salesman.test.model.gens.{HandlerModelGenerators, OfferModelGenerators}
import ru.auto.salesman.test.model.gens.user.{
  UserModelGenerators,
  UserProductServiceGenerators
}
import ru.auto.salesman.util.{PriceRequestContext, PriceRequestContextOffers}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.Future

trait HandlerSpec
    extends DeprecatedMockitoHandlerBaseSpec
    with BeforeAndAfter
    with OfferModelGenerators
    with UserModelGenerators
    with HandlerModelGenerators
    with UserProductServiceGenerators
    with SprayJsonSupport
    with IntegrationPropertyCheckConfig {

  lazy val service = mock[PriceService]
  lazy val productServiceMock = mock[AsyncProductService]

  override def priceService: PriceService = service
  override def productService: AsyncProductService = productServiceMock

  before {
    reset(service)
    reset(productService)
  }

  def satisfies[T](cond: T => Boolean): T =
    ArgumentMatchers.argThat[T](new ArgumentMatcher[T] {
      def matches(argument: T): Boolean = cond(argument)
    })

  "prices handlers" should {

    val mediaType: Binary =
      MediaType.applicationBinary("protobuf", comp = Compressible)

    val contentTypeProto: ContentType = mediaType

    def multipleOffersGen: Gen[ApiOfferModel.OffersList] =
      for {
        offers <- Gen.listOfN(3, carGen)
      } yield
        ApiOfferModel.OffersList
          .newBuilder()
          .addAllOffers(offers.asJava)
          .build()

    def carGen: Gen[ApiOfferModel.Offer] = offerGen(offerCategoryGen = CARS)

    def jsonOffer(o: ApiOfferModel.Offer): String =
      JsonFormat.printer().print(o)

    def offersToJson(os: ApiOfferModel.OffersList): String =
      JsonFormat.printer().print(os)

    def proto(offer: ApiOfferModel.Offer): RequestEntity =
      HttpEntity.Strict(
        contentTypeProto,
        ByteString.fromArray(offer.toByteArray)
      )

    def json(offer: ApiOfferModel.Offer): RequestEntity =
      HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString.fromString(jsonOffer(offer))
      )

    def protoOffers(offers: ApiOfferModel.OffersList): RequestEntity =
      HttpEntity.Strict(
        contentTypeProto,
        ByteString.fromArray(offers.toByteArray)
      )

    def jsonOffers(offers: ApiOfferModel.OffersList): RequestEntity =
      HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString.fromString(offersToJson(offers))
      )

    "NotFound post request for wrong url" in {
      forAll(carGen, bool) { (offer, entityAsJson) =>
        val entity =
          if (entityAsJson)
            json(offer)
          else
            proto(offer)

        val request =
          HttpRequest(POST, s"/products/notExistingPath", entity = entity)
            .withSalesmanTestHeader()
        request ~> Route.seal(route) ~> check {
          status shouldBe NotFound
        }

      }
    }

    "accept post request for price" in {
      forAll(AutoruUserGen, UserProductGen, carGen, bool) {
        (user, product, offer, entityAsJson) =>
          val entity =
            if (entityAsJson)
              json(offer)
            else
              proto(offer)

          val price = ProductPriceGen.next
          when(service.calculatePrices(?, ?)).thenReturnZ(List(price))

          val request =
            HttpRequest(
              POST,
              s"/products/prices?product=$product&user=$user",
              entity = entity
            )
              .withSalesmanTestHeader()
          request ~> route ~> check {
            status shouldBe OK
          }

          verify(service).calculatePrices(
            ?,
            satisfies[PriceRequestContext](_.autoruOffer.contains(offer))
          )

      }
    }

    "accept post request multipleOffersPrices with one offer with applyMoneyFeature" in {
      forAll(productGen[AutoruGoods], carGen, bool, ProductPricesGen, bool) {
        (product, offer, entityAsJson, prices, applyMoneyFeature) =>
          val offers = ApiOfferModel.OffersList
            .newBuilder()
            .addAllOffers(List(offer).asJava)
            .build()

          val entity =
            if (entityAsJson)
              jsonOffers(offers)
            else
              protoOffers(offers)

          when(service.calculatePricesForMultipleOffers(?, ?))
            .thenReturnZ(List(prices))

          val request =
            HttpRequest(
              POST,
              s"/products/multipleOffersPrices?product=$product&applyMoneyFeature=$applyMoneyFeature",
              entity = entity
            )
              .withSalesmanTestHeader()

          request ~> route ~> check {
            status shouldBe OK
            if (entityAsJson) {

              val Vector(goodJson) =
                responseAs[JsArray].elements.map(_.asJsObject.fields)

              goodJson("offerId").convertTo[String] shouldBe prices.offerId

              val productPricesList = goodJson("productPrices")
                .convertTo[JsArray]
                .elements
                .map(_.asJsObject.fields)
              productPricesList.head
                .get("price")
                .get
                .convertTo[JsObject]
                .fields
                .get("basePrice")
                .get
                .convertTo[String]
                .toLong shouldBe prices.prices.head.price.basePrice
            }
          }

          verify(service).calculatePricesForMultipleOffers(
            ?,
            satisfies[PriceRequestContextOffers](_.offers.head == offer)
          )
      }
    }

    "accept post request multipleOffersPrices with one offer without applyMoneyFeature" in {
      forAll(productGen[AutoruGoods], carGen, bool, ProductPricesGen) {
        (product, offer, entityAsJson, prices) =>
          val offers = ApiOfferModel.OffersList
            .newBuilder()
            .addAllOffers(List(offer).asJava)
            .build()

          val entity =
            if (entityAsJson)
              jsonOffers(offers)
            else
              protoOffers(offers)

          when(service.calculatePricesForMultipleOffers(?, ?))
            .thenReturnZ(List(prices))

          val request =
            HttpRequest(
              POST,
              s"/products/multipleOffersPrices?product=$product",
              entity = entity
            )
              .withSalesmanTestHeader()

          request ~> route ~> check {
            status shouldBe OK
            if (entityAsJson) {

              val Vector(goodJson) =
                responseAs[JsArray].elements.map(_.asJsObject.fields)

              goodJson("offerId").convertTo[String] shouldBe prices.offerId

            }
          }

          verify(service).calculatePricesForMultipleOffers(
            ?,
            satisfies[PriceRequestContextOffers](_.offers.head == offer)
          )
      }
    }

    "accept post request multipleOffersPrices with multiple offers" in {
      forAll(AutoruUserGen, productGen[AutoruGoods], multipleOffersGen, bool) {
        (user, product, offersGenerated, entityAsJson) =>
          val offers = offersGenerated.getOffersList.asScala
            .map(_.toBuilder.setUserRef(user.toString).build())
            .asJava

          val offersList = OffersList.newBuilder().addAllOffers(offers).build

          val entity =
            if (entityAsJson)
              protoOffers(offersList)
            else
              jsonOffers(offersList)

          val prices = ProductPricesGen.next
          when(service.calculatePricesForMultipleOffers(?, ?))
            .thenReturnZ(List(prices))

          val reqParams =
            s"/products/multipleOffersPrices?product=$product&applyMoneyFeature=false"
          val request =
            HttpRequest(POST, reqParams, entity = entity)
              .withSalesmanTestHeader()

          request ~> route ~> check {
            status shouldBe OK
            if (entityAsJson) {

              val Vector(goodJson) =
                responseAs[JsArray].elements.map(_.asJsObject.fields)

              goodJson("offerId").convertTo[String] shouldBe prices.offerId

              val productPricesList = goodJson("productPrices")
                .convertTo[JsArray]
                .elements
                .map(_.asJsObject.fields)
              productPricesList.head
                .get("price")
                .get
                .convertTo[JsObject]
                .fields
                .get("basePrice")
                .get
                .convertTo[String]
                .toLong shouldBe prices.prices.head.price.basePrice
            }
          }

      }
    }

    "accept get request for cars:used price" in {
      val user = AutoruUser(25115)
      val offerId = AutoruOfferId("1055234132-af3eed")
      val product = AutoruGoods.Boost
      val category = OfferCategories.Cars
      val section = Section.USED
      val price = ProductPriceGen.next
      val context = PriceRequestContext(
        contextType = None,
        userModerationStatus = None,
        Some(user),
        Some(offerId),
        autoruOffer = None,
        Some(category),
        Some(section),
        geoId = None,
        vin = None,
        vinReportParams = None,
        licensePlate = None,
        contentQuality = None,
        applyMoneyFeature = true,
        applyProlongInterval = true
      )
      when(service.calculatePrices(eq(List(product)), eq(context)))
        .thenReturnZ(List(price))

      val request =
        HttpRequest(
          GET,
          "/products/prices?" +
          "product=boost&user=user:25115&offerId=1055234132-af3eed&category=cars&section=used"
        ).withSalesmanTestHeader()
      request ~> route ~> check {
        status shouldBe OK
      }

      verify(service).calculatePrices(eq(List(product)), eq(context))
    }

    "accept get request for new atv price" in {
      val user = AutoruUser(25115)
      val offerId = AutoruOfferId("1055234132-af3eed")
      val product = AutoruGoods.Boost
      val category = OfferCategories.Atv
      val section = Section.NEW
      val price = ProductPriceGen.next
      val context = PriceRequestContext(
        contextType = None,
        userModerationStatus = None,
        Some(user),
        Some(offerId),
        autoruOffer = None,
        Some(category),
        Some(section),
        geoId = None,
        vin = None,
        vinReportParams = None,
        licensePlate = None,
        contentQuality = None,
        applyMoneyFeature = true,
        applyProlongInterval = true
      )
      when(service.calculatePrices(eq(List(product)), eq(context)))
        .thenReturnZ(List(price))

      val request =
        HttpRequest(
          GET,
          "/products/prices?" +
          "product=boost&user=user:25115&offerId=1055234132-af3eed&category=atv&section=new"
        ).withSalesmanTestHeader()
      request ~> route ~> check {
        status shouldBe OK
      }

      verify(service).calculatePrices(eq(List(product)), eq(context))
    }
  }

  "products handlers" should {
    val mediaType: Binary =
      MediaType.applicationBinary("protobuf", comp = Compressible)

    val contentTypeProto: ContentType = mediaType

    def jsonRequest(o: ApiModel.ActiveOffersProductsRequest): String =
      JsonFormat.printer().print(o)

    def proto(request: ApiModel.ActiveOffersProductsRequest): RequestEntity =
      HttpEntity.Strict(
        contentTypeProto,
        ByteString.fromArray(request.toByteArray)
      )

    def json(request: ApiModel.ActiveOffersProductsRequest): RequestEntity =
      HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString.fromString(jsonRequest(request))
      )

    "accept proto request" in {
      forAll(activeOffersProductsRequestsProtoGen()) { req =>
        val entity = proto(req)

        val resp = ProductResponses(Nil)

        when(productService.getActiveProducts(?)(?))
          .thenReturn(Future.successful(resp))

        val request =
          HttpRequest(POST, s"/products/activeProducts", entity = entity)
            .withSalesmanTestHeader()

        request ~> route ~> check {
          status shouldBe OK
        }

        verify(productService).getActiveProducts(
          satisfies[ActiveOffersProductsRequests](
            _.requests.head.offerId.value == req.getActiveOfferProductRequestsList.asScala.head.getOfferId
          )
        )(?)
      }
    }

    "accept json request" in {
      forAll(activeOffersProductsRequestsProtoGen(), productsResponsesGen()) {
        (req, resp) =>
          val entity = json(req)

          when(productService.getActiveProducts(?)(?))
            .thenReturn(Future.successful(resp))

          val request =
            HttpRequest(POST, s"/products/activeProducts", entity = entity)
              .withSalesmanTestHeader()

          request ~> route ~> check {
            status shouldBe OK
            val goodJson =
              responseAs[JsObject]

            val transactionId = goodJson.fields
              .get("productResponses")
              .get
              .convertTo[JsArray]
              .elements
              .head
              .asJsObject
              .fields
              .get("transactionId")
              .get
              .convertTo[String]
            transactionId shouldBe resp.responses.head.transactionId
          }

          verify(productService).getActiveProducts(
            satisfies[ActiveOffersProductsRequests](
              _.requests.head.offerId.value == req.getActiveOfferProductRequestsList.asScala.head.getOfferId
            )
          )(?)
      }
    }

  }

}
