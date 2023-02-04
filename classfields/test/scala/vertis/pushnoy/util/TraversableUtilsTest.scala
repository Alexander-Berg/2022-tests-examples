package vertis.pushnoy.util

import org.scalatest.funsuite.AsyncFunSuite
import vertis.pushnoy.util.TraversableUtils._

class TraversableUtilsTest extends AsyncFunSuite {

  test("implode strings") {
    assert(Seq("one").implode(", ", " and ") == "one")
    assert(Seq("one", "two").implode(", ", " and ") == "one and two")
    assert(Seq("one", "two", "three").implode(", ", " and ") == "one, two and three")
  }
}
