package auto.dealers.multiposting.clients.avito.test.model

import auto.dealers.multiposting.clients.avito.model.{Product, Vas, VasPackage}
import io.circe.syntax._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.sequential

object ProductSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("Product")(
    suite("parsing")(tests: _*),
    suite("vas to json")(vasToJson: _*),
    suite("product to json")(productToJson: _*)
  ) @@ sequential

  val tests: Seq[ZSpec[Any, Nothing]] = for {
    (value, expected) <- Seq(
      ("xl", Product.XL),
      ("highlight", Product.Highlight),
      ("x2_1", Product.X2_1),
      ("x2_7", Product.X2_7),
      ("x5_1", Product.X5_1),
      ("x5_7", Product.X5_7),
      ("x10_1", Product.X10_1),
      ("x10_7", Product.X10_7)
    )
  } yield test(value) {
    assert(Product.withNameInsensitive(value))(equalTo(expected))
  }

  val vasToJson: Seq[ZSpec[Any, Nothing]] = for {
    (value, expected) <- Seq(
      (Product.XL, """{"vas_id":"xl"}"""),
      (Product.Highlight, """{"vas_id":"highlight"}""")
    )
  } yield test(value.entryName) {
    val vas: Vas = value
    assert(vas.asJson.noSpaces)(equalTo(expected))
  }

  val productToJson: Seq[ZSpec[Any, Nothing]] = for {
    (value, expected) <- Seq(
      (Product.X2_1, """{"package_id":"x2_1"}"""),
      (Product.X2_7, """{"package_id":"x2_7"}"""),
      (Product.X5_1, """{"package_id":"x5_1"}"""),
      (Product.X5_7, """{"package_id":"x5_7"}"""),
      (Product.X10_1, """{"package_id":"x10_1"}"""),
      (Product.X10_7, """{"package_id":"x10_7"}""")
    )
  } yield test(value.entryName) {
    val vas: VasPackage = value
    assert(vas.asJson.noSpaces)(equalTo(expected))
  }
}
