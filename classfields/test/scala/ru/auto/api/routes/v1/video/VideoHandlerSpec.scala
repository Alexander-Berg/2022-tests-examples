package ru.auto.api.routes.v1.video

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{StatusCodes, Uri}
import org.mockito.Mockito._
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.VideoListingResponse
import ru.auto.api.managers.video.VideoManager
import ru.auto.api.model.ModelGenerators.SessionResultGen
import ru.auto.api.search.SearchModel.CatalogFilter
import ru.auto.api.services.MockedClients

class VideoHandlerSpec extends ApiSpec with MockedClients {

  override lazy val videoManager: VideoManager = mock[VideoManager]

  "video/search/{category}" should {
    "respond with video" in {
      val expected = VideoListingResponse.getDefaultInstance

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(videoManager.search(?, ?, ?, ?)(?)).thenReturnF(expected)

      val catalogFilter = CatalogFilter.newBuilder().setMark("MERCEDES").build()

      val params = Map(
        "mark" -> "MERCEDES",
        "model" -> "M5",
        "super_gen" -> "asf",
        "catalog_filter" -> "mark=MERCEDES"
      )
      val query = Uri.Query(params)

      Get(Uri(s"/1.0/video/search/cars/").withQuery(query)) ~>
        addHeader(Accept(`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[VideoListingResponse] shouldBe expected
        verify(videoManager).search(?, ?, eq(params), eq(Some(catalogFilter)))(?)
      }
    }
  }
}
