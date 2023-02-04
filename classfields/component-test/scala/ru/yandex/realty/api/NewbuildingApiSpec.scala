package ru.yandex.realty.api

import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AbstractBnbSearcherComponentTestSpec
import ru.yandex.realty.api.ProtoResponse.NewbuildingContactsResponse
import ru.yandex.realty.componenttest.data.campaigns.Campaign_56576
import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.model.phone.RealtyPhoneTags.MapsMobileTagName

import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class NewbuildingApiSpec extends AbstractBnbSearcherComponentTestSpec {

  import NewbuildingApiSpec._

  "GET /newbuilding/{newbuildingId}/contacts" should {

    "return newbuilding contacts" in {

      val newbuildingId = Site_57547.Id
      val expectedRedirectObjectId = newbuildingRedirectObjectId(newbuildingId)
      val phone = Campaign_56576.Proto.getRedirects(0).getSource

      stubTeleponyGetOrCreateOk(objectId = expectedRedirectObjectId, phone)
      stubTeleponyDeleteOk(objectId = expectedRedirectObjectId)

      Get(s"/newbuilding/$newbuildingId/contacts") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = entityAs[NewbuildingContactsResponse]
          response.getResponse.getSalesDepartmentsCount === 1
        }

    }

    "return newbuilding contacts when tag is provided" in {

      val newbuildingId = Site_57547.Id
      val expectedRedirectObjectId = newbuildingRedirectObjectId(newbuildingId)

      stubTeleponyDeleteOk(objectId = expectedRedirectObjectId)

      Get(s"/newbuilding/$newbuildingId/contacts?tag=$MapsMobileTagName") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = entityAs[NewbuildingContactsResponse]
          response.getResponse.getSalesDepartmentsCount === 1
        }

    }

    s"return status=${StatusCodes.NotFound} when newbuilding does not exist" in {

      val nonExistingNewbuildingId = 10101010

      Get(s"/newbuilding/$nonExistingNewbuildingId/contacts") ~>
        route ~>
        check {
          status should be(StatusCodes.NotFound)
        }

    }

    "return newbuilding contacts when failed to remove old redirects" in {

      val newbuildingId = Site_57547.Id
      val expectedRedirectObjectId = newbuildingRedirectObjectId(newbuildingId)

      stubTeleponyDeleteError(objectId = expectedRedirectObjectId)

      Get(s"/newbuilding/$newbuildingId/contacts") ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = entityAs[NewbuildingContactsResponse]
          response.getResponse.getSalesDepartmentsCount === 1
        }

    }

  }

}

object NewbuildingApiSpec {

  def newbuildingRedirectObjectId(newbuildingId: Long): String = {
    s"site_$newbuildingId"
  }

}
