package auto.common.clients.billing.test

import auto.common.clients.billing.{BillingClient, BillingClientLive}
import auto.common.clients.billing.model._
import common.zio.sttp.endpoint.Endpoint
import common.zio.sttp.Sttp
import common.zio.uuid.UUID
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.Response
import zio.test.{ZSpec, _}
import zio.test.Assertion._

import scala.io.Source

object BillingClientLiveSpec extends DefaultRunnableSpec {
  val balanceClientId: BalanceClientId = BalanceClientId(1)
  val balanceAgencyId: BalanceAgencyId = BalanceAgencyId(2)
  val autoruClientId: AutoruClientId = AutoruClientId(3)
  val balancePersonId: BalancePersonId = BalancePersonId(4)

  private val sttpStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case req
        if req.uri.path.mkString("/") == "api/1.x/service/autoru/customer/client/1/order" &&
          req.uri.querySegments.length == 3 =>
      Response.ok(Source.fromResource("getDirectCustomerOrders.json")(scala.io.Codec.UTF8).getLines().mkString)
    case req
        if req.uri.path.mkString("/") == "api/1.x/service/autoru/customer/agency/2/client/1/order" &&
          req.uri.querySegments.length == 3 =>
      Response.ok(Source.fromResource("getAgencyCustomerOrders.json")(scala.io.Codec.UTF8).getLines().mkString)
    case req if req.uri.path.mkString("/") == "api/1.x/service/autoru/client/notifyClient/1" =>
      Response.ok(Source.fromResource("getOverdraft.json")(scala.io.Codec.UTF8).getLines().mkString)
    case req if req.uri.path.mkString("/") == "api/1.x/service/autoru/requisites/3/requisites" =>
      Response.ok(Source.fromResource("getRequisites.json")(scala.io.Codec.UTF8).getLines().mkString)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("BillingClientLive")(
      testM("getDirectCustomerOrders should return balance by clientId") {
        BillingClient.getDirectCustomerOrders(balanceClientId, 0, BillingClientLive.defaultPageSize).map { result =>
          assert(result)(
            equalTo(OrdersResponse(1, 0, 0, Seq(Order(1, 2, None, OrderBalance(100, 200, Some(300L), 400)))))
          )
        }
      },
      testM("getAgencyCustomerOrders should return balance by clientId and agencyId") {
        BillingClient
          .getAgencyCustomerOrders(balanceClientId, balanceAgencyId, 0, BillingClientLive.defaultPageSize)
          .map { result =>
            assert(result)(
              equalTo(OrdersResponse(1, 0, 0, Seq(Order(1, 2, Some(3), OrderBalance(100, 200, Some(300L), 400)))))
            )
          }
      },
      testM("getOverdraft should return overdraft limit by clientId") {
        BillingClient
          .getOverdraft(balanceClientId)
          .map(
            assert(_)(
              equalTo(Overdraft(clientId = 1, overdraftLimit = 300L, overdraftBan = false, overdraftSpent = 100))
            )
          )
      },
      testM("getRequisites should return requisites by clientId") {
        BillingClient
          .getRequisites(autoruClientId)
          .map(
            assert(_)(
              equalTo(
                RequisitesResponse(
                  List(Requisite(balancePersonId, Requisite.Properties(Requisite.Property.Juridical("Тачки"))))
                )
              )
            )
          )
      }
    ).provideCustomLayerShared(
      (Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpStub) ++ UUID.live) >>> BillingClientLive.live
    )
}
