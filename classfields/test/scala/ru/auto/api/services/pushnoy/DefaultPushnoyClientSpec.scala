package ru.auto.api.services.pushnoy

import akka.http.scaladsl.model.StatusCodes
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.pushnoy.TransportProtocol
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.Request

class DefaultPushnoyClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with TestRequest {
  val pushnoyClient = new DefaultPushnoyClient(http)

  implicit override val request: Request = super.request

  "DefaultPushnoyClient" should {
    "addTokenInfo" in {
      val tokenInfo = TokenInfoGen.next.copy(platform = TransportProtocol.HMS)
      val deviceId = tokenInfo.uuid
      http.respondWithStatus(StatusCodes.OK)
      http.expectJson(
        s"""{"uuid":"$deviceId","platform":"hms","push_token":"${tokenInfo.pushToken}","hidden":${tokenInfo.hidden}}"""
      )
      pushnoyClient.addTokenInfo(deviceId, tokenInfo).futureValue
    }
  }
}
