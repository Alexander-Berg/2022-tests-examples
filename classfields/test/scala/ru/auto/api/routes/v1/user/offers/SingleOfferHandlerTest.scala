package ru.auto.api.routes.v1.user.offers

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, MediaTypes, StatusCodes}
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.Multiposting.Classified.ClassifiedName
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.ApiSuite
import ru.auto.api.RequestModel.OfferHideRequest
import ru.auto.api.ResponseModel._
import ru.auto.api.buyers.BuyersModel.PredictionsList
import ru.auto.api.exceptions._
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.offers.OffersManager
import ru.auto.api.managers.product.ProductManager
import ru.auto.api.model.CategorySelector.{All, Cars}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.gen.SalesmanModelGenerators.SalesmanDomainGen
import ru.auto.api.model.salesman.Prolongable
import ru.auto.api.model.{ActivationPrice, CategorySelector, ModelGenerators}
import ru.auto.api.services.{MockedClients, MockedPassport}
import ru.auto.api.util.{ManagerUtils, Protobuf}
import ru.auto.cabinet.AclResponse.{AccessLevel, ResourceAlias}
import ru.yandex.vertis.feature.model.Feature

import scala.annotation.nowarn
import scala.util.control.NoStackTrace

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 07.02.17
  */
@nowarn("cat=lint-infer-any")
class SingleOfferHandlerTest
  extends ApiSuite
  with MockedClients
  with MockedPassport
  with ScalaCheckPropertyChecks
  with OptionValues {

  before {
    reset(passportManager, productManager)
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
  }

  after {
    verifyNoMoreInteractions(passportManager, productManager)
  }

  val selector: CategorySelector = All

  override lazy val featureManager: FeatureManager = mock[FeatureManager]
  override lazy val offersManager: OffersManager = mock[OffersManager]
  override lazy val productManager: ProductManager = mock[ProductManager]

  when(featureManager.oldOptionsSearchMapping).thenReturn {
    new Feature[Boolean] {
      override def name: String = "default_any_stock_search_for_new_cars"
      override def value: Boolean = false
    }
  }

  when(featureManager.enrichDealerSessionWithGroup).thenReturn {
    new Feature[Boolean] {
      override def name: String = "enrich_dealer_session_with_group"
      override def value: Boolean = true
    }
  }

  private def responseFor(offer: Offer) = {
    OfferResponse
      .newBuilder()
      .setOffer(offer)
      .setStatus(ResponseStatus.SUCCESS)
      .build()
  }

  private val successResponse = {
    SuccessResponse
      .newBuilder()
      .setStatus(ResponseStatus.SUCCESS)
      .build()
  }

  private val activationResponse = {
    ActivationResponse
      .newBuilder()
      .setStatus(ResponseStatus.SUCCESS)
      .build()
  }

  test(s"Read offer (selector = $selector, not authorized)") {
    val offerId = ModelGenerators.OfferIDGen.next

    when(passportManager.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
    when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)

    Get(s"/1.0/user/offers/$selector/$offerId") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[String] should matchJson("""{
                                              |  "error": "NO_AUTH",
                                              |  "status": "ERROR",
                                              |  "detailed_error": "Need authentication"
                                              |}""".stripMargin)

        verifyNoMoreInteractions(offersManager)
        verify(passportManager).createAnonymousSession()(?)
        verify(passportManager).getSessionFromUserTicket()(?)
      }
  }

  test(s"Read offer (selector = $selector, no token)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    Get(s"/1.0/user/offers/$selector/$offerId") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.Forbidden
        }
        responseAs[String] should matchJson(
          """{
            |  "error": "NO_AUTH",
            |  "status": "ERROR",
            |  "detailed_error": "Unknown application. Please provide valid token in X-Authorization header"
            |}""".stripMargin
        )

        verifyNoMoreInteractions(offersManager)
      }
  }

  test(s"Read offer (selector = $selector, authorized)") {
    val offer = ModelGenerators.PrivateOfferGen.next
    val response = responseFor(offer)

    val offerId = offer.id
    val user = offer.privateUserRef

    when(offersManager.getOffer(?, ?, ?)(?))
      .thenReturnF(response)

    Get(s"/1.0/user/offers/$selector/$offerId") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }

        verify(offersManager).getOffer(eq(selector), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test(s"Read same offer (selector = $selector)") {
    val offer = ModelGenerators.PrivateOfferGen.next
    val response = responseFor(offer)

    val offerId = offer.id
    val user = offer.privateUserRef

    when(offersManager.getSameOffer(?, ?, ?)(?))
      .thenReturnF(Some(response))

    Get(s"/1.0/user/offers/$selector/$offerId/same") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }

        verify(offersManager).getSameOffer(eq(user), eq(selector), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test(s"Read same offer (selector = $selector) when offer doesn't exists") {
    val offer = ModelGenerators.PrivateOfferGen.next

    val offerId = offer.id
    val user = offer.privateUserRef

    when(offersManager.getSameOffer(?, ?, ?)(?))
      .thenReturnF(None)

    Get(s"/1.0/user/offers/$selector/$offerId/same") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.NoContent
          contentType shouldBe ContentTypes.NoContentType
        }

        verify(offersManager).getSameOffer(eq(user), eq(selector), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test(s"Read offer (selector = $selector, authorized, as dealer)") {
    val offer = ModelGenerators.DealerOfferGen.next

    val response = responseFor(offer)
    val offerId = offer.id
    val user = ModelGenerators.PrivateUserRefGen.next
    val dealer = offer.dealerUserRef

    when(offersManager.getOffer(?, ?, ?)(?))
      .thenReturnF(response)
    when(passportManager.getClientId(?)(?))
      .thenReturnF(Some(dealer))
    when(passportManager.getClientGroup(?)(?))
      .thenReturnF(Some("test_group"))
    when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
      dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_ONLY).next
    }

    Get(s"/1.0/user/offers/$selector/$offerId") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }

        verify(offersManager).getOffer(eq(selector), eq(dealer), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
        verify(passportManager).getClientGroup(eq(user))(?)
        verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
      }
  }

  test(s"Read offer orig photos (selector = $selector, no token)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    Get(s"/1.0/user/offers/$selector/$offerId/orig-photo-urls") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.Forbidden
        }
        responseAs[String] should matchJson(
          """{
            |  "error": "NO_AUTH",
            |  "status": "ERROR",
            |  "detailed_error": "Unknown application. Please provide valid token in X-Authorization header"
            |}""".stripMargin
        )

        verifyNoMoreInteractions(offersManager)
      }
  }

  test(s"Read offer orig photos (selector = $selector, authorized)") {
    val offer = ModelGenerators.PrivateOfferGen.next
    val response = ModelGenerators.OfferOrigPhotosResponseGen.next

    val offerId = offer.id
    val user = offer.privateUserRef

    when(offersManager.getOfferOrigPhotoUrlList(?, ?)(?))
      .thenReturnF(response)

    Get(s"/1.0/user/offers/$selector/$offerId/orig-photo-urls") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }

        verify(offersManager).getOfferOrigPhotoUrlList(eq(selector), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test(s"Offer not found (selector = $selector, authorized)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.getOffer(?, ?, ?)(?))
      .thenThrowF(new OfferNotFoundException)

    Get(s"/1.0/user/offers/$selector/$offerId") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
        }

        responseAs[String] should matchJson("""{
                                              |  "error": "OFFER_NOT_FOUND",
                                              |  "status": "ERROR",
                                              |  "detailed_error": "OFFER_NOT_FOUND"
                                              |}""".stripMargin)

        verify(offersManager).getOffer(eq(selector), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test(s"Offer read failure (selector = $selector)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.getOffer(?, ?, ?)(?))
      .thenThrowF(new RuntimeException("Vos failure") with NoStackTrace)

    Get(s"/1.0/user/offers/$selector/$offerId") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
                                              |  "error": "UNKNOWN_ERROR",
                                              |  "status": "ERROR",
                                              |  "detailed_error": "Vos failure"
                                              |}""".stripMargin)

        verify(offersManager).getOffer(eq(selector), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test(s"Offer orig photos not found (selector = $selector, authorized)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.getOfferOrigPhotoUrlList(?, ?)(?))
      .thenThrowF(new OfferNotFoundException)

    Get(s"/1.0/user/offers/$selector/$offerId/orig-photo-urls") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
        }

        responseAs[String] should matchJson("""{
                                              |  "error": "OFFER_NOT_FOUND",
                                              |  "status": "ERROR",
                                              |  "detailed_error": "OFFER_NOT_FOUND"
                                              |}""".stripMargin)

        verify(offersManager).getOfferOrigPhotoUrlList(eq(selector), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test(s"Offer orig photos read failure (selector = $selector)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.getOfferOrigPhotoUrlList(?, ?)(?))
      .thenThrowF(new RuntimeException("Vos failure") with NoStackTrace)

    Get(s"/1.0/user/offers/$selector/$offerId/orig-photo-urls") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
                                              |  "error": "UNKNOWN_ERROR",
                                              |  "status": "ERROR",
                                              |  "detailed_error": "Vos failure"
                                              |}""".stripMargin)

        verify(offersManager).getOfferOrigPhotoUrlList(eq(selector), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Incorrect category") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    Get(s"/1.0/user/offers/cars4/$offerId") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
                                              |  "error": "BAD_REQUEST",
                                              |  "status": "ERROR"
                                              |}""".stripMargin)
        (Json.parse(responseAs[String]) \ "detailed_error").as[String] should
          include("Unknown category selector: [cars4]. Known values: cars, moto, trucks, all")

        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Incorrect offer id") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val requestId = "xxx"

    Get(s"/1.0/user/offers/cars/bad-id") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-request-id", requestId) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson(s"""{
                                               |  "error": "BAD_REQUEST",
                                               |  "status": "ERROR"
                                               |}""".stripMargin)
        (Json.parse(responseAs[String]) \ "detailed_error").as[String] should include("Incorrect offer id: [bad-id]")
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Actualize offer") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.actualize(?, ?, ?)(?))
      .thenReturnF(successResponse)

    Post(s"/1.0/user/offers/cars/$offerId/actualize") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
                                              |  "status": "SUCCESS"
                                              |}""".stripMargin)

        verify(offersManager).actualize(eq(Cars), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Actualize offer (Offer not found)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.actualize(?, ?, ?)(?))
      .thenThrowF(new OfferNotFoundException)

    Post(s"/1.0/user/offers/cars/$offerId/actualize") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
            |  "error": "OFFER_NOT_FOUND",
            |  "status": "ERROR",
            |  "detailed_error": "OFFER_NOT_FOUND"
            |}""".stripMargin)

        verify(offersManager).actualize(eq(Cars), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Archive offer") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.archive(?, ?, ?)(?))
      .thenReturnF(successResponse)

    Delete(s"/1.0/user/offers/cars/$offerId") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
                                              |  "status": "SUCCESS"
                                              |}""".stripMargin)

        verify(offersManager).archive(eq(Cars), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Archive offer (Offer not found)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.archive(?, ?, ?)(?))
      .thenThrowF(new OfferNotFoundException)

    Delete(s"/1.0/user/offers/cars/$offerId") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
            |  "error": "OFFER_NOT_FOUND",
            |  "status": "ERROR",
            |  "detailed_error": "OFFER_NOT_FOUND"
            |}""".stripMargin)

        verify(offersManager).archive(eq(Cars), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Hide offer") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next
    val hideRequest = ModelGenerators.OfferHideRequestGen.next

    when(offersManager.hide(?, ?, ?, ?)(?))
      .thenReturnF(successResponse)

    Post(s"/1.0/user/offers/cars/$offerId/hide", hideRequest) ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
                                              |  "status": "SUCCESS"
                                              |}""".stripMargin)

        verify(offersManager).hide(eq(Cars), eq(user), eq(offerId), eq(hideRequest))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Hide offer (Offer not found)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next
    val hideRequest = ModelGenerators.OfferHideRequestGen.next

    when(offersManager.hide(?, ?, ?, ?)(?))
      .thenThrowF(new OfferNotFoundException)

    Post(s"/1.0/user/offers/cars/$offerId/hide", hideRequest) ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
                                              |  "error": "OFFER_NOT_FOUND",
                                              |  "status": "ERROR",
                                              |  "detailed_error": "OFFER_NOT_FOUND"
                                              |}""".stripMargin)

        verify(offersManager).hide(eq(Cars), eq(user), eq(offerId), eq(hideRequest))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Hide offer using PUT") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.hide(?, ?, ?, ?)(?))
      .thenReturnF(successResponse)

    Put(s"/1.0/user/offers/cars/$offerId/hide") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
            |  "status": "SUCCESS"
            |}""".stripMargin)

        verify(offersManager).hide(eq(Cars), eq(user), eq(offerId), eq(OfferHideRequest.getDefaultInstance))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Hide offer using PUT (Offer not found)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.hide(?, ?, ?, ?)(?))
      .thenThrowF(new OfferNotFoundException)

    Put(s"/1.0/user/offers/cars/$offerId/hide") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
                                              |  "error": "OFFER_NOT_FOUND",
                                              |  "status": "ERROR",
                                              |  "detailed_error": "OFFER_NOT_FOUND"
                                              |}""".stripMargin)

        verify(offersManager).hide(eq(Cars), eq(user), eq(offerId), eq(OfferHideRequest.getDefaultInstance))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Activate offer (activated)") {
    val offer = ModelGenerators.PrivateOfferGen.next

    val offerId = offer.id
    val user = offer.privateUserRef

    when(offersManager.activate(?, ?, ?)(?))
      .thenReturnF(activationResponse)

    Post(s"/1.0/user/offers/cars/$offerId/activate?session_id=fake_session") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
                                              |  "status": "SUCCESS"
                                              |}""".stripMargin)

        verify(offersManager).activate(eq(Cars), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Activate offer (Offer not found)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.activate(?, ?, ?)(?))
      .thenThrowF(new OfferNotFoundException)

    Post(s"/1.0/user/offers/cars/$offerId/activate?session_id=fake_session") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
            |  "error": "OFFER_NOT_FOUND",
            |  "status": "ERROR",
            |  "detailed_error": "OFFER_NOT_FOUND"
            |}""".stripMargin)

        verify(offersManager).activate(eq(Cars), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Activate offer (Offer too old)") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.activate(?, ?, ?)(?))
      .thenThrowF(new OfferTooOldException)

    Post(s"/1.0/user/offers/cars/$offerId/activate?session_id=fake_session") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.Conflict
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
            |  "error": "OLD_OFFER",
            |  "status": "ERROR",
            |  "detailed_error": "OLD_OFFER"
            |}""".stripMargin)

        verify(offersManager).activate(eq(Cars), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Activate offer (payment needed)") {
    val offer = ModelGenerators.PrivateOfferGen.next

    val offerId = offer.id
    val user = offer.privateUserRef

    when(offersManager.activate(?, ?, ?)(?))
      .thenThrowF(new PaymentNeeded(ActivationPrice(399, 399, None, None)))

    Post(s"/1.0/user/offers/cars/$offerId/activate?session_id=fake_session") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.PaymentRequired
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
            |  "price_info": {
            |    "price": 399.0,
            |    "currency": "RUR"
            |  },
            |  "error": "PAYMENT_NEEDED",
            |  "status": "ERROR",
            |  "detailed_error": "PAYMENT_NEEDED"
            |}""".stripMargin)

        verify(offersManager).activate(eq(Cars), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Activate offer (have similar offer)") {
    val offer = ModelGenerators.PrivateOfferGen.next
    val offer2 = ModelGenerators.PrivateOfferGen.next

    val offerId = offer.id
    val user = offer.privateUserRef

    when(offersManager.activate(?, ?, ?)(?))
      .thenThrowF(new HaveSimilarOffer(offer2, ActivationPrice(399, 399, None, None)))

    Post(s"/1.0/user/offers/cars/$offerId/activate?session_id=fake_session") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.PaymentRequired
          contentType shouldBe ContentTypes.`application/json`
        }

        responseAs[String] should matchJson(s"""{
                                               |  "similar_offer": { "id": "${offer2.getId}" },
                                               |  "price_info": { "price": 399.0, "currency": "RUR" },
                                               |  "error": "HAVE_SIMILAR_OFFER",
                                               |  "status": "ERROR",
                                               |  "detailed_error": "HAVE_SIMILAR_OFFER"
                                               |}""".stripMargin)

        verify(offersManager).activate(eq(Cars), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("Activate offer (not enough funds)") {
    val offer = ModelGenerators.PrivateOfferGen.next

    val offerId = offer.id
    val user = offer.privateUserRef

    when(offersManager.activate(?, ?, ?)(?)).thenThrowF(new NotEnoughFundsOnAccount)

    Post(s"/1.0/user/offers/cars/$offerId/activate?session_id=fake_session") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.PaymentRequired
          contentType shouldBe ContentTypes.`application/json`
        }

        responseAs[String] should matchJson(s"""{
                                               |  "error": "NOT_ENOUGH_FUNDS_ON_ACCOUNT",
                                               |  "status": "ERROR",
                                               |  "detailed_error": "NOT_ENOUGH_FUNDS_ON_ACCOUNT"
                                               |}""".stripMargin)

        verify(offersManager).activate(eq(Cars), eq(user), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("change price") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next
    val priceAttribute = ModelGenerators.PriceAttributeRequestGen.next

    when(offersManager.updatePrice(?, ?, ?, ?)(?))
      .thenReturnF(successResponse)

    Post(s"/1.0/user/offers/cars/$offerId/price", priceAttribute) ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
                                              |  "status": "SUCCESS"
                                              |}""".stripMargin)

        verify(offersManager).updatePrice(eq(Cars), eq(user), eq(offerId), eq(priceAttribute))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("change attribute") {
    val offerId = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next
    val attribute = ModelGenerators.AttributeRequestGen.next

    when(offersManager.updateAttribute(?, ?, ?, ?)(?))
      .thenReturnF(successResponse)

    Post(s"/1.0/user/offers/cars/$offerId/attribute", attribute) ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }
        responseAs[String] should matchJson("""{
                                              |  "status": "SUCCESS"
                                              |}""".stripMargin)

        verify(offersManager).updateAttribute(eq(Cars), eq(user), eq(offerId), eq(attribute))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("apply autoru good product to dealer's offer") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      ApplyOneAutoruProductRequestGen
    ) { (category, user, dealer, offerId, request) =>
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
      }
      when(productManager.applyProducts(?, ?, ?, ?, ?, ?)(?)).thenReturnF(successResponse)
      Post(s"/1.0/user/offers/$category/$offerId/products").withEntity(HttpEntity(request.toByteArray)) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(productManager).applyProducts(
            eq(category),
            eq(dealer),
            eq(offerId),
            eq(request),
            eq(ClassifiedName.AUTORU),
            eq(None)
          )(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("pass legacy flag to apply product") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      ApplyOneAutoruProductRequestGen
    ) { (category, user, dealer, offerId, request) =>
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
      }
      when(productManager.applyProducts(?, ?, ?, ?, ?, ?)(?)).thenReturnF(successResponse)
      Post(s"/1.0/user/offers/$category/$offerId/products?&use_legacy=true")
        .withEntity(HttpEntity(request.toByteArray)) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(productManager).applyProducts(
            eq(category),
            eq(dealer),
            eq(offerId),
            eq(request),
            eq(ClassifiedName.AUTORU),
            eq(Some(true))
          )(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("apply non-autoru good product to dealer's offer") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      ApplyOneAutoruProductRequestGen,
      NonAutoruClassifiedNameGen
    ) { (category, user, dealer, offerId, request, classified) =>
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
      }
      when(productManager.applyProducts(?, ?, ?, ?, ?, ?)(?)).thenReturnF(successResponse)
      Post(s"/1.0/user/offers/$category/$offerId/products?classified=${classified.toString}")
        .withEntity(HttpEntity(request.toByteArray)) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(productManager).applyProducts(
            eq(category),
            eq(dealer),
            eq(offerId),
            eq(request),
            eq(classified),
            eq(None)
          )(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("return error on apply product to invalid classified") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      ApplyOneAutoruProductRequestGen,
      ClassifiedNameGen
    ) { (category, user, dealer, offerId, request, c) =>
      val classified = c.toString + "_invalid"
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
      }
      when(productManager.applyProducts(?, ?, ?, ?, ?, ?)(?)).thenReturnF(successResponse)
      Post(s"/1.0/user/offers/$category/$offerId/products?classified=$classified")
        .withEntity(HttpEntity(request.toByteArray)) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
          responseAs[String] should matchJson(
            s"""{
               |  "error" : "BAD_REQUEST",
               |  "status" : "ERROR",
               |  "detailed_error" : "Invalid classified: [$classified]. Available values: [AUTORU, AVITO, DROM]"
               |}
               |""".stripMargin
          )
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("return error on apply product failure") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      ApplyOneAutoruProductRequestGen
    ) { (category, user, dealer, offerId, request) =>
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
      }
      when(productManager.applyProducts(?, ?, ?, ?, ?, ?)(?)).thenThrowF(new NotEnoughFundsOnAccount)
      Post(s"/1.0/user/offers/$category/$offerId/products").withEntity(HttpEntity(request.toByteArray)) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.PaymentRequired
          responseAs[String] should matchJson("""
                                                |{
                                                |  "error": "NOT_ENOUGH_FUNDS_ON_ACCOUNT",
                                                |  "status": "ERROR",
                                                |  "detailed_error": "NOT_ENOUGH_FUNDS_ON_ACCOUNT"
                                                |}""".stripMargin)
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(productManager).applyProducts(
            eq(category),
            eq(dealer),
            eq(offerId),
            eq(request),
            eq(ClassifiedName.AUTORU),
            eq(None)
          )(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("predict-buyers should respond with PredictionsList") {
    forAll(
      ModelGenerators.StrictCategoryGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen
    ) { (category, user, dealer, offerId) =>
      val respBuilder = PredictionsList.newBuilder()
      respBuilder.setOfferId(offerId.toPlain)
      val response = respBuilder.build
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)
      when(offersManager.getBuyerPrediction(?, ?)(?)).thenReturnF(response)

      Get(s"/1.0/user/offers/${category.toString}/${offerId.toPlain}/predict-buyers") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
          verify(offersManager, atLeastOnce).getBuyerPrediction(?, ?)(?)
          responseAs[String] shouldBe Protobuf.toJson(response)

        }
    }
  }

  test("return 405 on trying to invoke PUT /products") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      ApplyOneAutoruProductRequestGen
    ) { (category, user, dealer, offerId, request) =>
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)
      when(productManager.applyProducts(?, ?, ?, ?, ?, ?)(?)).thenReturnF(successResponse)
      Put(s"/1.0/user/offers/$category/$offerId/products").withEntity(HttpEntity(request.toByteArray)) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.MethodNotAllowed
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("delete autoru good product from dealer's offer") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      AutoruVasProductGen
    ) { (category, user, dealer, offerId, product) =>
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
      }
      when(productManager.deleteProduct(?, ?, ?, ?, ?, ?)(?)).thenReturnF(successResponse)
      Delete(s"/1.0/user/offers/$category/$offerId/products?product=${product.salesName}") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(productManager).deleteProduct(
            eq(category),
            eq(dealer),
            eq(offerId),
            eq(product.salesName),
            eq(ClassifiedName.AUTORU),
            eq(None)
          )(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("delete non-autoru good product from dealer's offer") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      AutoruVasProductGen,
      NonAutoruClassifiedNameGen
    ) { (category, user, dealer, offerId, product, classified) =>
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
      }
      when(productManager.deleteProduct(?, ?, ?, ?, ?, ?)(?)).thenReturnF(successResponse)
      Delete(
        s"/1.0/user/offers/$category/$offerId/products?product=${product.salesName}&classified=${classified.toString}"
      ) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(productManager).deleteProduct(
            eq(category),
            eq(dealer),
            eq(offerId),
            eq(product.salesName),
            eq(classified),
            eq(None)
          )(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("return error on delete product on invalid classified") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      AutoruVasProductGen,
      ClassifiedNameGen
    ) { (category, user, dealer, offerId, product, c) =>
      val classified = c.toString + "_invalid"
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
      }
      Delete(s"/1.0/user/offers/$category/$offerId/products?classified=$classified&product=${product.salesName}") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
          responseAs[String] should matchJson(
            s"""{
               |  "error" : "BAD_REQUEST",
               |  "status" : "ERROR",
               |  "detailed_error" : "Invalid classified: [$classified]. Available values: [AUTORU, AVITO, DROM]"
               |}
               |""".stripMargin
          )
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("return error on delete product failure") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      AutoruVasProductGen
    ) { (category, user, dealer, offerId, product) =>
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
      }
      when(productManager.deleteProduct(?, ?, ?, ?, ?, ?)(?)).thenThrowF(new RuntimeException)
      Delete(s"/1.0/user/offers/$category/$offerId/products?product=${product.salesName}") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.InternalServerError
          responseAs[String] should matchJson("""{"status": "ERROR"}""".stripMargin)
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(productManager).deleteProduct(
            eq(category),
            eq(dealer),
            eq(offerId),
            eq(product.salesName),
            eq(ClassifiedName.AUTORU),
            eq(None)
          )(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("pass legacy flag in delete product") {

    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      AutoruVasProductGen
    ) { (category, user, dealer, offerId, product) =>
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
      }
      when(productManager.deleteProduct(?, ?, ?, ?, ?, ?)(?)).thenReturnF(successResponse)
      Delete(s"/1.0/user/offers/$category/$offerId/products?product=${product.salesName}&use_legacy=true") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(productManager).deleteProduct(
            eq(category),
            eq(dealer),
            eq(offerId),
            eq(product.salesName),
            eq(ClassifiedName.AUTORU),
            eq(Some(true))
          )(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("return error on missing product") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen
    ) { (category, user, dealer, offerId) =>
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)
      Delete(s"/1.0/user/offers/$category/$offerId/products") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("return error on multiple products") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.DealerUserRefGen,
      ModelGenerators.OfferIDGen,
      Gen.nonEmptyListOf(AutoruVasProductGen).filter(_.size > 1)
    ) { (category, user, dealer, offerId, products) =>
      reset(passportManager, productManager, cabinetApiClient)
      when(passportManager.getClientId(?)(?)).thenReturnF(Some(dealer))
      when(passportManager.getClientGroup(?)(?)).thenReturnF(Some("test_group"))
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(DealerAccessGroupGen.next)
      Delete(
        s"/1.0/user/offers/$category/$offerId/products?" +
          products.map("product=" + _.salesName).mkString("&")
      ) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
          verify(passportManager).getClientId(eq(user))(?)
          verify(passportManager).getClientGroup(eq(user))(?)
          verify(cabinetApiClient).getAccessGroup(eq("test_group"))(?)
        }
    }
  }

  test("make offer's product prolongable") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.OfferIDGen,
      ProductGen,
      SalesmanDomainGen
    ) { (category, user, offerId, product, domain) =>
      when(productManager.setProlongable(?, ?, ?, Prolongable(eq(true)))(?))
        .thenReturnF(ManagerUtils.SuccessResponse)
      Put(s"/1.0/user/offers/$category/$offerId/product/$product/prolongable?domain=$domain") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(passportManager).getClientId(eq(user))(?)
          verify(productManager).setProlongable(eq(domain), eq(offerId), eq(product), Prolongable(eq(true)))(?)
        }
    }
  }

  test("make offer's product non-prolongable") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.OfferIDGen,
      ProductGen,
      SalesmanDomainGen
    ) { (category, user, offerId, product, domain) =>
      when(productManager.setProlongable(?, ?, ?, Prolongable(eq(false)))(?))
        .thenReturnF(ManagerUtils.SuccessResponse)
      Delete(s"/1.0/user/offers/$category/$offerId/product/$product/prolongable?domain=$domain") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(passportManager).getClientId(eq(user))(?)
          verify(productManager).setProlongable(eq(domain), eq(offerId), eq(product), Prolongable(eq(false)))(?)
        }
    }
  }

  test("respond with 400 if domain missing") {
    forAll(ModelGenerators.SelectorGen, ModelGenerators.PrivateUserRefGen, ModelGenerators.OfferIDGen, ProductGen) {
      (category, user, offerId, product) =>
        Put(s"/1.0/user/offers/$category/$offerId/product/$product/prolongable") ~>
          addHeader(Accept(MediaTypes.`application/json`)) ~>
          addHeader("x-uid", user.uid.toString) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            status shouldBe StatusCodes.BadRequest
            verify(passportManager).getClientId(eq(user))(?)
          }
    }
  }

  test("add panorama") {
    forAll(
      ModelGenerators.StrictCategoryGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.OfferIDGen,
      ModelGenerators.HashGen
    ) { (category, user, offerId, panoramaId) =>
      when(offersManager.addExternalPanorama(?, ?, ?, ?)(?))
        .thenReturnF(ManagerUtils.SuccessResponse)
      when(passportManager.getClientId(?)(?)).thenReturnF(None)
      Put(s"/1.0/user/offers/$category/$offerId/external-panorama/$panoramaId") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(offersManager).addExternalPanorama(eq(category), eq(user), eq(offerId), eq(panoramaId))(?)
          verify(passportManager).getClientId(eq(user))(?)
        }
    }
  }

  test("get panoramas") {
    forAll(
      ModelGenerators.StrictCategoryGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.OfferIDGen
    ) { (category, user, offerId) =>
      when(offersManager.getExternalPanoramas(?, ?, ?)(?))
        .thenReturnF(ExternalPanoramasResponse.newBuilder().build())
      when(passportManager.getClientId(?)(?)).thenReturnF(None)
      Get(s"/1.0/user/offers/$category/$offerId/external-panorama") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(offersManager).getExternalPanoramas(eq(category), eq(user), eq(offerId))(?)
          verify(passportManager).getClientId(eq(user))(?)
        }
    }
  }

  test("Enable classified") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.ActiveMultipostingOfferGen,
      ModelGenerators.ClassifiedNameGen
    ) { (category, user, offer, classified) =>
      reset(offersManager)
      val offerId = offer.id

      when(offersManager.enableClassified(?, ?, ?, ?)(?)).thenReturnF(successResponse)
      when(passportManager.getClientId(?)(?)).thenReturnF(None)

      Put(s"/1.0/user/offers/$category/$offerId/multiposting/${classified.name()}") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(offersManager).enableClassified(eq(offerId), eq(category), eq(user), eq(classified))(?)
          verify(passportManager).getClientId(eq(user))(?)
        }
    }
  }

  test("Disable classified") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.ActiveMultipostingOfferGen,
      ModelGenerators.ClassifiedNameGen
    ) { (category, user, offer, classified) =>
      reset(offersManager)
      val offerId = offer.id

      when(offersManager.disableClassified(?, ?, ?, ?)(?)).thenReturnF(successResponse)
      when(passportManager.getClientId(?)(?)).thenReturnF(None)

      Delete(s"/1.0/user/offers/$category/$offerId/multiposting/${classified.name()}") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson("""{"status": "SUCCESS"}""")
          verify(offersManager).disableClassified(eq(offerId), eq(category), eq(user), eq(classified))(?)
          verify(passportManager).getClientId(eq(user))(?)
        }
    }
  }

  test("Return error on incorrect classified name ") {
    forAll(
      ModelGenerators.SelectorGen,
      ModelGenerators.PrivateUserRefGen,
      ModelGenerators.ActiveMultipostingOfferGen
    ) { (category, user, offer) =>
      reset(offersManager)
      val offerId = offer.id

      when(offersManager.disableClassified(?, ?, ?, ?)(?)).thenReturnF(successResponse)
      when(passportManager.getClientId(?)(?)).thenReturnF(None)

      Put(s"/1.0/user/offers/$category/$offerId/multiposting/unknown") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
          verify(offersManager, never()).disableClassified(eq(offerId), eq(category), eq(user), ?)(?)
          verify(passportManager, atLeastOnce()).getClientId(eq(user))(?)
        }

      Delete(s"/1.0/user/offers/$category/$offerId/multiposting/random_value") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        addHeader("x-uid", user.uid.toString) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.BadRequest
          verify(offersManager, never()).disableClassified(eq(offerId), eq(category), eq(user), ?)(?)
          verify(passportManager, atLeastOnce()).getClientId(eq(user))(?)
        }
    }
  }
}
