package ru.yandex.vos2.api.directives.api.offers.list

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class ListOfPartnerIdDirectiveTest extends WordSpec with Matchers {

  "ListOfPartnerIdDirective " should {

    "make correct parsing for list of partnerIds" in {
      val input = Map("partnerId" -> List("1", "2,3", "4"))

      val result = ListOfPartnerIdDirective.parsePartnerIds(input.get("partnerId"))

      result should be(Set(1, 2, 3, 4))
    }

    "make correct parsing for empty list of partnerIds" in {
      val result = ListOfPartnerIdDirective.parsePartnerIds(Some(List.empty))

      result should be(Set.empty)
    }

    "make correct parsing for empty list of partnerIds is None" in {
      val result = ListOfPartnerIdDirective.parsePartnerIds(None)

      result should be(Set.empty)
    }
  }

}
