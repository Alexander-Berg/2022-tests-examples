package ru.auto.api.services

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.scalacheck.Gen
import ru.auto.api.GeneratorUtils
import ru.auto.api.http.{CacheProps, HttpClient, LogParams, NoCaching, RequestProps}
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

trait IntTestHttpClient extends HttpClient with GeneratorUtils {

  abstract override def doRequest[R](
      name: String,
      request: HttpRequestBase,
      props: RequestProps = RequestProps(),
      cacheProps: CacheProps[R],
      logParams: LogParams
  )(f: (HttpResponse) => R)(implicit trace: Traced): Future[R] = {
    assert(cacheProps == NoCaching)

    request.addHeader("X-Request-Id", Gen.identifier.filter(_.nonEmpty).next)
    super.doRequest(name, request, props, cacheProps, logParams)(f)
  }
}
