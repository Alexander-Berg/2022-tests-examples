package ru.yandex.vertis.baker.util.test.http

import akka.http.scaladsl.model.{HttpMethod, HttpMethods, StatusCode, StatusCodes}
import com.google.protobuf.Message
import org.apache.http._
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.{ByteArrayEntity, ContentType, StringEntity}
import org.apache.http.impl.DefaultHttpResponseFactory
import org.apache.http.util.EntityUtils
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsLookupResult, JsValue, Json}
import ru.yandex.vertis.baker.components.http.client.{HttpRequestContext, HttpRequestTimeout, Idempotency, LogParams, ProtobufContentType}
import ru.yandex.vertis.baker.components.io.IO
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf

import java.io.File
import java.net.URLDecoder
import scala.annotation.nowarn
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
@nowarn
class MockHttpClientImpl extends MockHttpClient with Matchers {

  private case class JsonAssertion(f: JsValue => Assertion) {

    def apply(value: JsValue): Assertion = f(value)
  }

  private var expectedMethod: Option[HttpMethod] = None
  private var expectedUrl: Option[String] = None
  private var expectedHeaders: mutable.Map[String, String] = mutable.HashMap[String, String]()
  private var expectedJson: Option[String] = None
  private var expectedJsonAssertions: ArrayBuffer[JsonAssertion] = ArrayBuffer[JsonAssertion]()
  private var expectedString: Option[String] = None
  private var expectFormData: Option[Map[String, String]] = None
  private var expectedProto: Option[Message] = None

  private var responseStatus: Option[StatusCode] = None
  private var responseFile: Option[File] = None
  private var responseJson: Option[String] = None
  private var responseProto: Option[Message] = None
  private var responseManyProto: Option[Iterable[Message]] = None
  private var responseString: Option[String] = None
  private var responseXml: Option[String] = None
  private var responseHeaders: mutable.Map[String, String] = mutable.HashMap[String, String]()

  private var requestDone: Boolean = false

  override def reset(): Unit = {
    expectedMethod = None
    expectedUrl = None
    expectedHeaders = mutable.HashMap[String, String]()
    expectedJson = None
    expectedJsonAssertions = ArrayBuffer()
    expectedString = None
    expectFormData = None
    expectedProto = None

    responseStatus = None
    responseFile = None
    responseJson = None
    responseProto = None
    responseManyProto = None
    responseString = None
    responseXml = None
    responseHeaders = mutable.HashMap[String, String]()

    requestDone = false
  }

  override def expectUrl(method: HttpMethod, url: String): Unit = {
    expectedMethod = Some(method)
    expectedUrl = Some(url)
  }

  override def expectUrl(url: String): Unit = expectUrl(HttpMethods.GET, url)

  override def expectHeader(httpHeader: Header): Unit = expectedHeaders(httpHeader.getName) = httpHeader.getValue

  override def expectHeader(name: String, value: String): Unit = {
    expectedHeaders(name) = value
  }

  override def expectJson(json: String): Unit = expectedJson = Some(json)

  override def expectJsonField(getField: JsValue => JsLookupResult): JsonLookup =
    new JsonLookup {

      override def shouldBe(expected: JsValue): Unit = {
        expectedJsonAssertions += JsonAssertion(jsValue => getField(jsValue).get shouldBe expected)
      }
    }

  override def expectString(string: String): Unit = expectedString = Some(string)

  override def expectFormData(params: Map[String, String]): Unit = expectFormData = Some(params)

  override def expectProto(proto: Message): Unit = expectedProto = Some(proto)

  override def respondWithStatus(status: StatusCode): Unit = responseStatus = Some(status)

  override def respondWithFile(file: File): Unit = responseFile = Some(file)

  override def respondWithJson(body: String): Unit = {
    respondWithJson(StatusCodes.OK, body)
  }

  override def respondWithJson(status: StatusCode, body: String): Unit = {
    responseStatus = Some(status)
    responseJson = Some(body)
  }

