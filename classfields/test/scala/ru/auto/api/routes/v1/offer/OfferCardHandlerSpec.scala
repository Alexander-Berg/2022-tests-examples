package ru.auto.api.routes.v1.offer

import java.util

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import com.google.protobuf.util.Timestamps
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.{Category, ProductPriceInRegion, ProductsPricesInRegions, Section}
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel._
import ru.auto.api.callback.CallbackModel.PhoneCallbackRequest
import ru.auto.api.exceptions.{ComplaintsBadRequestException, DeliveryUpdateFailed, OfferNotFoundException}
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.callback.PhoneCallbackManager
import ru.auto.api.managers.complaints.ComplaintsManager
import ru.auto.api.managers.dealer.DealerManager
import ru.auto.api.managers.delivery.{DeliveryManager, DeliveryRegionProductPriceManager}
import ru.auto.api.managers.offers.{OfferCardManager, OfferStatManager}
import ru.auto.api.managers.tradein.TradeInManager
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.gen.ComplaintsGenerators._
import ru.auto.api.model.gen.DateTimeGenerators
import ru.auto.api.model.searcher.OfferCardAdditionalParams
import ru.auto.api.model.{AutoruProduct, DealerUserRoles, OfferID}
import ru.auto.api.services.MockedClients
import ru.auto.api.services.cabinet.CheckAccessView
import ru.auto.api.ui.UiModel.TristateTumblerGroup
import ru.auto.api.util.{ManagerUtils, Protobuf}
import ru.auto.cabinet.TradeInRequest.TradeInRequestForm
import ru.auto.cabinet.TradeInRequest.TradeInRequestForm._
import ru.yandex.vertis.feature.model.Feature

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 07.03.17
  */
class OfferCardHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {

  override lazy val offerCardManager: OfferCardManager = mock[OfferCardManager]
  override lazy val offerStatManager: OfferStatManager = mock[OfferStatManager]
  override lazy val phoneCallbackManager: PhoneCallbackManager = mock[PhoneCallbackManager]
  override lazy val complaintsManager: ComplaintsManager = mock[ComplaintsManager]
  override lazy val tradeInManager: TradeInManager = mock[TradeInManager]
  override lazy val dealerManager: DealerManager = mock[DealerManager]
  override lazy val deliveryManager: DeliveryManager = mock[DeliveryManager]
  override lazy val productPriceManager: DeliveryRegionProductPriceManager = mock[DeliveryRegionProductPriceManager]
  override lazy val featureManager: FeatureManager = mock[FeatureManager]

  private val checkAccessClientView = CheckAccessView(role = DealerUserRoles.Client)

  when(featureManager.enrichDealerSessionWithGroup).thenReturn {
    new Feature[Boolean] {
      override def name: String = "enrich_dealer_session_with_group"
      override def value: Boolean = true
    }
  }

  "/1.0/offer/{offerId}/complaints" should {
    "post complaint correctly" in {
      val offerId = OfferIDGen.next
      val request = ComplaintsManagerRequestGen.next.copy(instanceId = InstanceOfferIdGen.next)
      when(complaintsManager.createComplaint(?)(?)).thenReturnF(ManagerUtils.SuccessResponse)
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      Post(s"/1.0/offer/cars/$offerId/complaints", request.complaintRequest) ~>
        xAuthorizationHeader ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "respond with 400 when ComplaintsManager fail with ComplaintsBadRequestException" in {
      val offerId = OfferIDGen.next
      val request = ComplaintsManagerRequestGen.next.copy(instanceId = InstanceOfferIdGen.next)
      when(complaintsManager.createComplaint(?)(?)).thenThrowF(new ComplaintsBadRequestException(""))
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      Post(s"/1.0/offer/cars/$offerId/complaints", request.complaintRequest) ~>
        xAuthorizationHeader ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }
  }

  "/offer/cars/{offerId}" should {
    "respond with offer" in {
      val offer = OfferGen.next
      val id = offer.id
      val response = ManagerUtils.successOfferResponse(offer)

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(offerCardManager.getOfferCardResponse(?, ?, ?)(?)).thenReturnF(response)

      Get(s"/1.0/offer/cars/$id") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)
        }
    }

