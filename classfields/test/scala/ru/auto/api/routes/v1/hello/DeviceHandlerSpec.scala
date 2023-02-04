package ru.auto.api.routes.v1.hello

import akka.http.scaladsl.model.headers.{Accept, RawHeader}
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.HelloResponse
import ru.auto.api.ResponseModel.HelloResponse.ExperimentsConfig
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelGenerators.{SecretSignGen, SessionResultGen}
import ru.auto.api.services.MockedClients
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._

import scala.concurrent.Future

/**
  * Created by mcsim-gr on 26.06.17.
  */
class DeviceHandlerSpec extends ApiSpec with MockedClients {

  override lazy val statEventsManager: StatEventsManager = mock[StatEventsManager]

  private val defaultExperimentsConfig = ExperimentsConfig.getDefaultInstance

  "device/hello" should {
    "respond ok" in {
      val device = ModelGenerators.DeviceGen.next
      val helloRequest = ModelGenerators.helloRequestGen(device).next
      val uaasHeaders = Seq(
        RawHeader("x-yandex-expflags", "11111,22222"),
        RawHeader("x-yandex-expboxes", "11111,0,-1;22222,0,-1")
      )

      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(pushnoyClient.addDeviceInfo(?, ?)(?)).thenReturn(Future.unit)
      when(pushnoyClient.detachUsersFromDevice(?)(?)).thenReturn(Future.unit)
      when(statEventsManager.logDeviceHello(?, ?)(?)).thenReturn(Future.unit)
      when(uaasClient.getExperiments(?)(?)).thenReturn(Future.successful((defaultExperimentsConfig, uaasHeaders)))

      featureRegistry.updateFeature(featureManager.hideExteriorPanoramas.name, true)
      featureRegistry.updateFeature(featureManager.useUaaSExperimentsConfig.name, true)

      Post(s"/1.0/device/hello", helloRequest) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = entityAs[HelloResponse]
          response.getActiveServerExperimentsCount shouldBe 1
          response.getActiveServerExperiments(0) shouldBe featureManager.hideExteriorPanoramas.name
          header("x-yandex-expflags") shouldBe Some(RawHeader("x-yandex-expflags", "11111,22222"))
          header("x-yandex-expboxes") shouldBe Some(RawHeader("x-yandex-expboxes", "11111,0,-1;22222,0,-1"))
        }
    }
  }

  "device/push-token" should {
    "respond ok" in {
      when(pushnoyClient.addTokenInfo(?, ?)(?)).thenReturn(Future.unit)
      when(pushnoyClient.attachDeviceToUser(?, ?)(?)).thenReturn(Future.unit)
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      val device = ModelGenerators.DeviceGen.next
      val savePushTokenRequest = ModelGenerators.savePushTokenRequestGen(device).next
      Post(s"/1.0/device/push-token", savePushTokenRequest) ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "device/secret-sign" should {
    "respond ok" in {
      when(pushnoyClient.getSecretSign(?)(?)).thenReturnF(SecretSignGen.next)
      Get(s"/1.0/device/websocket") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
        }
    }
  }
}
