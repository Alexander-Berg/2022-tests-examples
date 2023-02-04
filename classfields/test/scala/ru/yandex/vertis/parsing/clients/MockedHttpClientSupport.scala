package ru.yandex.vertis.parsing.clients

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait MockedHttpClientSupport extends HttpClientSpec {

  protected val http: MockHttpClient = new MockHttpClient

  before {
    http.reset()
  }

  after {
    http.reset()
  }
}
