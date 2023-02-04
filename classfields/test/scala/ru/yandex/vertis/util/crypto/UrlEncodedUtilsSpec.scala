package ru.yandex.vertis.util.crypto

import org.scalatest.{Matchers, WordSpecLike}

import scala.util.Success

/**
 * Specs for [[UrlEncodedUtils]]
 *
 * @author alesavin
  */
class UrlEncodedUtilsSpec
  extends WordSpecLike
  with Matchers {

    import UrlEncodedUtils.{from, to}

    "UrlEncoded" should {
      "correctly do from" in {
         from("a=1&b=2&c=3") match {
           case Success(fields) =>
             val Map = fields.toMap
             Map.get("a") should be (Some("1"))
             Map.get("c") should be (Some("3"))
           case other => fail(s"Unexpected $other")
         }
        from("md5=&a=1") match {
          case Success(fields) =>
            val Map = fields.toMap
            Map.get("md5") should be (Some(""))
          case other => fail(s"Unexpected $other")
        }
        from("a=1&=4") match {
          case Success(fields) =>
            val Map = fields.toMap
            Map.get("") should be (Some("4"))
          case other => fail(s"Unexpected $other")
        }
      }
      "pass to and from" in {

        def roundTrip(fields: Field*) = {
          from(to(fields).get).get should be (fields)
        }
        roundTrip("a" -> "b")
        roundTrip("a" -> "b", "c" -> "d")
        roundTrip("a" -> "b", "a" -> "b")
        roundTrip("a" -> "b", "a" -> "b", "a" -> "b")
        roundTrip("00001" -> "3.555")
        roundTrip("русс" -> "#@$@#$@$@")
        roundTrip("русс" -> "&&&&=")
        roundTrip("qwkjeh1231389123" -> "a djmbad asdakasdihu ku")
        roundTrip("\n\r\t" -> "\u0001")
        roundTrip("md5" -> "")
        roundTrip("" -> "ddd")
        roundTrip()
        roundTrip("a" -> "b", "" -> "c", "d" -> "", "e" -> "", "" -> "f")
      }
    }

  }

