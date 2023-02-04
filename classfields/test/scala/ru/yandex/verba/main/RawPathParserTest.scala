package ru.yandex.verba.main

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.util.RawPathParser


/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 26.06.14
  */
class RawPathParserTest extends AnyFlatSpec with Matchers with RawPathParser {
  it should "RawPath extract path from testing" in {
    val pathStr =
      parsePath("http://verba2.csfront01gt.yandex-team.ru/#!services/auto/marks/FORD/models/AEROSTAR/super-gen/6016010")
    pathStr shouldEqual Path("/auto/marks/FORD/models/AEROSTAR/super-gen/6016010")
  }
  it should "RawPath extract path from stable" in {
    val pathStr =
      parsePath("https://verba2.vertis.yandex-team.ru/#!services/auto/marks/BMW/models/1ER/super-gen/2305521")
    pathStr shouldEqual Path("/auto/marks/BMW/models/1ER/super-gen/2305521")
  }

  it should "RawPath extract path from stable with question symbol" in {
    val pathStr =
      parsePath("https://verba2.vertis.yandex-team.ru/?#!services/auto/marks/BMW/models/1ER/super-gen/2305521")
    pathStr shouldEqual Path("/auto/marks/BMW/models/1ER/super-gen/2305521")
  }

  it should "RawPath extract path from https url" in {
    val pathStr =
      parsePath("https://verba2.vertis.yandex-team.ru/?#!services/auto/marks/BMW/models/1ER/super-gen/2305521")
    pathStr shouldEqual Path("/auto/marks/BMW/models/1ER/super-gen/2305521")
  }

  it should "RawPath extract path from pathStr" in {
    val pathStr = parsePath("/auto/marks/BMW/models/1ER/super-gen/2305521")
    pathStr shouldEqual Path("/auto/marks/BMW/models/1ER/super-gen/2305521")
  }
}
