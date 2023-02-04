package ru.auto.api.routes.v1.comeback

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Accept
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.ComebackModel.{ComebackExportResponse, ComebackListingResponse}
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.comeback.ComebackManager
import ru.auto.api.model.DealerUserRoles
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients
import ru.auto.api.services.cabinet.CheckAccessView
import ru.auto.cabinet.AclResponse.{AccessLevel, ResourceAlias}
import ru.yandex.vertis.feature.model.Feature

class ComebackHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {

  override lazy val comebackManager = mock[ComebackManager]
  override lazy val featureManager: FeatureManager = mock[FeatureManager]

  val testingHandler = new ComebackHandler(comebackManager, featureManager)

  private val checkAccessClientView = CheckAccessView(role = DealerUserRoles.Client)

  when(featureManager.enrichDealerSessionWithGroup).thenReturn {
    new Feature[Boolean] {
      override def name: String = "enrich_dealer_session_with_group"
      override def value: Boolean = true
    }
  }

  "/1.0/comeback" should {
    "respond with comebacks" in {
      forAll(
        DealerUserRefGen,
        SessionIdGen,
        comebackListingResponseGen,
        comebackListingRequestGen,
        DealerSessionResultGen
      ) { (dealer, sessionId, response, request, dealerSessionResult) =>
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getSession(?)(?)).thenReturnF(dealerSessionResult)
        when(comebackManager.getComebacks(eq(request))(?)).thenReturnF(response)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.COMEBACK, AccessLevel.READ_ONLY).next
        }
        Post(s"/1.0/comeback", request) ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", sessionId.toString) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[ComebackListingResponse] shouldBe response
          }
      }
    }

    "respond with export comebacks" in {
      forAll(
        DealerUserRefGen,
        SessionIdGen,
        comebackExportResponseGen,
        comebackExportRequestGen,
        DealerSessionResultGen
      ) { (dealer, sessionId, response, request, dealerSessionResult) =>
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getSession(?)(?)).thenReturnF(dealerSessionResult)
        when(comebackManager.exportComebacks(eq(request))(?)).thenReturnF(response)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.COMEBACK, AccessLevel.READ_ONLY).next
        }
        Post(s"/1.0/comeback/export", request) ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", sessionId.toString) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            responseAs[ComebackExportResponse] shouldBe response
          }
      }
    }

    "fail on request without acl resource" in {
      forAll(
        DealerUserRefGen,
        SessionIdGen,
        comebackListingResponseGen,
        comebackListingRequestGen,
        DealerSessionResultGen
      ) { (dealer, sessionId, response, request, dealerSessionResult) =>
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
        when(passportClient.getSession(?)(?)).thenReturnF(dealerSessionResult)
        when(comebackManager.getComebacks(eq(request))(?)).thenReturnF(response)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.UNKNOWN_RESOURCE, AccessLevel.READ_ONLY).next
        }
        Post(s"/1.0/comeback", request) ~>
          xAuthorizationHeader ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", sessionId.toString) ~>
          addHeader("x-dealer-id", dealer.clientId.toString) ~>
          route ~>
          check {
            status shouldBe StatusCodes.Forbidden
          }
      }
    }
  }
}
