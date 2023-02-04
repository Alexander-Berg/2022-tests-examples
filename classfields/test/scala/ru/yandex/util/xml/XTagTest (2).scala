package ru.yandex.util.xml

import org.junit.{Assert, Test}
import ru.yandex.vos2.util.xml.{XSelector, XTag}

/**
 * User: willzyx
 * Date: 27.07.15 20:02
 */
class XTagTest {

  @Test
  def testText(): Unit = {
    implicit class A(selector: XSelector) {
      def assert(value: String): Unit =
        Assert.assertEquals(value, selector.text())
    }
    XTag.parse("<xml>text</xml>").selector().
      assert("text")
    XTag.parse("<xml><a>hello</a><a>_</a><a>hello</a></xml>").selector().at("xml").at("a").
      assert("hello_hello")
    XTag.parse("<xml>aba_<a>hello</a><a>_</a><a>hello</a>_aba</xml>").selector().
      assert("aba_hello_hello_aba")
    XTag.parse("<xml>aba_<a>hello</a><a>_</a><a>hello</a>_aba</xml>").selector().at("xml").at("a").
      assert("hello_hello")
  }

  @Test
  def testNav() {
    implicit class A(selector: XSelector) {
      def assert(names: String*): Unit =
        Assert.assertEquals(names, selector.tags().map(_.name))
    }
    val selector = XTag.parse(
      "<xml><body1><a /><b /></body1><body2><c /><d /></body2></xml>"
    ).selector()
    selector.
      assert("xml")
    selector.at("xml").
      assert("xml")
    selector.at("xml").at("body1").
      assert("body1")

    selector.children().
      assert("body1", "body2")
    selector.children("body1").children().
      assert("a", "b")
    selector.children().children("a").
      assert("a")
  }

}
