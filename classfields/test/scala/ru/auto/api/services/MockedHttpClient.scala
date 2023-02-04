package ru.auto.api.services

import ru.auto.api.BaseSpec
import ru.auto.api.testkit.MultiMockedHttpClient

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait MockedHttpClient { this: BaseSpec =>

  protected val http: MultiMockedHttpClient = new MultiMockedHttpClient

  before {
    http.reset()
  }

  after {
    http.reset()
  }
}
