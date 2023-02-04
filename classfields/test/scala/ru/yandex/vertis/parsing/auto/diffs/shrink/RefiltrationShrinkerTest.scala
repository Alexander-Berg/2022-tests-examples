package ru.yandex.vertis.parsing.auto.diffs.shrink

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.parsing.auto.ParsingAutoModel
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.scheduler.workers.shrink.shrinkers.RefiltrationShrinker
import ru.yandex.vertis.parsing.util.protobuf.Protobuf

@RunWith(classOf[JUnitRunner])
class RefiltrationShrinkerTest extends FunSuite {
  private val shrinker = RefiltrationShrinker

  test("shrink refiltration") {
    val data = Protobuf.fromJson[ParsingAutoModel.ParsedOffer](
      scala.io.Source.fromURL(getClass.getResource("/need_refiltration_shrink.json"))("UTF-8").getLines.mkString("\n")
    )
    val url = testAvitoCarsUrl
    val row = testRow(url, data.toBuilder, Category.CARS, doppelClusterSeq = Some(Seq()))
    val res = shrinker.shrinkData(row.data)
    val result = Protobuf.fromJson[ParsingAutoModel.ParsedOffer](
      scala.io.Source.fromURL(getClass.getResource("/refiltration_shrink_result.json"))("UTF-8").getLines.mkString("\n")
    )
    assert(res == result)
  }
}
