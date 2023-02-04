package ru.auto.api.testkit

import akka.http.javadsl.model.HttpMethod
import akka.http.scaladsl.model.{HttpMethods, StatusCode, StatusCodes}
import com.google.protobuf.Message
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.{Header, HttpResponse}
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsLookupResult, JsValue}
import ru.auto.api.http.{CacheProps, HttpClient, LogParams, RequestProps}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.concurrent.Threads

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.reflect.ClassTag

class MultiMockedHttpClient extends MockHttpClient with HttpClient with Matchers {

  override def doRequest[R](name: String,
                            request: HttpRequestBase,
                            props: RequestProps,
                            cacheProps: CacheProps[R],
                            logParams: LogParams)(
      f: HttpResponse => R
  )(implicit trace: Traced): Future[R] = {
    val future: Future[R] =
      innerClients(currentExecutingRequest).doRequest(name, request, props, cacheProps, logParams)(f)
    future.andThen { case _ => currentExecutingRequest = (currentExecutingRequest + 1) % innerClients.length }(
      Threads.SameThreadEc
    )
  }

  private val innerClients = ArrayBuffer[MockHttpClientImpl](new MockHttpClientImpl)
  private var currentPreparingRequest = 0
  @volatile private var currentExecutingRequest = 0

  def nextRequest(): Unit = {
    currentPreparingRequest += 1
    innerClients += new MockHttpClientImpl
  }

  def reset(): Unit = {
    innerClients.foreach(_.reset())
    currentPreparingRequest = 0
    currentExecutingRequest = 0
  }

  def expectUrl(method: HttpMethod, url: String): Unit = {
    innerClients(currentPreparingRequest).expectUrl(method, url)
  }

  def expectUrl(url: String): Unit = expectUrl(HttpMethods.GET, url)

  def expectHeader(httpHeader: Header): Unit = {
    innerClients(currentPreparingRequest).expectHeader(httpHeader)
  }

  def expectHeader(name: String, value: String): Unit = {
    innerClients(currentPreparingRequest).expectHeader(name, value)
  }

  def expectJson(json: String): Unit = {
    innerClients(currentPreparingRequest).expectJson(json)
  }

  def expectJsonField(getField: JsValue => JsLookupResult): JsonLookup = {
    innerClients(currentPreparingRequest).expectJsonField(getField)
  }

  def expectString(string: String): Unit = {
    innerClients(currentPreparingRequest).expectString(string)
  }

  def expectFormData(params: Map[String, String]): Unit = {
    innerClients(currentPreparingRequest).expectFormData(params)
  }

  def expectProto(proto: Message): Unit = {
    innerClients(currentPreparingRequest).expectProto(proto)
  }

  def respondWithStatus(status: StatusCode): Unit = {
    innerClients(currentPreparingRequest).respondWithStatus(status)
  }

  def respondWithJson(body: String): Unit = {
    respondWithJson(StatusCodes.OK, body)
  }

  def respondWithJson(status: StatusCode, body: String): Unit = {
    innerClients(currentPreparingRequest).respondWithJson(status, body)
  }

  def respondWithJsonFrom(path: String): Unit = {
    respondWithJsonFrom(StatusCodes.OK, path)
  }

  def respondWithJsonFrom(status: StatusCode, path: String): Unit = {
    innerClients(currentPreparingRequest).respondWithJsonFrom(status, path)
  }

  def respondWith[T <: Message](body: T): Unit = {
    respondWith(StatusCodes.OK, body)
  }

  def respondWithMany[T <: Message](body: Iterable[T]): Unit = {
    respondWithMany(StatusCodes.OK, body)
  }

  def respondWith[T <: Message](status: StatusCode, body: T): Unit = {
    innerClients(currentPreparingRequest).respondWith(status, body)
  }

  def respondWithMany[T <: Message](status: StatusCode, body: Iterable[T]): Unit = {
    innerClients(currentPreparingRequest).respondWithMany(status, body)
  }

  def respondWith(body: String): Unit = {
    respondWith(StatusCodes.OK, body)
  }

  def respondWith(status: StatusCode, body: String): Unit = {
    innerClients(currentPreparingRequest).respondWith(status, body)
  }

  def respondWithProto[T <: Message: ClassTag](status: StatusCode, message: T): Unit = {
    innerClients(currentPreparingRequest).respondWithProto(status, message)
  }

  def respondWithProtoFrom[T <: Message: ClassTag](path: String): Unit = {
    respondWithProtoFrom(StatusCodes.OK, path)
  }

  def respondWithProtoFrom[T <: Message: ClassTag](status: StatusCode, path: String): Unit = {
    innerClients(currentPreparingRequest).respondWithProtoFrom(status, path)
  }

  def respondWithProtoFromJson[T <: Message: ClassTag](status: StatusCode, body: String): Unit = {
    innerClients(currentPreparingRequest).respondWithProtoFromJson(status, body)
  }

  def respondWithXmlFrom(path: String): Unit = {
    respondWithXmlFrom(StatusCodes.OK, path)
  }

  def respondWithXmlFrom(status: StatusCode, path: String): Unit = {
    innerClients(currentPreparingRequest).respondWithXmlFrom(status, path)
  }

  def verifyRequestDone(): Unit = {
    innerClients.foreach(_.verifyRequestDone())
  }
}
