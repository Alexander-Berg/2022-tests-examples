package ru.yandex.vertis.general.common.model.user.testkit

import ru.yandex.vertis.general.common.model.user.OwnerId
import ru.yandex.vertis.general.common.model.user.OwnerId.{Anonymous, UserId}
import zio.random.Random
import zio.test.{Gen, Sized}

object OwnerIdGen {

  def anyUserId(
      id: Gen[Random with Sized, Long] = Gen.anyLong): Gen[Random with Sized, UserId] =
    for {
      id <- id
    } yield UserId(id)

  val anyUserId: Gen[Random with Sized, UserId] = anyUserId().noShrink

  def anyAnonymous(
      id: Gen[Random with Sized, String] = Gen.alphaNumericStringBounded(3, 10)): Gen[Random with Sized, Anonymous] =
    for {
      id <- id
    } yield Anonymous(id)

  val anyAnonymous: Gen[Random with Sized, OwnerId] = anyAnonymous().noShrink

  val anyOwnerId: Gen[Random with Sized, OwnerId] = Gen.oneOf(anyUserId, anyAnonymous).noShrink
}
