package ru.auto.cabinet.service.public_api

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.search.SearchModel.State
import ru.auto.api.ResponseModel.OfferResponse
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.trace.Context
import ru.auto.cabinet.util.TestServer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class HttpPublicApiClientSpec
    extends FlatSpec
    with Matchers
    with ScalaFutures
    with TestServer {

  implicit private val system: ActorSystem =
    ActorSystem("test-system", ConfigFactory.empty())
  implicit private val instr: Instr = new EmptyInstr("autoru")
  implicit private val rc = Context.unknown

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    Span(10, Seconds))

  "getOffer" should "return an Offer by id" in {
    val offerId = "1"
    val sessionId = "2"
    val category = Category.CARS

    val json = Source
      .fromInputStream(
        getClass.getResourceAsStream("/public_api/getOffer.json")
      )
      .mkString

    withServer {
      (get & pathPrefix("1.0" / "offer" / category.toString / offerId)) {
        headerValueByName(HttpPublicApiClient.sessionHeaderName) {
          sessionHeaderId =>
            sessionHeaderId shouldBe sessionId
            complete(HttpEntity(ContentTypes.`application/json`, json))
        }
      }
    } { address =>
      val client = new HttpPublicApiClient(
        PublicApiSettings(address.futureValue.toString, "auth"))

      client
        .getOffer(offerId, category.toString, Some(sessionId))
        .futureValue shouldBe a[OfferResponse]
    }
  }

  "getSalons" should "return all Salons by mark using pagination" in {
    val json = Source
      .fromInputStream(
        getClass.getResourceAsStream("/public_api/getSalons.json")
      )
      .mkString

    withServer {
      (get & pathPrefix("1.0" / "salon" / "search")) {
        complete(HttpEntity(ContentTypes.`application/json`, json))
      }
    } { address =>
      val client = new HttpPublicApiClient(
        PublicApiSettings(address.futureValue.toString, "auth"))

      client
        .getSalons("", Category.CARS, pageSize = 10)
        .runWith(Sink.seq)
        .futureValue
        .length shouldBe 10
    }
  }

  "getFirstOffer" should "return any Offer" in {
    val dealerId = "95"
    val mark = "Lightning"
    val model = "McQueen"

    val json = Source
      .fromInputStream(
        getClass.getResourceAsStream("/public_api/getFirstOffer.json")
      )
      .mkString

    withServer {
      (get & pathPrefix("1.0" / "search" / "cars")) {
        complete(HttpEntity(ContentTypes.`application/json`, json))
      }
    } { address =>
      val client = new HttpPublicApiClient(
        PublicApiSettings(address.futureValue.toString, "auth"))

      client
        .getFirstOffer(dealerId, mark, model, State.NEW)
        .futureValue
        .getId shouldBe dealerId
    }
  }
}
