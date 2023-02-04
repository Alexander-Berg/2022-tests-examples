package ru.auto.api.testkit

import akka.http.javadsl.model.HttpMethod
import akka.http.scaladsl.model.StatusCode
import com.google.protobuf.Message
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.{Header, HttpResponse}
import org.apache.http.client.config.RequestConfig
import play.api.libs.json.{JsLookupResult, JsValue}
import ru.auto.api.http.{CacheProps, HostPort, HttpClientConfig, LogParams, RequestProps}
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.reflect.ClassTag

trait MockHttpClient {

  def config: HttpClientConfig =
    HttpClientConfig(
      serviceName = "???",
      hostPort = HostPort("localhost", -1),
      serviceId = Option.empty,
      destination = Option.empty,
      requestConfig = RequestConfig.DEFAULT
    )

  def reset(): Unit

  def expectUrl(method: HttpMethod, url: String): Unit

  def expectUrl(url: String): Unit

  def expectHeader(httpHeader: Header): Unit

  def expectHeader(name: String, value: String): Unit

  def expectJson(json: String): Unit

  def expectJsonField(getField: JsValue => JsLookupResult): JsonLookup

  def expectString(string: String): Unit

  def expectFormData(params: Map[String, String]): Unit

  def expectProto(proto: Message): Unit

  def respondWithStatus(status: StatusCode): Unit

  def respondWithJson(body: String): Unit

  def respondWithJson(status: StatusCode, body: String): Unit

  def respondWithJsonFrom(path: String): Unit

  def respondWithJsonFrom(status: StatusCode, path: String): Unit

  def respondWith[T <: Message](body: T): Unit

  def respondWithMany[T <: Message](body: Iterable[T]): Unit

  def respondWith[T <: Message](status: StatusCode, body: T): Unit

  def respondWithMany[T <: Message](status: StatusCode, body: Iterable[T]): Unit

  def respondWith(body: String): Unit

  def respondWith(status: StatusCode, body: String): Unit

  def respondWithProto[T <: Message: ClassTag](status: StatusCode, message: T): Unit

  def respondWithProtoFrom[T <: Message: ClassTag](path: String): Unit

  def respondWithProtoFrom[T <: Message: ClassTag](status: StatusCode, path: String): Unit

  def respondWithProtoFromJson[T <: Message: ClassTag](status: StatusCode, body: String): Unit

  def respondWithXmlFrom(path: String): Unit

  def respondWithXmlFrom(status: StatusCode, path: String): Unit

  //scalastyle:off method.length
  def doRequest[R](
      name: String,
      request: HttpRequestBase,
      props: RequestProps = RequestProps(),
      cacheProps: CacheProps[R],
      logParams: LogParams
  )(f: HttpResponse => R)(implicit trace: Traced): Future[R]

  def verifyRequestDone(): Unit
}
