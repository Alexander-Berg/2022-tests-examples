package ru.yandex.realty.util.geometry

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 18.08.17
  */
@RunWith(classOf[JUnitRunner])
class ConverterTest extends FlatSpec with Matchers {
  "Converter.toPolygon" should "correct process polygons from NMarket" in {
    val p = Converter.parsePolygon("POLYGON ((553 256, 754 256, 754 375, 553 375, 553 256))")
    p should not be (null)
  }
}
