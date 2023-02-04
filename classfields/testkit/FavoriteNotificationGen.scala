package ru.yandex.vertis.general.favorites.model.testkit

import io.circe.Json
import ru.yandex.vertis.general.favorites.model.{EmailNotification, FavoriteNotification, PushNotification}
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.test.magnolia.DeriveGen
import general.common.image_model.ImageData
import general.common.image_model.MDSImage

object FavoriteNotificationGen {

  implicit private val unknownFieldSetGen: DeriveGen[_root_.scalapb.UnknownFieldSet] =
    DeriveGen.instance(Gen.const(_root_.scalapb.UnknownFieldSet.empty))

  val pushNotificationGen: Gen[Random with Sized, PushNotification] = DeriveGen[PushNotification]
  implicit val emailParamsGen: DeriveGen[Json] = DeriveGen.instance(Gen.const(Json.Null))
  val emailNotificationGen: Gen[Random with Sized, EmailNotification] = DeriveGen[EmailNotification]

  val favoriteNotificationGen: Gen[Random with Sized, FavoriteNotification] =
    Gen.oneOf(pushNotificationGen, emailNotificationGen)

  def favoriteNotifications(count: Int): Gen[Random with Sized, List[FavoriteNotification]] =
    Gen.listOfN(count)(favoriteNotificationGen.noShrink)
}
