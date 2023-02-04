package ru.yandex.verba.core.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import QueryBuilders._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 08.11.13 16:19
  */
class QueryBuildersTest extends AnyFreeSpec {

  "inSectionParams(count)" - {
    "should produce valid sql in section with '?' placeholders" in {
      inSectionParams(10).replaceAll(" ", "") shouldEqual "(?,?,?,?,?,?,?,?,?,?)"
      //todo generate tests with scalaCheck
      pending
    }

    "should throw IAE if `count` not in range [1, 1000]" in {
      intercept[IllegalArgumentException] {
        inSectionParams(-1)
      }
      intercept[IllegalArgumentException] {
        inSectionParams(1002)
      }
    }
  }

}
