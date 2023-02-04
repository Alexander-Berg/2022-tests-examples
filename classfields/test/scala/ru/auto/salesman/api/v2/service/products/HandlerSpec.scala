package ru.auto.salesman.api.v2.service.products

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.MediaType.{Binary, Compressible}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.util.ByteString
import com.google.protobuf.util.JsonFormat
import org.mockito.Mockito.{reset, verify}
import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import org.scalatest.BeforeAndAfter
import ru.auto.salesman.Task
import ru.auto.salesman.api.v2.HandlerBaseSpec
import ru.auto.salesman.api.v2.SalesmanApiUtils.SalesmanHttpRequest
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.PriceRequestModel.PriceRequest
import ru.auto.salesman.service.user.PriceService
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.PriceRequestContext
import ru.yandex.vertis.mockito.MockitoSupport

trait HandlerSpec
    extends HandlerBaseSpec
    with MockitoSupport
    with BeforeAndAfter
    with OfferModelGenerators
    with UserModelGenerators
    with SprayJsonSupport {

  lazy val service = MockitoSupport.mock[PriceService]

  override def priceService: PriceService = service

  before {
    reset(service)
  }

  def satisfies[T](cond: T => Boolean): T =
    ArgumentMatchers.argThat[T](new ArgumentMatcher[T] {
      def matches(argument: T): Boolean = cond(argument)
    })

  "prices handlers" should {
    val mediaType: Binary =
      MediaType.applicationBinary("protobuf", comp = Compressible)

    val contentTypeProto: ContentType = mediaType

    def proto(priceRequest: PriceRequest): RequestEntity =
      HttpEntity.Strict(
        contentTypeProto,
        ByteString.fromArray(priceRequest.toByteArray)
      )

    def json(priceRequest: PriceRequest): RequestEntity =
      HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString.fromString {
          JsonFormat.printer().print(priceRequest)
        }
      )

    "accept post request for price" in {
      forAll(AutoruUserGen, UserProductGen.map(_.name), bool) {
        (user, product, asProtobuf) =>
          reset(service)

          val priceRequest = PriceRequest
            .newBuilder()
            .setOfferHistory {
              PriceRequest.OfferHistory
                .newBuilder()
                .setOfferId("123-fff")
            }
            .build()

          val entity =
            if (asProtobuf)
              proto(priceRequest)
            else
              json(priceRequest)

          val price = ProductPriceGen.next
          when(service.chooseAvailableProductsAndCalculatePrices(?, ?))
            .thenReturn(Task.succeed(List(price)))

          val request =
            HttpRequest(
              POST,
              s"/products/prices?product=$product&user=$user",
              entity = entity
            ).withSalesmanTestHeader()

          request ~> route ~> check {
            status shouldBe OK
          }

          verify(service).chooseAvailableProductsAndCalculatePrices(
            eq(List(product)),
            satisfies[PriceRequestContext] {
              _.offerId.contains(AutoruOfferId("123-fff"))
            }
          )

      }
    }

  }

}
