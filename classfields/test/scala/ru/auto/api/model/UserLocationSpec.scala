package ru.auto.api.model

import org.scalactic.{Equality, TolerantNumerics}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.exceptions.BadRequestDetailedException

/**
  * Created by mcsim-gr on 16.08.17.
  */
class UserLocationSpec extends BaseSpec with ScalaCheckPropertyChecks {
  val tolerance = 1e-4f

  implicit val doubleEq: Equality[Float] = TolerantNumerics.tolerantFloatEquality(tolerance)

  "UserLocationParser" should {
    "parse some location" in {
      val location = UserLocation.fromHeader("lat=-30.78;lon=+178;acc=123.899")
      location.latitude shouldEqual -30.78f
      location.longitude shouldEqual 178f
      location.accuracy shouldEqual 123.899f
    }

    "throw an Exception if some values are not defined" in {
      a[BadRequestDetailedException] should be thrownBy UserLocation.fromHeader("lat=1;acc=2")
    }

    "throw an Exception if some incorrect coords are present" in {
      a[BadRequestDetailedException] should be thrownBy UserLocation.fromHeader("lat=108.56;lon=-489.9;acc=200;")
    }

    "throw an Exception if incorrect accuracy are present" in {
      a[BadRequestDetailedException] should be thrownBy UserLocation.fromHeader("lat=108.56;lon=-89.9;acc=-200;")
    }
  }
}
