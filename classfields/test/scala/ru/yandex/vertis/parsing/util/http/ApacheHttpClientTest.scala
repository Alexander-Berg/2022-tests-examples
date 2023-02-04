package ru.yandex.vertis.parsing.util.http

import java.util.concurrent.{Future, TimeUnit}

import com.typesafe.config.ConfigFactory
import org.apache.http.{HttpHost, HttpResponse, HttpVersion}
import org.apache.http.client.methods.{HttpGet, HttpRequestBase, HttpUriRequest}
import org.apache.http.concurrent.FutureCallback
import org.apache.http.message.{BasicHttpResponse, BasicStatusLine}
import org.apache.http.nio.client.HttpAsyncClient
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.util.IO
import ru.yandex.vertis.parsing.util.http.config.HttpClientConfig
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ApacheHttpClientTest extends FunSuite with MockitoSupport {
  private val httpAsyncClient = mock[HttpAsyncClient]
  private val io = mock[IO]

  implicit private val trace: Traced = TracedUtils.empty

  test("X-Yandex-Api-Client header not set for outer service") {
    reset(httpAsyncClient)
    val config = HttpClientConfig.newBuilder("test").isOuterService().build()
    val client = new ApacheHttpClient(httpAsyncClient, config, io)
    val req = new HttpGet("http://example.com")

    val response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, null), null, null)
    stub(httpAsyncClient.execute(_: HttpUriRequest, _: FutureCallback[HttpResponse])) {
      case (request, _) =>
        assert(request.getHeaders("Accept-Encoding").length == 1)
        assert(request.getHeaders("Accept-Encoding").head.getValue == "gzip")
        assert(request.getHeaders("X-Yandex-Api-Client").length == 0)
        assert(request.getHeaders("Proxy-Authorization").length == 0)
        requestResult(response)
    }
    client.doRequest("test", req) { res =>
      assert(res.getStatusLine.getStatusCode == 200)
    }
    verify(httpAsyncClient).execute(?, ?)
  }

  test("X-Yandex-Api-Client header set for inner service") {
    reset(httpAsyncClient)
    val config = HttpClientConfig.newBuilder("test").isInnerService().build()
    val client = new ApacheHttpClient(httpAsyncClient, config, io)
    val req = new HttpGet("http://example.com")

    val response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, null), null, null)
    stub(httpAsyncClient.execute(_: HttpUriRequest, _: FutureCallback[HttpResponse])) {
      case (request, _) =>
        assert(request.getHeaders("Accept-Encoding").length == 1)
        assert(request.getHeaders("Accept-Encoding").head.getValue == "gzip")
        assert(request.getHeaders("X-Yandex-Api-Client").length == 1)
        assert(request.getHeaders("X-Yandex-Api-Client").head.getValue == "parsing")
        assert(request.getHeaders("Proxy-Authorization").length == 0)
        requestResult(response)
    }
    client.doRequest("test", req) { res =>
      assert(res.getStatusLine.getStatusCode == 200)
    }
    verify(httpAsyncClient).execute(?, ?)
  }

  test("X-Yandex-Api-Client header set by default") {
    reset(httpAsyncClient)
    val config = HttpClientConfig.newBuilder("test").build()
    val client = new ApacheHttpClient(httpAsyncClient, config, io)
    val req = new HttpGet("http://example.com")

    val response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, null), null, null)
    stub(httpAsyncClient.execute(_: HttpUriRequest, _: FutureCallback[HttpResponse])) {
      case (request, _) =>
        assert(request.getHeaders("Accept-Encoding").length == 1)
        assert(request.getHeaders("Accept-Encoding").head.getValue == "gzip")
        assert(request.getHeaders("X-Yandex-Api-Client").length == 1)
        assert(request.getHeaders("X-Yandex-Api-Client").head.getValue == "parsing")
        assert(request.getHeaders("Proxy-Authorization").length == 0)
        requestResult(response)
    }
    client.doRequest("test", req) { res =>
      assert(res.getStatusLine.getStatusCode == 200)
    }
    verify(httpAsyncClient).execute(?, ?)
  }

  test("proxy is set") {
    reset(httpAsyncClient)
    val config = HttpClientConfig.newBuilder("test").withProxyConfig(ConfigFactory.parseString("""
        |host = "ya.ru"
        |port = 8010
        |user = user1
        |password = pass1
      """.stripMargin)).build()
    val client = new ApacheHttpClient(httpAsyncClient, config, io)
    val req = new HttpGet("http://example.com")

    val response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, null), null, null)
    stub(httpAsyncClient.execute(_: HttpUriRequest, _: FutureCallback[HttpResponse])) {
      case (request, _) =>
        assert(request.getHeaders("Proxy-Authorization").length == 1)
        assert(request.getHeaders("Proxy-Authorization").head.getValue == "Basic dXNlcjE6cGFzczE=")
        val config = request.asInstanceOf[HttpRequestBase].getConfig
        assert(config.getProxy == new HttpHost("ya.ru", 8010, "http"))
        requestResult(response)
    }
    client.doRequest("test", req) { res =>
      assert(res.getStatusLine.getStatusCode == 200)
    }
    verify(httpAsyncClient).execute(?, ?)
  }

  private def requestResult(response: HttpResponse) = new Future[HttpResponse] {
    override def cancel(mayInterruptIfRunning: Boolean): Boolean = true

    override def isCancelled: Boolean = false

    override def isDone: Boolean = true

    override def get(): HttpResponse = response

    override def get(timeout: Long, unit: TimeUnit): HttpResponse = response
  }
}
