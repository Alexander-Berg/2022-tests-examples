package ru.yandex.vertis.general.wisp.consumers

import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.{HasTxRunner, Ydb}
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.common.price_model
import general.common.price_model.Price
import general.common.price_model.Price.Price.PriceInCurrency
import general.common.seller_model.SellerId
import general.common.seller_model.SellerId.SellerId.UserId
import general.gost.offer_model.AttributeValue.Value.Number
import general.gost.offer_model.OfferStatusEnum.OfferStatus
import general.gost.offer_model._
import general.gost.seller_model.Seller
import ru.yandex.vertis.general.wisp.clients.messenger_meta_api.MessengerMetaApiClient._
import ru.yandex.vertis.general.wisp.clients.testkit.TestMessengerMetaApiClient
import ru.yandex.vertis.general.wisp.consumers.OfferUpdatesHandler.OfferUpdatesHandler
import ru.yandex.vertis.general.wisp.logic.OfferInfoHashUtil
import ru.yandex.vertis.general.wisp.model.Chat
import ru.yandex.vertis.general.wisp.storage.ChatDao
import ru.yandex.vertis.general.wisp.storage.ChatDao.ChatDao
import ru.yandex.vertis.general.wisp.storage.ydb.YdbChatDao
import common.zio.logging.Logging
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation.{toLayer, value}
import zio.{ULayer, URLayer, ZIO, ZLayer}

object OfferUpdatesHandlerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("OfferUpdatesHandler")(
      testM("Update obsolete chat")(
        for {
          chat <- ZIO.succeed(Chat("buyerId1", "offerId1", "SellerPuid", "br1", Some("sr1"), 0L))
          _ <- runTx(ChatDao.createChat(chat))
          offerView = getValidOfferView("offerId1", "title")
          _ <- OfferUpdatesHandler.handleOfferUpdate(offerView)
          afterUpdate <- runTx(ChatDao.getChat("buyerId1", "offerId1"))
        } yield assert(afterUpdate.map(_.offerHash))(isSome(equalTo(OfferInfoHashUtil.getOfferHash(offerView))))
      ).provideLayer {
        val messengerMetaApi =
          TestMessengerMetaApiClient
            .UpdateChat(
              hasField[UpdateChatParams, String]("id", _.chatId, equalTo("br1")),
              value(())
            )
            .andThen(
              TestMessengerMetaApiClient.UpdateChat(
                hasField[UpdateChatParams, String]("id", _.chatId, equalTo("sr1")),
                value(())
              )
            )
        buildLayer(messengerMetaApi)
      },
      testM("Update nothing if offer doesn't have chats") {
        for {
          _ <- OfferUpdatesHandler.handleOfferUpdate(getValidOfferView("offerId2", "Title"))
        } yield assertCompletes
      }.provideLayer {
        val messengerMetaApi = TestMessengerMetaApiClient.empty
        buildLayer(messengerMetaApi)
      },
      testM("Update nothing if all is up to date") {
        val offerView = getValidOfferView("offerId3", "TUITL")
        val offerHash = OfferInfoHashUtil.getOfferHash(offerView)
        val chat = Chat("buyerId3", "offerId3", "sellerId3", "br3", Some("sr3"), offerHash)
        for {
          _ <- runTx(ChatDao.createChat(chat))
          _ <- OfferUpdatesHandler.handleOfferUpdate(offerView)
        } yield assertCompletes
      }.provideLayer {
        val messengerMetaApi = TestMessengerMetaApiClient.empty
        buildLayer(messengerMetaApi)
      }
    ).provideCustomLayer {
      TestYdb.ydb
    }

  private def getValidOfferView(offerId: String, title: String) = OfferView(
    sellerId = Some(SellerId(UserId(123))),
    offerId = offerId,
    offer = Some(
      Offer(
        title = title,
        description = "DESCRIPTION",
        price = Some(Price(PriceInCurrency(price_model.PriceInCurrency(100)))),
        category = Some(Category("category_id", 1)),
        attributes = Seq(Attribute("attribute_id", 2, Some(AttributeValue(Number(3))))),
        seller = Some(Seller())
      )
    ),
    status = OfferStatus.ACTIVE,
    version = 1
  )

  private def buildLayer(
      messengerMetaApi: ULayer[
        MessengerMetaApiClient
      ]): URLayer[Ydb with Clock with Blocking, OfferUpdatesHandler with ChatDao with HasTxRunner with Clock with Blocking] = {
    val blocking = ZLayer.requires[Blocking]
    val ydb = ZLayer.requires[Ydb]
    val txRunner = ydb >>> Ydb.txRunner
    val clock = ZLayer.requires[Clock]
    val chatDao = ydb ++ clock >>> YdbChatDao.live
    val log = Logging.live

    val offerUpdatesHandler = log ++ messengerMetaApi ++ chatDao ++ clock ++ txRunner >>> OfferUpdatesHandler.live

    offerUpdatesHandler ++ chatDao ++ txRunner ++ clock ++ blocking
  }
}
