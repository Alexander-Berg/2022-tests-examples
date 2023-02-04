package ru.yandex.vertis.chat.components.tracing

import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, TracingSupport => VertisTracingSupport}

/**
  * TODO
  *
  * @author aborunov
  */
trait TestTracingSupport extends TracingAware {
  private val tracing: VertisTracingSupport = LocalTracingSupport(EndpointConfig.Empty)

  override val traceCreator: TraceCreator = TraceCreator.fromVertisTracing(tracing)
}
