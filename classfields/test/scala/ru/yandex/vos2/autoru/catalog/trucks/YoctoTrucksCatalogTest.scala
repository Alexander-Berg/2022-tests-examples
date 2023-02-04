package ru.yandex.vos2.autoru.catalog.trucks

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.catalog.trucks.model.TruckCard

/**
  * Created by andrey on 2/13/17.
  */
@RunWith(classOf[JUnitRunner])
class YoctoTrucksCatalogTest extends AnyFunSuite with InitTestDbs {
  /*private val x = new DefaultAutoruCoreComponents
  private val trucksCatalog = x.trucksCatalog*/

  private val trucksCatalog = components.trucksCatalog

  test("Read yocto index from stream") {
    trucksCatalog.size should be > 0
    trucksCatalog.cards should not be empty
    val testCard: TruckCard = trucksCatalog.cards.drop(50).next
    assert(testCard.message.getType.getCode == "BUS")

    /*println(trucksCatalog.size)
    println("==========")
    println(testCard.message)
    println("==========")*/

  }
}
