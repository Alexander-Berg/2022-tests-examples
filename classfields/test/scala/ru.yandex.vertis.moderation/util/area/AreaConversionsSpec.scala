package ru.yandex.vertis.moderation.util.area

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.realty.AreaInfo
import ru.yandex.vertis.moderation.proto.RealtyLight.AreaUnit

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class AreaConversionsSpec extends SpecBase {

  "AreaConversions" should {

    val Cases: Seq[(Any, Any)] =
      Seq(
        1.m2 -> 1,
        1.squareMeter -> 1,
        5.squareMeters -> 5,
        1.a -> 100,
        1.sotka -> 100,
        1.ha -> 10000,
        1.hectare -> 10000,
        10.hectares -> 100000,
        1000.m2 -> 1000,
        1000.a -> 100000,
        10000.hectares -> 100000000,
        100.km2 -> 100000000,
        0.m2 -> 0,
        0.a -> 0,
        0.hectare -> 0,
        0.km2 -> 0,
        Int.MaxValue.m2 -> Int.MaxValue,
        Int.MinValue.m2 -> Int.MinValue,
        1L.m2 -> 1L,
        1000L.km2 -> 1000000000L,
        0L.m2 -> 0L,
        Int.MaxValue.toLong.m2 -> Int.MaxValue.toLong,
        Long.MaxValue.m2 -> Long.MaxValue,
        10.1f.m2 -> 10.1f,
        10.0001f.a -> 1000.01f,
        10.0001f.ha -> 100001.0f,
        10.0001f.km2 -> 1.00001e7f
      )

    Cases.zipWithIndex.foreach { case ((actual, expected), i) =>
      s"converts int to square metres correctly at case $i ($expected)" in {
        actual should be(expected)
      }
    }

    val OverflowCases: Seq[() => Any] =
      Seq(
        () => 10000.km2,
        () => Int.MaxValue.hectares,
        () => Int.MinValue.km2,
        () => 100000000000000L.km2,
        () => -10000000.hectares,
        () => Long.MaxValue.a,
        () => java.lang.Float.POSITIVE_INFINITY.ha
      )

    OverflowCases.zipWithIndex.foreach { case (f, i) =>
      s"throw overflow at case $i" in {
        intercept[ArithmeticException](f())
      }
    }
  }
}
