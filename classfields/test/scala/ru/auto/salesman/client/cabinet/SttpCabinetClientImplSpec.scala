package ru.auto.salesman.client.cabinet

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.joda.time.DateTime
import ru.auto.cabinet.ApiModel.ClientIdsResponse.ClientInfo
import ru.auto.cabinet.ApiModel._
import ru.auto.salesman.client.cabinet.CabinetClient.CantCreateBalanceOrderWithoutBalanceInfoException
import ru.auto.salesman.client.cabinet.impl.SttpCabinetClient
import ru.auto.salesman.client.cabinet.model.{
  BalanceOrder,
  LoyaltyReportFilter,
  LoyaltyReportInfo,
  ManagerNotFound
}
import ru.auto.salesman.environment.TimeZone
import ru.auto.salesman.proto.user.ModelProtoFormats
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.sttp.SttpClientImpl
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.protobuf.ProtobufUtils
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

import java.net.URI

class SttpCabinetClientImplSpec extends BaseSpec {

  import SttpCabinetClientImplSpec._

  val serverAddress: URI =
    runServer(
      concat(
        loyaltyReportsRoute,
        createBalanceOrderRoute,
        getClientStocksRoute,
        getClientDetailsRoute,
        getDetailedClientsRoute,
        getClientPoiPropertiesRoute,
        setLoyaltyRoute,
        getManagerRoute,
        getClientIdsRoute,
        getClientDiscountsRoute
      )
    )

  val client: CabinetClient =
    new SttpCabinetClient(
      serverAddress.toString,
      SttpClientImpl(TestOperationalSupport)
    )

  "CabinetClient" should {

    "successfully get loyalty reports" in {
      val rq = LoyaltyReportFilter(
        from = Some(now.minusDays(1)),
        to = Some(now),
        cashbackApplied = Some(true)
      )

      client
        .loyaltyReports(rq)
        .success
        .value should contain theSameElementsAs reports
    }

    "successfully create balance order" in {
      client
        .createBalanceOrder(1L, requireActualBalanceData = true)
        .success
        .value shouldBe balanceOrder
    }

    "throw domain error if unable create balance order cause of missing balance info" in {
      client
        .createBalanceOrder(clientWithoutBalanceInfo, requireActualBalanceData = true)
        .failure
        .exception shouldBe an[CantCreateBalanceOrderWithoutBalanceInfoException]
    }

    "successfully get client stocks" in {
      client
        .getClientStocks(1L)
        .success
        .value
        .getStocks(0)
        .getCustomerId shouldBe 1L
    }

    "successfully get client poi properties" in {
      client
        .getClientPoiProperties(1L)
        .success
        .value
        .getPoiId shouldBe 100L
    }

    "successfully get client details" in {
      val result =
        client
          .getClientDetails(1L)
          .success
          .value

      result.getCompanyId shouldBe 1L
      result.getCompanyName shouldBe "Company"
    }

    "successfully get detailed clients" in {
      val result =
        client
          .getDetailedClients(
            DetailedClientRequest.newBuilder().addClientIds(1L).build()
          )
          .success
          .value

      result.getClients(0).getCompanyId shouldBe 1L
      result.getClients(0).getCompanyName shouldBe "Company"
    }

    "successfully set loyalty to true" in {
      client
        .setLoyalty(1L, isLoyal = true)
        .success
        .value
    }

    "successfully set loyalty to false" in {
      client
        .setLoyalty(1L, isLoyal = false)
        .success
        .value
    }

    "fail to set loyalty" in {
      client
        .setLoyalty(2L, isLoyal = true)
        .failure
        .exception
    }

    "successfully get manager" in {
      val result =
        client
          .getManager(1L)
          .success
          .value

      result.getId shouldBe 1L
    }

    "fail to get manager with ManagerNotFound if 404" in {
      client
        .getManager(2L)
        .failure
        .exception shouldBe an[ManagerNotFound]
    }

    "successfully get client ids" in {
      val balanceId =
        ClientIdsBalanceRequest.BalanceClientInfo
          .newBuilder()
          .setBalanceId(1L)
          .build()
      val request =
        ClientIdsBalanceRequest
          .newBuilder()
          .addBalanceIds(balanceId)
          .build()

      client
        .getClientIds(request)
        .success
        .value
        .getClientsInfo(0)
        .getBalanceId shouldBe 1L
    }

    "successfully get client discounts" in {
      val product = CustomerDiscount.Product.PLACEMENT
      val result =
        client
          .getClientDiscounts(1L, Some(product))
          .success
          .value
          .getDiscounts(0)

      result.getCustomerId shouldBe 1L
      result.getProduct shouldBe product
    }

  }

}

