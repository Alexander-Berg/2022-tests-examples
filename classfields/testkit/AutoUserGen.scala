package ru.auto.comeback.model.testkit

import auto.common.model.user.AutoruUser.UserRef
import auto.common.model.user.AutoruUser.UserRef.{DealerId, UserId}
import ru.auto.comeback.model.testkit.UserRefGen.{dealerUserRef, privateUserRef}
import zio.random.Random
import zio.test.Gen

object AutoUserGen {

  val anyDealerId: Gen[Random, DealerId] = Gen.long(0, Long.MaxValue).map(DealerId)
  val anyUserId: Gen[Random, UserId] = Gen.long(0, Long.MaxValue).map(UserId)
}
