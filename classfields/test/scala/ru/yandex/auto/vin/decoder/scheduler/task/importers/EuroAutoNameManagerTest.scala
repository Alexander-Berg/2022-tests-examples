package ru.yandex.auto.vin.decoder.scheduler.task.importers

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.raw.euroauto.EuroAutoNameManager

class EuroAutoNameManagerTest extends AnyFunSuite {

  val manager = new EuroAutoNameManager

  test("correct parse") {
    val name = "https://autoteka:xu3hhe4q@euroauto.ru/yml/autoteka/euro_auto_2016_01_1.json"

    val result = manager.getDataTimestamp(name, 100L)

    assert(result === Some(1451606400001L))
  }

  test("add week index to timestamp") {
    val name1 = "https://autoteka:xu3hhe4q@euroauto.ru/yml/autoteka/euro_auto_2019_05_2.json"
    val name2 = "https://autoteka:xu3hhe4q@euroauto.ru/yml/autoteka/euro_auto_2019_05_4.json"

    val result1 = manager.getDataTimestamp(name1, 100L).get
    val result2 = manager.getDataTimestamp(name2, 100L).get

    assert(result2 - result1 === 2)
  }

}
