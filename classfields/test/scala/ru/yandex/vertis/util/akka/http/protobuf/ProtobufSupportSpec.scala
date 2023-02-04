package ru.yandex.vertis.util.akka.http.protobuf

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.generators.BasicGenerators._
import ru.yandex.vertis.protobuf.TestMessageGenerators._
import ru.yandex.vertis.protobuf.test.Foo
import ru.yandex.vertis.util.akka.http.marshalling.DeferredMarshallerSpec

/**
  * Specs on [[ProtobufSupport]]
  *
  * @author darl
  * @author zvez
  */
class ProtobufSupportSpec
  extends DeferredMarshallerSpec
    with PropertyChecks {

  import ProtobufSupport._

  "ProtobufSupport" should {
    "support json" in {
      forAll(foo) { foo =>
        val route = complete { foo }
        Get() ~> addHeader(Accept(MediaTypes.`application/json`)) ~> route ~> check {
          contentType shouldBe ContentTypes.`application/json`
          responseAs[Foo] shouldBe foo
          responseAs[String] shouldBe Protobuf.toJson(foo)
        }
      }
    }

    "support multiple json" in {
      forAll(list(0, 10, foo)) { foos =>
        val route = complete { foos }
        Get() ~> addHeader(Accept(MediaTypes.`application/json`)) ~> route ~> check {
          contentType shouldBe ContentTypes.`application/json`
          responseAs[Seq[Foo]] shouldBe foos
          responseAs[String] shouldBe Protobuf.toJsonArray(foos)
        }
      }
    }

    "support protobuf" in {
      forAll(foo) { foo =>
        val route = complete { foo }
        Get() ~> addHeader(Accept(Protobuf.mediaType)) ~> route ~> check {
          contentType shouldBe Protobuf.contentType
          responseAs[Foo] shouldBe foo
          responseAs[Array[Byte]] shouldBe foo.toByteArray
        }
      }
    }

    "support multiple protobuf" in {
      forAll(list(0, 10, foo)) { foos =>
        val route = complete { foos }
        Get() ~> addHeader(Accept(Protobuf.mediaType)) ~> route ~> check {
          contentType shouldBe Protobuf.contentType
          responseAs[Seq[Foo]] shouldBe foos
          responseAs[Array[Byte]] shouldBe Protobuf.writeDelimited(foos)
        }
      }
    }

    "support application/octet-stream" in {
      forAll(foo) { foo =>
        val route = complete { foo }
        Get() ~> addHeader(Accept(MediaTypes.`application/octet-stream`)) ~> route ~> check {
          contentType shouldBe ContentTypes.`application/octet-stream`
          responseAs[Foo] shouldBe foo
          responseAs[Array[Byte]] shouldBe foo.toByteArray
        }
      }
    }

    "fail on unsupported content types" in {
      forAll(foo) { foo =>
        val route = complete { foo }
        Get() ~> addHeader(Accept(MediaTypes.`text/plain`)) ~> Route.seal(route) ~> check {
          status shouldBe StatusCodes.NotAcceptable
        }
      }
    }

    "use json by default" in {
      forAll(foo) { foo =>
        val route = complete { foo }
        Get() ~> route ~> check {
          contentType shouldBe ContentTypes.`application/json`
          responseAs[Foo] shouldBe foo
          responseAs[String] shouldBe Protobuf.toJson(foo)
        }
      }
    }

    "provide autoUnmarshaller" in {
      forAll(foo) { foo =>
        val route = complete { foo }
        Get() ~> addHeader(Accept(MediaTypes.`application/json`)) ~> route ~> check {
          contentType shouldBe ContentTypes.`application/json`
          responseAs[Foo] shouldBe foo
          responseAs[String] shouldBe Protobuf.toJson(foo)
        }
      }
    }
  }
}
