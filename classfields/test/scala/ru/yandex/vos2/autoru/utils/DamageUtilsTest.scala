package ru.yandex.vos2.autoru.utils

import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.Inspectors
import org.scalatest.funsuite.AnyFunSuite

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 01.02.17
  */
@RunWith(classOf[JUnitRunner])
class DamageUtilsTest extends AnyFunSuite with Inspectors {

  private val carParts = Seq(
    "trunkdoor",
    "rearrightfender",
    "rearleftfender",
    "rearrightdoor",
    "rearbumper",
    "frontbumper",
    "glass",
    "trunkdoor",
    "hood",
    "roof"
  )

  test("convert car parts") {
    forEvery(carParts) { part =>
      noException shouldBe thrownBy {
        DamageUtils.carPartByName(part)
      }
    }
  }
}
