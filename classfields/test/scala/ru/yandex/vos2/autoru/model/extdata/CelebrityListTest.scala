package ru.yandex.vos2.autoru.model.extdata

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.autoru.InitTestDbs

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-02-13.
  */
@RunWith(classOf[JUnitRunner])
class CelebrityListTest extends AnyFunSuite with InitTestDbs {

  test("parse celebrity list from bunker data type") {
    val celebrityList = CelebrityList.from(components.extDataEngine)
    assert(celebrityList.emails.contains("mkorshunov@hsmedia.ru"))
    assert(celebrityList.phones.contains("79037965115"))
  }

}
