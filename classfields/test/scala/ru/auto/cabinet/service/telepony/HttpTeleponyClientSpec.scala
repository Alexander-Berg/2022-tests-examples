package ru.auto.cabinet.service.telepony

import java.time.LocalDateTime
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.trace.Context
import ru.auto.cabinet.util.TestServer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class HttpTeleponyClientSpec
    extends FlatSpec
    with Matchers
    with ScalaFutures
    with TestServer {
  implicit private val rc = Context.unknown

  implicit private val system: ActorSystem =
    ActorSystem("test-system", ConfigFactory.empty())
  implicit private val instr: Instr = new EmptyInstr("autoru")

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    Span(10, Seconds))

  "getOrCreate" should "return Redirect" in {
    val target = Phone("+79999999999")
    val objectId = "objectId"
    val tag = "tag"
    val json = Source
      .fromInputStream(
        getClass.getResourceAsStream("/telepony/getOrCreate.json")
      )
      .mkString

    withServer {
      (post & pathPrefix(
        "api" / "2.x" / "autoru_billing" / "redirect" / "getOrCreate" / "objectId"
      )) {
        complete(
          HttpEntity(ContentTypes.`application/json`, json)
        )
      }
    } { address =>
      val client =
        new HttpTeleponyClient(TeleponyClientSettings(address.futureValue))

      client
        .getOrCreate(target, objectId, Some(tag))
        .futureValue shouldBe Redirect(
        "1",
        Phone("+70000000000"),
        target,
        LocalDateTime.of(2020, 9, 3, 0, 0),
        objectId,
        Some(tag)
      )
    }
  }
}
