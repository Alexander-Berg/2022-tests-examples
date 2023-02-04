package auto.carfax.promo_dispenser.storage.testkit

import auto.carfax.promo_dispenser.model.PoolPromocode
import auto.common.model.user.AutoruUser.UserRef.UserId
import zio.random.Random
import zio.test.Gen

object Generators {

  val anyUserId: Gen[Random, String] = Gen.long(0, Long.MaxValue).map(UserId).map(_.toString)
  val anyPoolId: Gen[Random, String] = Gen.stringN(20)(Gen.alphaNumericChar)
  val anyPromocode: Gen[Random, String] = Gen.stringN(20)(Gen.alphaNumericChar)

  val notUsedPoolPromocode: Gen[Random, PoolPromocode] = for {
    poolId <- anyPoolId
    promocode <- anyPromocode
  } yield PoolPromocode(poolId, promocode, None)

  val usedPoolPromocode: Gen[Random, PoolPromocode] = for {
    poolId <- anyPoolId
    promocode <- anyPromocode
    user <- anyUserId
  } yield PoolPromocode(poolId, promocode, Some(user))

  val anyPoolPromocode: Gen[Random, PoolPromocode] = Gen.oneOf(notUsedPoolPromocode, usedPoolPromocode)
}
