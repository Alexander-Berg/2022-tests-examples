package ru.yandex.vertis.general.common.model.user.testkit

import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.common.model.user.SellerId.{AggregatorId, StoreId, UserId}
import zio.random.Random
import zio.test.magnolia.DeriveGen
import zio.test.{Gen, Sized}

object SellerGen {

  def anyUserId(
      id: Gen[Random with Sized, Long] = Gen.anyLong): Gen[Random with Sized, UserId] =
    for {
      id <- id
    } yield UserId(id)

  val anyUserId: Gen[Random with Sized, UserId] = anyUserId()

  def anyStringId(
      id: Gen[Random with Sized, String] = Gen.alphaNumericStringBounded(3, 10)): Gen[Random with Sized, String] = id

  val anyStoreId: Gen[Random with Sized, StoreId] = anyStringId().map(StoreId)

  val anyAggregatorId: Gen[Random with Sized, AggregatorId] = anyStringId().map(AggregatorId)

  val anySellerId: Gen[Random with Sized, SellerId] = Gen.oneOf(anyUserId, anyStoreId)

  implicit val anySellerIdDeriveGen: DeriveGen[SellerId] = DeriveGen.instance(anySellerId)
}
