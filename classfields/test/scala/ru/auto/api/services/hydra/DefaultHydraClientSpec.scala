package ru.auto.api.services.hydra

import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes

/**
  * @author pnaydenov
  */
class DefaultHydraClientSpec extends HttpClientSpec with MockedHttpClient {
  val hydraClient = new DefaultHydraClient(http)

  "HydraClient" should {
    "decrement limiter" in {
      http.expectUrl(GET, s"/api/v2/limiter/test-service/ru/test-component/user-id?limit=100")
      http.respondWith(StatusCodes.OK, "97")

      val result = hydraClient.limiter("test-service", "test-component", "user-id", Some(100)).futureValue
      result shouldEqual 97
    }
  }
}
