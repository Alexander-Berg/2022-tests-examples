package ru.yandex.vertis.general.wisp.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.wisp.model.Chat
import ru.yandex.vertis.general.wisp.storage.ChatDao
import ru.yandex.vertis.general.wisp.storage.ydb.YdbChatDao
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.{assert, assertCompletes, assertTrue, suite, testM, DefaultRunnableSpec, ZSpec}

import java.time.Instant

object YdbChatDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YdbChatDao")(
      testM("Create chat and get it by buyerRoomId") {
        for {
          chat <- ZIO.succeed(Chat("b", "o", "s", "br", Some("sr"), 1L))
          _ <- runTx(ChatDao.createChat(chat))
          created <- runTx(ChatDao.getChatByRoomId("br"))
        } yield assert(created)(isSome(equalTo(chat)))
      },
      testM("Create chat and get it by sellerRoomId") {
        for {
          chat <- ZIO.succeed(Chat("b1", "o1", "s1", "br1", Some("sr1"), 1L))
          _ <- runTx(ChatDao.createChat(chat))
          created <- runTx(ChatDao.getChatByRoomId("sr1"))
        } yield assert(created)(isSome(equalTo(chat)))
      },
      testM("Create chat and get it by offerId and buyerId") {
        for {
          chat <- ZIO.succeed(Chat("b2", "o2", "s2", "br2", Some("sr2"), 1L))
          _ <- runTx(ChatDao.createChat(chat))
          created <- runTx(ChatDao.getChat("b2", "o2"))
        } yield assert(created)(isSome(equalTo(chat)))
      },
      testM("Set sellerRoomId after creation") {
        for {
          chat <- ZIO.succeed(Chat("b3", "o3", "s3", "br3", None, 1L))
          _ <- runTx(ChatDao.createChat(chat))
          created <- runTx(ChatDao.getChat("b3", "o3"))
          _ <- runTx(ChatDao.setSellerRoom(chat.buyerPuid, chat.offerId, "sr3", Instant.ofEpochMilli(100L)))
          afterUpdate <- runTx(ChatDao.getChat("b3", "o3"))
        } yield assert(created)(isSome(equalTo(chat))) && assert(afterUpdate.flatMap(_.sellerRoomId))(
          isSome(equalTo("sr3"))
        )
      },
      testM("Return user chats") {
        for {
          sellerChat <- ZIO.succeed(Chat("b5", "o5", "user_id", "br5", None, 1L))
          buyerChat1 <- ZIO.succeed(Chat("user_id", "o6", "s6", "br6", Some("sr6"), 1L))
          buyerChat2 <- ZIO.succeed(Chat("user_id", "o7", "s7", "br7", None, 1L))
          _ <- runTx(ChatDao.createChat(sellerChat))
          _ <- runTx(ChatDao.createChat(buyerChat1))
          _ <- runTx(ChatDao.createChat(buyerChat2))
          chats <- runTx(ChatDao.getUserChats("user_id", 0, 2))
        } yield assert(chats)(hasSize(equalTo(2)))
      },
      testM("Count user chats") {
        for {
          sellerChat <- ZIO.succeed(Chat("b8", "o8", "supa_user_id", "br8", None, 1L))
          buyerChat <- ZIO.succeed(Chat("supa_user_id", "o9", "s9", "br9", Some("sr9"), 1L))
          _ <- runTx(ChatDao.createChat(sellerChat))
          _ <- runTx(ChatDao.createChat(buyerChat))
          chatsCount <- runTx(ChatDao.getUserChatsCount("supa_user_id"))
        } yield assert(chatsCount)(equalTo(2))
      },
      testM("Update offerHash") {
        for {
          chat <- ZIO.succeed(Chat("b_offerHash", "o_offerHash", "s_offerHash", "br_offerHash", None, 66L))
          _ <- runTx(ChatDao.createChat(chat))
          newHash = 97L
          _ <- runTx(ChatDao.updateOfferHash("b_offerHash", "o_offerHash", newHash))
          updated <- runTx(ChatDao.getChat("b_offerHash", "o_offerHash"))
        } yield assert(updated.map(_.offerHash))(isSome(equalTo(newHash)))
      },
      testM("Get obsolete chats") {
        for {
          goodChat <- ZIO.succeed(Chat("b10", "o10", "s10", "br10", None, 77L))
          badChat <- ZIO.succeed(Chat("b11", "o11", "s11", "br11", None, 0L))
          _ <- runTx(ChatDao.createChat(goodChat))
          _ <- runTx(ChatDao.createChat(badChat))
          obsolete <- runTx(ChatDao.getObsoleteChats("o11", 77L, 0, 10))
        } yield assert(obsolete)(hasSize(equalTo(1))) && assert(obsolete.headOption)(isSome(equalTo(badChat)))
      },
      testM("Create chats and get them by offerId") {
        for {
          chat1 <- ZIO.succeed(Chat("b11", "o11", "s11", "br11", Some("sr11"), 1L))
          chat2 <- ZIO.succeed(Chat("b12", "o11", "s11", "br12", Some("sr12"), 1L))
          ignored_chat <- ZIO.succeed(Chat("b13", "o13", "s13", "br13", Some("sr13"), 1L))
          _ <- runTx(ChatDao.createChat(chat1))
          _ <- runTx(ChatDao.createChat(chat2))
          _ <- runTx(ChatDao.createChat(ignored_chat))
          chats <- runTx(ChatDao.getChatsByOfferId("o11", 0, 3))
        } yield assertTrue(chats.size == 2)
      },
      testM("Count offer chats") {
        for {
          chat1 <- ZIO.succeed(Chat("b11", "schitay_menya_polnostu", "s11", "br11", Some("sr11"), 1L))
          chat2 <- ZIO.succeed(Chat("b12", "schitay_menya_polnostu", "s11", "br12", Some("sr12"), 1L))
          chat3 <- ZIO.succeed(Chat("b13", "schitay_menya_polnostu", "s11", "br13", Some("sr13"), 1L))
          _ <- runTx(ChatDao.createChat(chat1))
          _ <- runTx(ChatDao.createChat(chat2))
          _ <- runTx(ChatDao.createChat(chat3))
          chatsCount <- runTx(ChatDao.getOfferChatsCount("schitay_menya_polnostu"))
        } yield assert(chatsCount)(equalTo(3))
      }
    )
      .provideCustomLayer(
        TestYdb.ydb ++ Clock.live >>> (YdbChatDao.live ++ Ydb.txRunner) ++ Clock.live
      )
}
