package ru.yandex.auto.clone.unifier.modifier

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}

/**
  * Created by goodfella on 28.12.16.
  */
@RunWith(classOf[JUnitRunner])
class AutoruPhotoUrlModifierSpec extends WordSpecLike with Matchers {
  val urlModifier = new AutoruPhotoUrlModifier

  "AutoruPhotoUrlModifier" should {
    "have a 'me' val".which {
      "equals to its class name" in {
        urlModifier.me should be(urlModifier.getClass.getName)
      }
    }
    "have a trimProtocol method".which {
      "trim anything before '//'" in {
        val addr = "host/path/next"
        val list = ("http://" + addr) :: ("ftp://" + addr) :: Nil
        list.map(urlModifier.trimProtocol(_)).foreach(_ should be("//" + addr))
      }
      "does not modify url starts with //" in {
        val url = "//host/path/prefix"
        urlModifier.trimProtocol(url) should be(url)
      }
      "is not greedy" in {
        val url = "//host//path//anything"
        urlModifier.trimProtocol(url) should be(url)
      }
    }
  }
}
