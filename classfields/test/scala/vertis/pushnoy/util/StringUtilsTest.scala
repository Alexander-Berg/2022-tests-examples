package vertis.pushnoy.util

import StringUtils._
import org.scalatest.funsuite.AsyncFunSuite

class StringUtilsTest extends AsyncFunSuite {

  test("RichString") {
    assert("".noneIfEmpty.isEmpty)
    assert(" ".noneIfEmpty.isEmpty)
    assert("s".noneIfEmpty.isDefined)
  }
}
