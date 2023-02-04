package ru.yandex.realty.http

import ru.yandex.realty.SpecBase

/**
  * Introduces mocked HTTP client into tests.
  *
  * @author dimas
  */
trait HttpClientMock {

  this: SpecBase =>

  protected val httpClient: MockHttpClient = new MockHttpClient

  protected val httpService: RemoteHttpService =
    new RemoteHttpService("unit-test", HttpEndpoint("localhost", 80), client = httpClient)

  before {
    httpClient.reset()
    httpClient.expectEndpoint(httpService.endpoint)
  }

  after {
    httpClient.reset()
  }

}