  override def respondWithJsonFrom(path: String): Unit = {
    respondWithJsonFrom(StatusCodes.OK, path)
  }

  override def respondWithJsonFrom(status: StatusCode, path: String): Unit = {
    responseStatus = Some(status)
    responseJson = Some(IO.toString(path))
  }

  override def respondWithProto[T <: Message](body: T): Unit = {
    respondWithProto(StatusCodes.OK, body)
  }

  override def respondWithManyProto[T <: Message](body: Iterable[T]): Unit = {
    respondWithManyProto(StatusCodes.OK, body)
  }

  override def respondWithProto[T <: Message](status: StatusCode, body: T): Unit = {
    responseStatus = Some(status)
    responseProto = Some(body)
  }

  override def respondWithManyProto[T <: Message](status: StatusCode, body: Iterable[T]): Unit = {
    responseStatus = Some(status)
    responseManyProto = Some(body)
  }

  override def respondWithString(body: String): Unit = {
    respondWithString(StatusCodes.OK, body)
  }

  override def respondWithString(status: StatusCode, body: String): Unit = {
    responseStatus = Some(status)
    responseString = Some(body)
  }

  override def respondWithProtoFrom[T <: Message: ClassTag](path: String): Unit = {
    respondWithProtoFrom(StatusCodes.OK, path)
  }

  override def respondWithProtoFrom[T <: Message: ClassTag](status: StatusCode, path: String): Unit = {
    responseStatus = Some(status)
    responseProto = Some(IO.toProto[T](path))
  }

  override def respondWithProtoFromJson[T <: Message: ClassTag](status: StatusCode, body: String): Unit = {
    responseStatus = Some(status)
    responseProto = Some(Protobuf.fromJson[T](IO.getProtoInstance, body))
  }

  override def respondWithXmlFrom(path: String): Unit = {
    respondWithXmlFrom(StatusCodes.OK, path)
  }

  override def respondWithXmlFrom(status: StatusCode, path: String): Unit = {
    responseStatus = Some(status)
    responseXml = Some(IO.toString(path))
  }

  override def respondWithHeader(httpHeader: Header): Unit = {
    responseHeaders(httpHeader.getName) = httpHeader.getValue
  }

  override def respondWithHeader(name: String, value: String): Unit = {
    responseHeaders(name) = value
  }

  // scalastyle:off method.length
  override def doRequest[R](
      name: String,
      request: HttpRequestBase,
      timeout: HttpRequestTimeout,
      idempotency: Idempotency,
      logParams: LogParams
  )(f: HttpResponse => R)(implicit trace: Traced, context: HttpRequestContext = new HttpRequestContext): Future[R] = {

    requestDone = true

    expectedMethod.foreach { method =>
      if (method.value != request.getRequestLine.getMethod) {
        throw new IllegalArgumentException(s"Expected method [$method] but got [${request.getRequestLine.getMethod}]")
      }
    }

    expectedUrl.foreach { url =>
      if (url != request.getRequestLine.getUri) {
        throw new IllegalArgumentException(s"Expected url [$url] but got [${request.getRequestLine.getUri}]")
      }
    }

    expectedHeaders.foreach {
      case (k, v) =>
        require(request.getHeaders(k).map(_.getValue).contains(v), s"Expected header $k to be equal to $v")
    }

    expectedJson.foreach { json =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toString(withEntity.getEntity)
          require(body == json, s"Expected json [$json] but got [$body]")
        case _ => sys.error(s"Expected request with entity but got $request")
      }
    }

