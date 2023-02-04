package ru.yandex.auto.vin.decoder.raw

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.report.converters.raw.SamplePreparedData

class SamplePreparedDataTest extends AnyFunSuite {

  test("Sample recall data is set correctly") {
    val recalls = SamplePreparedData.data.recalls
    assert(recalls.nonEmpty)
    assert(recalls.head.getRecallsList.size() == 1)
  }
}
