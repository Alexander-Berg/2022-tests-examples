package ru.auto.api.routes.v1.subscriptions

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import org.mockito.Mockito.{clearInvocations, verify}
import ru.auto.api.ApiSpec
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.model.ModelGenerators.SessionResultGen
import ru.auto.api.services.MockedClients

import scala.concurrent.Future

class DeviceSubscriptionsHandlerSpec extends ApiSpec with MockedClients {
  override lazy val statEventsManager: StatEventsManager = mock[StatEventsManager]

  when(passportClient.createAnonymousSession()(?)).thenReturn(Future.successful(SessionResultGen.next))

  "device/subscriptions" should {
    "respond ok" in {
      when(pushnoyClient.getDeviceDisabledSubscriptions(?)(?)).thenReturn(Future.successful(Set.empty[String]))

      Get(s"/1.0/device/subscriptions") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "device/subscriptions/{name}" should {
    when(pushnoyClient.updateDeviceSubscription(?, ?, ?)(?)).thenReturn(Future.unit)

    for (method <- Seq(POST, PUT)) {
      s"subscribe by ${method.value}" in {
        new RequestBuilder(method)(s"/1.0/device/subscriptions/subscriptionName") ~>
          addHeader(Accept(MediaTypes.`application/json`)) ~>
          xAuthorizationHeader ~>
          route ~>
          check {
            status shouldBe StatusCodes.OK
            verify(pushnoyClient).updateDeviceSubscription(?, eq("subscriptionName"), enable = eq(true))(?)
          }
        clearInvocations(pushnoyClient)
      }
    }

    "unsubscribe" in {
      Delete(s"/1.0/device/subscriptions/subscriptionName") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(pushnoyClient).updateDeviceSubscription(?, eq("subscriptionName"), enable = eq(false))(?)
        }
    }
  }
}
