package ru.yandex.auto.vin.decoder.raw.autoru

import org.scalatest.funsuite.AnyFunSuite

class AutoruNameManagerTest extends AnyFunSuite {

  test("beliyservice") {
    val autoruNameManager = new AutoruNameManager(2)
    val ts = autoruNameManager.parseDataTimestampFromFilename("beliyservice_2_20201126-074640.json", 1)
    assert(ts.nonEmpty)
    assert(ts.get == 1606376800000L)
  }

  test("freshauto") {
    val autoruNameManager = new AutoruNameManager(6)
    val ts = autoruNameManager.parseDataTimestampFromFilename("freshauto_6_20201228-003105.json", 1)
    assert(ts.nonEmpty)
    assert(ts.get == 1609115465000L)
  }
}
