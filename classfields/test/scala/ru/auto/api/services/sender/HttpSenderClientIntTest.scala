package ru.auto.api.services.sender

import ru.auto.api.http.HttpClientConfig
import ru.auto.api.services.HttpClientSuite

/**
  * Created by amisyura on 21.06.17.
  */
class HttpSenderClientIntTest extends HttpClientSuite {

  override protected def config: HttpClientConfig =
    HttpClientConfig("https", "test.sender.yandex-team.ru", 443)

  val senderClient = new HttpSenderClient(http, "35737570d429418884a40e469265b951", "autoru")

  test("send letter") {
    senderClient.sendText("no-reply@yandex-team.ru", "Integration test", "<h1>Test letter</h1><br/>Testing").futureValue
  }
}
