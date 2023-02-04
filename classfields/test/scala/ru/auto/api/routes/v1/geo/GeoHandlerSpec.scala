package ru.auto.api.routes.v1.geo

import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import akka.http.scaladsl.model.headers.Accept
import ru.auto.api.ApiSpec
import ru.auto.api.managers.geo.GeoManager
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelGenerators.SessionResultGen
import ru.auto.api.services.MockedClients
import ru.auto.api.util.Protobuf

class GeoHandlerSpec extends ApiSpec with MockedClients {
  override lazy val geoManager: GeoManager = mock[GeoManager]

  "/geo/suggest" should {
    "suggest by params" in {
      val response = ModelGenerators.GeoSuggestResponseGen.next
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(geoManager.suggest(?)(?)).thenReturnF(response)
      Get("/1.0/geo/suggest") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[String] shouldBe Protobuf.toJson(response)
        }
    }
  }

  "/geo/regions" should {
    "return regions list" in {
      val response = ModelGenerators.RegionListingResponseGen.next
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(geoManager.regions()).thenReturn(response)
      Get("/1.0/geo/regions") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[String] shouldBe Protobuf.toJson(response)
        }
    }
  }

}
