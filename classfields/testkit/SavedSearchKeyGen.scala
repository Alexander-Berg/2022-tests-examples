package ru.yandex.vertis.general.favorites.model.testkit

import ru.yandex.vertis.general.common.model.user.OwnerId
import ru.yandex.vertis.general.common.model.user.testkit.OwnerIdGen
import ru.yandex.vertis.general.favorites.model.searches.SavedSearchInvertedKey
import zio.random.Random
import zio.test.{Gen, Sized}

object SavedSearchKeyGen {

  def anySavedSearchKey(
      shardId: Gen[Random, Int] = Gen.anyInt,
      searchId: Gen[Random with Sized, String] = Gen.anyASCIIString,
      ownerId: Gen[Random with Sized, OwnerId] =
        OwnerIdGen.anyOwnerId): Gen[Random with Sized, SavedSearchInvertedKey] = {
    for {
      shardId <- shardId
      searchId <- searchId
      ownerId <- ownerId
    } yield SavedSearchInvertedKey(
      shardId = shardId,
      searchId = searchId,
      ownerId = ownerId
    )
  }

  val anySavedSearchKey: Gen[Random with Sized, SavedSearchInvertedKey] = anySavedSearchKey()

  def anySavedSearchKeys(count: Int): Gen[Random with Sized, List[SavedSearchInvertedKey]] =
    Gen.listOfN(count)(anySavedSearchKey)

}
