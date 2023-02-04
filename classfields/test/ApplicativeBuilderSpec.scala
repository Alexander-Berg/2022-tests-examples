package baker.cats

import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import zio.test.environment.TestEnvironment
import cats.implicits._

object ApplicativeBuilderSpec extends DefaultRunnableSpec {
  private case class Foo(a: Int, b: String)

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ApplicativeBuilder")(
      test("should handle simple examples") {
        val result = ApplicativeBuilder[Option, Foo].build(
          1.some,
          "foo".some
        )
        assertTrue(result.get == Foo(1, "foo"))
      },
      test("should handle overly specific inputs") {
        val result = ApplicativeBuilder[Either[String, *], Foo].build(
          1.asRight[Nothing],
          "foo".asRight[String]
        )
        assertTrue(result.right.get == Foo(1, "foo"))
      }
    )
}
