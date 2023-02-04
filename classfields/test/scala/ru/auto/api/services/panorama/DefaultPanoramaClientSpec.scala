package ru.auto.api.services.panorama

import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.StatusCodes
import org.apache.http.client.utils.URIBuilder
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.{ResponseStatus, SuccessResponse}
import ru.auto.api.exceptions.{InvalidUrlException, PanoramaNotFound, UnexpectedResponseException}
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.panoramas.PanoramasModel.Metadata

import scala.util.Random

class DefaultPanoramaClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with TestRequest {
  private val panoramaClient = new DefaultPanoramaClient(http)

  private def panoramaId: String = Random.alphanumeric.take(10).mkString
  private val metadata: Metadata = Metadata.getDefaultInstance

  private val successResponse: SuccessResponse =
    SuccessResponse.newBuilder().setStatus(ResponseStatus.SUCCESS).build()

  "Exterior" should {
    "add panorama" in {
      forAll(ExteriorPanoramaGen) { expectedPanorama =>
        val url = "http://www.yandex.ru/download"
        val expectedUrl = new URIBuilder("/v1/panorama/add")
        expectedUrl.addParameter("url", url)

        http.expectUrl(POST, expectedUrl.toString)
        http.respondWithProto(StatusCodes.OK, expectedPanorama)

        val panorama = panoramaClient.addExterior(url).futureValue
        panorama.getId should not be empty
      }
    }

    "fail url" in {
      forAll(ExteriorPanoramaGen) { expectedPanorama =>
        val url = "not_valid_url"
        val expectedUrl = new URIBuilder("/v1/panorama/add")
        expectedUrl.addParameter("url", url)

        http.expectUrl(POST, expectedUrl.toString)
        http.respondWithStatus(StatusCodes.Unauthorized)

        panoramaClient.addExterior(url).failed.futureValue shouldBe an[InvalidUrlException]
      }
    }

    "add metadata for existing panorama" in {
      val id = panoramaId
      val meta = metadata
      http.expectUrl(POST, s"/v1/panorama/$id/metadata")
      http.expectProto(meta)
      http.respondWith(successResponse)
      panoramaClient.addExteriorMetadata(id, meta).futureValue
    }

    "add metadata for not existing panorama" in {
      val id = panoramaId
      val meta = metadata
      http.expectUrl(POST, s"/v1/panorama/$id/metadata")
      http.expectProto(meta)
      http.respondWithStatus(StatusCodes.NotFound)
      panoramaClient.addExteriorMetadata(id, meta).failed.futureValue shouldBe an[PanoramaNotFound]
    }

    "add metadata with unexpected response" in {
      val id = panoramaId
      val meta = metadata
      http.expectUrl(POST, s"/v1/panorama/$id/metadata")
      http.expectProto(meta)
      http.respondWith(StatusCodes.GatewayTimeout, "Gateway Timeout")
      panoramaClient.addExteriorMetadata(id, meta).failed.futureValue shouldBe an[UnexpectedResponseException]
    }
  }

  "Interior" should {
    "add panorama" in {
      forAll(InteriorPanoramaGen) { expectedPanorama =>
        val url = "http://www.yandex.ru/download"
        val expectedUrl = new URIBuilder("/v1/panorama/interior/add")
        expectedUrl.addParameter("url", url)

        http.expectUrl(POST, expectedUrl.toString)
        http.respondWithProto(StatusCodes.OK, expectedPanorama)

        val panorama = panoramaClient.addInterior(url).futureValue
        panorama.getId should not be empty
      }
    }

    "fail url" in {
      forAll(InteriorPanoramaGen) { expectedPanorama =>
        val url = "not_valid_url"
        val expectedUrl = new URIBuilder("/v1/panorama/interior/add")
        expectedUrl.addParameter("url", url)

        http.expectUrl(POST, expectedUrl.toString)
        http.respondWithStatus(StatusCodes.Unauthorized)

        panoramaClient.addInterior(url).failed.futureValue shouldBe an[InvalidUrlException]
      }
    }
  }

}
