package ru.yandex.auto.vin.decoder.scheduler.task.importers

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.raw.autopilot.AutopilotNameManager

class AutopilotNameManagerTest extends AnyFunSuite {

  val manager = new AutopilotNameManager

  test("parse date from filename") {
    val filename = "autopilot30.03.2020.json"

    val timestamp = manager.getDataTimestamp(filename, 123L)

    assert(timestamp === Some(1585526400000L))
  }

  test("parse date from mailgun filename") {
    val filename = "AgIFr9LbLUHC_2IDH_RGLrHD87mSwhAoZA==-autopilot30.03.2020.json"

    val timestamp = manager.getDataTimestamp(filename, 123L)

    assert(timestamp === Some(1585526400000L))
  }

}
