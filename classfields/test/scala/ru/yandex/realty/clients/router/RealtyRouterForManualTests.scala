package ru.yandex.realty.clients.router

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.common.monitoring.CompoundHealthCheckRegistry
import ru.yandex.realty.application.ng.router.{FrontendRouterClientConfig, FrontendRouterClientSupplier}
import ru.yandex.realty.http.{DefaultHttpComponents, HttpEndpoint}
import ru.yandex.realty.ops.OperationalComponents
import ru.yandex.realty.tvm.TvmLibrarySupplier
import ru.yandex.realty.util.tracing.{NoopTracingSupport, TracingProvider, TracingSupport}
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.codahale.CodahaleRegistry
import ru.yandex.vertis.ops.prometheus.{PrometheusRegistry, SimpleCompositeCollector}

trait RealtyRouterForManualTests
  extends DefaultHttpComponents
  with FrontendRouterClientSupplier
  with TracingProvider
  with TvmLibrarySupplier
  with OperationalComponents {

  override def ops: OperationalSupport = new OperationalSupport {
    implicit override def codahaleRegistry: CodahaleRegistry = new CodahaleRegistry
    implicit override def prometheusRegistry: PrometheusRegistry = new SimpleCompositeCollector
    override def healthChecks: CompoundHealthCheckRegistry = new CompoundHealthCheckRegistry
  }

  override protected def routerClientConfig: FrontendRouterClientConfig =
    FrontendRouterClientConfig(
      HttpEndpoint(
        host = "realty-front-router-realtyfront-13910-http.vrts-slb.test.vertis.yandex.net",
        port = 80
      )
    )

  override protected def tvmConf: Option[Config] =
    Some(
      ConfigFactory.parseString(
        s"""
           |selfClientId = 2015012
           |secret = "pRTAEFPYONt2beMzfamo0Q"
           |serviceIds = {
           |  router = 2010296
           |}
            """.stripMargin
      )
    )

  implicit override def tracingSupport: TracingSupport = new NoopTracingSupport()
}
