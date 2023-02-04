package ru.yandex.vertis.baker.util.test.http

import akka.http.scaladsl.model
import akka.http.scaladsl.model.StatusCode
import com.google.protobuf.Message
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.{Header, HttpResponse}
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsLookupResult, JsValue}
import ru.yandex.vertis.baker.components.http.client.{HttpRequestContext, HttpRequestTimeout, Idempotency, LogParams}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.concurrent.Threads

import java.io.File
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.reflect.ClassTag

class MultiMockedHttpClient extends MockHttpClient with Matchers {

  override def doRequest[R](
      name: String,
      request: HttpRequestBase,
      timeout: HttpRequestTimeout,
      idempotency: Idempotency,
      logParams: LogParams
  )(f: HttpResponse => R)(implicit trace: Traced, context: HttpRequestContext = new HttpRequestContext): Future[R] = {
    val future: Future[R] =
      innerClients(currentExecutingRequest).doRequest(name, request, timeout, idempotency, logParams)(f)
    future.andThen { case _ => currentExecutingRequest = (currentExecutingRequest + 1) % innerClients.length }(
      Threads.SameThreadEc
    )
  }

  override def download[R](
      name: String,
      request: HttpRequestBase,
      timeout: HttpRequestTimeout,
      idempotency: Idempotency,
      logParams: LogParams
  )(f: (HttpResponse, File) => R)(implicit trace: Traced, context: HttpRequestContext): Future[R] = {
    val future: Future[R] =
      innerClients(currentExecutingRequest).download(name, request, timeout, idempotency, logParams)(f)
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

  def verifyRequestDone(): Unit = {
    innerClients.foreach(_.verifyRequestDone())
  }

  override def expectUrl(method: model.HttpMethod, url: String): Unit = {
    innerClients(currentPreparingRequest).expectUrl(method, url)
  }

  override def expectUrl(url: String): Unit = {
    innerClients(currentPreparingRequest).expectUrl(url)
  }

  override def expectHeader(httpHeader: Header): Unit = {
    innerClients(currentPreparingRequest).expectHeader(httpHeader)
  }

  override def expectHeader(name: String, value: String): Unit = {
    innerClients(currentPreparingRequest).expectHeader(name, value)
  }

  override def expectJson(json: String): Unit = {
    innerClients(currentPreparingRequest).expectJson(json)
  }

  override def expectJsonField(getField: JsValue => JsLookupResult): JsonLookup = {
    innerClients(currentPreparingRequest).expectJsonField(getField)
  }

  override def expectString(string: String): Unit = {
    innerClients(currentPreparingRequest).expectString(string)
  }

  override def expectFormData(params: Map[String, String]): Unit = {
    innerClients(currentPreparingRequest).expectFormData(params)
  }

  override def expectProto(proto: Message): Unit = {
    innerClients(currentPreparingRequest).expectProto(proto)
  }

  override def respondWithStatus(status: StatusCode): Unit = {
    innerClients(currentPreparingRequest).respondWithStatus(status)
  }

  override def respondWithFile(file: File): Unit = {
    innerClients(currentPreparingRequest).respondWithFile(file)
  }

  override def respondWithJson(body: String): Unit = {
    innerClients(currentPreparingRequest).respondWithJson(body)
  }

  override def respondWithJson(status: StatusCode, body: String): Unit = {
    innerClients(currentPreparingRequest).respondWithJson(status, body)
  }

  override def respondWithJsonFrom(path: String): Unit = {
    innerClients(currentPreparingRequest).respondWithJsonFrom(path)
  }

  override def respondWithJsonFrom(status: StatusCode, path: String): Unit = {
    innerClients(currentPreparingRequest).respondWithJsonFrom(status, path)
  }

  override def respondWithProto[T <: Message](body: T): Unit = {
    innerClients(currentPreparingRequest).respondWithProto(body)
  }

  override def respondWithManyProto[T <: Message](body: Iterable[T]): Unit = {
    innerClients(currentPreparingRequest).respondWithManyProto(body)
  }

  override def respondWithProto[T <: Message](status: StatusCode, body: T): Unit = {
    innerClients(currentPreparingRequest).respondWithProto(status, body)
  }

  override def respondWithManyProto[T <: Message](status: StatusCode, body: Iterable[T]): Unit = {
    innerClients(currentPreparingRequest).respondWithManyProto(status, body)
  }

  override def respondWithString(body: String): Unit = {
    innerClients(currentPreparingRequest).respondWithString(body)
  }

  override def respondWithString(status: StatusCode, body: String): Unit = {
    innerClients(currentPreparingRequest).respondWithString(status, body)
  }

  override def respondWithProtoFrom[T <: Message: ClassTag](path: String): Unit = {
    innerClients(currentPreparingRequest).respondWithProtoFrom(path)
  }

  override def respondWithProtoFrom[T <: Message: ClassTag](status: StatusCode, path: String): Unit = {
    innerClients(currentPreparingRequest).respondWithProtoFrom(status, path)
  }

  override def respondWithProtoFromJson[T <: Message: ClassTag](status: StatusCode, body: String): Unit = {
    innerClients(currentPreparingRequest).respondWithProtoFromJson(status, body)
  }

  override def respondWithXmlFrom(path: String): Unit = {
    innerClients(currentPreparingRequest).respondWithXmlFrom(path)
  }

  override def respondWithXmlFrom(status: StatusCode, path: String): Unit = {
    innerClients(currentPreparingRequest).respondWithXmlFrom(status, path)
  }

  override def respondWithHeader(httpHeader: Header): Unit = {
    innerClients(currentPreparingRequest).respondWithHeader(httpHeader)
  }

  override def respondWithHeader(name: String, value: String): Unit = {
    innerClients(currentPreparingRequest).respondWithHeader(name, value)
  }
}
