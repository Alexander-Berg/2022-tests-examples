package ru.auto.cabinet.service.salesman

import akka.stream.scaladsl.{Keep, Sink}
import com.typesafe.config.ConfigFactory
import org.scalatest.Ignore
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import ru.auto.cabinet.TestActorSystem
import ru.auto.cabinet.reporting.bundled.now
import ru.auto.cabinet.service.instr.EmptyInstr
import ru.auto.cabinet.trace.Context
import ru.auto.cabinet.util.TestServer

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class HttpSalesmanClientSpec
    extends AnyFlatSpec
    with Matchers
    with ScalaFutures
    with TestActorSystem
    with TestServer {
  implicit private val rc = Context.unknown

  implicit private val instr = new EmptyInstr("autoru")

  System.setProperty("config.resource", "test.conf")

  private val settings = SalesmanClientSettings(ConfigFactory.load())

  private val client =
    new HttpSalesmanClient(settings)

  behavior.of("ClientSalesmanService")

  ignore should "have something in response on last ten year activations request" in {
    implicit val patienceConfig = PatienceConfig(Span(10, Seconds))
    val quotas =
      client.stoPriorityServiceActivations(now().minusYears(10), now())
    quotas.futureValue.isEmpty shouldBe false
  }

  "HttpSalesmanClient" should "receive client match applications campaigns with stream" in {

    implicit val patienceConfig = PatienceConfig(Span(10, Seconds))

    val clientIds = List(20101L, 16453L)

    val campaigns =
      client
        .clientsMatchApplicationCampaigns(clientIds)
        .toMat(Sink.collection)(Keep.right)
        .run()
        .futureValue(Timeout(Span(1.5, Seconds)))

    campaigns.size shouldBe 2
    campaigns.map(_.clientId).forall(clientIds.contains) shouldBe true
  }

}
