package common.text.cases

import zio.test.DefaultRunnableSpec
import zio.test._

object CamelToSnakeSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("CamelToSnake")(
      test("snakify text") {
        assertTrue(CamelToSnake("getObject") == "get_object") &&
        assertTrue(CamelToSnake("HelloWorld") == "hello_world") &&
        assertTrue(CamelToSnake("multipleWordsConcatenated") == "multiple_words_concatenated") &&
        assertTrue(CamelToSnake("AlsoWith99Digits") == "also_with99_digits")
      }
    )
  }
}