    "respond with offer getting in account params" in {
      val offer = OfferGen.next
      val id = offer.id
      val response = ManagerUtils.successOfferResponse(offer)

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(offerCardManager.getOfferCardResponse(?, ?, ?)(?)).thenReturnF(response)

      Get(s"/1.0/offer/cars/$id?rid=213&geo_radius=200") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)
          verify(offerCardManager).getOfferCardResponse(
            ?,
            ?,
            eq(OfferCardAdditionalParams(List(213), Some(200), TristateTumblerGroup.BOTH))
          )(?)
        }
    }

    "respond with 404 status and OFFER_NOT_FOUND if offer not found" in {
      val id = OfferIDGen.next

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(offerCardManager.getOfferCardResponse(?, ?, ?)(?)).thenReturn(Future.failed(new OfferNotFoundException))

      Get(s"/1.0/offer/cars/$id") ~>
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
        }
    }
  }

  "/1.0/offer/cars/{offerId}/related" should {
    "respond with offerListResponse" in {
      val respBuilder = OfferListingResponse.newBuilder()

      respBuilder.addAllOffers(Gen.listOfN(10, OfferGen).next.asJava)

      val response = respBuilder.build

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(offerCardManager.related(?, ?, ?, ?, ?)(?)).thenReturnF(response)

      val id = OfferIDGen.next

      Get(s"/1.0/offer/cars/$id/related") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)
        }
    }
  }

  "/1.0/offer/cars/{offerId}/calls-stats" should {
    "respond with callsStatsResponse" in {
      val response = {
        val builder = CallsStatsResponse.newBuilder()

        builder.setCount(Gen.posNum[Int].next)
        val lastCallDate = Gen.option(DateTimeGenerators.instantInPast).next
        lastCallDate.foreach(date => builder.setLastCallTimestamp(Timestamps.fromMillis(date.toEpochMilli)))
        builder.build()
      }

      val id = OfferIDGen.next

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(offerStatManager.getLastCallsStatResponse(?, ?)(?)).thenReturnF(response)

      Get(s"/1.0/offer/cars/$id/calls-stats") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
          }
          responseAs[String] shouldBe Protobuf.toJson(response)
        }
    }
  }

  "/1.0/offer/cars/{offerId}/register-callback" should {
    "register phone callback" in {
      val params = PhoneCallbackRequest.newBuilder().setPhone("791611111111").build()
      val offerId = "123-123"
      when(phoneCallbackManager.registerPhoneCallback(?, ?, ?, ?)(?))
        .thenReturnF(ManagerUtils.SuccessResponse)
      Post(s"/1.0/offer/cars/$offerId/register-callback", params) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[String] shouldBe Protobuf.toJson(ManagerUtils.SuccessResponse)
        }
    }
  }

  "POST /1.0/offer/{category}/{id}/trade-in" should {

    val userOfferId = "12345-1234"
    val userOfferMark = "Kia"
    val userOfferModel = "Rio"
    val userOfferMileage = 500000
    val userOfferYear = 2010
    val userOfferPrice = 100000
    val userName = "Vasiya"
    val phoneNumber = "+79175586983"

    val clientOfferId = "1234-456"
    val clientOfferMark = "Porche"
    val clientOfferModel = "r9"
    val clientOfferMileage = 1000
    val clientOfferYear = 2019
    val clientOfferPrice = 9000000

    val form = TradeInRequestForm
      .newBuilder()
      .setUserInfo(
        UserInfo
          .newBuilder()
          .setName(userName)
          .setPhoneNumber(phoneNumber)
      )
      .setClientInfo(
        ClientInfo
          .newBuilder()
          .setClientId(123)
      )
      .setClientOfferInfo(
        OfferInfo
          .newBuilder()
          .setCategory(Category.CARS)
          .setSection(Section.NEW)
          .setOfferId(clientOfferId)
          .setDescription(
            OfferDescription
              .newBuilder()
              .setMark(clientOfferMark)
              .setModel(clientOfferModel)
              .setPrice(clientOfferPrice)
              .setYear(clientOfferYear)
              .setMileage(clientOfferMileage)
          )
      )
      .setUserOfferInfo(
        OfferInfo
          .newBuilder()
          .setOfferId(userOfferId)
          .setCategory(Category.CARS)
          .setDescription(
            OfferDescription
              .newBuilder()
              .setMark(userOfferMark)
              .setModel(userOfferModel)
              .setPrice(userOfferPrice)
              .setYear(userOfferYear)
              .setMileage(userOfferMileage)
          )
      )
      .build()

    "work fine when data is correct and request is anonymous" in {
      reset(cabinetApiClient, tradeInManager)
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(tradeInManager.createTradeInRequest(?)(?)).thenReturnF(ManagerUtils.SuccessResponse)

      Post(s"/1.0/offer/cars/$clientOfferId/trade-in", form) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(tradeInManager).createTradeInRequest(?)(?)
        }
    }

    "return 500 if exception is thrown" in {
      reset(cabinetApiClient, tradeInManager)
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(tradeInManager.createTradeInRequest(?)(?)).thenThrowF(new IllegalStateException())

      Post(s"/1.0/offer/cars/$clientOfferId/trade-in", form) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.InternalServerError
          verify(tradeInManager).createTradeInRequest(?)(?)
        }
    }
  }

  "GET /1.0/offer/{category}/{id}/products-price/region" should {

    "return price for products in given location" in {
      val clientOfferId = "123-4567"
      val latitude = 1.0
      val longitude = 2.0
      val expectedResponse = ProductsPricesInRegions
        .newBuilder()
        .addAllProductsPricesInRegions(
          util.Arrays.asList(
            ProductPriceInRegion
              .newBuilder()
              .setPrice(200)
              .setProduct(AutoruProduct.Premium.toString)
              .setRegionId(123)
              .build(),
            ProductPriceInRegion
              .newBuilder()
              .setPrice(300)
              .setProduct(AutoruProduct.PackageCart.toString)
              .setRegionId(321)
              .build()
          )
        )
        .build()

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(productPriceManager.getActiveProductsPriceInRegion(?, ?, ?, ?)(?))
        .thenReturnF(expectedResponse)

      Get(s"/1.0/offer/cars/$clientOfferId/products-price/region?latitude=$latitude&longitude=$longitude") ~>
        xAuthorizationHeader ~> route ~> check {
        status shouldBe StatusCodes.OK
        verify(productPriceManager).getActiveProductsPriceInRegion(
          eq(OfferID.parse(clientOfferId)),
          eq(Cars),
          eq(1.0),
          eq(2.0)
        )(?)

        responseAs[ProductsPricesInRegions] shouldBe expectedResponse
      }
    }
  }
  "DELETE /1.0/offer/{category}/{id}/delivery" should {

    "return 200" in {
      reset(deliveryManager)
      when(deliveryManager.updateDelivery(?, ?, ?)(?))
        .thenReturnF(ManagerUtils.SuccessResponse)

      val clientOfferId = "1234-456"

      Delete(s"/1.0/offer/cars/$clientOfferId/delivery") ~>
        xAuthorizationHeader ~> route ~> check {
        status shouldBe StatusCodes.OK
        verify(deliveryManager).updateDelivery(
          eq(OfferID.parse(clientOfferId)),
          eq(Cars),
          eq(None)
        )(?)
      }
    }

    "return 400 if bad request" in {
      reset(deliveryManager)
      when(deliveryManager.updateDelivery(?, ?, ?)(?))
        .thenThrowF(new DeliveryUpdateFailed("400"))

      val clientOfferId = "1234-456"

      Delete(s"/1.0/offer/cars/$clientOfferId/delivery") ~>
        xAuthorizationHeader ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        verify(deliveryManager).updateDelivery(?, ?, ?)(?)
      }
    }

    "return 500 if exception is thrown" in {
      reset(deliveryManager)
      when(deliveryManager.updateDelivery(?, ?, ?)(?))
        .thenThrowF(new RuntimeException("test"))

      val clientOfferId = "1234-456"

      Delete(s"/1.0/offer/cars/$clientOfferId/delivery") ~>
        xAuthorizationHeader ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
        verify(deliveryManager).updateDelivery(?, ?, ?)(?)
      }
    }
  }

  "PUT /1.0/offer/{category}/{id}/delivery" should {

    "return 200" in {
      reset(deliveryManager)
      when(deliveryManager.updateDelivery(?, ?, ?)(?))
        .thenReturnF(ManagerUtils.SuccessResponse)

      val clientOfferId = "1234-456"
      val deliveryInfo = DeliveryInfoGen.next

      Put(s"/1.0/offer/cars/$clientOfferId/delivery", deliveryInfo) ~>
        xAuthorizationHeader ~> route ~> check {
        status shouldBe StatusCodes.OK
        verify(deliveryManager).updateDelivery(eq(OfferID.parse(clientOfferId)), eq(Cars), eq(Some(deliveryInfo)))(?)
      }
    }

    "return 400 if bad request" in {
      reset(deliveryManager)
      when(deliveryManager.updateDelivery(?, ?, ?)(?))
        .thenThrowF(new DeliveryUpdateFailed("400"))

      val clientOfferId = "1234-456"
      val deliveryInfo = DeliveryInfoGen.next

      Put(s"/1.0/offer/cars/$clientOfferId/delivery", deliveryInfo) ~>
        xAuthorizationHeader ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
        verify(deliveryManager).updateDelivery(eq(OfferID.parse(clientOfferId)), eq(Cars), eq(Some(deliveryInfo)))(?)
      }
    }

    "return 500 if exception is thrown" in {
      reset(deliveryManager)
      when(deliveryManager.updateDelivery(?, ?, ?)(?))
        .thenThrowF(new RuntimeException("test"))

      val clientOfferId = "1234-456"
      val deliveryInfo = DeliveryInfoGen.next

      Put(s"/1.0/offer/cars/$clientOfferId/delivery", deliveryInfo) ~>
        xAuthorizationHeader ~> route ~> check {
        status shouldBe StatusCodes.InternalServerError
        verify(deliveryManager).updateDelivery(eq(OfferID.parse(clientOfferId)), eq(Cars), eq(Some(deliveryInfo)))(?)
      }
    }
  }

}
