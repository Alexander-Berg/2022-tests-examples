package auto.dealers.application.api.test

import ru.auto.application.palma.proto.application_palma_model.CreditConfiguration
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.test.magnolia._

object Gens {

  implicit private val unknownFieldSetGen: DeriveGen[_root_.scalapb.UnknownFieldSet] =
    DeriveGen.instance(Gen.const(_root_.scalapb.UnknownFieldSet.empty))

  val CreditConfigurationGen: Gen[Random with Sized, CreditConfiguration] = DeriveGen[CreditConfiguration]
}
