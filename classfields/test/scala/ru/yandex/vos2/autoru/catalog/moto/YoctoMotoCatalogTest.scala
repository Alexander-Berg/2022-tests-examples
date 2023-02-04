package ru.yandex.vos2.autoru.catalog.moto

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.catalog.moto.model.MotoCard

/**
  * Created by andrey on 2/13/17.
  */
@RunWith(classOf[JUnitRunner])
class YoctoMotoCatalogTest extends AnyFunSuite with InitTestDbs {
  /*private val x = new DefaultAutoruCoreComponents
  private val motoCatalog = x.motoCatalog*/

  private val motoCatalog = components.motoCatalog

  test("Read yocto index from stream") {
    motoCatalog.size should be > 0
    motoCatalog.cards should not be empty
    val testCard: MotoCard = motoCatalog.cards.drop(50).next

    /*println(motoCatalog.size)
    println("==========")
    println(testCard.message)
    println("==========")*/

    assert(testCard.message.getType.getCode == "motorcycle")
  }
}
