package ru.yandex.vertis.chat.util.http

import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.util.http.trace.EmptyTraceSupport

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait HttpClientSpec extends SpecBase with EmptyTraceSupport with SameThreadExecutionContextSupport {
  protected val http: HttpClient
}
