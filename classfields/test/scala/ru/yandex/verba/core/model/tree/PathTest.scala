package ru.yandex.verba.core.model.tree

import org.json4s.JsonAST.JString
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.util.Logging

/**
  * User: Vladislav Dolbilov (darl@yandex-team.ru)
  * Date: 28.01.13 21:28
  */
class PathTest extends AnyFlatSpec with Matchers with Logging {
  import Path._

  "Absolute" should "parse string path to parts" in {
    Path("/abc/ttt").parts shouldEqual "abc" :: "ttt" :: Nil
  }

  it should "build path from parts" in {
    new Path("1" :: "2" :: Nil).toString shouldEqual "/1/2"
  }

  it should "throw exception on non-absolute string path" in {
    intercept[IllegalArgumentException] {
      Path("ss/aa")
    }
  }

  it should "throw NPE on null string path" in {
    intercept[NullPointerException] {
      Path(null.asInstanceOf[String])
    }
  }

  it should "be unique if not contain wildcards" in {
    Path("/a/bc/d").unique shouldEqual true
    Path("/a/**/d").unique shouldEqual true //not treated as wildcard
  }

  it should "be non unique if contain wildcards" in {
    Path("/a/*/d").unique shouldEqual false
  }

  it should "be case-sensitive" in {
    Path("/A/B") should !==(Path("/a/b"))
    Path("/a/b") shouldEqual Path("/a/b")
  }

  it should "return parent path from `parent` method" in {
    Path("/a/bc/d").parent shouldEqual Path("/a/bc")
    intercept[UnsupportedOperationException] {
      Path("/").parent
    }
  }

  it should "serializable to json" in {
    val path = Path("/a/b/c/d")
    path.asJson shouldEqual JString(path.toString)
  }

  "Relative" should "parse string path to parts" in {
    Relative("tt1").parts shouldEqual "tt1" :: Nil
    Relative("../tt2").parts shouldEqual ".." :: "tt2" :: Nil
    Relative("./../ggg/./tt1").parts shouldEqual "." :: ".." :: "ggg" :: "." :: "tt1" :: Nil
  }

  it should "produce Absolute when joined with absolute" in {
    Path("/a/b/c") / Relative("../../d/e") shouldEqual Path("/a/d/e")
    Path("/a/b/c") / "./d/e" shouldEqual Path("/a/b/c/d/e")
    Path("/a/b/c") / "d/e" shouldEqual Path("/a/b/c/d/e")
  }

  it should "produce Relative when joined with relative" in {
    Relative("a/b/c") / Relative("d/e/f") shouldEqual Relative("a/b/c/d/e/f")
    Relative("a/b/c") / "../d" shouldEqual Relative("a/b/c/../d")
  }
}
