package ru.yandex.realty.api

import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AbstractBnbSearcherComponentTestSpec
import ru.yandex.realty.componenttest.data.villages.Village_1852045
import ru.yandex.realty.model.message.VillageCampaign
import ru.yandex.realty.model.phone.RealtyPhoneTags.MapsMobileTagName
import ru.yandex.realty.proto.village.api.VillageContactsResponse

import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class VillageApiSpec extends AbstractBnbSearcherComponentTestSpec {

  import VillageApiSpec._

  "GET /village/{villageId}/contacts" should {

    "return village contacts" in {

      val villageId = Village_1852045.Id
      val expectedRedirectObjectId = villageRedirectObjectId(Village_1852045.Campaign)
      val villagePhone = Village_1852045.DynamicInfo.getAuction
        .getSortedParticipants(0)
        .getSalesDepartment
        .getPhones(0)

      stubTeleponyGetOrCreateOk(objectId = expectedRedirectObjectId, source = villagePhone)
      stubTeleponyDeleteOk(objectId = expectedRedirectObjectId)

      Get(s"/village/$villageId/contacts") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = entityAs[VillageContactsResponse]
          response.getResponse.getSalesDepartmentsCount === 1
        }

    }

    "return village contacts when tag is provided" in {

      val villageId = Village_1852045.Id
      val expectedRedirectObjectId = villageRedirectObjectId(Village_1852045.Campaign)

      stubTeleponyDeleteOk(objectId = expectedRedirectObjectId)

      Get(s"/village/$villageId/contacts?tag=$MapsMobileTagName") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = entityAs[VillageContactsResponse]
          response.getResponse.getSalesDepartmentsCount === 1
        }

    }

    s"return status=${StatusCodes.NotFound} when village does not exist" in {

      val nonExistingVillageId = 10101010

      Get(s"/village/$nonExistingVillageId/contacts") ~>
        route ~>
        check {
          status should be(StatusCodes.NotFound)
        }

    }

    "return village contacts when failed to remove old redirects" in {

      val villageId = Village_1852045.Id
      val expectedRedirectObjectId = villageRedirectObjectId(Village_1852045.Campaign)

      stubTeleponyDeleteError(objectId = expectedRedirectObjectId)

      Get(s"/village/$villageId/contacts") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = entityAs[VillageContactsResponse]
          response.getResponse.getSalesDepartmentsCount === 1
        }

    }

  }

}

object VillageApiSpec {

  def villageRedirectObjectId(campaign: VillageCampaign): String = {
    campaign.getBilling.getKnownCampaign.getCampaign.getId
  }

}
