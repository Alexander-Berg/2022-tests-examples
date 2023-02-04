package ru.yandex.vertis.hipe.clients

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait MockedHttpClient { this: BaseSpec =>

  protected val http: MockHttpClient = new MockHttpClient

  before {
    http.reset()
  }

  after {
    http.reset()
  }
}