    expectedJsonAssertions.foreach { assertion =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = Json.parse(EntityUtils.toString(withEntity.getEntity))
          assertion(body)
        case _ => sys.error(s"Expected request with entity but got $request")
      }
    }

    expectedString.foreach { string =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toString(withEntity.getEntity)
          require(body == string, s"Expected string [$string] but got [$body]")
        case _ => sys.error(s"Expected request with entity but got $request")
      }
    }

    expectFormData.foreach { expectedParams =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = URLDecoder.decode(EntityUtils.toString(withEntity.getEntity), "UTF-8")
          val requestParams = EntityUtils
            .toString(withEntity.getEntity)
            .split("&")
            .map(stringAsPair => stringAsPair.split("=").map(URLDecoder.decode(_, "UTF-8")))
            .map { array =>
              if (array.length == 2) {
                (array(0), array(1))
              } else {
                (array(0), "")
              }
            }
            .toMap

          expectedParams.foreach {
            case (key, value) =>
              val mappedValue = requestParams(key)
              mappedValue shouldBe value
          }
        case _ => sys.error(s"Expected request with entity but got $request")
      }
    }

    expectedProto.foreach { proto =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toByteArray(withEntity.getEntity)
          val parsed = proto.newBuilderForType().mergeFrom(body).build()
          require(parsed == proto, s"Expected json [$proto] but got [$parsed]")
        case _ => sys.error(s"Expected request with entity but got $request")
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

    responseManyProto.foreach { body =>
      res.setEntity(new ByteArrayEntity(Protobuf.writeDelimited(body), ProtobufContentType))
    }

    responseString.foreach { body =>
      res.setEntity(new StringEntity(body, ContentType.TEXT_PLAIN))
    }

    responseHeaders.foreach {
      case (k, v) =>
        res.setHeader(k, v)
    }

    Future.successful(f(res))
  }

  override def download[R](
      name: String,
      request: HttpRequestBase,
      timeout: HttpRequestTimeout,
      idempotency: Idempotency,
      logParams: LogParams
  )(f: (HttpResponse, File) => R)(implicit trace: Traced,
                                  context: HttpRequestContext = new HttpRequestContext): Future[R] = {

    requestDone = true

    expectedMethod.foreach { method =>
      if (method.value != request.getRequestLine.getMethod) {
        throw new IllegalArgumentException(s"Expected method [$method] but got [${request.getRequestLine.getMethod}]")
      }
    }

    expectedUrl.foreach { url =>
      if (url != request.getRequestLine.getUri) {
        throw new IllegalArgumentException(s"Expected url [$url] but got [${request.getRequestLine.getUri}]")
      }
    }

    expectedHeaders.foreach {
      case (k, v) =>
        require(request.getHeaders(k).map(_.getValue).contains(v), s"Expected header $k to be equal to $v")
    }

    expectedJson.foreach { json =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toString(withEntity.getEntity)
          require(body == json, s"Expected json [$json] but got [$body]")
        case _ => sys.error(s"Expected request with json but got $request")
      }
    }

    expectedJsonAssertions.foreach { assertion =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = Json.parse(EntityUtils.toString(withEntity.getEntity))
          assertion(body)
        case _ => sys.error(s"Expected request with entity but got $request")
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

    expectFormData.foreach { expectedParams =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = URLDecoder.decode(EntityUtils.toString(withEntity.getEntity), "UTF-8")
          val requestParams = EntityUtils
            .toString(withEntity.getEntity)
            .split("&")
            .map(stringAsPair => stringAsPair.split("=").map(URLDecoder.decode(_, "UTF-8")))
            .map { array =>
              if (array.length == 2) {
                (array(0), array(1))
              } else {
                (array(0), "")
              }
            }
            .toMap

          expectedParams.foreach {
            case (key, value) =>
              val mappedValue = requestParams(key)
              mappedValue shouldBe value
          }
        case _ => sys.error(s"Expected request with entity but got $request")
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

    responseManyProto.foreach { body =>
      res.setEntity(new ByteArrayEntity(Protobuf.writeDelimited(body), ProtobufContentType))
    }

    responseString.foreach { body =>
      res.setEntity(new StringEntity(body, ContentType.TEXT_PLAIN))
    }

    responseHeaders.foreach {
      case (k, v) =>
        res.setHeader(k, v)
    }

    Future.successful(f(res, file))
  }

  override def verifyRequestDone(): Unit = {
    if (!requestDone) {
      sys.error("client.doRequest() hasn't been invoked")
    }
  }

  // scalastyle:on method.length
}
