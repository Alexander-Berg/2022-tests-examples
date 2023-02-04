package ru.yandex.vertis.parsing.clients

import ru.yandex.vertis.parsing.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.parsing.util.http.HttpClient
import ru.yandex.vertis.parsing.util.http.tracing.EmptyTraceSupport

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait HttpClientSpec extends BaseSpec with EmptyTraceSupport with SameThreadExecutionContextSupport {
  protected val http: HttpClient
}
