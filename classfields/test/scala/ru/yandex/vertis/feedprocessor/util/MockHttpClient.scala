package ru.yandex.vertis.feedprocessor.util

import akka.http.scaladsl.model.{HttpMethod, HttpMethods, StatusCode, StatusCodes}
import com.google.protobuf.Message
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.{ByteArrayEntity, ContentType, StringEntity}
import org.apache.http.impl.DefaultHttpResponseFactory
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpEntityEnclosingRequest, HttpResponse, HttpVersion}
import ru.yandex.vertis.feedprocessor.http.{HostPort, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
class MockHttpClient extends HttpClient {
  implicit override val ec: ExecutionContext = ExecutionContext.Implicits.global

  override def serviceName: String = "???"

  override def hostPort: HostPort = HostPort("localhost", -1)

  private var expectedMethod: Option[HttpMethod] = None
  private var expectedUrl: Option[String] = None
  private var expectedJson: Option[String] = None
  private var expectedProto: Option[Message] = None
  private var responseStatus: Option[StatusCode] = None
  private var responseJson: Option[String] = None
  private var responseProto: Option[Message] = None
  private var responseString: Option[String] = None

  def reset(): Unit = {
    expectedMethod = None
    expectedUrl = None
    expectedJson = None
    expectedProto = None
    responseStatus = None
    responseJson = None
    responseProto = None
    responseString = None
  }

  def expect(method: HttpMethod, url: String): Unit = {
    expectedMethod = Some(method)
    expectedUrl = Some(url)
  }

  def expect(url: String): Unit = expect(HttpMethods.GET, url)

  def expectJson(json: String): Unit = expectedJson = Some(json)

  def expectProto(proto: Message): Unit = expectedProto = Some(proto)

  def respondWithStatus(status: StatusCode): Unit = responseStatus = Some(status)

  def respondWithJson(body: String): Unit = {
    respondWithJson(StatusCodes.OK, body)
  }

  def respondWithJson(status: StatusCode, body: String): Unit = {
    responseStatus = Some(status)
    responseJson = Some(body)
  }

  def respondWithJsonFrom(path: String): Unit = {
    respondWithJsonFrom(StatusCodes.OK, path)
  }

  def respondWithJsonFrom(status: StatusCode, path: String): Unit = {
    responseStatus = Some(status)
    responseJson = Some(Resources.toString(path))
  }

  def respondWith[T <: Message](body: T): Unit = {
    respondWith(StatusCodes.OK, body)
  }

  def respondWith[T <: Message](status: StatusCode, body: T): Unit = {
    responseStatus = Some(status)
    responseProto = Some(body)
  }

  def respondWith(body: String): Unit = {
    respondWith(StatusCodes.OK, body)
  }

  def respondWith(status: StatusCode, body: String): Unit = {
    responseStatus = Some(status)
    responseString = Some(body)
  }

  //scalastyle:off method.length
  override def doRequest[R](name: String, request: HttpRequestBase)(f: (HttpResponse) => R): Future[R] = {

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

    expectedJson.foreach { json =>
      request match {
        case withEntity: HttpEntityEnclosingRequest =>
          val body = EntityUtils.toString(withEntity.getEntity)
          require(body == json, s"Expected json [$json] but got [$body]")
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
      res.setEntity(new ByteArrayEntity(proto.toByteArray, ContentType.parse("application/protobuf")))
    }

    responseString.foreach { body =>
      res.setEntity(new StringEntity(body, ContentType.TEXT_PLAIN))
    }

    Future.successful(f(res))
  }

  //scalastyle:on method.length
}
