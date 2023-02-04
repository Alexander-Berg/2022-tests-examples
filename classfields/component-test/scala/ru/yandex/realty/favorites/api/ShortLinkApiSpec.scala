package ru.yandex.realty.favorites.api

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.favorites.{
  AbstractFavoritesComponentTestSpec,
  CreateOffersShortLinkRequest,
  CreateSitesShortLinkRequest,
  CreateVillagesShortLinkRequest,
  GetOffersRequest,
  GetSitesRequest,
  GetVillagesRequest
}
import ru.yandex.realty.proto.api.error.ErrorCode

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ShortLinkApiSpec extends AbstractFavoritesComponentTestSpec {

  "CreateOffersShortLink" should {

    "create valid link ID when several offers was received" in {
      // given
      val offerIds = Seq(
        "6937155746319343003",
        "3179879834876516998",
        "6053139209874256526"
      )

      // when
      val linkIdResponse = favoritesService.createOffersShortLink(
        CreateOffersShortLinkRequest
          .newBuilder()
          .addAllOfferIds(offerIds.asJava)
          .build()
      )

      // then
      linkIdResponse.hasResponse should be(true)

      val linkId = linkIdResponse.getResponse.getLinkId
      linkId.nonEmpty should be(true)

      // when
      val offerIdsResponse = favoritesService.getOffers(
        GetOffersRequest
          .newBuilder()
          .setLinkId(linkId)
          .build()
      )

      // then
      offerIdsResponse.hasResponse should be(true)

      val receivedOfferIds = offerIdsResponse.getResponse.getOfferIdsList.asScala
      receivedOfferIds should contain allElementsOf (offerIds)
    }

    "return error when received empty offer list" in {
      // given
      val emptyOfferIdsRequest =
        CreateOffersShortLinkRequest
          .newBuilder()
          .build()

      // when
      val response = favoritesService.createOffersShortLink(emptyOfferIdsRequest)

      // then
      response.hasError should be(true)
      response.getError.getCode should be(ErrorCode.EMPTY_FAVORITES_OFFER_IDS)
    }

    "return error when offers were not found by provided link" in {
      // given
      val nonExistentLinkId = shortLinkGenerator.generateShortLink()
      val request =
        GetOffersRequest
          .newBuilder()
          .setLinkId(nonExistentLinkId)
          .build()

      // when
      val response = favoritesService.getOffers(request)

      // then
      response.hasError should be(true)
      response.getError.getCode should be(ErrorCode.INVALID_FAVORITES_LINK_ID)
    }

    "return error when link has invalid format" in {
      // given
      val invalidLinkId = ""
      val request =
        GetOffersRequest
          .newBuilder()
          .setLinkId(invalidLinkId)
          .build()

      // when
      val response = favoritesService.getOffers(request)

      // then
      response.hasError should be(true)
      response.getError.getCode should be(ErrorCode.INVALID_FAVORITES_LINK_ID)
    }

  }

  "CreateSitesShortLink" should {

    "create valid link ID when several sites was received" in {
      // given
      val siteIds = Seq(
        57547L,
        1632667L,
        73030L
      )

      // when
      val linkIdResponse = favoritesService.createSitesShortLink(
        CreateSitesShortLinkRequest
          .newBuilder()
          .addAllSiteIds(siteIds.map(Long.box).asJava)
          .build()
      )

      // then
      linkIdResponse.hasResponse should be(true)

      val linkId = linkIdResponse.getResponse.getLinkId
      linkId.nonEmpty should be(true)

      // when
      val siteIdsResponse = favoritesService.getSites(
        GetSitesRequest
          .newBuilder()
          .setLinkId(linkId)
          .build()
      )

      // then
      siteIdsResponse.hasResponse should be(true)

      val receivedSiteIds = siteIdsResponse.getResponse.getSiteIdsList.asScala
      receivedSiteIds should contain allElementsOf (siteIds)
    }

    "return error when received empty site list" in {
      // given
      val emptySiteIdsRequest =
        CreateSitesShortLinkRequest
          .newBuilder()
          .build()

      // when
      val response = favoritesService.createSitesShortLink(emptySiteIdsRequest)

      // then
      response.hasError should be(true)
      response.getError.getCode should be(ErrorCode.EMPTY_FAVORITES_SITE_IDS)
    }

    "return error when sites were not found by provided link" in {
      // given
      val nonExistentLinkId = shortLinkGenerator.generateShortLink()
      val request =
        GetSitesRequest
          .newBuilder()
          .setLinkId(nonExistentLinkId)
          .build()

      // when
      val response = favoritesService.getSites(request)

      // then
      response.hasError should be(true)
      response.getError.getCode should be(ErrorCode.INVALID_FAVORITES_LINK_ID)
    }

    "return error when link has invalid format" in {
      // given
      val invalidLinkId = ""
      val request =
        GetSitesRequest
          .newBuilder()
          .setLinkId(invalidLinkId)
          .build()

      // when
      val response = favoritesService.getSites(request)

      // then
      response.hasError should be(true)
      response.getError.getCode should be(ErrorCode.INVALID_FAVORITES_LINK_ID)
    }

  }

  "CreateVillagesShortLink" should {

    "create valid link ID when several villages was received" in {
      // given
      val villageIds = Seq(
        57547L,
        1632667L,
        73030L
      )

      // when
      val linkIdResponse = favoritesService.createVillagesShortLink(
        CreateVillagesShortLinkRequest
          .newBuilder()
          .addAllVillageIds(villageIds.map(Long.box).asJava)
          .build()
      )

      // then
      linkIdResponse.hasResponse should be(true)

      val linkId = linkIdResponse.getResponse.getLinkId
      linkId.nonEmpty should be(true)

      // when
      val villageIdsResponse = favoritesService.getVillages(
        GetVillagesRequest
          .newBuilder()
          .setLinkId(linkId)
          .build()
      )

      // then
      villageIdsResponse.hasResponse should be(true)

      val receivedVillageIds = villageIdsResponse.getResponse.getVillageIdsList.asScala
      receivedVillageIds should contain allElementsOf (villageIds)
    }

    "return error when received empty village list" in {
      // given
      val emptyVillageIdsRequest =
        CreateVillagesShortLinkRequest
          .newBuilder()
          .build()

      // when
      val response = favoritesService.createVillagesShortLink(emptyVillageIdsRequest)

      // then
      response.hasError should be(true)
      response.getError.getCode should be(ErrorCode.EMPTY_FAVORITES_VILLAGE_IDS)
    }

    "return error when villages were not found by provided link" in {
      // given
      val nonExistentLinkId = shortLinkGenerator.generateShortLink()
      val request =
        GetVillagesRequest
          .newBuilder()
          .setLinkId(nonExistentLinkId)
          .build()

      // when
      val response = favoritesService.getVillages(request)

      // then
      response.hasError should be(true)
      response.getError.getCode should be(ErrorCode.INVALID_FAVORITES_LINK_ID)
    }

    "return error when link has invalid format" in {
      // given
      val invalidLinkId = "ABCDE0198475"
      val request =
        GetVillagesRequest
          .newBuilder()
          .setLinkId(invalidLinkId)
          .build()

      // when
      val response = favoritesService.getVillages(request)

      // then
      response.hasError should be(true)
      response.getError.getCode should be(ErrorCode.INVALID_FAVORITES_LINK_ID)
    }

  }

}
