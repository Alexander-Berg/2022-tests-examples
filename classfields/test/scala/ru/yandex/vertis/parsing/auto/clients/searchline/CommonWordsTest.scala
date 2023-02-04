package ru.yandex.vertis.parsing.auto.clients.searchline

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.auto.clients.searchline.CommonWords

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class CommonWordsTest extends FunSuite {
  test("keep displacement") {
    val text = "LADA Priora 1.6 МТ"
    assert(CommonWords.tokenize(text).toList == List("LADA", "Priora", "1.6", "МТ"))
  }

  test("replace dot") {
    val text = "Скания scania G400 2013г.в"
    assert(CommonWords.tokenize(text).toList == List("Скания", "scania", "G400", "2013г", "в"))
  }

  test("exact match") {
    assert(CommonWords.improve("Huanghai 5.2 МТ").toList == List("Huanghai", "5.2"))
    assert(CommonWords.improve("Hyundai ix35 2.0 AT").toList == List("Hyundai", "ix35", "2.0"))
    assert(CommonWords.improve("Mercedes-Benz Sprinter 2.2 AMT").toList == List("Mercedes", "Benz", "Sprinter", "2.2"))
  }
}
