package ru.auto.api.services

import ru.auto.api.BaseSpec
import ru.auto.api.http.HttpClient
import ru.yandex.vertis.tracing.Traced

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait HttpClientSpec extends BaseSpec {

  protected val http: HttpClient

  implicit val trace: Traced = Traced.empty

}
