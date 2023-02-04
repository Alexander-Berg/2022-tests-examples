package ru.yandex.vertis.billing.banker.util

import akka.http.scaladsl.model.{
  ContentType,
  ContentTypes,
  HttpEntity,
  HttpHeader,
  HttpRequest,
  HttpResponse,
  StatusCode,
  StatusCodes
}
import akka.stream.Materializer
import akka.util.ByteString
import com.google.protobuf.Message
import ru.yandex.vertis.billing.banker.util.AkkaHttpUtil.HttpResponder
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf
import ru.yandex.vertis.util.crypto.UrlEncodedUtils
import spray.json.{JsValue, JsonParser, ParserInput}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

/**
  * @author alex-kovalenko
  */
trait AkkaHttpTestUtils {

  class MockHttpResponder(implicit mat: Materializer, timeout: FiniteDuration, ec: ExecutionContext)
    extends HttpResponder {

    private var expectedHeaders: ArrayBuffer[HttpHeader] = ArrayBuffer.empty
    private var expectedJson: Option[JsValue] = None
    private var expectedProto: Option[Message] = None
    private var expectedManyProto: Option[Iterable[Message]] = None
    private var expectedMultipartUrlencoded: Option[Seq[(String, String)]] = None
    private var responseStatus: Option[StatusCode] = None
    private var responseJson: Option[String] = None
    private var responseContentType: Option[ContentType] = None

    def reset(): Unit = {
      expectedHeaders = ArrayBuffer.empty
      expectedJson = None
      expectedProto = None
      expectedManyProto = None
      expectedMultipartUrlencoded = None
      responseStatus = None
      responseJson = None
      responseContentType = None
    }

    def expectHeaders(headers: HttpHeader*): Unit = {
      expectedHeaders ++= headers: Unit
    }

    def expectJson(json: JsValue): Unit = {
      expectedJson = Some(json)
    }

    def expectedProto(message: Message): Unit = {
      expectedProto = Some(message)
    }

    def expectedManyProto(messages: Iterable[Message]): Unit = {
      expectedManyProto = Some(messages)
    }

    def expectMultipartUrlencoded(params: Seq[(String, String)]): Unit = {
      expectedMultipartUrlencoded = Some(params)
    }

    def respondWithJson(status: StatusCode, ct: ContentType, json: String): Unit = {
      responseStatus = Some(status)
      responseJson = Some(json)
      responseContentType = Some(ct)
    }

    def respondWithJson(ct: ContentType, json: String): Unit = {
      respondWithJson(StatusCodes.OK, ct, json)
    }

    def respondWithStatus(status: StatusCode): Unit = {
      responseStatus = Some(status)
    }

    def apply(req: HttpRequest): Future[HttpResponse] =
      for {
        _ <- Future {
          expectedHeaders.foreach { h =>
            require(req.headers.contains(h), s"Expected header [$h], but got [${req.headers.mkString(", ")}]")
          }
        }
        _ <- expectedJson
          .map { json =>
            req.entity.toStrict(timeout).map { e =>
              require(
                e.contentType.mediaType.value == "application/json",
                s"Not json content-type: ${e.contentType.mediaType.value}"
              )
              val got = JsonParser(ParserInput(e.data.toArray))
              require(got == json, s"Expected ${json.prettyPrint}, but got ${got.prettyPrint}")
            }
          }
          .getOrElse(Future.unit)
        _ <- expectedProto
          .map { msg =>
            req.entity.toStrict(timeout).map { e =>
              require(
                e.contentType.mediaType.value == "application/protobuf",
                s"Not protobuf content-type: ${e.contentType.mediaType.value}"
              )
              val got = msg.getParserForType.parseFrom(e.data.toArray)
              require(got == msg, s"Expected $msg, but got $got")
            }
          }
          .getOrElse(Future.unit)
        _ <- expectedManyProto
          .map { msgs =>
            req.entity.toStrict(timeout).map { e =>
              require(
                e.contentType.mediaType.value == "application/protobuf",
                s"Not protobuf content-type: ${e.contentType.mediaType.value}"
              )
              val gots = Protobuf.parseDelimited(msgs.head.getDefaultInstanceForType, e.data.toArray)
              require(gots.size == msgs.size, s"Expected ${msgs.size}, but got ${gots.size}")
              val gotSet = gots.toSet
              msgs.foreach { msg =>
                require(gotSet(msg), s"Not found expected message $msg")
              }
            }
          }
          .getOrElse(Future.unit)
        _ <- expectedMultipartUrlencoded
          .map { params =>
            req.entity.toStrict(timeout).map { e =>
              require(
                e.contentType.mediaType.value == "application/x-www-form-urlencoded",
                s"Not application/x-www-form-urlencoded content-type: ${e.contentType.mediaType.value}"
              )
              val formFields = UrlEncodedUtils.from(new String(e.data.toArray)).get
              require(formFields.size == params.size, s"Expected ${params.size}, but got ${formFields.size}")
              val paramsSet = params.toSet
              formFields.foreach { field =>
                require(paramsSet(field), s"Not found expected param [$field]")
              }
            }
          }
          .getOrElse(Future.unit)
        status = responseStatus.getOrElse(StatusCodes.OK)
        response = responseJson
          .map { json =>
            val ct = responseContentType.getOrElse(ContentTypes.`application/json`)
            HttpResponse(status, entity = HttpEntity.Strict(ct, ByteString(json)))
          }
          .getOrElse(HttpResponse(status))
      } yield response
  }
}

object AkkaHttpTestUtils extends AkkaHttpTestUtils
