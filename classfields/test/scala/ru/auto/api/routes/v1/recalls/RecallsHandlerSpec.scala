package ru.auto.api.routes.v1.recalls

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.{RecallsUserCardResponse, RecallsUserCardsResponse}
import ru.auto.api.managers.recalls.UserCardsManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients
import ru.auto.api.util.ManagerUtils

class RecallsHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {
  override lazy val userCardsManager: UserCardsManager = mock[UserCardsManager]

  private val vin = "Z8T4C5S19BM005269"
  private val sessionId = SessionIdGen.next

  private val defaultHeaders = xAuthorizationHeader ~>
    addHeader("x-session-id", sessionId.toString) ~>
    addHeader(Accept(MediaTypes.`application/json`))

  when(passportClient.getSession(?)(?)).thenReturnF(SessionResultGen.next)

  "RecallsHandler" should {
    "return cards" in {
      when(userCardsManager.get(?)(?)).thenReturnF(RecallsUserCardsResponse.newBuilder().build())

      Get("/1.0/recalls/user-cards") ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
        }
    }

    "add card" in {
      when(userCardsManager.add(?)(?)).thenReturnF(RecallsUserCardResponse.newBuilder().build())

      Put(s"/1.0/recalls/user-cards?vin_or_license_plate=$vin") ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
        }
    }

    "remove card" in {
      when(userCardsManager.delete(?)(?)).thenReturnF(ManagerUtils.SuccessResponse)

      Delete(s"/1.0/recalls/user-cards/1") ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
        }
    }

    "subscribe card" in {
      when(userCardsManager.subscribe(?)(?)).thenReturnF(ManagerUtils.SuccessResponse)

      Put(s"/1.0/recalls/user-cards/1/subscription") ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
        }
    }

    "unsubscribe card" in {
      when(userCardsManager.unsubscribe(?)(?)).thenReturnF(ManagerUtils.SuccessResponse)

      Delete(s"/1.0/recalls/user-cards/1/subscription") ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
        }
    }
  }
}
