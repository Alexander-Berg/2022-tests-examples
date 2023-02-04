package ru.yandex.vertis.parsing.api.components.handlers

import ru.yandex.vertis.akka.http.swagger.SwaggerHandler
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.api.routes.DocumentationHandler
import ru.yandex.vertis.parsing.util.api.routes.prometheus.PrometheusHandler

/**
  * TODO
  *
  * @author aborunov
  */
trait MockedHandlersSupport extends HandlersAware with MockitoSupport {
  override val documentationHandler: DocumentationHandler = mock[DocumentationHandler]

  override val prometheusHandler: PrometheusHandler = mock[PrometheusHandler]

  override val swaggerHandler: SwaggerHandler = mock[SwaggerHandler]
}
