package auto.carfax.common.utils.http

import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultAsyncClient
import ru.yandex.vertis.commons.http.client.{
  ApacheHttpClient,
  HttpClient,
  LoggedHttpClient,
  MonitoredHttpClient,
  RemoteHttpService,
  RetryHttpClient
}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object TestHttpUtils {

  val proxy = new HttpHost("proxy-ext.test.vertis.yandex.net", 3128)

  val configWithProxy: RequestConfig =
    RequestConfig.copy(RemoteHttpService.DefaultRequestConfig).setProxy(proxy).build()

  lazy val DefaultHttpClient: HttpClient =
    new ApacheHttpClient(DefaultAsyncClient)
      with LoggedHttpClient
      with MonitoredHttpClient
      with JaegerTracedHttpClient
      with RetryHttpClient {
      implicit override val jaegerTracer: Tracer = NoopTracerFactory.create()
      implicit override val exec: ExecutionContext = global
    }

}
