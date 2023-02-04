package ru.yandex.vos2.autoru.model.extdata

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.autoru.InitTestDbs

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-01-31.
  */
@RunWith(classOf[JUnitRunner])
class VinAndLicensePlateRegionsTest extends AnyFunSuite with InitTestDbs {

  test("parse regions from bunker data type") {
    val regions = VinAndLicensePlateRequiredRegions.from(components.extDataEngine)
    pendingUntilFixed { //todo: update bukner
      assert(regions.regions == Set(213L))
    }
  }

}
