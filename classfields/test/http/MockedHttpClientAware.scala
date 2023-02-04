package ru.yandex.vertis.baker.util.test.http

import ru.yandex.vertis.baker.components.http.client.HttpClient

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait MockedHttpClientAware extends BaseSpec {
  protected val http: HttpClient
}
