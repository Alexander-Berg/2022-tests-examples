package ru.yandex.auto.eds.tax

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ElectroCarTaxBenefitsTest extends FunSuite {

  test("check for Tambov") {
    val rid = 10802
    val yearFrom = 2020
    val yearTo = 2023
    val hpMax = 250

    assert(ElectroCarTaxBenefits.hasBenefit(rid, yearFrom, hpMax))
    assert(ElectroCarTaxBenefits.hasBenefit(rid, yearTo, hpMax))

    assert(ElectroCarTaxBenefits.hasBenefit(rid, yearFrom, hpMax - 1))
    assert(!ElectroCarTaxBenefits.hasBenefit(rid, yearFrom, hpMax + 1))

    assert(!ElectroCarTaxBenefits.hasBenefit(rid, yearFrom - 1, hpMax))
    assert(ElectroCarTaxBenefits.hasBenefit(rid, yearFrom + 1, hpMax))
    assert(ElectroCarTaxBenefits.hasBenefit(rid, yearTo - 1, hpMax))
    assert(!ElectroCarTaxBenefits.hasBenefit(rid, yearTo + 1, hpMax))

    assert(!ElectroCarTaxBenefits.hasBenefit(rid + 1, yearFrom, hpMax))
  }
}
