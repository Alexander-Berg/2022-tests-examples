package ru.auto.salesman.service.telepony.tariff

import org.scalacheck.Gen
import ru.auto.api.api_offer_model.{Category, Section}
import ru.auto.salesman.calls.calls_tariff_response.{CallTariff, CallTariffResponse}
import ru.auto.salesman.model.RegionId
import ru.auto.salesman.service.DealerFeatureService
import ru.auto.salesman.service.client.ClientService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.clientRecordGen
import ru.yandex.vertis.generators.ProducerProvider.asProducer

class TeleponyCallTariffServiceImplSpec extends BaseSpec {

  private val clientService = mock[ClientService]
  private val featureService = mock[DealerFeatureService]
  private val service = new TeleponyCallTariffServiceImpl(clientService, featureService)

  (featureService.carsUsedCallsRegions _)
    .expects()
    .returning(Set(RegionId(1)))

  private val singleWithCalls = CallTariffResponse(
    category = Category.CARS,
    section = Section.USED,
    callTariff = CallTariff.SINGLE_WITH_CALLS
  )

  private val calls = CallTariffResponse(
    category = Category.CARS,
    section = Section.NEW,
    callTariff = CallTariff.CALLS
  )

  "TeleponyCallTariffServiceImpl" should {
    "return SingleWithCalls for proper region" in {
      val client = clientRecordGen(
        paidCallsAvailableGen = Gen.const(false),
        regionIdGen = Gen.const(RegionId(1))
      ).next

      (clientService.getById _)
        .expects(client.clientId, false)
        .returningZ(Some(client))

      service
        .getCallTariffs(client.clientId)
        .success
        .value
        .callTariffResponse should contain(singleWithCalls)
    }

    "return SingleWithCalls for proper client" in {
      val client = clientRecordGen(
        paidCallsAvailableGen = Gen.const(true),
        regionIdGen = Gen.const(RegionId(100500))
      ).next

      (clientService.getById _)
        .expects(client.clientId, false)
        .returningZ(Some(client))

      service
        .getCallTariffs(client.clientId)
        .success
        .value
        .callTariffResponse should contain(calls)
    }

    "return both if possible" in {
      val client = clientRecordGen(
        paidCallsAvailableGen = Gen.const(true),
        regionIdGen = Gen.const(RegionId(1))
      ).next

      (clientService.getById _)
        .expects(client.clientId, false)
        .returningZ(Some(client))

      service
        .getCallTariffs(client.clientId)
        .success
        .value
        .callTariffResponse should contain theSameElementsAs Seq(singleWithCalls, calls)
    }
  }

}
