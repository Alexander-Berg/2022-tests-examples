package ru.yandex.realty.utils

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json
import ru.yandex.realty.util.crypto.Crypto

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 11.05.17
  */
@RunWith(classOf[JUnitRunner])
class CryptoTest extends FlatSpec with Matchers {
  "Crypto" should "correct encrypt" in {
    val crypto = new Crypto("^tz+nmyi3(crf$8k")
    println(crypto.encrypt(Json.obj("test" -> "res", "test2" -> 435)))
  }

}
