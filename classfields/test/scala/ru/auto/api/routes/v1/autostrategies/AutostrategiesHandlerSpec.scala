package ru.auto.api.routes.v1.autostrategies

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes.{Forbidden, OK, Unauthorized}
import akka.http.scaladsl.model.headers.Accept
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.RequestModel.{AddAutostrategiesRequest, AutostrategyIdsList}
import ru.auto.api.ResponseModel.{DeleteAutostrategiesResponse, PutAutostrategiesResponse}
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.autostrategies.AutostrategiesManager
import ru.auto.api.model.DealerUserRoles
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.autostrategies._
import ru.auto.api.model.gen.SalesmanModelGenerators._
import ru.auto.api.services.MockedClients
import ru.auto.api.services.cabinet.CheckAccessView
import ru.auto.api.util.Protobuf
import ru.auto.cabinet.AclResponse.{AccessLevel, ResourceAlias}
import ru.yandex.vertis.feature.model.Feature

import scala.jdk.CollectionConverters._

class AutostrategiesHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {

  override lazy val autostrategiesManager: AutostrategiesManager = mock[AutostrategiesManager]
  override lazy val featureManager: FeatureManager = mock[FeatureManager]

  private val checkAccessAgencyView =
    CheckAccessView(role = DealerUserRoles.Agency)

  when(featureManager.enrichDealerSessionWithGroup).thenReturn {
    new Feature[Boolean] {
      override def name: String = "enrich_dealer_session_with_group"
      override def value: Boolean = true
    }
  }

  "PUT /autostrategies" should {

    "respond with OK to dealer" in {
      forAll(Gen.listOf(AutostrategyGen), DealerSessionResultGen) { (autostrategies, session) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
        }
        when(autostrategiesManager.putAutostrategies(eq(autostrategies))(?)).thenReturnF(putAutostrategiesResponse)
        val request = AddAutostrategiesRequest.newBuilder().addAllAutostrategies(autostrategies.asJava).build()
        Put("/1.0/autostrategies", request) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            val result = responseAs[String]
            withClue(result) {
              status shouldBe OK
              val proto = Protobuf.fromJson[PutAutostrategiesResponse](result)
              proto shouldBe putAutostrategiesResponse
            }
          }
      }
    }

    "respond with 403 to dealer on no access" in {
      forAll(Gen.listOf(AutostrategyGen), DealerSessionResultGen) { (autostrategies, session) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_ONLY).next
        }
        when(autostrategiesManager.putAutostrategies(eq(autostrategies))(?)).thenReturnF(putAutostrategiesResponse)
        val request = AddAutostrategiesRequest.newBuilder().addAllAutostrategies(autostrategies.asJava).build()
        Put("/1.0/autostrategies", request) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            val result = responseAs[String]
            withClue(result) {
              status shouldBe Forbidden
            }
          }
      }
    }

    "respond with OK to agency" in {
      forAll(Gen.listOf(AutostrategyGen), DealerUserRefGen, SessionResultGen) { (autostrategies, dealer, session) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessAgencyView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
        }
        when(autostrategiesManager.putAutostrategies(eq(autostrategies))(?)).thenReturnF(putAutostrategiesResponse)
        val request = AddAutostrategiesRequest.newBuilder().addAllAutostrategies(autostrategies.asJava).build()
        Put("/1.0/autostrategies", request) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("x-uid", "12345") ~>
          addHeader("X-Dealer-ID", dealer.clientId.toString) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            val result = responseAs[String]
            withClue(result) {
              status shouldBe OK
              val proto = Protobuf.fromJson[PutAutostrategiesResponse](result)
              proto shouldBe putAutostrategiesResponse
            }
          }
      }
    }

    "respond with 401 to user" in {
      forAll(Gen.listOf(AutostrategyGen), SessionResultGen) { (autostrategies, session) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        val request = AddAutostrategiesRequest.newBuilder().addAllAutostrategies(autostrategies.asJava).build()
        Put("/1.0/autostrategies", request) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            status shouldBe Unauthorized
          }
      }
    }
  }

  "PUT /autostrategies/delete" should {

    "respond with OK to dealer" in {
      forAll(Gen.listOf(AutostrategyIdGen), DealerSessionResultGen) { (ids, session) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
        }
        when(autostrategiesManager.deleteAutostrategies(eq(ids))(?)).thenReturnF(deleteAutostrategiesResponse)
        val request = AutostrategyIdsList.newBuilder().addAllIds(ids.asJava).build()
        Put("/1.0/autostrategies/delete", request) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            val result = responseAs[String]
            withClue(result) {
              status shouldBe OK
              val proto = Protobuf.fromJson[DeleteAutostrategiesResponse](result)
              proto shouldBe deleteAutostrategiesResponse
            }
          }
      }
    }

    "respond with OK to agency" in {
      forAll(Gen.listOf(AutostrategyIdGen), DealerUserRefGen, SessionResultGen) { (ids, dealer, session) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessAgencyView)
        when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
        when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
          dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_WRITE).next
        }
        when(autostrategiesManager.deleteAutostrategies(eq(ids))(?)).thenReturnF(deleteAutostrategiesResponse)
        val request = AutostrategyIdsList.newBuilder().addAllIds(ids.asJava).build()
        Put("/1.0/autostrategies/delete", request) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          addHeader("X-Dealer-ID", dealer.clientId.toString) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            val result = responseAs[String]
            withClue(result) {
              status shouldBe OK
              val proto = Protobuf.fromJson[DeleteAutostrategiesResponse](result)
              proto shouldBe deleteAutostrategiesResponse
            }
          }
      }
    }

    "respond with 401 to user" in {
      forAll(Gen.listOf(AutostrategyIdGen), SessionResultGen) { (ids, session) =>
        when(passportClient.getSession(?)(?)).thenReturnF(session)
        val request = AutostrategyIdsList.newBuilder().addAllIds(ids.asJava).build()
        Put("/1.0/autostrategies/delete", request) ~>
          addHeader(Accept(`application/json`)) ~>
          addHeader("x-session-id", session.getSession.getId) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            status shouldBe Unauthorized
          }
      }
    }
  }
}
