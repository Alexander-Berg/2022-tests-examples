package ru.auto.salesman.client.billing.model

import ru.auto.salesman.model.RegionId
import ru.auto.salesman.service.billingcampaign.UpsertParams
import ru.auto.salesman.service.billingcampaign.BillingTestData.ClientDetails
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.GeoUtils.{RegMoscow, RegSverdlovsk}
import spray.json.{enrichAny, JsValue}

class CampaignPatchSpec extends BaseSpec with BillingMarshaller {

  private def client(regionId: RegionId) =
    ClientDetails.copy(regionId = regionId)

  private val params = UpsertParams(
    clientId = 1,
    dayLimit = None,
    weekLimit = None,
    costPerCall = None,
    enabled = None,
    recalculateCostPerCall = None,
    createNew = false
  )

  private def depositCoefficient(json: JsValue) =
    json.asJsObject.fields
      .get("deposit")
      .value
      .asJsObject
      .fields
      .get("coefficient")
      .value
      .convertTo[Int]

  "CampaignPatch -> json" should {

    "marshal deposit.coefficient for moscow = 3 when deposit is enabled" in {
      val json =
        CampaignPatch(
          client(RegMoscow),
          params,
          newCostPerCall = None,
          depositDisabled = false
        ).toJson
      withClue(json)(depositCoefficient(json) shouldBe 3)
    }

    "marshal deposit.coefficient for sverdlovsk = 0 when deposit is enabled" in {
      val json = CampaignPatch(
        client(RegSverdlovsk),
        params,
        newCostPerCall = None,
        depositDisabled = false
      ).toJson
      withClue(json)(depositCoefficient(json) shouldBe 0)
    }

    "marshal deposit.coefficient for moscow = 0 when deposit is disabled" in {
      val json =
        CampaignPatch(
          client(RegMoscow),
          params,
          newCostPerCall = None,
          depositDisabled = true
        ).toJson
      withClue(json)(depositCoefficient(json) shouldBe 0)
    }

    "marshal deposit.coefficient for sverdlovsk = 0 when deposit is disabled" in {
      val json = CampaignPatch(
        client(RegSverdlovsk),
        params,
        newCostPerCall = None,
        depositDisabled = true
      ).toJson
      withClue(json)(depositCoefficient(json) shouldBe 0)
    }
  }
}
