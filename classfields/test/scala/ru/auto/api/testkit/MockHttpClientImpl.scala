package ru.auto.api.testkit

import akka.http.javadsl.model.HttpMethod
import akka.http.scaladsl.model.{HttpMethods, StatusCode, StatusCodes}
import com.google.protobuf.Message
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.{ByteArrayEntity, ContentType, StringEntity}
import org.apache.http.impl.DefaultHttpResponseFactory
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpResponse, _}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsLookupResult, JsValue, Json}
import ru.auto.api.http.{CacheProps, HttpClient, LogParams, NoCaching, ProtobufContentType, RequestProps}
import ru.auto.api.util.{Protobuf, Resources}
import ru.yandex.vertis.tracing.Traced

import java.net.URLDecoder
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Try

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
class MockHttpClientImpl extends HttpClient with Matchers with MockHttpClient {
  private var expectedMethod: Option[HttpMethod] = None
  private var expectedUrl: Option[String] = None
  private var expectedHeaders: mutable.Map[String, String] = mutable.HashMap[String, String]()
  private var expectedJson: Option[String] = None
  private var expectedJsonAssertions: ArrayBuffer[JsonAssertion] = ArrayBuffer[JsonAssertion]()
  private var expectedString: Option[String] = None
  private var expectFormData: Option[Map[String, String]] = None
  private var expectedProto: Option[Message] = None
  private var responseStatus: Option[StatusCode] = None
  private var responseJson: Option[String] = None
  private var responseProto: Option[Message] = None
  private var responseManyProto: Option[Iterable[Message]] = None
  private var responseString: Option[String] = None
  private var responseXml: Option[String] = None
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
    responseJson = None
    responseProto = None
    responseManyProto = None
    responseString = None
    responseXml = None
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

  private case class JsonAssertion(f: JsValue => Assertion) {

    def apply(value: JsValue): Assertion = f(value)
  }

  /**
    * Вытаскивает из json-тела http-запроса поле.
    * Сравнивает его с ожидаемым значением (см. [[JsonLookup.shouldBe]]).
    * @example http.expectJsonField(_ \ "fieldName1" \ "fieldName2") shouldBe JsBoolean(true)
    */
  def expectJsonField(getField: JsValue => JsLookupResult): JsonLookup =
    new JsonLookup {

      override def shouldBe(expected: JsValue): Unit = {
        expectedJsonAssertions += JsonAssertion(jsValue => getField(jsValue).get shouldBe expected)
      }
    }

  override def expectString(string: String): Unit = expectedString = Some(string)

  override def expectFormData(params: Map[String, String]): Unit = expectFormData = Some(params)

  override def expectProto(proto: Message): Unit = expectedProto = Some(proto)

  override def respondWithStatus(status: StatusCode): Unit = responseStatus = Some(status)

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
    responseJson = Some(Resources.toString(path))
  }

  override def respondWith[T <: Message](body: T): Unit = {
    respondWith(StatusCodes.OK, body)
  }

  override def respondWithMany[T <: Message](body: Iterable[T]): Unit = {
    respondWithMany(StatusCodes.OK, body)
  }

  override def respondWith[T <: Message](status: StatusCode, body: T): Unit = {
    responseStatus = Some(status)
    responseProto = Some(body)
  }

  override def respondWithMany[T <: Message](status: StatusCode, body: Iterable[T]): Unit = {
    responseStatus = Some(status)
    responseManyProto = Some(body)
  }

  override def respondWith(body: String): Unit = {
    respondWith(StatusCodes.OK, body)
  }

  override def respondWith(status: StatusCode, body: String): Unit = {
    responseStatus = Some(status)
    responseString = Some(body)
  }

  override def respondWithProto[T <: Message: ClassTag](status: StatusCode, message: T): Unit = {
    responseStatus = Some(status)
    responseProto = Some(message)
  }

  override def respondWithProtoFrom[T <: Message: ClassTag](path: String): Unit = {
    respondWithProtoFrom(StatusCodes.OK, path)
  }

  override def respondWithProtoFrom[T <: Message: ClassTag](status: StatusCode, path: String): Unit = {
    responseStatus = Some(status)
    responseProto = Some(Resources.toProto[T](path))
  }

  override def respondWithProtoFromJson[T <: Message: ClassTag](status: StatusCode, body: String): Unit = {
    responseStatus = Some(status)
    responseProto = Some(Protobuf.fromJson[T](body))
  }

  override def respondWithXmlFrom(path: String): Unit = {
    respondWithXmlFrom(StatusCodes.OK, path)
  }

  override def respondWithXmlFrom(status: StatusCode, path: String): Unit = {
    responseStatus = Some(status)
    responseXml = Some(Resources.toString(path))
  }

  //scalastyle:off method.length
  override def doRequest[R](
      name: String,
      request: HttpRequestBase,
      props: RequestProps = RequestProps(),
      cacheProps: CacheProps[R],
      logParams: LogParams
  )(f: HttpResponse => R)(implicit trace: Traced): Future[R] = {
    assert(cacheProps == NoCaching)

    requestDone = true

    expectedMethod.foreach(method => method.value shouldBe request.getRequestLine.getMethod)

    expectedUrl.foreach(url => url shouldBe request.getRequestLine.getUri)

    expectedHeaders.foreach {
      case (k, v) =>
        request.getHeaders(k).map(_.getValue) should contain(v)
    }

    expectedJson.foreach { json =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toString(withEntity.getEntity)
          body shouldBe json
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
          body shouldBe string
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
          parsed shouldBe proto
        case _ => sys.error(s"Expected request with entity but got $request")
      }
    }

    val status = responseStatus.getOrElse(sys.error("`respondWithStatus` not called"))
    val res = DefaultHttpResponseFactory.INSTANCE
      .newHttpResponse(HttpVersion.HTTP_1_1, status.intValue(), null)

    responseJson.foreach(json => res.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON)))

    responseXml.foreach { xml =>
      res.setEntity(new StringEntity(xml, ContentType.create("application/xml", Consts.UTF_8)))
    }

    responseProto.foreach(proto => res.setEntity(new ByteArrayEntity(proto.toByteArray, ProtobufContentType)))

    responseString.foreach(body => res.setEntity(new StringEntity(body, ContentType.TEXT_PLAIN)))

    responseManyProto.foreach { body =>
      res.setEntity(new ByteArrayEntity(Protobuf.writeIterable(body), ProtobufContentType))
    }

    Future.fromTry(Try(f(res)))
  }

  //scalastyle:on method.length

  override def verifyRequestDone(): Unit = {
    if (!requestDone) {
      sys.error("client.doRequest() hasn't been invoked")
    }
  }
}
