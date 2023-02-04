package ru.yandex.vertis.general.users.storage.testkit

import common.zio.ydb.Ydb.HasTxRunner
import ru.yandex.vertis.general.users.model.{StoreInfo, User}
import ru.yandex.vertis.general.users.model.testkit.UserGen
import ru.yandex.vertis.general.users.storage.UserDao
import ru.yandex.vertis.general.users.storage.UserDao.UserDao
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test._

import java.time.Instant

object UserDaoSpec {

  def spec(
      label: String): Spec[UserDao with Clock with HasTxRunner with Random with Sized with TestConfig, TestFailure[Throwable], TestSuccess] = {
    suite(label)(
      testM("create user") {
        checkNM(1)(UserGen.anyUser, UserGen.anyUserId, UserGen.anyAddress()) { (userInput, userId, address) =>
          val storeInfo = StoreInfo(Some("what"), Some("We dont work"), None, None)
          val user = User.fromUserInput(userInput).copy(storeInfo = Some(storeInfo))
          for {
            _ <- runTx(UserDao.createOrUpdateUser(userId, user))
            saved <- runTx(UserDao.getUser(userId))
          } yield assert(saved)(isSome(equalTo(user)))
        }
      },
      testM("create user list") {
        checkNM(1)(UserGen.anyUser, UserGen.anyUser, UserGen.anyUserId, UserGen.anyUserId) {
          (userInput1, userInput2, userId1, userId2) =>
            val user1 = User.fromUserInput(userInput1)
            val user2 = User.fromUserInput(userInput2)
            val userMap = Map(userId1 -> user1, userId2 -> user2)
            for {
              _ <- runTx(UserDao.createOrUpdateUserList(userMap))
              saved <- runTx(UserDao.getUserList(List(userId1, userId2)))
            } yield assert(saved)(equalTo(userMap))
        }
      },
      testM("return None if user does not exist") {
        checkNM(1)(UserGen.anyUserId) { userId =>
          for {
            saved <- runTx(UserDao.getUser(userId))
          } yield assert(saved)(isNone)
        }
      },
      testM("get multiple users") {
        checkNM(1)(UserGen.anyUser, UserGen.anyUser, UserGen.anyUserId, UserGen.anyUserId) {
          (userInput1, userInput2, userId1, userId2) =>
            val user1 = User.fromUserInput(userInput1)
            val user2 = User.fromUserInput(userInput2)
            for {
              _ <- runTx(UserDao.createOrUpdateUser(userId1, user1))
              _ <- runTx(UserDao.createOrUpdateUser(userId2, user2))
              saved <- runTx(UserDao.getUserList(List(userId1, userId2)))
            } yield assert(saved)(equalTo(Map(userId1 -> user1, userId2 -> user2)))
        }
      },
      testM("stream user ids") {
        checkNM(1)(UserGen.anyUser, UserGen.anyUser, UserGen.anyUserId, UserGen.anyUserId) {
          (userInput1, userInput2, userId1, userId2) =>
            val user1 = User.fromUserInput(userInput1)
            val user2 = User.fromUserInput(userInput2)
            for {
              _ <- runTx(UserDao.createOrUpdateUser(userId1, user1))
              _ <- runTx(UserDao.createOrUpdateUser(userId2, user2))
              (smallerId, biggerId) = if (userId1.id < userId2.id) (userId1, userId2) else (userId2, userId1)
              allIds <- UserDao.streamUserIds(None).runCollect
              idsFromCursor <- UserDao.streamUserIds(Some(smallerId)).runCollect
            } yield assert(allIds)(hasSubset(List(userId1, userId2))) &&
              assert(idsFromCursor)(contains(biggerId)) &&
              assert(idsFromCursor)(not(contains(smallerId)))
        }
      }
    )
  }
}
