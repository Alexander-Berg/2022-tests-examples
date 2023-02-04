package ru.yandex.auto.vin.decoder.api.handlers.relationships

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.lemonlabs.uri.Uri
import io.lemonlabs.uri.typesafe.QueryKey.stringQueryKey
import io.lemonlabs.uri.typesafe.dsl._
import org.mockito.Mockito.reset
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.ResponseModel.VinResponse
import ru.yandex.auto.vin.decoder.api.RequestDirectives
import ru.yandex.auto.vin.decoder.api.exceptions._
import ru.yandex.auto.vin.decoder.manager.RelationshipManager
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.utils.{EmptyRequestInfo, RequestInfo}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class RelationshipHandlerSpec
  extends AnyWordSpecLike
  with Matchers
  with ScalatestRouteTest
  with MockitoSupport
  with BeforeAndAfterEach {

  implicit val requestInfo: RequestInfo = EmptyRequestInfo

  val manager: RelationshipManager = mock[RelationshipManager]
  val handler = new RelationshipHandler(manager)
  val route: Route = RequestDirectives.wrapRequest(handler.route)

  override protected def beforeEach(): Unit = {
    reset(manager)
  }

  val lp: LicensePlate = LicensePlate("K718CE178")
  val url: Uri = "" ? ("license_plate" -> lp.toString)

  "RelationshipHandler" should {
    "return vin" in {
      when(manager.resolveVinAndBuildResponse(?, ?)(?, ?, ?)).thenReturn(Future.successful {
        VinResponse
          .newBuilder()
          .setVin("XTA21099043576182")
          .build()
      })
      Get(url.toStringRaw) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe """{"vin":"XTA21099043576182"}"""
      }
    }

    "invalid lp" in {
      when(manager.resolveVinAndBuildResponse(?, ?)(?, ?, ?))
        .thenReturn(Future.failed(InvalidLicensePlateException(lp)))
      Get(url.toStringRaw) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] shouldBe
          """{"error":{"errorCode":"LP_NOT_VALID","detailedError":"Invalid license plate K718CE178"}}"""
      }
    }

    "unknown lp" in {
      when(manager.resolveVinAndBuildResponse(?, ?)(?, ?, ?))
        .thenReturn(Future.failed(UnknownLicensePlateException(lp)))
      Get(url.toStringRaw) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[String] shouldBe
          """{"error":{"errorCode":"LP_NOT_FOUND","detailedError":"Unknown license plate K718CE178"}}"""
      }
    }

    "unknown vin" in {
      when(manager.resolveVinAndBuildResponse(?, ?)(?, ?, ?)).thenReturn(Future.failed(new UnknownVinException()))
      Get(url.toStringRaw) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[String] shouldBe """{"error":{"errorCode":"VIN_NOT_FOUND","detailedError":"Vin not found"}}"""
      }
    }

    "report not ready" in {
      when(manager.resolveVinAndBuildResponse(?, ?)(?, ?, ?)).thenReturn(Future.failed(VinInProgressException()))
      Get(url.toStringRaw) ~> route ~> check {
        status shouldBe StatusCodes.Accepted
        responseAs[String] shouldBe
          """{"error":{"errorCode":"IN_PROGRESS","detailedError":"Vin code is still in progress, try again later"}}"""
      }
    }
  }
}
