package ru.yandex.vertis.parsing.util.http.tracing

import ru.yandex.vertis.tracing.Traced

/**
  * TODO
  *
  * @author aborunov
  */
trait EmptyTraceSupport {
  implicit val trace: Traced = TracedUtils.empty
}
