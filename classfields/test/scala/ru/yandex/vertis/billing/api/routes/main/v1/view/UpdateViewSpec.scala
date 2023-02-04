package ru.yandex.vertis.billing.api.routes.main.v1.view

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.api.routes.main.v1.view.CampaignPatchView
import ru.yandex.vertis.billing.dao.gens.CampaignPatchGen
import ru.yandex.vertis.billing.model_core.AttachRule.Resources
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{Producer, ResourceRefsGen}
import ru.yandex.vertis.billing.service.CampaignService
import ru.yandex.vertis.billing.service.CampaignService.Patch
import spray.json.enrichString

/**
  * @author @logab
  */
class UpdateViewSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  "mutation view" should {
    val format = CampaignPatchView.jsonFormat

    def roundTrip(patch: CampaignService.Patch): Assertion = {
      format.read(format.write(CampaignPatchView.asView(patch))).asModel shouldEqual patch
    }

    "convert arbitrary patch" in {
      forAll(CampaignPatchGen)(roundTrip)
    }

    "marshall and unmarshall don't touch patch" in {
      roundTrip(CampaignService.Patch(attachRule = None))
    }
    "marshall unset operation" in {
      roundTrip(CampaignService.Patch(attachRule = Some(Update(None))))
    }
    "marshall update operation" in {
      roundTrip(CampaignService.Patch(attachRule = Some(Update(Some(Resources(ResourceRefsGen.next.toSet))))))
    }
    "unmarshall don't touch patch" in {
      val jsonString = "{}"
      deserialized(jsonString) shouldEqual CampaignService.Patch()
    }
    "unmarshall unset patch" in {
      val jsonString = "{\"attachRule\":\"unset\"}"
      deserialized(jsonString) shouldEqual CampaignService.Patch(attachRule = Some(Update(None)))
    }
    "unmarshall update patch" in {
      val jsonString = "{\"attachRule\":{\"resources\":[{\"capaPartnerId\":\"100\"}]}}"
      deserialized(jsonString) shouldEqual CampaignService.Patch(
        attachRule = Some(Update(Some(Resources(Set(PartnerRef("100"))))))
      )
    }
  }

  def deserialized(jsonString: SimpleBalanceOrderId): Patch =
    CampaignPatchView.jsonFormat.read(jsonString.parseJson).asModel
}
