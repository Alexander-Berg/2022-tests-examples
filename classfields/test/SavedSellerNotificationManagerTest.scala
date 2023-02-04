package ru.yandex.vertis.general.favorites.logic.test

import common.jwt.Jwt
import common.jwt.Jwt.HMACConfig
import common.scalapb.ScalaProtobuf.instantToTimestamp
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.bonsai.category_model.{Category => BonsaiCategory}
import general.common.price_model
import general.common.price_model.Price
import general.common.price_model.Price.Price.PriceInCurrency
import general.favorites.notification_model.NotificationChannel._
import general.favorites.notification_model.NotificationSettings
import general.gost.offer_api.CabinetOffersResponse
import general.gost.offer_model.AttributeValue.Value.Number
import general.gost.offer_model.OfferStatusEnum.OfferStatus
import general.gost.offer_model._
import general.gost.seller_model.Seller
import general.users.model.{User, UserView}
import io.circe.Json
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.common.model.user.SellerId.toApiSellerId
import ru.yandex.vertis.general.common.model.user.{OwnerId, SellerId}
import ru.yandex.vertis.general.favorites.logic.{FavoritesStore, SavedSellersManager, SavedSellersNotificationManager}
import ru.yandex.vertis.general.favorites.model.searches.ShardCount
import ru.yandex.vertis.general.favorites.model.sellers.SavedSeller
import ru.yandex.vertis.general.favorites.model.{EmailNotification, PushNotification}
import ru.yandex.vertis.general.favorites.storage.FavoritesNotificationQueueDao
import ru.yandex.vertis.general.favorites.storage.ydb.counts.YdbFavoritesCountDao
import ru.yandex.vertis.general.favorites.storage.ydb.favorites.YdbFavoritesDao
import ru.yandex.vertis.general.favorites.storage.ydb.inverted.{
  YdbSavedSellerInvertedCountDao,
  YdbSavedSellerInvertedDao
}
import ru.yandex.vertis.general.favorites.storage.ydb.notifications.YdbFavoritesNotificationQueueDao
import ru.yandex.vertis.general.gateway.clients.router.testkit.RouterClientMock
import ru.yandex.vertis.general.gost.testkit.TestOfferService
import ru.yandex.vertis.general.users.testkit.TestUserService
import common.zio.logging.Logging
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.{assert, DefaultRunnableSpec, ZSpec}
import zio.{Ref, ZIO, ZLayer}
import java.time.Instant

import common.cache.memory.MemoryCache

import scala.concurrent.duration._

object SavedSellerNotificationManagerTest extends DefaultRunnableSpec {

  private def offer(id: String, sellerId: SellerId, createdAt: Instant) = OfferView(
    sellerId = Some(toApiSellerId(sellerId)),
    offerId = id,
    offer = Some(
      Offer(
        title = "TITLE",
        description = "DESCRIPTION",
        price = Some(Price(PriceInCurrency(price_model.PriceInCurrency(100)))),
        category = Some(Category("category_id", 1)),
        attributes = Seq(Attribute("attribute_id", 2, Some(AttributeValue(Number(3))))),
        seller = Some(Seller())
      )
    ),
    status = OfferStatus.ACTIVE,
    createdAt = Some(instantToTimestamp(createdAt)),
    version = 1
  )

  private def emailNotification(ownerId: Long, userId: Long, snippetsCount: Int) = EmailNotification(
    0,
    "",
    Vector(ownerId),
    "template",
    Json.obj(
      "name" -> Json.fromString(s"Юзер $userId"),
      "profile" -> Json.fromString("o.yandex.ru?utm_source=yandex&utm_medium=email&utm_campaign=fav-seller"),
      "snippets" -> Json.arr(
        List.fill(snippetsCount)(
          Json.obj(
            "title" -> Json.fromString("TITLE"),
            "price" -> Json.fromString("100 р."),
            "photo" -> Json.Null,
            "link" -> Json.fromString("o.yandex.ru?utm_source=yandex&utm_medium=email&utm_campaign=fav-seller"),
            "is_new" -> Json.True
          )
        ): _*
      ),
      "unsubscribe_user" -> Json.fromString("o.yandex.ru"),
      "unsubscribe_all" -> Json.fromString("o.yandex.ru")
    )
  )

