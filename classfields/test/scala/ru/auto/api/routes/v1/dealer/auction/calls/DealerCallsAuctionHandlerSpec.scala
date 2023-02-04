package ru.auto.api.routes.v1.dealer.auction.calls

import akka.http.scaladsl.model.StatusCodes
import org.mockito.Mockito.verify
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.auction.CallAuction.{OfferLeaveAuctionRequest, OfferPlaceBidRequest}
import ru.auto.api.managers.auction.DefaultDealerCallsAuctionManager
import ru.auto.api.managers.passport.PassportManager
import ru.auto.api.model.ModelGenerators.{dealerAccessGroupWithGrantGen, DealerSessionResultGen}
import ru.auto.api.model._
import ru.auto.api.services.MockedClients
import ru.auto.api.services.cabinet.CheckAccessView
import ru.auto.api.services.dealer_aliases.DealerAliasesClient
import ru.auto.cabinet.AclResponse.{AccessGrants, AccessLevel, ResourceAlias}
import ru.auto.dealer_calls_auction.proto.ApiModel.AuctionCurrentState

class DealerCallsAuctionHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {
  override lazy val passportManager: PassportManager = mock[PassportManager]
  override lazy val dealerAliasesClient: DealerAliasesClient = mock[DealerAliasesClient]

  override lazy val dealerCallsAuctionManager: DefaultDealerCallsAuctionManager = mock[DefaultDealerCallsAuctionManager]

  private val checkAccessClientView = CheckAccessView(role = DealerUserRoles.Client)

  "GET /1.0/dealers/auction/offer/{offer_id}/current-state" should {

    "return current auction state" in {
      val session = DealerSessionResultGen.next
      val clientId = session.getUser.getClientId.toLong
      val offerId = OfferID(1, hash = None)

      val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_ONLY).next

      val sessionWithGrants = session.toBuilder.setAccess {
        AccessGrants
          .newBuilder()
          .setGroup(accessGroup.toBuilder.clearGrants())
          .addAllGrants(accessGroup.getGrantsList)
      }.build

      when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
      when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
      when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)

      when(dealerCallsAuctionManager.getCurrentState(?, ?)(?)).thenReturnF(Some(AuctionCurrentState.getDefaultInstance))

      Get(s"/1.0/dealer/auction/offer/$offerId/current-state") ~>
        xAuthorizationHeader ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", clientId.toString) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(dealerCallsAuctionManager).getCurrentState(eq(offerId), eq(AutoruDealer(clientId)))(?)
        }
    }
  }

  "POST /1.0/dealers/auction/offer/{offer_id}/place-bid" should {

    "place bid successfully" in {
      val session = DealerSessionResultGen.next
      val clientId = session.getUser.getClientId.toLong
      val offerId = OfferID(1, hash = None)

      val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_WRITE).next

      val sessionWithGrants = session.toBuilder.setAccess {
        AccessGrants
          .newBuilder()
          .setGroup(accessGroup.toBuilder.clearGrants())
          .addAllGrants(accessGroup.getGrantsList)
      }.build

      val request = OfferPlaceBidRequest
        .newBuilder()
        .setBid(220000)
        .setPreviousBid(210000)
        .build()

      when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
      when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
      when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)

      when(dealerCallsAuctionManager.placeBid(?, ?, ?)(?)).thenReturnF(())

      Post(s"/1.0/dealer/auction/offer/$offerId/place-bid", request) ~>
        xAuthorizationHeader ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", clientId.toString) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(dealerCallsAuctionManager).placeBid(eq(offerId), eq(request), eq(AutoruDealer(clientId)))(?)
        }
    }
  }
  "POST /1.0/dealers/auction/offer/{offer_id}/leave" should {

    "leave auction successfully" in {
      val session = DealerSessionResultGen.next
      val clientId = session.getUser.getClientId.toLong
      val offerId = OfferID(1, hash = None)

      val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.TARIFFS, AccessLevel.READ_WRITE).next

      val sessionWithGrants = session.toBuilder.setAccess {
        AccessGrants
          .newBuilder()
          .setGroup(accessGroup.toBuilder.clearGrants())
          .addAllGrants(accessGroup.getGrantsList)
      }.build

      val request = OfferLeaveAuctionRequest
        .newBuilder()
        .setPreviousBid(22000)
        .build()

      when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
      when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
      when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)

      when(dealerCallsAuctionManager.leaveAuction(?, ?, ?)(?)).thenReturnF(())

      Post(s"/1.0/dealer/auction/offer/$offerId/leave", request) ~>
        xAuthorizationHeader ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        addHeader("x-dealer-id", clientId.toString) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(dealerCallsAuctionManager).leaveAuction(eq(offerId), eq(request), eq(AutoruDealer(clientId)))(?)
        }
    }
  }
}
