package ru.yandex.verba.core.util

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.model.tree.Path

import scala.annotation.nowarn
import scala.io.Source

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 21.10.14
  */
@nowarn("cat=w-flag-value-discard")
class SqlCreatorTest extends AnyFreeSpec with VerbaUtils with Matchers {

  "diff paths test" - {
    "empty path" - {
      QueryBuilders.makeSql(Seq(Path("/"))) shouldEqual None
    }
    "wildcard path" - {
      val Some((sql, params)) = QueryBuilders.makeSql(Seq(Path("/auto/*")))
      sql shouldEqual "l1.code = ?"
      params shouldEqual Seq("auto")
    }
    "complex wildcard path" - {
      val Some((sql, params)) = QueryBuilders.makeSql(Seq(Path("/auto/*/x/*")))
      sql shouldEqual "l1.code = ? and l3.code = ?"
      params shouldEqual Seq("auto", "x")
    }
    "service wildcard path" - {
      QueryBuilders.makeSql(Seq(Path("/*"))) shouldEqual None
    }
    "usual path" - {
      val Some((sql, params)) = QueryBuilders.makeSql(Seq(Path("/a/b/c")))
      sql shouldEqual "l1.code = ? and l2.code = ? and l3.code = ?"
      params shouldEqual Seq("a", "b", "c")
    }
    "complex test" ignore {
      using(Source.fromInputStream(getClass.getResourceAsStream("/ids-for-sql-optimize.txt"))) { src =>
        val paths = src.getLines().map(Path.apply).toSeq
        val Some((sql, params)) = QueryBuilders.makeSql(paths)
        assert(sql.startsWith("l1.code = ? and l2.code = ? and ((l4.code = ? and (l3.code in "))
        sql.startsWith("l1.code = ? and l2.code = ? and ((l4.code = ? and (l3.code in ") shouldEqual true
        sql.count(_ == '?') shouldEqual 1176
        params.size shouldEqual 1176
        params.take(3) shouldEqual Seq("auto", "unauthorized-dealers", "add-url")
      }
    }
  }

}