object SttpCabinetClientImplSpec
    extends CabinetProtocol
    with ModelProtoFormats
    with ProtobufSupport
    with ProtobufUtils {

  private val now = DateTime.now(TimeZone)

  private val report = LoyaltyReportInfo(
    clientId = 1,
    resolution = true,
    reportDate = now,
    status = "status",
    cashbackApplied = true,
    cashbackAmount = Some(10),
    cashbackPercent = 10
  )
  private val reports = List(report, report.copy(clientId = 2))

  private val reportsJson =
    s"""
      |[{
      |  "cashbackAmount": 10,
      |  "cashbackApplied": true,
      |  "cashbackPercent": 10,
      |  "clientId": 1,
      |  "reportDate": "${now.toString()}",
      |  "resolution": true,
      |  "status": "status"
      |}, {
      |  "cashbackAmount": 10,
      |  "cashbackApplied": true,
      |  "cashbackPercent": 10,
      |  "clientId": 2,
      |  "reportDate": "${now.toString()}",
      |  "resolution": true,
      |  "status": "status"
      |}]
      |""".stripMargin

  private val balanceOrder = BalanceOrder(
    15006630L,
    99L,
    1L,
    7320375L,
    175L,
    0L,
    0L,
    1L,
    now,
    now,
    "Технические услуги в отношении Объявлений Заказчика в соответствии с «Условиями оказания услуг на сервисе Auto.ru» msk7471",
    0L,
    now,
    now
  )

  private val clientWithoutBalanceInfo = 3220

  private val balanceOrderCreationFailedJson =
    s"""
         |{
         |  "errorCode":"ru.auto.cabinet.dao.jdbc.BalanceClientIdNotFound",
         |  "message":"Balance Client id not found for the client: $clientWithoutBalanceInfo"
         |}
         |
         |""".stripMargin

  private val balanceOrderJson =
    s"""
        |{
        |  "agencyId": 7320375,
        |  "amount": 1,
        |  "clientId": 1,
        |  "createDate": "${now.toString()}",
        |  "dateStart": "${now.toString()}",
        |  "id": 15006630,
        |  "productId": 175,
        |  "provided": 0,
        |  "providedDate": "${now.toString()}",
        |  "quantity": 0,
        |  "serviceId": 99,
        |  "status": 0,
        |  "text": "Технические услуги в отношении Объявлений Заказчика в соответствии с «Условиями оказания услуг на сервисе Auto.ru» msk7471",
        |  "updateDate": "${now.toString()}"
        |}
        |""".stripMargin

  private def loyaltyReportsRoute: Route =
    get {
      path("api" / "1.x" / "loyalty" / "report") {
        parameters("from", "to", "cashbackApplied") { (_, _, _) =>
          complete(
            HttpEntity(
              ContentTypes.`application/json`,
              reportsJson
            )
          )
        }
      }
    }

  private def createBalanceOrderRoute: Route =
    post {
      path("api" / "1.x" / "client" / LongNumber / "order") { clientId =>
        parameter("require_actual_balance_data") { _ =>
          complete {
            if (clientId == clientWithoutBalanceInfo)
              StatusCodes.NotFound -> HttpEntity(
                ContentTypes.`application/json`,
                balanceOrderCreationFailedJson
              )
            else
              HttpEntity(
                ContentTypes.`application/json`,
                balanceOrderJson
              )
          }
        }
      }
    }

  private def getClientStocksRoute: Route =
    get {
      path("api" / "1.x" / "client" / LongNumber / "stocks") { clientId =>
        complete {
          val stock =
            DealerStock
              .newBuilder()
              .setCustomerId(clientId)
              .setFullStock(true)
              .build
          DealerStocks
            .newBuilder()
            .addStocks(stock)
            .build()
        }
      }
    }

  private def getClientDetailsRoute: Route =
    get {
      path("api" / "1.x" / "client" / LongNumber / "detailed") { clientId =>
        complete {
          DetailedClient
            .newBuilder()
            .setCompanyId(clientId)
            .setCompanyName("Company")
            .build
        }
      }
    }

  private def getDetailedClientsRoute: Route =
    post {
      path("api" / "1.x" / "client" / "detailed" / "batch") {
        entity(as[Array[Byte]]) { bytes =>
          val rq = DetailedClientRequest.parseFrom(bytes)
          val client =
            DetailedClient
              .newBuilder()
              .setCompanyId(rq.getClientIds(0))
              .setCompanyName("Company")
              .build()
          complete {
            DetailedClientResponse
              .newBuilder()
              .addClients(client)
              .build()
          }
        }
      }
    }

  private def getClientPoiPropertiesRoute: Route =
    get {
      path("api" / "1.x" / "client" / LongNumber / "poi_properties") { _ =>
        complete {
          PoiProperties
            .newBuilder()
            .setPoiId(100)
            .build()
        }
      }
    }

  private def setLoyaltyRoute: Route = {
    val route =
      path("api" / "1.x" / "client" / LongNumber / "is_loyal") { clientId =>
        if (clientId == 1L) complete(StatusCodes.OK)
        else complete(StatusCodes.NotFound)
      }

    put(route) ~ delete(route)
  }

  private def getManagerRoute: Route =
    get {
      path("api" / "1.x" / "client" / LongNumber / "manager" / "internal") { clientId =>
        if (clientId == 1)
          complete {
            ManagerRecord
              .newBuilder()
              .setId(clientId)
              .build()
          }
        else complete(StatusCodes.NotFound)
      }
    }

  private def getClientIdsRoute: Route =
    post {
      path("api" / "1.x" / "internal" / "client" / "by_balance_ids") {
        entity(as[Array[Byte]]) { bytes =>
          val rq = ClientIdsBalanceRequest.parseFrom(bytes)
          val clientInfo =
            ClientInfo
              .newBuilder()
              .setClientId(1L)
              .setAgencyId(1L)
              .setBalanceId(rq.getBalanceIdsOrBuilder(0).getBalanceId)
              .build()
          complete {
            ClientIdsResponse
              .newBuilder()
              .addClientsInfo(clientInfo)
              .build()
          }
        }
      }
    }

  private def getClientDiscountsRoute: Route =
    (get & path(
      "api" / "1.x" / "client" / LongNumber / "discounts"
    ) & parameter(
      "product"
    )) { (clientId, _) =>
      complete {
        val discount =
          CustomerDiscount
            .newBuilder()
            .setCustomerId(clientId)
            .setProduct(CustomerDiscount.Product.PLACEMENT)
            .build
        GetCustomerDiscountsResponse
          .newBuilder()
          .addDiscounts(discount)
          .build
      }
    }

}
