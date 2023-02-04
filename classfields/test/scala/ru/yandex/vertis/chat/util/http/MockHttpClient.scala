package ru.yandex.vertis.chat.util.http

import java.io.File

import com.google.protobuf.Message
import org.apache.http._
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.{ByteArrayEntity, ContentType, StringEntity}
import org.apache.http.impl.DefaultHttpResponseFactory
import org.apache.http.util.EntityUtils
import ru.yandex.vertis.tracing.Traced

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
class MockHttpClient extends HttpClient {
  override def config: HttpClientConfig = HttpClientConfig("???", Some(HostPort("localhost", -1)))

  private var expectedMethod: Option[String] = None
  private var expectedUrl: Option[String] = None
  private var expectedHeaders: ArrayBuffer[Header] = ArrayBuffer[Header]()
  private var expectedJson: Option[String] = None
  private var expectedString: Option[String] = None
  private var expectedProto: Option[Message] = None

  private var responseStatus: Option[Int] = None
  private var responseFile: Option[File] = None
  private var responseJson: Option[String] = None
  private var responseProto: Option[Message] = None
  private var responseString: Option[String] = None

  def reset(): Unit = {
    expectedMethod = None
    expectedUrl = None
    expectedHeaders = ArrayBuffer[Header]()
    expectedJson = None
    expectedString = None
    expectedProto = None

    responseStatus = None
    responseFile = None
    responseJson = None
    responseProto = None
    responseString = None
  }

  def expect(method: String, url: String): Unit = {
    expectedMethod = Some(method)
    expectedUrl = Some(url)
  }

  def expect(url: String): Unit = expect("GET", url)

  def expectHeader(httpHeader: Header): Unit = expectedHeaders += httpHeader

  def expectJson(json: String): Unit = expectedJson = Some(json)

  def expectString(string: String): Unit = expectedString = Some(string)

  def expectProto(proto: Message): Unit = expectedProto = Some(proto)

  def respondWithStatus(status: Int): Unit = responseStatus = Some(status)

  def respondWithFile(file: File): Unit = responseFile = Some(file)

  def respondWithJson(body: String): Unit = {
    respondWithJson(HttpStatus.SC_OK, body)
  }

  def respondWithJson(status: Int, body: String): Unit = {
    responseStatus = Some(status)
    responseJson = Some(body)
  }

  def respondWith[T <: Message](body: T): Unit = {
    respondWith(HttpStatus.SC_OK, body)
  }

  def respondWith[T <: Message](body: Iterable[T]): Unit = {
    respondWith(HttpStatus.SC_OK, body)
  }

  def respondWith[T <: Message](status: Int, body: T): Unit = {
    responseStatus = Some(status)
    responseProto = Some(body)
  }

  def respondWith[T <: Message](status: Int, body: Iterable[T]): Unit = {
    responseStatus = Some(status)
  }

  def respondWith(body: String): Unit = {
    respondWith(HttpStatus.SC_OK, body)
  }

  def respondWith(status: Int, body: String): Unit = {
    responseStatus = Some(status)
    responseString = Some(body)
  }

  //scalastyle:off method.length
  override def doRequest[R](
      name: String,
      request: HttpRequestBase,
      timeout: HttpRequestTimeout = DefaultTimeout,
      idempotency: Idempotency = DefaultIdempotency
  )(f: HttpResponse => R)(implicit trace: Traced): Future[R] = {

    expectedMethod.foreach { method =>
      if (method != request.getRequestLine.getMethod) {
        throw new IllegalArgumentException(s"Expected method [$method] but got [${request.getRequestLine.getMethod}]")
      }
    }

    expectedUrl.foreach { url =>
      if (url != request.getRequestLine.getUri) {
        throw new IllegalArgumentException(s"Expected url [$url] but got [${request.getRequestLine.getUri}]")
      }
    }

    expectedHeaders.foreach { h =>
      if (!request.getHeaders(h.getName).exists(h.getValue == _.getValue)) {
        val msg = s"Expected header [${h.getName}: ${h.getValue}] but got [\n${request.getAllHeaders.mkString("\n")}\n]"
        throw new IllegalArgumentException(msg)
      }
    }

    expectedJson.foreach { json =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toString(withEntity.getEntity)
          require(body == json, s"Expected json [$json] but got [$body]")
        case _ => sys.error(s"Expected request with json but got $request")
      }
    }

