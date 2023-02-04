package ru.auto.salesman.service.call.cashback

import ru.auto.salesman.model.cashback.call_cashback_percentage_policy.CallCashbackPercentagePolicy
import ru.auto.salesman.model.cashback.call_cashback_percentage_policy.CallCashbackPercentagePolicy.Policy
import ru.auto.salesman.model.{CityId, Client, ClientStatuses, RegionId}
import ru.auto.salesman.service.call.cashback.domain.CallCashbackRule
import ru.auto.salesman.service.call.cashback.impl.CallCashbackRulesServiceImpl
import ru.auto.salesman.service.palma.PalmaService
import ru.auto.salesman.service.palma.domain.CallCashbackPoliciesIndex
import ru.auto.salesman.test.BaseSpec

class CallCashbackRulesServiceImplSpec extends BaseSpec {

  val palma = mock[PalmaService]
  val service = new CallCashbackRulesServiceImpl(palma)

  val baseClient =
    Client(
      clientId = 1,
      agencyId = None,
      categorizedClientId = None,
      companyId = None,
      RegionId(1L),
      CityId(2L),
      ClientStatuses.Active,
      singlePayment = Set(),
      firstModerated = false,
      paidCallsAvailable = false,
      priorityPlacement = true
    )

  val policy1 = CallCashbackPercentagePolicy.defaultInstance.withPolicy(Policy(50))
  val policy2 = CallCashbackPercentagePolicy.defaultInstance.withPolicy(Policy(45))
  val policy3 = CallCashbackPercentagePolicy.defaultInstance.withPolicy(Policy(40))

  val index = CallCashbackPoliciesIndex(
    byClient = Map(1L -> policy1),
    byCity = Map(CityId(2L) -> policy2),
    byRegion = Map(RegionId(1L) -> policy3)
  )

  (palma.getCallCashbackPolicies _)
    .expects()
    .returningZ(index)

  "CallCashbackRulesServiceImpl" should {

    "return rule by client with the highest priority" in {
      service
        .getCashbackRule(baseClient)
        .success
        .value shouldBe Some(CallCashbackRule(50))
    }

    "return rule by city with second priority" in {
      val cityClient = baseClient.copy(clientId = 100)

      service
        .getCashbackRule(cityClient)
        .success
        .value shouldBe Some(CallCashbackRule(45))
    }

    "return rule by region with the lowest priority" in {
      val regionClient = baseClient.copy(clientId = 100, cityId = CityId(100L))

      service
        .getCashbackRule(regionClient)
        .success
        .value shouldBe Some(CallCashbackRule(40))
    }

    "return None if there is no appropriate rule" in {
      val unexpectedClient = baseClient.copy(
        clientId = 100,
        regionId = RegionId(100L),
        cityId = CityId(100L)
      )

      service
        .getCashbackRule(unexpectedClient)
        .success
        .value shouldBe None
    }

  }

}
