package ru.auto.cabinet.service.vos

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpec => WordSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.trace.Context
import ru.auto.cabinet.util.TestServer

import scala.concurrent.ExecutionContext.Implicits.global

class HttpVosClientSpec
    extends WordSpec
    with Matchers
    with ScalaFutures
    with TestServer
    with BeforeAndAfterAll {

  implicit private val system: ActorSystem =
    ActorSystem("test-system", ConfigFactory.empty())
  implicit private val instr: Instr = new EmptyInstr("autoru")
  implicit private val rc = Context.unknown

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    Span(10, Seconds))

  override protected def afterAll(): Unit = system.terminate().futureValue

  "HttpVosClient" should {
    "obtain vin numbers by offer ids" in {
      val json =
        """
           [
            {"offer_id": "id1-hash1", "vin": "vin1"},
            {"offer_id": "id2-hash2", "vin": "vin2"}
           ]
        """
      withServer {
        (put & pathPrefix(
          "api" / "v1" / "offers" / "dealer:1" / "vins"
        )) {
          complete(HttpEntity(ContentTypes.`application/json`, json))
        }
      } { address =>
        val client =
          new HttpVosClient(
            VosConfig(
              address.futureValue.toString(),
              rps = 10,
              actorName = "test-vos-client-1"))

        client
          .obtainVinByHashedId(1L, Seq("id1-hash1", "id2-hash2", "id3-hash3"))
          .futureValue shouldBe Map(
          "id1-hash1" -> Some("vin1"),
          "id2-hash2" -> Some("vin2"),
          "id3-hash3" -> None
        )
      }
    }

    "obtain hash" in {
      val json = """["id1-hash1", "id2-hash2", "id3-hash3"]"""
      withServer {
        (put & pathPrefix(
          "api" / "v1" / "offers" / "all" / "dealer:1" / "i-refs" / "hashed"
        )) {
          complete(HttpEntity(ContentTypes.`application/json`, json))
        }
      } { address =>
        val client =
          new HttpVosClient(
            VosConfig(
              address.futureValue.toString(),
              rps = 10,
              actorName = "test-vos-client-2"))

        client
          .obtainOfferHashById(1L, Seq("id1", "id2", "id3"))
          .futureValue shouldBe Map(
          "id1" -> "hash1",
          "id2" -> "hash2",
          "id3" -> "hash3"
        )
      }
    }

    "return Map[Vin, OfferId]" in {
      val json = """{"VIN1": "OfferId1", "VIN2": "OfferId2"}"""
      withServer {
        (put & pathPrefix(
          "api" / "v1" / "offers" / "dealer:1" / "offer_ids" / "by_vins"
        )) {
          complete(HttpEntity(ContentTypes.`application/json`, json))
        }
      } { address =>
        val client =
          new HttpVosClient(
            VosConfig(
              address.futureValue.toString(),
              rps = 10,
              actorName = "test-vos-client-3"))

        client.getOfferIds(1L, Set("VIN1", "VIN2")).futureValue shouldBe Map(
          "VIN1" -> "OfferId1",
          "VIN2" -> "OfferId2"
        )
      }
    }
  }
}
