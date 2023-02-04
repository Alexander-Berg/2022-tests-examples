package ru.auto.salesman.model

import ru.auto.salesman.model.ClientStatuses.{Active, Inactive}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.{balanceRecordGen, clientRecordGen}

class DetailedClientSpec extends BaseSpec {

  "Detailed client factory" should {

    "create with active status" in {
      forAll(clientRecordGen(), balanceRecordGen) { (baseClient, balanceClient) =>
        val client = baseClient.copy(status = Active)
        DetailedClient(client, balanceClient) shouldBe
          DetailedClient(
            client.clientId,
            client.agencyId,
            balanceClient.balanceClientId,
            balanceClient.balanceAgencyId,
            client.categorizedClientId,
            client.companyId,
            client.regionId,
            client.cityId,
            balanceClient.accountId,
            isActive = true,
            client.firstModerated,
            client.singlePayment,
            paidCallsAvailable = client.paidCallsAvailable
          )
      }
    }

    "create with inactive status" in {
      forAll(clientRecordGen(), balanceRecordGen) { (baseClient, balanceClient) =>
        val client = baseClient.copy(status = Inactive)
        DetailedClient(client, balanceClient) shouldBe
          DetailedClient(
            client.clientId,
            client.agencyId,
            balanceClient.balanceClientId,
            balanceClient.balanceAgencyId,
            client.categorizedClientId,
            client.companyId,
            client.regionId,
            client.cityId,
            balanceClient.accountId,
            isActive = false,
            client.firstModerated,
            client.singlePayment,
            paidCallsAvailable = client.paidCallsAvailable
          )
      }
    }
  }
}
