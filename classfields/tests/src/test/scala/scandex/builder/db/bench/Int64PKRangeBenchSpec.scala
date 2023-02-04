package scandex.builder.db.bench

import scandex.model.gen.*
import scandex.model.meta.FieldDataTypeMeta.IndexType
import strict.*
import tests.strict.types.*
import zio.test.*
import zio.test.Assertion.*

object Int64PKRangeBenchSpec extends ZIOSpecDefault {

  val schema = DocumentGenSchema(
    anyInt64,
    SingleValueFieldGen("singleUtf8", IndexType.RANGE, anyUtf8),
  )

  override def spec: Spec[TestEnvironment, Any] =
    suite("DatabaseBuilder[Int64] with Int64Range")(
      Common.fillRef(schema, 800000),
      test("build 10000 (warming up)") {
        for {
          _ <- Common.build[Int64](10000)
        } yield assert(())(isUnit)
      },
      test("build 10000") {
        for {
          _ <- Common.build[Int64](10000)
        } yield assert(())(isUnit)
      },
      test("build 25000") {
        for {
          _ <- Common.build[Int64](25000)
        } yield assert(())(isUnit)
      },
      test("build 50000") {
        for {
          _ <- Common.build[Int64](50000)
        } yield assert(())(isUnit)

      },
      test("build 100000") {
        for {
          _ <- Common.build[Int64](100000)
        } yield assert(())(isUnit)

      },
      test("build 200000") {
        for {
          _ <- Common.build[Int64](200000)
        } yield assert(())(isUnit)

      },
      test("build 400000") {
        for {
          _ <- Common.build[Int64](400000)
        } yield assert(())(isUnit)
      },
      test("build 800000") {
        for {
          _ <- Common.build[Int64](800000)
        } yield assert(())(isUnit)
      },
    ).provideCustomLayerShared(Common.refLayer[Int64]()) @@
      TestAspect.sequential @@ TestAspect.ignore

}
