package ru.auto.api.services.cabinet

import akka.http.scaladsl.model.HttpMethods.{DELETE, GET, PUT}
import akka.http.scaladsl.model.StatusCodes.{Forbidden, NotFound, OK}
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ResponseModel.DealerOverdraft
import ru.auto.api.exceptions.{AccessGroupNotFound, AgentAccessForbidden, ClientNotFound, CustomerAccessForbidden}
import ru.auto.api.http.{ExactlyProtobufAcceptHeader, PreferProtobufAcceptHeader}
import ru.auto.api.model.{AutoruDealer, ClientGroup}
import ru.auto.api.model.gen.BasicGenerators._
import ru.auto.api.services.cabinet.BalanceTestData._
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.cabinet.AclResponse._
import ru.auto.cabinet.ApiModel.{Client, ClientProperties, GetDealerRedirectsResult}

class DefaultCabinetApiClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with OptionValues {

  private val client = new DefaultCabinetApiClient(http)

  private val accessGroupId = 1L

  private val accessGroup =
    Group
      .newBuilder()
      .setName("Administrators")
      .setId(accessGroupId)
      .setEditable(false)
      .addGrants {
        Resource
          .newBuilder()
          .setAlias(ResourceAlias.DASHBOARD)
          .setAccess(AccessLevel.READ_WRITE)
      }
      .build()

