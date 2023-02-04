package ru.yandex.auto.vin.decoder.scheduler.task.importers

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.raw.autoteka.AutotekaNameManager

class AutotekaNameManagerTest extends AnyFunSuite {

  val salesManager = new AutotekaNameManager(1)

  test("parse sales file name") {
    val filename = "AXSEL_111111_1_20200513-023000.csv"
    val res = salesManager.getDataTimestamp(filename, 100)

    assert(res === Some(1589337000000L))

  }

  val productsManager = new AutotekaNameManager(3)

  test("parse products file name") {
    val filename = "AgEFz_hxTBVtkXBQZtFBgqwh4hxUlqABZA==-aaamotors_0057_3_20201222-171849.csv"
    val first = productsManager.getDataTimestamp(filename, 100)

    assert(first === Some(1608657529000L))

    val name = "aaamotors_0057_3_20200101-000000.csv"
    val second = productsManager.getDataTimestamp(name, 100)
    assert(productsManager.getSimpleName(name) === name)

    assert(second === Some(1577836800000L))

  }

  test("AVTOVEKEKB_type_1") {
    val autoruNameManager = new AutotekaNameManager(1)
    val ts = autoruNameManager.parseDataTimestampFromFilename(
      "AgIFVLFSfZX79U494LZFS7k8HqWfSZgzZQ==-AVTOVEKEKB_1_20210227-032401.csv",
      1
    )
    assert(ts.nonEmpty)
    assert(ts.get == 1614396241000L)
  }

  test("AVTOVEKEKB_type_2") {
    val autoruNameManager = new AutotekaNameManager(2)
    val ts = autoruNameManager.parseDataTimestampFromFilename(
      "AgEFI3w8KA_mYCLrhChFeaAIn8ME8RlYZA==-AVTOVEKEKB_2_20210407-032401.csv",
      1
    )
    assert(ts.nonEmpty)
    assert(ts.get == 1617765841000L)
  }

  test("with type and timestamp") {
    val autoruNameManager = new AutotekaNameManager(2)
    val ts = autoruNameManager.parseDataTimestampFromFilename("2_20210407-032401.csv", 1)
    assert(ts.nonEmpty)
    assert(ts.get == 1617765841000L)
  }

  test("without type") {
    val autoruNameManager = new AutotekaNameManager(1)
    val ts = autoruNameManager.parseDataTimestampFromFilename("20210407-032401.csv", 1)
    assert(ts.isEmpty)
  }

}