  private def pushNotification(ownerId: Long, userId: Long) = PushNotification(
    0,
    "",
    title = s"Новые предложения у Юзер $userId",
    body = "Посмотрите объявления пользователя, на которого вы подписаны ❤️",
    link = Some("o.yandex.ru?utm_source=yandex&utm_medium=push&utm_campaign=fav-seller"),
    pushId = Some("classified.saved_sellers"),
    None,
    Vector(ownerId)
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SavedSellerNotificationManagerTest") {
      testM("Добавляет нотификации в очередь отправки") {
        val ownerId1 = OwnerId.UserId(10)
        val ownerId2 = OwnerId.UserId(20)
        val ownerId3 = OwnerId.UserId(30)
        val sellerId1 = SellerId.UserId(1)
        val sellerId2 = SellerId.UserId(2)
        val sellerId3 = SellerId.UserId(3)

        for {
          _ <- TestUserService.addUsers(
            List(
              UserView(1, Some(User(Some("Юзер 1"))), email = Some("1@ya.ru")),
              UserView(2, Some(User(Some("Юзер 2"))), email = Some("2@ya.ru")),
              UserView(3, Some(User(Some("Юзер 3"))), email = Some("3@ya.ru")),
              UserView(10, Some(User(Some("Юзер 10"))), email = Some("10@ya.ru")),
              UserView(20, Some(User(Some("Юзер 20"))), email = Some("20@ya.ru")),
              UserView(30, Some(User(Some("Юзер 30"))), email = Some("30@ya.ru"))
            )
          )
          _ <- SavedSellersManager.createOrUpdateSavedSellers(
            ownerId1,
            List(
              SavedSeller(sellerId1, NotificationSettings(Seq(EMAIL))),
              SavedSeller(sellerId2, NotificationSettings(Seq(PUSH)))
            )
          )
          _ <- SavedSellersManager.createOrUpdateSavedSellers(
            ownerId2,
            List(
              SavedSeller(sellerId1, NotificationSettings(Seq(PUSH))),
              SavedSeller(sellerId2, NotificationSettings(Seq(EMAIL)))
            )
          )
          _ <- SavedSellersManager.createOrUpdateSavedSellers(
            ownerId3,
            List(
              SavedSeller(sellerId1, NotificationSettings(Seq(PUSH, EMAIL)))
            )
          )
          now <- zio.clock.instant
          offer1 = offer("1", sellerId1, now.plusSeconds(1))
          offer2 = offer("2", sellerId1, now.plusSeconds(1)) // must be between last_seen_at and last_sent_at
          offer3 = offer("3", sellerId2, now.plusSeconds(1))
          offer4 = offer("3", sellerId3, now.plusSeconds(1))
          _ <- TestOfferService.setCabinetOffersResponse(_ => ZIO.succeed(CabinetOffersResponse(Seq(offer1, offer2))))
          _ <- SavedSellersNotificationManager.enqueueNotifications(sellerId1, offer1).orDie
          _ <- SavedSellersNotificationManager.enqueueNotifications(sellerId1, offer2).orDie
          _ <- TestOfferService.setCabinetOffersResponse(_ => ZIO.succeed(CabinetOffersResponse(Seq(offer3))))
          _ <- SavedSellersNotificationManager.enqueueNotifications(sellerId2, offer3).orDie
          _ <- SavedSellersNotificationManager.enqueueNotifications(sellerId3, offer4).orDie
          notifications <- runTx(FavoritesNotificationQueueDao.peek(0, 10))
          normalizedNotifications = notifications.map {
            case email: EmailNotification => email.copy(timestamp = 0, notificationId = "")
            case push: PushNotification => push.copy(timestamp = 0, notificationId = "")
          }
        } yield assert(normalizedNotifications)(
          hasSameElements(
            Seq(
              emailNotification(10, 1, 2),
              emailNotification(20, 2, 1),
              emailNotification(30, 1, 2),
              pushNotification(20, 1),
              pushNotification(30, 1),
              pushNotification(10, 2)
            )
          )
        )
      }
    }.provideCustomLayer {
      val ydb = TestYdb.ydb
      val txRunner = ydb >+> Ydb.txRunner
      val clock = Clock.live
      val logging = Logging.live

      val router = RouterClientMock.layer
      val gost = TestOfferService.layer
      val user = TestUserService.layer
      val testBonsaiSnapshot =
        BonsaiSnapshot(Seq(BonsaiCategory(id = "test_category", name = "test category")), Seq.empty)
      val testBonsaiRef = Ref.make(testBonsaiSnapshot).toLayer
      val notificationQueueDao = (ZLayer.succeed(ShardCount(1)) ++ ydb) >>> YdbFavoritesNotificationQueueDao.live
      val favoritesDao = TestYdb.ydb >+> YdbFavoritesDao.live
      val favoritesCountDao = TestYdb.ydb >+> YdbFavoritesCountDao.live
      val savedSellerInvertedDao = TestYdb.ydb >+> YdbSavedSellerInvertedDao.live
      val savedSellerInvertedCountDao = TestYdb.ydb >+> YdbSavedSellerInvertedCountDao.live
      val favoritesStore = (favoritesDao ++ favoritesCountDao) >+> FavoritesStore.live
      val jwt = ZLayer.succeed(HMACConfig("secret")) ++ ZLayer.succeed(Jwt.Config(1.day)) ++ clock >>> Jwt.HMAC256
      val savedSellerManager =
        (favoritesStore ++ txRunner ++ clock ++ savedSellerInvertedDao ++ savedSellerInvertedCountDao ++ jwt) >+> SavedSellersManager.live
      val cache = ZLayer.succeed(MemoryCache.Config(100, Some(1.minute))) >>> MemoryCache.live[SellerId, Unit]

      val savedSellerNotificationManagerConfig =
        ZLayer.succeed(SavedSellersNotificationManager.Config(10.minutes, "o.yandex.ru", "template", "avatars"))
      val savedSellerNotificationManager =
        (savedSellerInvertedDao ++ savedSellerInvertedCountDao ++ notificationQueueDao ++ txRunner ++ router ++ gost ++ user ++ testBonsaiRef ++ clock ++ logging ++ jwt ++ savedSellerNotificationManagerConfig ++ cache) >>> SavedSellersNotificationManager.live
      notificationQueueDao ++ txRunner ++ savedSellerManager ++ savedSellerNotificationManager ++ user ++ gost ++ savedSellerNotificationManagerConfig
    }
  }
}
