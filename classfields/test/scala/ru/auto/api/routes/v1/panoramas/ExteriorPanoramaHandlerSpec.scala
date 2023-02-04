package ru.auto.api.routes.v1.panoramas

import akka.http.scaladsl.model.StatusCodes
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.{ErrorCode, ErrorResponse, ResponseStatus, SuccessResponse}
import ru.auto.api.exceptions.PanoramaNotFound
import ru.auto.api.managers.panorama.PanoramaManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients
import ru.auto.panoramas.PanoramasModel.Metadata

import scala.util.Random

class ExteriorPanoramaHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks with BeforeAndAfter {

  override lazy val panoramaManager: PanoramaManager = mock[PanoramaManager]

  private val sessionId = SessionIdGen.next

  private val defaultHeaders = xAuthorizationHeader ~>
    addHeader("x-session-id", sessionId.toString)

  private def panoramaId: String = Random.alphanumeric.take(10).mkString
  private val metadata: Metadata = Metadata.getDefaultInstance

  private val successResponse: SuccessResponse =
    SuccessResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()

  before {
    reset(passportClient)
    reset(panoramaManager)
  }

  "PanoramasHandler" should {
    "add metadata for existing panorama" in {
      val existPanoramaId = panoramaId
      when(passportClient.getSession(?)(?)).thenReturnF(SessionResultGen.next)
      when(panoramaManager.addExteriorMetadata(eq(existPanoramaId), eq(metadata))(?)).thenReturnF(successResponse)
      Post(s"/1.0/panorama/$existPanoramaId/metadata", metadata) ~>
        defaultHeaders ~>
        route ~>
        check {
          val response = responseAs[SuccessResponse]
          withClue(response) {
            status shouldBe StatusCodes.OK
            response.getStatus shouldBe ResponseStatus.SUCCESS
          }
        }
    }

    "add metadata for not existing panorama" in {
      val randomPanoramaId = panoramaId
      when(passportClient.getSession(?)(?)).thenReturnF(SessionResultGen.next)
      when(panoramaManager.addExteriorMetadata(?, ?)(?)).thenThrow(PanoramaNotFound(randomPanoramaId))
      Post(s"/1.0/panorama/$randomPanoramaId/metadata", metadata) ~>
        defaultHeaders ~>
        route ~>
        check {
          val response = responseAs[ErrorResponse]
          withClue(response) {
            status shouldBe StatusCodes.NotFound
            response.getError shouldBe ErrorCode.NOT_FOUND
            response.getDetailedError shouldBe s"panorama $randomPanoramaId not found"
          }
        }
    }
  }
}
