package ru.yandex.vertis.billing.yandexkassa.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.yandexkassa.model.Fields
import ru.yandex.vertis.util.crypto.UrlEncodedUtils

import scala.util.Success

/**
  * Specs for [[UrlEncodedUtils]]
  *
  * @author alesavin
  */
class UrlEncodedUtilsSpec extends AnyWordSpecLike with Matchers {

  import UrlEncodedUtils.{from, to}

  "UrlEncoded" should {
    "correctly do from" in {
      from("a=1&b=2&c=3") match {
        case Success(fields) =>
          val Map = fields.toMap
          Map.get("a") should be(Some("1"))
          Map.get("c") should be(Some("3"))
        case other => fail(s"Unexpected $other")
      }
      from("md5=&a=1") match {
        case Success(fields) =>
          val Map = fields.toMap
          Map.get("md5") should be(Some(""))
        case other => fail(s"Unexpected $other")
      }
      from("a=1&=4") match {
        case Success(fields) =>
          val Map = fields.toMap
          Map.get("") should be(Some("4"))
        case other => fail(s"Unexpected $other")
      }
    }
    "pass to and from" in {

      def toFrom(fields: Fields) = {
        from(to(fields).get).get should be(fields)
      }
      toFrom(Seq("a" -> "b"))
      toFrom(Seq("a" -> "b", "c" -> "d"))
      toFrom(Seq("a" -> "b", "a" -> "b"))
      toFrom(Seq("a" -> "b", "a" -> "b", "a" -> "b"))
      toFrom(Seq("00001" -> "3.555"))
      toFrom(Seq("русс" -> "#@$@#$@$@"))
      toFrom(Seq("русс" -> "&&&&="))
      toFrom(Seq("qwkjeh1231389123" -> "a djmbad asdakasdihu ku"))
      toFrom(Seq("\n\r\t" -> "\u0001"))
      toFrom(Seq("md5" -> ""))
      toFrom(Seq("" -> "ddd"))
    }
  }

}
