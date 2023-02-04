package ru.yandex.vertis.parsing.common

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.parsing.util.lang.{CategoryValue, LValue, SiteValue, StringSlice}

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class SiteTest extends FunSuite {
  private val slit3 = StringSlice.split3('|')
  private val slit2 = StringSlice.split2('-')

  test("apply") {
    val offerId = "avito|cars|100500"
    val (site, category, id) = offerId match {
      case slit3(SiteValue(s), CategoryValue(c), LValue(i)) =>
        (s, c, i)
      case _ =>
        (Site.Unknown(""), Category.CATEGORY_UNKNOWN, -1L)
    }
    assert(site == Site.Avito)
    assert(category == Category.CARS)
    assert(id == 100500)
  }

  test("apply 2") {
    val offerId = "100500-hash"
    val id = offerId match {
      case slit2(LValue(i), _) => i
      case _ => -1L
    }
    assert(id == 100500)
  }
}
