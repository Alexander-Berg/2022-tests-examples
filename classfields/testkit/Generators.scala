package auto.c2b.carp.storage.testkit

import auto.c2b.carp.model._
import auto.c2b.common.testkit.CommonGenerators._
import zio.random.Random
import zio.test.magnolia.DeriveGen
import zio.test.{Gen, Sized}

object Generators {

  object Application {

    val InspectionPlaceGen: Gen[Random with Sized, InspectionPlace] = DeriveGen.gen[InspectionPlace].derive
    val StatusGen: Gen[Random with Sized, Status] = DeriveGen.gen[Status].derive

    val applicationAny: Gen[Random with Sized, Application] = DeriveGen.gen[Application].derive
  }
}
