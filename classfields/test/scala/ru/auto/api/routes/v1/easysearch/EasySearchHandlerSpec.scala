package ru.auto.api.routes.v1.easysearch

import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import akka.http.scaladsl.model.headers.Accept
import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions}
import org.scalacheck.Gen
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.GrouppedOffersListingResponse
import ru.auto.api.easy_search.EasySearchModel.OffersListingGroup
import ru.auto.api.managers.easysearch.EasySearchManager
import ru.auto.api.model.ModelGenerators.{OfferGen, SessionResultGen}
import ru.auto.api.model.Paging
import ru.auto.api.search.SearchModel.CatalogFilter
import ru.auto.api.services.MockedClients
import ru.auto.api.util.Protobuf

import scala.jdk.CollectionConverters._

class EasySearchHandlerSpec extends ApiSpec with MockedClients {

  override lazy val easySearchManager: EasySearchManager = mock[EasySearchManager]

  "/1.0/easy-search/cars/groups-listing" should {
    "respond with offerListResponse" in {
      val respBuilder = GrouppedOffersListingResponse.newBuilder()
      val group = OffersListingGroup.newBuilder()

      group.addAllOffers(Gen.listOfN(10, OfferGen).next.asJava)
      respBuilder.addAllOfferGroups(List(group.build()).asJava)

      val response = respBuilder.build

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(easySearchManager.groupsListing(?, ?, ?, ?)(?)).thenReturnF(response)

      val liked = "liked_catalog_filter=mark%3DHONDA,model%3DTORNEO,generation%3D8294326"
      val disliked = "disliked_catalog_filter=mark%3DZENVO,model%3DST1,generation%3D20479211"

      Get(s"/1.0/easy-search/cars/groups-listing?$liked&$disliked") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)

          verify(easySearchManager).groupsListing(
            ?,
            eq(Paging.Default),
            eq(Seq(CatalogFilter.newBuilder().setMark("HONDA").setModel("TORNEO").setGeneration(8294326L).build())),
            eq(Seq(CatalogFilter.newBuilder().setMark("ZENVO").setModel("ST1").setGeneration(20479211L).build()))
          )(?)
          verify(passportClient).createAnonymousSession()(?)
          verifyNoMoreInteractions(easySearchManager, passportClient)
          reset(easySearchManager, passportClient)
        }
    }

  }

}
