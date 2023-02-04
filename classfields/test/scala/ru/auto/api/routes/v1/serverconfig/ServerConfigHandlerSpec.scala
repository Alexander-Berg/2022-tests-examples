package ru.auto.api.routes.v1.serverconfig

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Accept
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.ServerConfigResponse
import ru.auto.api.features.FeatureManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients
import ru.auto.api.util.Protobuf
import ru.yandex.vertis.feature.model.Feature
import ru.auto.api.ResponseModel.ResponseStatus

class ServerConfigHandlerSpec extends ApiSpec with MockedClients {
  override lazy val featureManager = mock[FeatureManager]

  "GET /1.0/server-config" should {
    "work as expected" in {
      val methods = ServerConfigResponseUserAuthenticationMethodsGen.next
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      when(featureManager.userAuthenticationMethods).thenReturn {
        new Feature[ServerConfigResponse.UserAuthenticationMethods] {
          override def name: String = "user_authentication_methods"

          override def value = methods
        }
      }
      Get("/1.0/server-config") ~>
        addHeader(Accept(MediaTypes.`application/json`)) ~>
        xAuthorizationHeader ~>
        route ~>
        check {
          val responseRaw = responseAs[String]
          withClue(responseRaw) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`
            val response = Protobuf.fromJson[ServerConfigResponse](responseRaw)
            response.getStatus shouldBe ResponseStatus.SUCCESS
            response.getUserAuthenticationMethods shouldEqual methods
          }
        }
    }
  }
}
