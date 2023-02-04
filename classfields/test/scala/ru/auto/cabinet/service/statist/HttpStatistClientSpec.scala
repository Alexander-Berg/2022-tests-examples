package ru.auto.cabinet.service.statist

import java.time.LocalDate
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.service.statist.StatistClient.NumericOfferId
import ru.auto.cabinet.trace.Context
import ru.auto.cabinet.util.TestServer
import ru.yandex.vertis.statist.model.api.ApiModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class HttpStatistClientSpec
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

  "getCountersMultiple" should "return MultipleDailyValues" in {
    val now = LocalDate.now()

    val domain = Domain.AutoruPublic
    val counter = Counter.EventTypePerCardByDay
    val id = NumericOfferId.fromOfferId("123").fold(throw _, identity)
    val component = Component.CardView
    val from: LocalDate = now.minusWeeks(1)
    val until: LocalDate = now

    val json = Source
      .fromInputStream(
        getClass.getResourceAsStream("/statist/getCountersMultiple.json")
      )
      .mkString

    val result = ApiModel.MultipleDailyValues
      .newBuilder()
      .putObjects(
        "123",
        ApiModel.ObjectDailyValues
          .newBuilder()
          .addDays {
            ApiModel.ObjectDayValues
              .newBuilder()
              .setDay("2021-07-01")
              .putComponents("card_view", 2)
              .putComponents("phone_call", 3)
          }
          .build()
      )
      .build()

    withServer {
      (get & pathPrefix(
        "api" / "1.x" / domain.entryName / "counters" / counter.entryName / "components" / "multiple" /
          "by-day"
      )) {
        complete(
          HttpEntity(ContentTypes.`application/json`, json)
        )
      }
    } { address =>
      val client =
        new HttpStatistClient(StatistClientSettings(address.futureValue))

      client
        .getCountersMultiple(domain, counter, id, component, from, until)
        .futureValue shouldBe result
    }
  }
}
