package ru.auto.api.routes.v1.search

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito._
import org.scalacheck.Gen
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.{BreadcrumbsResponse, CrossLinksCountResponse, OfferCountResponse, OfferListingResponse, ShowcaseResponse}
import ru.auto.api.managers.catalog.CatalogManager
import ru.auto.api.managers.personalization.PersonalizationManager
import ru.auto.api.managers.searcher.SearcherManager
import ru.auto.api.model.ModelGenerators.{OfferGen, SessionResultGen}
import ru.auto.api.model.searcher.ApiSearchRequest
import ru.auto.api.model.{NoSorting, Paging}
import ru.auto.api.services.MockedClients
import ru.auto.api.ui.UiModel.TristateTumblerGroup._
import ru.auto.api.util.Protobuf

import scala.jdk.CollectionConverters._

/**
  * Created by artvl on 05.04.17.
  */
class SearchHandlerSpec extends ApiSpec with MockedClients {

  override lazy val searcherManager: SearcherManager = mock[SearcherManager]

  override lazy val catalogManager: CatalogManager = mock[CatalogManager]

  override lazy val personalizationManager: PersonalizationManager = mock[PersonalizationManager]

  "/1.0/search/cars/" should {
    "respond with offerListResponse" in {
      val respBuilder = OfferListingResponse.newBuilder()

      respBuilder.addAllOffers(Gen.listOfN(10, OfferGen).next.asJava)

      val response = respBuilder.build

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(searcherManager.getListing(?, ?, ?, ?, ?)(?)).thenReturnF(response)

      Get(s"/1.0/search/cars/") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)

          verify(searcherManager).getListing(?, eq(Paging.Default), eq(NoSorting), ?, ?)(?)
          verify(passportClient).createAnonymousSession()(?)
          verifyNoMoreInteractions(searcherManager, passportClient)
          reset(searcherManager, passportClient)
        }
    }

    "respond with offerListResponse with recommended=true" in {
      val respBuilder = OfferListingResponse.newBuilder()

      respBuilder.addAllOffers(Gen.listOfN(10, OfferGen).next.asJava)

      val response = respBuilder.build

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(personalizationManager.getRecommendedOffersAsSearchResult(?, ?, ?, ?, ?)(?))
        .thenReturnF(response)

      Get(s"/1.0/search/cars/?recommended=true") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)

          verify(personalizationManager).getRecommendedOffersAsSearchResult(?, ?, ?, ?, ?)(?)
          verify(passportClient).createAnonymousSession()(?)
          verifyNoMoreInteractions(personalizationManager, passportClient)
          verifyNoMoreInteractions(searcherClient)
          reset(personalizationManager, passportClient)
        }
    }

    "handle with_autoru_expert=NONE param" in {
      val respBuilder = OfferListingResponse.newBuilder()

      respBuilder.addAllOffers(Gen.listOfN(10, OfferGen).next.asJava)

      val response = respBuilder.build

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(searcherManager.getListing(?, ?, ?, ?, ?)(?)).thenReturnF(response)

      Get(s"/1.0/search/cars/?with_autoru_expert=NONE") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)

          verify(searcherManager).getListing(
            argThat[ApiSearchRequest](v => v.params.getWithAutoruExpert == NONE),
            eq(Paging.Default),
            eq(NoSorting),
            ?,
            ?
          )(?)
          verify(passportClient).createAnonymousSession()(?)
          verifyNoMoreInteractions(searcherManager, passportClient)
          reset(searcherManager, passportClient)
        }
    }

  }

  "/1.0/search/cars/count" should {
    "respond with OfferCountResponse" in {
      val response = OfferCountResponse
        .newBuilder()
        .setCount(54)
        .build

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(searcherManager.getOffersCount(?, ?, ?, ?, ?)(?)).thenReturnF(response)

      Get(s"/1.0/search/cars/count") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)

          verify(searcherManager).getOffersCount(?, ?, ?, ?, ?)(?)
          verify(passportClient).createAnonymousSession()(?)
          verifyNoMoreInteractions(searcherManager, passportClient)
          reset(searcherManager, passportClient)
        }
    }
  }

  "/1.0/search/cars/breacrumbs" should {
    "respond with Breadcrumbs response" in {
      val respBuilder = BreadcrumbsResponse.newBuilder()
      val response = respBuilder.build

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(catalogManager.breadcrumbs(?, ?)(?)).thenReturnF(response)

      Get(s"/1.0/search/cars/breadcrumbs") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)

          verify(catalogManager).breadcrumbs(?, ?)(?)
          verify(passportClient).createAnonymousSession()(?)
          verifyNoMoreInteractions(searcherManager, passportClient)
          reset(searcherManager, passportClient)
        }
    }
  }

  "/1.0/search/cars/showcase" should {
    "respond with Showcase response" in {
      val response = ShowcaseResponse.newBuilder().build

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(searcherManager.showcase(?)(?)).thenReturnF(response)

      Get(s"/1.0/search/cars/showcase") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)

          verify(searcherManager).showcase(?)(?)
          verify(passportClient).createAnonymousSession()(?)
          verifyNoMoreInteractions(searcherManager, passportClient)
          reset(searcherManager, passportClient)
        }
    }
  }

  "/1.0/search/cars/cross-links-count" should {
    "respond with CrossLinksCount response" in {
      val response = CrossLinksCountResponse.newBuilder().build

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(searcherManager.getCrossLinksCounters(?, ?)(?)).thenReturnF(response)

      Get(s"/1.0/search/cars/cross-links-count") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)

          verify(searcherManager).getCrossLinksCounters(?, ?)(?)
          verify(passportClient).createAnonymousSession()(?)
          verifyNoMoreInteractions(searcherManager, passportClient)
          reset(searcherManager, passportClient)
        }
    }
  }
}
