package ru.yandex.realty.componenttest.spec

import com.github.tomakehurst.wiremock.WireMockServer
import ru.yandex.realty.componenttest.env.ComponentTestEnvironment
import ru.yandex.realty.componenttest.http.ExternalHttpStubConfigProvider
import ru.yandex.realty.componenttest.wiremock.{WireMockExternalHttpStub, WireMockProvider, WireMockSpec}

trait WireMockComponentTestSpec[T <: ComponentTestEnvironment[_] with WireMockProvider with ExternalHttpStubConfigProvider]
  extends ComponentTestSpec[T]
  with WireMockExternalHttpStub
  with WireMockSpec {

  override lazy val wireMock: WireMockServer = env.wireMock

  override def shouldCheckForUnmatchedRequests: Boolean = true

}
