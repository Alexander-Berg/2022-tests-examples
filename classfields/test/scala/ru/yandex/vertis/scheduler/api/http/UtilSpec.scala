package ru.yandex.vertis.scheduler.api.http

import org.scalatest.{Matchers, WordSpecLike}

/**
 * Specs on [[Util]].
 *
 * @author alesavin
 */
class UtilSpec
  extends Matchers
  with WordSpecLike {

  import Util._

  "Util" should {
    "convert message to pretty print format" in {
      prettyPrint("") should be ("")
      prettyPrint("\n") should be ("?")
      prettyPrint("\n\r") should be ("??")
      prettyPrint("string\nstring2\n") should be ("string?string2?")
      prettyPrint("\u0000\u0080\u008c") should be ("???")
      prettyPrint("тест") should be ("тест")
    }
  }
}