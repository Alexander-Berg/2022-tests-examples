package infra.feature_toggles.model.testkit

import infra.feature_toggles.model.Value
import zio.random.Random
import zio.test.{Gen => ZGen, Sized}
import zio.test.magnolia.DeriveGen

object Gen {
  val service: ZGen[Random with Sized, String] = ZGen.alphaNumericStringBounded(3, 5)
  val key: ZGen[Random with Sized, String] = ZGen.alphaNumericStringBounded(3, 5)

  val value: ZGen[Random with Sized, Value] = DeriveGen[Value]
}
