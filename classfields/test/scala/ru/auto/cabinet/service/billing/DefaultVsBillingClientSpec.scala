package ru.auto.cabinet.service.billing

import java.time.LocalDate
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.util.TestServer

import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global
import VsBillingTestUtils._
import ru.auto.cabinet.model.billing.{
  Balance2,
  CreateClientRequest,
  CreateClientResponse,
  Order,
  OverdraftResponse
}
import ru.auto.cabinet.trace.Context

class DefaultVsBillingClientSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with TestServer {
  implicit private val rc = Context.unknown

  implicit private val system: ActorSystem =
    ActorSystem("test-system", ConfigFactory.empty())
  implicit private val instr: Instr = new EmptyInstr("autoru")

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    Span(10, Seconds))

  "DefaultVsBilling.getOrders()" should {

    "get dealer orders" in {
      val json = Source
        .fromInputStream(
          getClass.getResourceAsStream("/billing/order_client_response.json")
        )
        .mkString

      withServer {
        (get & path(
          "api" / "1.x" / "service" / "autoru" / "customer" / "client" / dealerBalanceId.toString / "order")) {
          headerValueByName("X-Billing-User") { billingUser =>
            billingUser shouldBe "autoru:salesman"
            complete(HttpEntity(ContentTypes.`application/json`, json))
          }
        }
      } { address =>
        val client = new DefaultVsBillingClient(
          VsBillingSettings(address.futureValue.toString))

        val res =
          client.getOrders(dealerBalanceId, agencyBalanceId = None).futureValue

        res.total shouldBe 1
        val page = res.page
        page.size shouldBe 10
        page.number shouldBe 0
        res.values shouldBe List(Order(dealerOrderId, Balance2(9095334909L)))
      }
    }

    "get agency orders" in {
      val json = Source
        .fromInputStream(
          getClass.getResourceAsStream("/billing/order_agency_response.json")
        )
        .mkString

      withServer {
        (get & path(
          "api" / "1.x" / "service" / "autoru" / "customer" / "agency" / agencyBalanceId.toString / "client" / agencyDealerBalanceId.toString / "order")) {
          headerValueByName("X-Billing-User") { billingUser =>
            billingUser shouldBe "autoru:salesman"
            complete(HttpEntity(ContentTypes.`application/json`, json))
          }
        }
      } { address =>
        val client = new DefaultVsBillingClient(
          VsBillingSettings(address.futureValue.toString))

        val res = client
          .getOrders(agencyDealerBalanceId, Some(agencyBalanceId))
          .futureValue

        res.total shouldBe 1
        val page = res.page
        page.size shouldBe 10
        page.number shouldBe 0
        res.values shouldBe List(Order(agencyOrderId, Balance2(9100000)))
      }
    }
  }

  "DefaultVsBilling.getOverdraft()" should {

    "get dealer overdraft" in {
      val json = Source
        .fromInputStream(
          getClass.getResourceAsStream("/billing/get_overdraft_response.json")
        )
        .mkString

      withServer {
        (get & path(
          "api" / "1.x" / "service" / "autoru" / "client" / "notifyClient" / dealerBalanceId.toString)) {
          headerValueByName("X-Billing-User") { billingUser =>
            billingUser shouldBe "autoru:salesman"
            complete(HttpEntity(ContentTypes.`application/json`, json))
          }
        }
      } { address =>
        val client = new DefaultVsBillingClient(
          VsBillingSettings(address.futureValue.toString))

        val res = client.getOverdraft(dealerBalanceId).futureValue

        res shouldBe Some {
          OverdraftResponse(
            8168241,
            21418000L,
            100L,
            Some(LocalDate.parse("2018-11-30")),
            overdraftBan = false
          )
        }
      }
    }

    "get dealer overdraft with empty response" in {
      withServer {
        (get & path(
          "api" / "1.x" / "service" / "autoru" / "client" / "notifyClient" / dealerBalanceId.toString)) {
          headerValueByName("X-Billing-User") { billingUser =>
            billingUser shouldBe "autoru:salesman"
            complete(StatusCodes.NotFound -> "no content")
          }
        }
      } { address =>
        val client = new DefaultVsBillingClient(
          VsBillingSettings(address.futureValue.toString))

        val res = client.getOverdraft(dealerBalanceId).futureValue

        res shouldBe None
      }
    }
  }

  "DefaultVsBilling.createYaBalanceClient()" should {

    "post a client creation request" in {
      val json = Source
        .fromInputStream(
          getClass.getResourceAsStream(
            "/billing/post_create_client_response.json")
        )
        .mkString

      withServer {
        (post & path(
          "api" / "1.x" / "service" / "autoru" / "balance" / "client")) {
          headerValueByName("X-Yandex-Operator-Uid") { uid =>
            uid shouldEqual "some-operator-uid"
            complete(HttpEntity(ContentTypes.`application/json`, json))
          }
        }
      } { address =>
        val client = new DefaultVsBillingClient(
          VsBillingSettings(address.futureValue.toString))
        val req = CreateClientRequest(
          Some(1234L),
          Some(true),
          "SomeName",
          Some("IndividualPerson"),
          Some("some@example.com"),
          None,
          None,
          None,
          None,
          Some("Moscow"),
          None)
        val res =
          client.createYaBalanceClient(req, "some-operator-uid").futureValue
        res shouldBe CreateClientResponse(
          Some(1234L),
          Some(true),
          "SomeName",
          Some("IndividualPerson"),
          Some("some@example.com"),
          Some("8-987-654-32-10"),
          Some("8(123)4567890"),
          Some("example.com"),
          Some(42),
          Some("Moscow"),
          Some(213)
        )
      }
    }

  }
}
