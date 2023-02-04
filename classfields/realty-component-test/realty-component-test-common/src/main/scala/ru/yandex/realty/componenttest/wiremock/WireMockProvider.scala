package ru.yandex.realty.componenttest.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import ru.yandex.realty.componenttest.http.ExternalHttpStubConfigProvider

trait WireMockProvider {

  def wireMock: WireMockServer

}

trait DefaultWireMockProvider extends WireMockProvider {
  self: ExternalHttpStubConfigProvider =>

  override lazy val wireMock: WireMockServer = {
    val wm = new WireMockServer(externalHttpStubConfig.port)
    wm.start()
    wm
  }

}
