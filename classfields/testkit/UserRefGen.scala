package ru.auto.comeback.model.testkit

import zio.random.Random
import zio.test.Gen

object UserRefGen {

  val privateUserRef: Gen[Random, String] = Gen.anyInt.map("user:" + _)
  val dealerUserRef: Gen[Random, String] = Gen.anyInt.map("dealer:" + _)
  val anyUserRef: Gen[Random, String] = Gen.oneOf(privateUserRef, dealerUserRef)
}