    expectedString.foreach { string =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toString(withEntity.getEntity)
          require(body == string, s"Expected string [$string] but got [$body]")
        case _ => sys.error(s"Expected request with string but got $request")
      }
    }
    expectedProto.foreach { proto =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toByteArray(withEntity.getEntity)
          val parsed = proto.newBuilderForType().mergeFrom(body).build()
          require(parsed == proto, s"Expected json [$proto] but got [$parsed]")
        case _ => sys.error(s"Expected request with proto but got $request")
      }
    }

    val status = responseStatus.getOrElse(sys.error("`respondWithStatus` not called"))
    val res = DefaultHttpResponseFactory.INSTANCE
      .newHttpResponse(HttpVersion.HTTP_1_1, status.intValue(), null)

    responseJson.foreach { json =>
      res.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON))
    }

    responseProto.foreach { proto =>
      res.setEntity(new ByteArrayEntity(proto.toByteArray, ProtobufContentType))
    }

    responseString.foreach { body =>
      res.setEntity(new StringEntity(body, ContentType.TEXT_PLAIN))
    }

    Future.successful(f(res))
  }
  /*
  override def download[R](
      name: String,
      request: HttpRequestBase,
      timeout: HttpRequestTimeout,
      idempotency: Idempotency
  )(f: (HttpResponse, File) => R)(implicit trace: Traced, context: HttpRequestContext): Future[R] = {
    expectedMethod.foreach { method =>
      if (method != request.getRequestLine.getMethod) {
        throw new IllegalArgumentException(s"Expected method [$method] but got [${request.getRequestLine.getMethod}]")
      }
    }

    expectedUrl.foreach { url =>
      if (url != request.getRequestLine.getUri) {
        throw new IllegalArgumentException(s"Expected url [$url] but got [${request.getRequestLine.getUri}]")
      }
    }

    expectedHeaders.foreach { h =>
      if (!request.getHeaders(h.getName).exists(h.getValue == _.getValue)) {
        val msg = s"Expected header [${h.getName}: ${h.getValue}] but got [\n${request.getAllHeaders.mkString("\n")}\n]"
        throw new IllegalArgumentException(msg)
      }
    }

    expectedJson.foreach { json =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toString(withEntity.getEntity)
          require(body == json, s"Expected json [$json] but got [$body]")
        case _ => sys.error(s"Expected request with json but got $request")
      }
    }

    expectedString.foreach { string =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toString(withEntity.getEntity)
          require(body == string, s"Expected string [$string] but got [$body]")
        case _ => sys.error(s"Expected request with string but got $request")
      }
    }
    expectedProto.foreach { proto =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toByteArray(withEntity.getEntity)
          val parsed = proto.newBuilderForType().mergeFrom(body).build()
          require(parsed == proto, s"Expected json [$proto] but got [$parsed]")
        case _ => sys.error(s"Expected request with proto but got $request")
      }
    }

    val status = responseStatus.getOrElse(sys.error("`respondWithStatus` not called"))
    val file = responseFile.getOrElse(sys.error("`respondWithFile` not called"))
    val res: HttpResponse = DefaultHttpResponseFactory.INSTANCE
      .newHttpResponse(HttpVersion.HTTP_1_1, status.intValue(), null)

    responseJson.foreach { json =>
      res.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON))
    }

    responseProto.foreach { proto =>
      res.setEntity(new ByteArrayEntity(proto.toByteArray, ProtobufContentType))
    }

    responseString.foreach { body =>
      res.setEntity(new StringEntity(body, ContentType.TEXT_PLAIN))
    }

    Future.successful(f(res, file))
  }

 */
}
