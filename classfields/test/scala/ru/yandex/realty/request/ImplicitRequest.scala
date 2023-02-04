package ru.yandex.realty.request

import akka.http.scaladsl.model.headers.ProductVersion
import ru.yandex.realty.auth.AuthInfo
import ru.yandex.realty.platform.PlatformInfo

trait ImplicitRequest {
  implicit val request: RequestImpl = {
    val request = new RequestImpl

    request.setIp("127.0.0.1")
    request.setPlatformInfo(Some(PlatformInfo("my device", "100")))
    request.setUserAgent(Some(UserAgent(Seq(ProductVersion("Internet Explorer", "4", "")))))
    request.setAuthInfo(
      AuthInfo(uidOpt = Some("4027541928"), uuid = Some("98765"), yandexUid = Some("4027541928"))
    )

    request
  }
}