  "DefaultCabinetApiClient.getBalanceClient()" should {
    "get balance client" in {
      http.expectUrl(GET, s"/api/1.x/client/$dealerId/invoice/client")
      http.expectHeader("X-Autoru-Operator-Uid", dealerUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithJsonFrom("/cabinet/balance_client_response.json")
      val balanceClient = client.getBalanceClient(dealer)(dealerRequest).futureValue
      balanceClient.balanceClientId shouldBe dealerBalanceId
      balanceClient.balanceAgencyId shouldBe None
      balanceClient.accountId shouldBe accountId
    }

    "get balance agency client" in {
      http.expectUrl(GET, s"/api/1.x/client/$agencyDealerId/invoice/client")
      http.expectHeader("X-Autoru-Operator-Uid", agencyDealerUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithJsonFrom("/cabinet/balance_agency_client_response.json")
      val balanceClient = client.getBalanceClient(agencyDealer)(agencyDealerRequest).futureValue
      balanceClient.balanceClientId shouldBe agencyDealerBalanceId
      balanceClient.balanceAgencyId.value shouldBe agencyBalanceId
      balanceClient.accountId shouldBe accountId
    }

    "throw forbidden" in {
      http.expectUrl(GET, s"/api/1.x/client/$agencyDealerId/invoice/client")
      http.expectHeader("X-Autoru-Operator-Uid", wrongUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(Forbidden)
      val ex = client.getBalanceClient(agencyDealer)(wrongUserRequest).failed.futureValue
      ex shouldBe an[AgentAccessForbidden]
    }
  }

  "DefaultCabinetApiClient.isOverdraftAvailable()" should {
    "get overdraft access = true" in {
      http.expectUrl(GET, s"/api/1.x/client/$dealerId/overdraft?average_outcome=100.0")
      http.expectHeader("X-Autoru-Operator-Uid", dealerUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithProtoFrom[DealerOverdraft]("/cabinet/is_overdraft_available_response.json")
      val response = client.isOverdraftAvailable(dealer, 100.0)(dealerRequest).futureValue
      response shouldBe true
    }

    "get overdraft access = false (empty response)" in {
      http.expectUrl(GET, s"/api/1.x/client/$dealerId/overdraft?average_outcome=100.0")
      http.expectHeader("X-Autoru-Operator-Uid", dealerUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithProtoFromJson[DealerOverdraft](OK, """{}""")
      val response = client.isOverdraftAvailable(dealer, 100.0)(dealerRequest).futureValue
      response shouldBe false
    }

    "throw forbidden" in {
      http.expectUrl(GET, s"/api/1.x/client/$agencyDealerId/overdraft?average_outcome=100.0")
      http.expectHeader("X-Autoru-Operator-Uid", wrongUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(Forbidden)
      val ex = client.isOverdraftAvailable(agencyDealer, 100.0)(wrongUserRequest).failed.futureValue
      ex shouldBe an[AgentAccessForbidden]
    }

    "throw not found" in {
      http.expectUrl(GET, s"/api/1.x/client/123/overdraft?average_outcome=100.0")
      http.expectHeader("X-Autoru-Operator-Uid", dealerUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(NotFound)
      val ex = client.isOverdraftAvailable(AutoruDealer(123), 100L)(dealerRequest).failed.futureValue
      ex shouldBe a[ClientNotFound]
    }
  }

  "DefaultCabinetApiClient.getLastOverdraftInvoiceId()" should {
    "get last overdraft invoice" in {
      http.expectUrl(GET, s"/api/1.x/client/$dealerId/overdraft/invoice")
      http.expectHeader("X-Autoru-Operator-Uid", dealerUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithProtoFrom[DealerOverdraft]("/cabinet/last_overdraft_invoice_id.json")
      val response = client.getLastOverdraftInvoiceId(dealer)(dealerRequest).futureValue.value
      response shouldBe 123456789L
    }

    "get last overdraft invoice if empty" in {
      http.expectUrl(GET, s"/api/1.x/client/$dealerId/overdraft/invoice")
      http.expectHeader("X-Autoru-Operator-Uid", dealerUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithProtoFromJson[DealerOverdraft](OK, """{}""")
      val response = client.getLastOverdraftInvoiceId(dealer)(dealerRequest).futureValue.value
      response shouldBe 0L
    }

    "throw forbidden" in {
      http.expectUrl(GET, s"/api/1.x/client/$agencyDealerId/overdraft/invoice")
      http.expectHeader("X-Autoru-Operator-Uid", wrongUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(Forbidden)
      val ex = client.getLastOverdraftInvoiceId(agencyDealer)(wrongUserRequest).failed.futureValue
      ex shouldBe an[AgentAccessForbidden]
    }

    "return None if not found" in {
      http.expectUrl(GET, s"/api/1.x/client/123/overdraft/invoice")
      http.expectHeader("X-Autoru-Operator-Uid", dealerUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(NotFound)
      val empty = client.getLastOverdraftInvoiceId(AutoruDealer(123))(dealerRequest).futureValue
      empty shouldBe None
    }
  }

  "DefaultCabinetApiClient.getClientAccessGroups()" should {
    "get client access groups" in {
      http.expectUrl(GET, s"/api/1.x/acl/client/$dealerId/groups")
      http.expectHeader("X-Autoru-Operator-Uid", dealerUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithProtoFrom[GroupsList]("/cabinet/client_access_groups_list.json")

      val expected =
        GroupsList
          .newBuilder()
          .addGroups(accessGroup)
          .build()

      val response = client.getClientAccessGroups(dealer)(dealerRequest).futureValue
      response shouldBe expected
    }

    "throw forbidden" in {
      http.expectUrl(GET, s"/api/1.x/acl/client/$agencyDealerId/groups")
      http.expectHeader("X-Autoru-Operator-Uid", wrongUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(Forbidden)
      val ex = client.getClientAccessGroups(agencyDealer)(wrongUserRequest).failed.futureValue
      ex shouldBe a[CustomerAccessForbidden]
    }

    "throw not found" in {
      http.expectUrl(GET, s"/api/1.x/acl/client/123/groups")
      http.expectHeader("X-Autoru-Operator-Uid", dealerUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(NotFound)
      val ex = client.getClientAccessGroups(AutoruDealer(123))(dealerRequest).failed.futureValue
      ex shouldBe a[ClientNotFound]
    }
  }

  "DefaultCabinetApiClient.putClientAccessGroup()" should {
    "put client access group" in {
      http.expectUrl(PUT, s"/api/1.x/acl/client/$dealerId/group")
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.expectProto(accessGroup)
      http.respondWithProto[Group](OK, accessGroup)

      val response = client.putClientAccessGroup(dealer, accessGroup)(dealerRequest).futureValue
      response shouldBe accessGroup
    }

    "throw forbidden" in {
      http.expectUrl(PUT, s"/api/1.x/acl/client/$dealerId/group")
      http.expectHeader("X-Autoru-Operator-Uid", wrongUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(Forbidden)
      val ex = client.putClientAccessGroup(dealer, accessGroup)(wrongUserRequest).failed.futureValue
      ex shouldBe a[CustomerAccessForbidden]
    }
  }

  "DefaultCabinetApiClient.deleteClientAccessGroup()" should {
    "delete client access group" in {
      val groupId = accessGroupId.toString
      http.expectUrl(DELETE, s"/api/1.x/acl/client/$dealerId/group/$groupId")
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(OK)
      client.deleteClientAccessGroup(dealer, groupId)(dealerRequest)
    }

    "throw on not found" in {
      val groupId = accessGroupId.toString
      http.expectUrl(DELETE, s"/api/1.x/acl/client/$dealerId/group/$groupId")
      http.expectHeader("X-Autoru-Operator-Uid", wrongUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(NotFound)
      val ex = client.deleteClientAccessGroup(dealer, groupId)(wrongUserRequest).failed.futureValue
      ex shouldBe a[AccessGroupNotFound]
    }

    "throw forbidden" in {
      val groupId = accessGroupId.toString
      http.expectUrl(DELETE, s"/api/1.x/acl/client/$dealerId/group/$groupId")
      http.expectHeader("X-Autoru-Operator-Uid", wrongUserId.toString)
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(Forbidden)
      val ex = client.deleteClientAccessGroup(dealer, groupId)(wrongUserRequest).failed.futureValue
      ex shouldBe a[CustomerAccessForbidden]
    }
  }

  "DefaultCabinetApiClient.getAccessGroup()" should {
    "get client access groups" in {
      val groupId: ClientGroup = "1"

      http.expectUrl(GET, s"/api/1.x/acl/group/$groupId")
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithProtoFrom[Group]("/cabinet/client_access_group.json")

      val expected =
        Group
          .newBuilder()
          .setName("Test group")
          .setId(1L)
          .setEditable(true)
          .addGrants {
            Resource
              .newBuilder()
              .setAlias(ResourceAlias.DASHBOARD)
              .setAccess(AccessLevel.READ_WRITE)
          }
          .build()

      val response = client.getAccessGroup(groupId)(dealerRequest).futureValue
      response shouldBe expected
    }

    "throw not found" in {
      http.expectUrl(GET, s"/api/1.x/acl/group/unknown_group")
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithStatus(NotFound)
      val ex = client.getAccessGroup("unknown_group")(dealerRequest).failed.futureValue
      ex shouldBe an[AccessGroupNotFound]
    }

  }

  "DefaultCabinetApiClient.getAccessResources()" should {
    "get customer access resources list" in {
      http.expectUrl(GET, s"/api/1.x/acl/client/$dealerId/resources")
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.respondWithProtoFrom[ResourcesList]("/cabinet/client_access_resources_list.json")

      val expected =
        ResourcesList
          .newBuilder()
          .addResources {
            Resource
              .newBuilder()
              .setAlias(ResourceAlias.DASHBOARD)
              .setName("Дашборд")
          }
          .addResources {
            Resource
              .newBuilder()
              .setAlias(ResourceAlias.WALLET)
              .setName("Кошелёк")
          }
          .addResources {
            Resource
              .newBuilder()
              .setAlias(ResourceAlias.OFFERS)
              .setName("Объявления")
          }
          .build()

      val response = client.getAccessResources(dealer)(dealerRequest).futureValue
      response shouldBe expected
    }
  }

  "DefaultCabinetApiClient.getDealerRedirects()" should {
    "call all available dealer redirects from cabinet by mark and model " in {
      val mark = "Mark"
      val model = "Model"

      val response = GetDealerRedirectsResult.newBuilder().build()

      http.expectUrl(GET, s"/api/1.x/call-center/dealer_redirects?mark=$mark&model=$model")
      http.expectHeader("X-Autoru-Request-ID", testRequestId)
      http.expectHeader(PreferProtobufAcceptHeader)
      http.respondWith(response)

      client.getDealerRedirects(mark, model)(dealerRequest)
    }
  }

  "DefaultCabinetApiClient.isMultipostingEnabled()" should {
    "return multiposting_enabled field" in {
      forAll(bool) { isMultipostingEnabled =>
        val properties = ClientProperties.newBuilder().setMultipostingEnabled(isMultipostingEnabled).build()
        val response = Client.newBuilder().setId(dealerId).setProperties(properties).build()

        http.expectUrl(GET, s"/api/1.x/client/$dealerId")
        http.expectHeader("X-Autoru-Operator-Uid", dealerUserId.toString)
        http.expectHeader("X-Autoru-Request-ID", testRequestId)
        http.expectHeader(ExactlyProtobufAcceptHeader)
        http.respondWith(response)

        client.isMultipostingEnabled(dealer)(dealerRequest)
      }
    }
  }
}
