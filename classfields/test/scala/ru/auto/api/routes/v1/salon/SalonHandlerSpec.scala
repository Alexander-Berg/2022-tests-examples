package ru.auto.api.routes.v1.salon

import akka.http.scaladsl.model.StatusCodes
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.SalonResponse
import ru.auto.api.managers.callback.PhoneCallbackManager
import ru.auto.api.managers.dealer.DealerManager
import ru.auto.api.managers.passport.PassportManager
import ru.auto.api.managers.searcher.SearcherManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients
import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.cabinet.AclResponse.{AccessGrants, AccessLevel, ResourceAlias}
import ru.auto.cabinet.ApiModel.SalonInfoResponse

class SalonHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {
  override lazy val phoneCallbackManager: PhoneCallbackManager = mock[PhoneCallbackManager]
  override lazy val dealerManager: DealerManager = mock[DealerManager]
  override lazy val searcherManager: SearcherManager = mock[SearcherManager]
  override lazy val passportManager: PassportManager = mock[PassportManager]

  "/1.0/salon/by-dealer-id/{dealerId}" should {
    "return salon by dealerId" in {
      val dealerId = ReadableStringGen.next
      val salon = SalonGen.next
      val response = SalonResponse.newBuilder().setSalon(salon).build()
      when(dealerManager.getSalonByDealerId(eq(dealerId))(?)).thenReturnF(response)
      val session = SessionResultGen.next
      when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)
      when(passportManager.createAnonymousSession()(?)).thenReturnF(session)

      Get(s"/1.0/salon/by-dealer-id/$dealerId") ~> xAuthorizationHeader ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[SalonResponse] shouldBe response
      }

      verify(dealerManager).getSalonByDealerId(eq(dealerId))(?)
      verify(passportManager).getSessionFromUserTicket()(?)
      reset(passportManager, dealerManager)
    }
  }

  "/1.0/salon/info" should {
    "return salon info" in {
      forAll(SalonInfoResponseGen, DealerSessionResultGen) {
        case (salonInfo, session) =>
          val dealerId = session.getUser.getClientId.toLong

          val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.SALON, AccessLevel.READ_ONLY).next

          val sessionWithGrants = session.toBuilder
            .setAccess {
              AccessGrants
                .newBuilder()
                .setGroup(accessGroup.toBuilder.clearGrants())
                .addAllGrants(accessGroup.getGrantsList)
            }
            .build()

          when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
          when(passportManager.getSessionFromUserTicket()(?)).thenReturnF(None)
          when(dealerManager.getSalonInfo(?)(?)).thenReturnF(salonInfo)

          Get(s"/1.0/salon/info") ~>
            xAuthorizationHeader ~>
            addHeader("x-session-id", sessionWithGrants.getSession.getId) ~>
            route ~>
            check {
              withClue(responseAs[String]) {
                status shouldBe StatusCodes.OK
                responseAs[SalonInfoResponse] shouldBe salonInfo
              }
            }

          verify(dealerManager).getSalonInfo(eq(dealerId))(?)
          reset(passportManager, dealerManager)
      }
    }
  }

}
