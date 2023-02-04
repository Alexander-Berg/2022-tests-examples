package ru.yandex.vertis.general.favorites.model.testkit

import general.favorites.notification_model.NotificationSettings
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.favorites.model.sellers.SavedSeller
import zio.random.Random
import zio.test.{Gen, Sized}

object SavedSellerGen {

  def anySavedSeller(
      sellerId: Gen[Random with Sized, SellerId] = SellerGen.anySellerId): Gen[Random with Sized, SavedSeller] =
    for {
      sellerId <- sellerId
    } yield SavedSeller(sellerId = sellerId, NotificationSettings())

  val anySavedSeller: Gen[Random with Sized, SavedSeller] = anySavedSeller().noShrink

  def anySavedSeller(count: Int): Gen[Random with Sized, List[SavedSeller]] =
    Gen.listOfN(count)(anySavedSeller).noShrink
}
