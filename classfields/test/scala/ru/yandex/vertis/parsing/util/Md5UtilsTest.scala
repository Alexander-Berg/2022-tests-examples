package ru.yandex.vertis.parsing.util

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * Created by andrey on 11/8/17.
  */
@RunWith(classOf[JUnitRunner])
class Md5UtilsTest extends FunSuite {
  test("md5") {
    val url = "https://m.avito.ru/tula/avtomobili/chevrolet_cruze_2015_1006544693"
    assert(Md5Utils.md5(url) == "d195ccecb6eb826c636744059e3611c0")

    val url2 = "https://m.avito.ru/protvino/gruzoviki_i_spetstehnika/skaniya_113_scania_1046559435"
    assert(Md5Utils.md5(url2) == "609aef71e448a9580fb7642e4aa0d2a3")
  }
}
