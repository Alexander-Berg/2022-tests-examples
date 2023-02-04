package ru.yandex.realty.rent.backend

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.clients.maps.RentPolygon
import ru.yandex.realty.rent.backend.XmlCianMetroDictionary.CianMetroId

@RunWith(classOf[JUnitRunner])
class XmlCianMetroDictionarySpec extends WordSpec with Matchers {
  import XmlCianMetroDictionary.findByName

  "XmlCianMetroDictionary.findByName" should {
    "return expected values" in {
      findByName("unknown", RentPolygon.MSK) shouldEqual None
      findByName("Октябрьская", RentPolygon.MSK) shouldEqual Some(CianMetroId("80"))

      findByName("unknown", RentPolygon.SPB) shouldEqual None
      findByName("Академическая", RentPolygon.SPB) shouldEqual Some(CianMetroId("169"))

      findByName("unknown", RentPolygon.NSB) shouldEqual None
      findByName("Октябрьская", RentPolygon.NSB) shouldEqual Some(CianMetroId("253"))

      findByName("unknown", RentPolygon.EKB) shouldEqual None
      findByName("Ботаническая", RentPolygon.EKB) shouldEqual Some(CianMetroId("348"))
    }
  }
}
