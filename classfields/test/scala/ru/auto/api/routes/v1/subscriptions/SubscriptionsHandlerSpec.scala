package ru.auto.api.routes.v1.subscriptions

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import org.mockito.Mockito._
import ru.auto.api.ApiSpec
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients

import scala.concurrent.Future

class SubscriptionsHandlerSpec extends ApiSpec with MockedClients {
  before {
    when(passportClient.createAnonymousSession()(?)).thenReturn(Future.successful(SessionResultGen.next))
  }

  "SubscriptionsHandler" should {
    "unsubscribe by letter link" in {
      when(unsubscribeClient.unsubscribe(?)(?)).thenReturn(Future.unit)

      Put("/1.0/unsubscribe/subscription?token=skd4235") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(unsubscribeClient).unsubscribe(eq("skd4235"))(?)
        }
    }
  }
}
