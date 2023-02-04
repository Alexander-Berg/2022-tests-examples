package ru.yandex.realty.util

import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase

import scala.util.{Failure, Success, Try}

@RunWith(classOf[JUnitRunner])
class UrlUtilsTest extends SpecBase {

  "Encode URL idempotent" should {

    "work without provided protocol" in {
      val url: String = "//p_e.test.ru/А1-201.png"

      val encodedUrl = UrlUtils.encodeUrlIdempotent(url)

      assertEquals("//p_e.test.ru/%D0%901-201.png", encodedUrl)
    }

    "work with IPV6 hosts" in {
      val url = "http://2001:db8:85a3:8d3:1319:8a2e:370:7348/АП14"

      val encodedUrl = UrlUtils.encodeUrlIdempotent(url)

      assertEquals("http://2001:db8:85a3:8d3:1319:8a2e:370:7348/%D0%90%D0%9F14", encodedUrl)
    }

    "work with non ASCII int the host" in {
      val url: String = "https://p e.рф/test"

      val encodedUrl = UrlUtils.encodeUrlIdempotent(url)

      assertEquals("https://p%20e.%D1%80%D1%84/test", encodedUrl)
    }

    "leave unchanged already encoded URL" in {
      val url = "https://vertis.yandex-team.ru/some/ascii/path/w%D0%B0g.jpeg"

      assertEquals(url, UrlUtils.encodeUrlIdempotent(url))
    }

    "work with empty URL" in {
      assertEquals("", UrlUtils.encodeUrlIdempotent(""))
    }

    "fail with null URL" in {
      //noinspection ScalaStyle
      Try(UrlUtils.encodeUrlIdempotent(null)) match {
        case Failure(ex) if ex.isInstanceOf[NullPointerException] =>
        case _ => fail()
      }
    }
  }
}
