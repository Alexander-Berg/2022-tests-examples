package ru.auto.api.services.video

import ru.auto.api.auth.Application
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.RequestParams
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.util.RequestImpl

class YandexVideoClientIntTest extends HttpClientSuite {
  val uid = 20871601L
  val deviceId = "SomeTestDeviceId"

  implicit val request: RequestImpl = {
    val req = new RequestImpl
    req.setApplication(Application.desktop)
    req.setTrace(trace)
    req.setRequestParams(RequestParams.empty)
    req
  }

  override protected def config: HttpClientConfig =
    HttpClientConfig.apply("https", "yandex.ru", 443)

  //val videoClient = new YandexVideoClient(http)

  test("search video") {
    //val result = videoClient.search("BMW", Paging(1, 4)).futureValue.getVideosList.asScala

    //result.length shouldBe 4
  }
}
