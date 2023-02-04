package ru.auto.api.routes.v1.booking

import akka.http.scaladsl.model.StatusCodes.OK
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.booking.BookingManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.BookingModelGenerators._
import ru.auto.api.model.{CategorySelector, DealerUserRoles, OfferID, Paging}
import ru.auto.api.services.MockedClients
import ru.auto.api.services.cabinet.CheckAccessView
import ru.auto.api.util.ManagerUtils
import ru.auto.booking.api.ApiModel.{GetBookingTermsResponse, ListBookingsResponse}
import ru.auto.cabinet.AclResponse.{AccessLevel, ResourceAlias}
import ru.yandex.vertis.feature.model.Feature

class BookingHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {

  override lazy val bookingManager: BookingManager = mock[BookingManager]

  private val checkAccessClientView = CheckAccessView(role = DealerUserRoles.Client)

  override lazy val featureManager: FeatureManager = mock[FeatureManager]
  when(featureManager.enrichDealerSessionWithGroup).thenReturn {
    new Feature[Boolean] {
      override def name: String = "enrich_dealer_session_with_group"

      override def value: Boolean = true
    }
  }

  "GET /booking" should {

    "respond with bookings listing" in {
      forAll(DealerSessionResultGen, listBookingsResponseGen) { (session, response) =>
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.BOOKING, AccessLevel.READ_ONLY).next
        }
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(
          bookingManager.getBookings(eq(session.getUser.getClientId.toLong), eq(Paging(page = 1, pageSize = 5)))(?)
        ).thenReturnF(response)
        Get("/1.0/booking?page=1&page_size=5") ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
              responseAs[ListBookingsResponse] shouldBe response
            }
          }
      }
    }
  }

  "GET /booking/terms" should {

    "respond with booking terms" in {
      forAll(DealerSessionResultGen, getBookingTermsResponseGen) { (session, response) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(bookingManager.getBookingTerms(eq(CategorySelector.Cars), eq(OfferID.parse("100000-abc")))(?))
          .thenReturnF(response)
        Get("/1.0/booking/terms/cars/100000-abc") ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
              responseAs[GetBookingTermsResponse] shouldBe response
            }
          }
      }
    }
  }

  "PUT /booking/status" should {

    "update status" in {
      forAll(DealerSessionResultGen, updateBookingStatusRequestGen) { (session, request) =>
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.BOOKING, AccessLevel.READ_WRITE).next
        }
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(bookingManager.updateBookingStatus(eq(request), eq(session.getUser.getClientId.toLong))(?))
          .thenReturnF(ManagerUtils.SuccessResponse)
        Put("/1.0/booking/status", request) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe OK
            }
          }
      }
    }
  }
}
