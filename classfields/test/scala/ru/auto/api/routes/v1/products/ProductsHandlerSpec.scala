package ru.auto.api.routes.v1.products

import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.features.FeatureManager
import ru.auto.api.model.DealerUserRoles
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients
import ru.auto.api.services.cabinet.CheckAccessView
import ru.auto.cabinet.AclResponse.{AccessLevel, ResourceAlias}
import ru.auto.salesman.products.ProductsOuterClass.{ActiveProductNaturalKey, ProductRequest}
import ru.yandex.vertis.feature.model.Feature

class ProductsHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {

  private val checkAccessClientView = CheckAccessView(role = DealerUserRoles.Manager)

  override lazy val featureManager: FeatureManager = mock[FeatureManager]
  when(featureManager.enrichDealerSessionWithGroup).thenReturn {
    new Feature[Boolean] {
      override def name: String = "enrich_dealer_session_with_group"

      override def value: Boolean = true
    }
  }

  "ProductsHandler" should {

    "return 200 on valid request" in {
      val session = DealerSessionResultGen.next
      when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.CREDIT_APPLICATIONS, AccessLevel.READ_WRITE).next
      }
      when(passportClient.getSession(?)(?)).thenReturnF(session)
      when(salesmanClient.createProduct(?)(?)).thenReturnF(())
      val request = ProductRequest
        .newBuilder()
        .setKey(
          ActiveProductNaturalKey
            .newBuilder()
            .setDomain("application-credit")
            .setPayer(s"dealer:${session.getUser.getClientId}")
            .setTarget("cars:new")
            .setProductType("access")
        )
        .build()
      Post("/1.0/products/create", request) ~>
        xAuthorizationHeader ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        route ~>
        check {
          status shouldBe OK
        }
    }

    "return 400 on invalid target" in {
      val session = DealerSessionResultGen.next
      when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.CREDIT_APPLICATIONS, AccessLevel.READ_WRITE).next
      }
      when(passportClient.getSession(?)(?)).thenReturnF(session)
      val request = ProductRequest
        .newBuilder()
        .setKey(
          ActiveProductNaturalKey
            .newBuilder()
            .setDomain("application-credit")
            .setPayer(s"dealer:${session.getUser.getClientId}")
            .setTarget("invalid")
            .setProductType("access")
        )
        .build()
      Post("/1.0/products/create", request) ~>
        xAuthorizationHeader ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        route ~>
        check {
          status shouldBe BadRequest
        }
    }

    "return 401 if payer in request is invalid" in {
      val session = DealerSessionResultGen.next
      when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(DealerUserEssentialsGen.next)
      when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF {
        dealerAccessGroupWithGrantGen(ResourceAlias.CREDIT_APPLICATIONS, AccessLevel.READ_WRITE).next
      }
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(salesmanClient.createProduct(?)(?)).thenReturnF(())
      val request = ProductRequest
        .newBuilder()
        .setKey(
          ActiveProductNaturalKey
            .newBuilder()
            .setDomain("application-credit")
            .setPayer(s"dealer:${session.getUser.getClientId}123")
            .setTarget("cars:new")
            .setProductType("access")
        )
        .build()
      Post("/1.0/products/create", request) ~>
        xAuthorizationHeader ~>
        addHeader("x-session-id", session.getSession.getId) ~>
        route ~>
        check {
          status shouldBe BadRequest
          responseAs[String].contains("Invalid payer for") shouldBe true
        }
    }
  }
}
