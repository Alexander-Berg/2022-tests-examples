package common.zio.doobie.test

import cats.data.NonEmptyList
import common.zio.doobie.syntax.QueryBuilder
import doobie.implicits._
import zio.test.Assertion._
import zio.test._

object QueryBuilderTest extends DefaultRunnableSpec {

  override def spec: ZSpec[environment.TestEnvironment, Any] = suite("QueryBuilder")(
    test("""in for three columns should generate correct SQL""") {
      val actual = QueryBuilder.in(fr"foo", NonEmptyList.of((1, true, "hi"), (2, false, "world"))).query[Unit].sql
      val expected = "foo IN ((?,?,?), (?,?,?)) "
      assert(actual)(equalTo(expected))
    }
  )

}
