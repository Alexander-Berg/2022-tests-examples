package ru.yandex.vertis.baker.util.test.http

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait MockedHttpClientSupport extends MockedHttpClientAware {

  protected val http: MockHttpClient = new MockHttpClientImpl

  before {
    http.reset()
  }

  after {
    http.reset()
  }
}
