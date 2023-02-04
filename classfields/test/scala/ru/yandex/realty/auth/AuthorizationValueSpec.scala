package ru.yandex.realty.auth

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase

/**
  * Specs on [[AuthorizationValue]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class AuthorizationValueSpec extends SpecBase {

  "XAuthorizationVertisValue" should {
    "be extracted from plain value" in {
      "Vertis ABC DEF" match {
        case XAuthorizationVertisValue(Some("ABC"), "DEF") => // ok
        case _ => fail("Unable to extract AuthorizationValue")
      }
    }
    "be extracted from plain value without uuid" in {
      "Vertis DEF" match {
        case XAuthorizationVertisValue(None, "DEF") => // ok
        case _ => fail("Unable to extract AuthorizationValue")
      }
    }
    "not be extracted from plain without Vertis prefix" in {
      "ABC DEF" match {
        case XAuthorizationVertisValue(_, _) => fail("Not expected to be extracted")
        case _ => // ok
      }
    }
    "not be extracted from plain with incorrect prefix" in {
      "Boobs ABC DEF" match {
        case XAuthorizationVertisValue(_, _) => fail("Not expected to be extracted")
        case _ => // ok
      }
    }
  }

}
