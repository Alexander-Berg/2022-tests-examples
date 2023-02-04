package ru.auto.salesman.service.impl

import org.scalacheck.Gen
import ru.auto.salesman.billing.BootstrapClient.ProductTypeFilter.CustomType
import ru.auto.salesman.billing.BootstrapClient.{
  OrderOrCustomerNotFound,
  ProductTypeFilter
}
import ru.auto.salesman.billing.{BootstrapClient, RequestContext => BillingRequestContext}
import ru.auto.salesman.service.impl.BillingServiceImpl.TooManyCampaignsFound
import ru.auto.salesman.test.dao.gens._
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.yandex.vertis.billing.Model.CustomerId
import ru.yandex.vertis.billing.model.Dsl

class BillingServiceImplSpec extends BaseSpec {

  private val billingClient = mock[BootstrapClient]
  private val service = new BillingServiceImpl(billingClient)

  private val getCampaignMock = toMockFunction4 {
    billingClient.getCampaign(_: CustomerId, _: Long, _: ProductTypeFilter)(
      _: BillingRequestContext
    )
  }

  "Billing service" should {

    "get call campaign for client" in {
      forAll(clientDetailsGen(balanceAgencyIdGen = None), campaignHeaderGen()) {
        (client, campaign) =>
          val customer = Dsl.customer(client.balanceClientId, false, -1)
          getCampaignMock
            .expects(customer, client.accountId, CustomType("call"), *)
            .returningT(List(campaign))
          service.getCallCampaign(client).success.value.value shouldBe campaign
      }
    }

    "get call campaign for agency" in {
      forAll(
        clientDetailsGen(balanceAgencyIdGen = Some(20102L)),
        campaignHeaderGen()
      ) { (client, campaign) =>
        val customer = Dsl.customer(client.balanceClientId, true, 20102)
        getCampaignMock
          .expects(customer, client.accountId, CustomType("call"), *)
          .returningT(List(campaign))
        service.getCallCampaign(client).success.value.value shouldBe campaign
      }
    }

    "not get call campaign when billing returns zero campaigns" in {
      forAll(clientDetailsGen()) { client =>
        getCampaignMock
          .expects(*, client.accountId, CustomType("call"), *)
          .returningT(Nil)
        service.getCallCampaign(client).success.value shouldBe None
      }
    }

    "not get call campaign when billing returns 404" in {
      forAll(clientDetailsGen()) { client =>
        getCampaignMock
          .expects(*, client.accountId, CustomType("call"), *)
          .throwingT(OrderOrCustomerNotFound("test"))
        service.getCallCampaign(client).success.value shouldBe None
      }
    }

    "return error when billing returns multiple campaigns" in {
      forAll(clientDetailsGen(), Gen.listOfN(2, campaignHeaderGen())) {
        (client, campaigns) =>
          getCampaignMock
            .expects(*, client.accountId, CustomType("call"), *)
            .returningT(campaigns)
          service
            .getCallCampaign(client)
            .failure
            .exception shouldBe a[TooManyCampaignsFound]
      }
    }

    "return error when billing returns error" in {
      forAll(clientDetailsGen()) { client =>
        getCampaignMock
          .expects(*, client.accountId, CustomType("call"), *)
          .throwingT(new TestException)
        service
          .getCallCampaign(client)
          .failure
          .exception shouldBe a[TestException]
      }
    }
  }
}
