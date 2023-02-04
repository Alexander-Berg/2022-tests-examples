package auto.c2b.lotus.model.testkit

import auto.c2b.common.model.ApplicationTypes.ApplicationId
import auto.c2b.common.model.LotTypes.{ClientId, LotId, Price, Url, UserId}
import auto.c2b.common.testkit.CommonGenerators._
import auto.c2b.lotus.model.{Bid, Lot}
import ru.auto.api.api_offer_model.Offer
import zio.random.Random
import zio.test.magnolia.DeriveGen
import zio.test.{Gen, Sized}

import java.time.Instant

object Generators {

  trait common {

    implicit val clientIdGen: DeriveGen[ClientId] =
      DeriveGen.instance(Gen.stringN(9)(Gen.numericChar).map(id => ClientId(id)))

    implicit val userIdGen: DeriveGen[UserId] =
      DeriveGen.instance(Gen.stringN(7)(Gen.numericChar).map(id => UserId(id)))
  }

  object Lot extends common {

    val lotAny: Gen[Random with Sized, Lot] = DeriveGen.gen[Lot].derive // TODO generate only filled lot

    val paramForNewLotAny =
      DeriveGen
        .gen[(ApplicationId, Offer, Url, Instant, Price, Long, Instant)]
        .derive
  }

  object Bid extends common {

    val bidAny: Gen[Random with Sized, Bid] = DeriveGen.gen[Bid].derive

    val paramForNewBidAny =
      DeriveGen
        .gen[(LotId, UserId, ClientId, Price)]
        .derive
  }
}
