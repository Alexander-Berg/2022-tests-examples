package ru.auto.api.util

import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.model.ModelGenerators
import org.scalatest.matchers.should.Matchers._
import ru.auto.api.CounterModel.AggregatedCounter

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 09.02.17
  */
class ProtobufSupportTest extends AnyFunSuite with ScalaCheckPropertyChecks with ScalatestRouteTest {
  import ProtobufSupport.protobufMessageToEntityMarshaller

  implicit private val offerUnmarshaller: FromEntityUnmarshaller[Offer] =
    ProtobufSupport.protobufMessageFromEntityUnmarshaller(Offer.getDefaultInstance)

  test("round trip to json") {
    forAll(ModelGenerators.OfferGen) { offer =>
      val route = complete(offer)
      Get() ~> addHeader(Accept(MediaTypes.`application/json`)) ~> route ~> check {
        contentType shouldBe ContentTypes.`application/json`
        responseAs[Offer] shouldBe offer
        responseAs[String] shouldBe Protobuf.toJson(offer)
      }
    }
  }

  test("round trip to protobuf") {
    forAll(ModelGenerators.OfferGen) { offer =>
      val route = complete(offer)
      Get() ~> addHeader(Accept(Protobuf.mediaType)) ~> route ~> check {
        contentType shouldBe Protobuf.contentType
        responseAs[Offer] shouldBe offer
        responseAs[Array[Byte]] shouldBe offer.toByteArray
      }
    }
  }

  test("support application/octet-stream") {
    forAll(ModelGenerators.OfferGen) { offer =>
      val route = complete(offer)
      Get() ~> addHeader(Accept(MediaTypes.`application/octet-stream`)) ~> route ~> check {
        contentType shouldBe ContentTypes.`application/octet-stream`
        responseAs[Offer] shouldBe offer
        responseAs[Array[Byte]] shouldBe offer.toByteArray
      }
    }
  }

  test("fail on unsupported content types") {
    forAll(ModelGenerators.OfferGen) { offer =>
      val route = complete(offer)
      Get() ~> addHeader(Accept(MediaTypes.`text/plain`)) ~> Route.seal(route) ~> check {
        status shouldBe StatusCodes.NotAcceptable
      }
    }
  }

  test("defaults to json") {
    forAll(ModelGenerators.OfferGen) { offer =>
      val route = complete(offer)
      Get() ~> route ~> check {
        contentType shouldBe ContentTypes.`application/json`
        responseAs[Offer] shouldBe offer
        responseAs[String] shouldBe Protobuf.toJson(offer)
      }
    }
  }

  test("autoUnmarshaller") {
    import ProtobufSupport._

    forAll(ModelGenerators.CountersGen) { counters =>
      val route = complete(counters)
      Get() ~> addHeader(Accept(MediaTypes.`application/json`)) ~> route ~> check {
        contentType shouldBe ContentTypes.`application/json`
        responseAs[AggregatedCounter] shouldBe counters
        responseAs[String] shouldBe Protobuf.toJson(counters)
      }
    }
  }
}
