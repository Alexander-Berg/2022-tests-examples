package ru.auto.api.services.hydra

import org.scalacheck.Gen
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.services.HttpClientSuite

/**
  * @author pnaydenov
  */
class DefaultHydraClientIntTest extends HttpClientSuite {

  override protected def config: HttpClientConfig =
    HttpClientConfig("http", "back-rt-01-sas.test.vertis.yandex.net", 14110)

  val hydraClient = new DefaultHydraClient(http)
  val component = "api"
  val project = "auto"

  test("pick limiter") {
    val user = s"user-${Gen.posNum[Int].next}"

    val limit = hydraClient.limiter(project, component, user, Some(100)).futureValue
    limit should be > 0
    limit should be <= 100
  }

  test("omit limit param") {
    val user = s"user-${Gen.posNum[Int].next}"

    val limit = hydraClient.limiter(project, component, user, None).futureValue
    limit should be > 0
  }
}
