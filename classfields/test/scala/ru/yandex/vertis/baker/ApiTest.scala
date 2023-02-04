package ru.yandex.vertis.baker

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Route
import io.prometheus.client.Counter
import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.baker.components.akka.AkkaSupport
import ru.yandex.vertis.baker.components.api.decider.DeciderSupport
import ru.yandex.vertis.baker.components.api.handlers.HandlersAware
import ru.yandex.vertis.baker.components.api.route.RouteSupport
import ru.yandex.vertis.baker.components.api.swagger.{SwaggerApiDocs, SwaggerApiInfoHandler}
import ru.yandex.vertis.baker.components.operational.OperationalSupport
import ru.yandex.vertis.baker.components.tracing.TracingSupport
import ru.yandex.vertis.baker.lifecycle.{Application, DefaultApplication}
import ru.yandex.vertis.baker.util.api.ApiRequestContext
import ru.yandex.vertis.baker.util.api.directives.ResponseMetricSupport
import ru.yandex.vertis.baker.util.api.routes.BaseHandler

import java.net.URL
import ru.yandex.vertis.baker.components.pool.ForkJoinPoolSupport
import sttp.tapir.swagger.akkahttp.SwaggerAkka

import scala.reflect.runtime.universe.Type

class ApiHandler extends BaseHandler {

  val route: Route = pathPrefix("ping") {
    named("ping") {
      complete {
        "pong"
      }
    }
  }
}

class DocumentationHandler extends SwaggerApiInfoHandler {

  private val apiTypes: Seq[Type] = Seq.empty[Type]

  override def docs: SwaggerApiDocs = new SwaggerApiDocs(apiTypes, Seq())

  override def route: Route = new SwaggerAkka(docs.yaml).routes
}

trait HandlersSupport extends HandlersAware {
  val apiHandler: BaseHandler = new ApiHandler

  val documentationHandler: SwaggerApiInfoHandler = new DocumentationHandler
}

trait AdditionalResponseMetricsSupport extends ResponseMetricSupport {

  val counterByName = Counter
    .build("api_http_request_by_name", "Http request count by name")
    .labelNames("name")
    .create()

  operational.prometheusRegistry.register(counterByName)

  override def meterResponse(response: HttpResponse, ctx: ApiRequestContext): Unit = {
    super.meterResponse(response, ctx)
    counterByName.labels(ctx.name).inc()
  }
}

class ApiTestComponents(val app: Application)
  extends OperationalSupport
  with AkkaSupport
  with ForkJoinPoolSupport
  with TracingSupport
  with DeciderSupport
  with HandlersSupport
  with AdditionalResponseMetricsSupport
  with RouteSupport {
  override def apiPrefix: String = "spi"
}

class ApiTestApp extends DefaultApplication {
  new ApiTestComponents(this)
}

@RunWith(classOf[JUnitRunner])
class ApiTest extends AnyWordSpec with Matchers with Eventually with OptionValues {
  private val app = new ApiTestApp
  app.main(Array())

  "Baker Api" should {
    "respond to /spi/ping" in {
      eventually {
        val in = new URL("http://localhost:5000/spi/ping").openStream()
        scala.io.Source.fromInputStream(in).getLines().mkString("") shouldBe "pong"
      }
    }

    "report metrics on /metrics route" in {
      val in = new URL("http://localhost:5000/metrics").openStream()
      val lines = scala.io.Source.fromInputStream(in).getLines().mkString("\n").split("\n")
      val metricName = "baker_test_baker_test_component_api_http_request_duration_seconds_count"
      val pingCallMetric = lines.find(_.startsWith(metricName)).value
      val envType = app.env.environmentType.toString
      pingCallMetric shouldBe metricName + s"""{name="ping",status="200",env="$envType",dc="development",instance="localhost",} 1.0"""
    }

    "report additional metrics on /metrics route" in {
      val in = new URL("http://localhost:5000/metrics").openStream()
      val lines = scala.io.Source.fromInputStream(in).getLines().mkString("\n").split("\n")
      val metricName = "baker_test_baker_test_component_api_http_request_by_name"
      val pingCallMetric = lines.find(_.startsWith(metricName)).value
      val envType = app.env.environmentType.toString
      pingCallMetric shouldBe metricName + s"""{name="ping",env="$envType",dc="development",instance="localhost",} 1.0"""
    }
  }
}
