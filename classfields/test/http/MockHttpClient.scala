package ru.yandex.vertis.baker.util.test.http

import akka.http.scaladsl.model.{HttpMethod, StatusCode}
import com.google.protobuf.Message
import org.apache.http.Header
import play.api.libs.json.{JsLookupResult, JsValue}
import ru.yandex.vertis.baker.components.http.client.HttpClient
import ru.yandex.vertis.baker.components.http.client.config.HostPort

import java.io.File
import scala.reflect.ClassTag

trait MockHttpClient extends HttpClient {

  def apiName: String = "???"

  def hostPort: Option[HostPort] = Some(HostPort("localhost", -1))

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

  def respondWithFile(file: File): Unit

  def respondWithJson(body: String): Unit

  def respondWithJson(status: StatusCode, body: String): Unit

  def respondWithJsonFrom(path: String): Unit

  def respondWithJsonFrom(status: StatusCode, path: String): Unit

  def respondWithProto[T <: Message](body: T): Unit

  def respondWithManyProto[T <: Message](body: Iterable[T]): Unit

  def respondWithProto[T <: Message](status: StatusCode, body: T): Unit

  def respondWithManyProto[T <: Message](status: StatusCode, body: Iterable[T]): Unit

  def respondWithString(body: String): Unit

  def respondWithString(status: StatusCode, body: String): Unit

  def respondWithProtoFrom[T <: Message: ClassTag](path: String): Unit

  def respondWithProtoFrom[T <: Message: ClassTag](status: StatusCode, path: String): Unit

  def respondWithProtoFromJson[T <: Message: ClassTag](status: StatusCode, body: String): Unit

  def respondWithXmlFrom(path: String): Unit

  def respondWithXmlFrom(status: StatusCode, path: String): Unit

  def respondWithHeader(httpHeader: Header): Unit

  def respondWithHeader(name: String, value: String): Unit

  def verifyRequestDone(): Unit
}
