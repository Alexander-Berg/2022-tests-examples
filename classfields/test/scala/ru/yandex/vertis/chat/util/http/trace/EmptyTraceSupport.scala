package ru.yandex.vertis.chat.util.http.trace

import ru.yandex.vertis.chat.components.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced

/**
  * TODO
  *
  * @author aborunov
  */
trait EmptyTraceSupport {
  implicit val trace: Traced = TracedUtils.empty
}
