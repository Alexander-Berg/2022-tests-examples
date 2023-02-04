package ru.auto.api.managers.panorama

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.{ResponseStatus, SuccessResponse}
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.PanoramaNotFound
import ru.auto.api.model.ModelGenerators.PrivateUserRefGen
import ru.auto.api.model.RequestParams
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.mds.PrivateMdsClient
import ru.auto.api.services.panorama.PanoramaClient
import ru.auto.api.services.uploader.UploaderClient
import ru.auto.api.util.RequestImpl
import ru.auto.panoramas.PanoramasModel.Metadata
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.util.Random

class PanoramaManagerSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with BeforeAndAfter {

  private val uploaderClient: UploaderClient = mock[UploaderClient]
  private val panoramaClient: PanoramaClient = mock[PanoramaClient]
  private val privateMdsClient: PrivateMdsClient = mock[PrivateMdsClient]

  private val selfAddress: String = "/localhost:80"

  private def panoramaId: String = Random.alphanumeric.take(10).mkString
  private val metadata = Metadata.getDefaultInstance

  implicit private val trace: Traced = Traced.empty

  implicit private val request: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
    r.setUser(PrivateUserRefGen.next)
    r.setApplication(Application.iosApp)
    r.setToken(TokenServiceImpl.iosApp)
    r.setTrace(trace)
    r
  }

  private val successResponse: SuccessResponse =
    SuccessResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()

  private val panoramaManager: PanoramaManager =
    new PanoramaManager(uploaderClient, panoramaClient, selfAddress, privateMdsClient)

  before {
    reset(uploaderClient)
    reset(panoramaClient)
  }

  "PanoramaManager" should {

    "add metadata for existing panorama" in {
      val existPanoramaId = panoramaId
      when(panoramaClient.addExteriorMetadata(eq(existPanoramaId), eq(metadata))(?)).thenReturnF(successResponse)
      val res = panoramaManager.addExteriorMetadata(existPanoramaId, metadata).futureValue
      res shouldBe successResponse
    }

    "add metadata for not existing panorama" in {
      val randomPanoramaId = panoramaId
      when(panoramaClient.addExteriorMetadata(?, ?)(?)).thenThrow(PanoramaNotFound(randomPanoramaId))

      assertThrows[PanoramaNotFound] {
        panoramaManager.addExteriorMetadata(randomPanoramaId, metadata).futureValue
      }
    }
  }
}
