package common.html.sanitizer

import zio.test.Assertion._
import zio.test._

object HtmlSanitizerSpec extends DefaultRunnableSpec {

  def spec =
    suite("HtmlSanitizer")(
      testM("Keep save tags") {
        assertM(HtmlSanitizer.sanitize("<p>HelloWorld</p>"))(equalTo("<p>HelloWorld</p>"))
      },
      testM("Remove unsafe tags") {
        assertM(HtmlSanitizer.sanitize("<script>alert('')</script>lalala"))(equalTo("lalala"))
      },
      testM("Remove h1 tag") {
        assertM(HtmlSanitizer.sanitize("<h1>Hello world!</h1>"))(equalTo("Hello world!"))
      },
      testM("escape html entities") {
        assertM(HtmlSanitizer.sanitize("два < чем три"))(equalTo("два &lt; чем три"))
      },
      testM("escape html entities #2") {
        assertM(HtmlSanitizer.sanitize("2<3"))(equalTo("2&lt;3"))
      }
    )
}
