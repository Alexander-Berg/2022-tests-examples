package ru.yandex.realty.unification.unifier.processor.unifiers

import java.net.URL

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 16.11.17
  */
@RunWith(classOf[JUnitRunner])
class NMarketPlansProcessorTest extends FlatSpec with Matchers {
  "NMarket processor" should "correct change url" in {
    val oldUrl =
      new URL("http://img.nmarket.pro/photo/pid/71CC934D-D26C-4E64-B0F9-DE233162118C/?wpsid=17&v=2&w=1600&h=986")
    val newUrl = new URL("http", "host", 1254, oldUrl.getFile)
    newUrl.toString should be(
      "http://host:1254/photo/pid/71CC934D-D26C-4E64-B0F9-DE233162118C/?wpsid=17&v=2&w=1600&h=986"
    )
  }

}
