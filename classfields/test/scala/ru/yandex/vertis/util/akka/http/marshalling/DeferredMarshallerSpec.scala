package ru.yandex.vertis.util.akka.http.marshalling

import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, MediaRange, MediaTypes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import ru.yandex.vertis.util.akka.http.AkkaHttpSpecBase
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf
import scala.collection.immutable


/**
  *
  * @author zvez
  */
class DeferredMarshallerSpec extends AkkaHttpSpecBase {

  private class DistinctException extends IllegalArgumentException

  def req(mediaRanges: MediaRange*): HttpRequest =
    HttpRequest(uri = "/test").withHeaders(Accept(immutable.Seq(mediaRanges :_*)))

  implicit class ResponseHelper(resp: HttpResponse) {
    def asString = Unmarshal(resp).to[String].futureValue
  }

  "Marshaller.oneOf " should {

    "call the right marshaller only" in {
      implicit val composedMarshaller = {
        val jsonMarshaller = Marshaller
          .stringMarshaller(MediaTypes.`application/json`)
          .compose[String] { v =>
          "ok"
        }
        val protoMarshaller = Marshaller
          .byteArrayMarshaller(Protobuf.mediaType)
          .compose[String] { _ =>
          throw new DistinctException
        }

        Marshaller.oneOf(jsonMarshaller, protoMarshaller)
      }

      val request = req(MediaTypes.`application/json`)

      Marshal("something").toResponseFor(request).futureValue.asString shouldBe "ok"
    }

  }

}
