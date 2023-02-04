package common.html.parser.test

import common.html.parser.src.HtmlParser
import zio.test._
import zio.test.Assertion._

object HtmlParserSpec extends DefaultRunnableSpec {

  def spec = suite("HtmlParser")(
    testM("Вытаскивает текст из html") {
      assertM(
        HtmlParser.extractTextContent(
          """<br><p><em><strong>Канекалоны модные косички</strong></em></p> <p><strong>Цвет Белый</strong></p>"""
        )
      )(equalTo("""Канекалоны модные косички Цвет Белый"""))
    },
    testM("Вытаскиевает текст из невалидного html") {
      assertM(
        HtmlParser.extractTextContent(
          """<br><p><em><strong>Канекалоны модные косички 1<2"""
        )
      )(equalTo("""Канекалоны модные косички 1<2"""))
    }
  )

}
