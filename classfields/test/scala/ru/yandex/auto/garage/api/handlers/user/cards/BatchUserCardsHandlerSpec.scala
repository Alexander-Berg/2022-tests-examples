package ru.yandex.auto.garage.api.handlers.user.cards

import akka.http.scaladsl.model.headers.CustomHeader
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.garage.ResponseModel.GetListingResponse
import ru.yandex.auto.vin.decoder.api.RequestDirectives
import ru.yandex.auto.vin.decoder.exceptions.InvalidUser
import ru.yandex.auto.vin.decoder.utils.{EmptyRequestInfo, RequestInfo}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class BatchUserCardsHandlerSpec
  extends AnyWordSpecLike
  with Matchers
  with ScalatestRouteTest
  with MockitoSupport
  with BeforeAndAfter {

  implicit val requestInfo: RequestInfo = EmptyRequestInfo

  private val processor = mock[BatchUserCardsHandlerRequestProcessor]
  val handler = new BatchUserCardsHandler(processor)
  val route: Route = RequestDirectives.wrapRequest(handler.route)

  private val defaultUserHeader = new CustomHeader {
    override def name(): String = "x-user-id"

    override def value(): String = "user:123"

    override def renderInRequests(): Boolean = true

    override def renderInResponses(): Boolean = true
  }

  private val defaultGetListingIdRequest = {
    Post("/search").addHeader(defaultUserHeader).withEntity(ContentTypes.`application/json`, "{}")
  }

  "get listing" should {
    "return 200" in {
      when(processor.getListing(?, ?)(?)).thenReturn(
        Future.successful(
          GetListingResponse.newBuilder().build()
        )
      )

      defaultGetListingIdRequest ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
    "return 400" when {
      "processor throw InvalidUser" in {
        when(processor.getListing(?, ?)(?)).thenReturn(Future.failed(InvalidUser("Invalid user id 123")))
        defaultGetListingIdRequest ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          responseAs[
            String
          ] shouldBe "{\n  \"error\": \"BAD_REQUEST\",\n  \"status\": \"ERROR\",\n  \"detailedError\": \"Invalid user id 123\"\n}"
        }
      }
    }
  }

}
