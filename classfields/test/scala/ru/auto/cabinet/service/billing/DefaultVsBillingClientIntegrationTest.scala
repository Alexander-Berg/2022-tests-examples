package ru.auto.cabinet.service.billing

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.Ignore
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.cabinet.model.billing.{Balance2, Order}
import ru.auto.cabinet.service.billing.VsBillingTestUtils._
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.trace.Context

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class DefaultVsBillingClientIntegrationTest
    extends AnyWordSpec
    with Matchers
    with ScalaFutures {
  implicit private val rc = Context.unknown

  implicit private val system: ActorSystem =
    ActorSystem("test-system", ConfigFactory.empty())
  implicit private val instr: Instr = new EmptyInstr("autoru")

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    Span(10, Seconds))

  val settings = VsBillingSettings(
    "http://billing-api-http-api.vrts-slb.test.vertis.yandex.net:80")
  val client = new DefaultVsBillingClient(settings)

  "DefaultVsBillingClient" should {
    "get dealer orders" in {
      val res =
        client.getOrders(dealerBalanceId, agencyBalanceId = None).futureValue
      res.total shouldBe 1
      val page = res.page
      page.size shouldBe 10
      page.number shouldBe 0
      res.values should matchPattern {
        case List(Order(`dealerOrderId`, Balance2(_))) =>
      }
    }

    "get agency orders" in {
      val res =
        client
          .getOrders(agencyDealerBalanceId, Some(agencyBalanceId))
          .futureValue
      res.total shouldBe 1
      val page = res.page
      page.size shouldBe 10
      page.number shouldBe 0
      res.values should matchPattern {
        case List(Order(`agencyOrderId`, Balance2(_))) =>
      }
    }
  }
}
