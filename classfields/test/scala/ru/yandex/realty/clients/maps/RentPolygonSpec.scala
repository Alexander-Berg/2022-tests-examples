package ru.yandex.realty.clients.maps

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.clients.maps.RentPolygonSpec._
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.model.region.Regions

@RunWith(classOf[JUnitRunner])
class RentPolygonSpec extends WordSpec with Matchers with Logging {
  "RentPolygon.fromSubjectFederationId" should {
    "return polygon correctly" in {
      subjectFederationIdRentPolygon.foreach(regPoly => {
        RentPolygon.fromSubjectFederationId(regPoly._1) shouldEqual Some(regPoly._2)
      })
    }

    "return None for unknown subject federation id" in {
      RentPolygon.fromSubjectFederationId(999999999) shouldEqual None
    }
  }

  "RentPolygon.getPhoneDefaultBySubjectFederationId" should {
    "return default MSK phone for unknown  subject federation id" in {
      RentPolygon.getPhoneDefaultBySubjectFederationId(Some(999999999)) shouldEqual mskFeedPhone
      RentPolygon.getPhoneDefaultBySubjectFederationId(999999999) shouldEqual mskFeedPhone
    }
  }
}

object RentPolygonSpec {

  val subjectFederationIdRentPolygon = Map(
    Regions.MSK_AND_MOS_OBLAST -> RentPolygon.MSK,
    Regions.SPB_AND_LEN_OBLAST -> RentPolygon.SPB,
    Regions.NOVOSIBIRSKAYA_OBLAST -> RentPolygon.NSB,
    Regions.SVERDLOVSKAYA_OBLAST -> RentPolygon.EKB
  )

  val mskFeedPhone = FeedPhone(
    yandex = "+74951384823",
    avito = "+74951468459",
    cian = "+74951468470",
    jcat = "+74951384823"
  )

}
