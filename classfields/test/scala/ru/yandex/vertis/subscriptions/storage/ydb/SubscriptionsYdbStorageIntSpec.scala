package ru.yandex.vertis.subscriptions.storage.ydb

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.subscriptions.SpecBase
import ru.yandex.vertis.subscriptions.model.UserKey
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators
import ru.yandex.vertis.ydb.Ydb
import ru.yandex.vertis.ydb.zio.YdbZioWrapper
import zio.{RIO, ZIO}

import scala.concurrent.duration.DurationInt

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class SubscriptionsYdbStorageIntSpec extends SpecBase with TestYdb with ProducerProvider {

  private val tokens = new IntTokens(16)

  def ioTest(action: SubscriptionsYdbStorage => RIO[YEnv, _]): Unit = {
    val ydb = YdbZioWrapper.make(container.tableClient, "/local", 30.seconds)
    val dao = new SubscriptionsYdbStorage(ydb, tokens)
    zioRuntime.unsafeRun(action(dao))
  }

  override def beforeStop(): Unit = {
    container.tableClient.close()
    container.rpc.close()
    super.beforeStop()
  }

  "initSchema" should {
    "init" in ioTest { dao =>
      dao.initSchema()
    }
  }

  "create" should {

    "work" in ioTest { dao =>
      val sub = CoreGenerators.subscriptions.next
      for {
        _ <- Ydb.runTx(dao.create(sub))
        loaded <- Ydb.runTx(dao.get(sub.getUser, sub.getId))
      } yield loaded shouldBe sub
    }

    "rewrite existing subscription with the same name" in ioTest { dao =>
      val sub = CoreGenerators.subscriptions.next
      val sub2 =
        CoreGenerators.subscriptions.next.toBuilder
          .setUser(sub.getUser)
          .setId(sub.getId)
          .build()

      for {
        _ <- Ydb.runTx(dao.create(sub))
        loaded <- Ydb.runTx(dao.get(sub.getUser, sub.getId))
        _ = loaded shouldBe sub
        _ <- Ydb.runTx(dao.create(sub2))
        loaded2 <- Ydb.runTx(dao.get(sub.getUser, sub.getId))
      } yield loaded2 shouldBe sub2
    }
  }

  "get" should {
    "fail on non-existent subscription" in ioTest { dao =>
      val user = CoreGenerators.userGen.next
      Ydb.runTx(dao.get(user, "123")).either.map { el =>
        el.isLeft shouldBe true
        el.left.get shouldBe a[NoSuchElementException]
      }
    }
  }

  "delete" should {
    "remove subscription" in ioTest { dao =>
      val sub = CoreGenerators.subscriptions.next
      val test = for {
        _ <- Ydb.runTx(dao.create(sub))
        _ <- Ydb.runTx(dao.get(sub.getUser, sub.getId))
        _ <- Ydb.runTx(dao.delete(sub.getUser, sub.getId))
        loaded <- Ydb.runTx(dao.get(sub.getUser, sub.getId))
      } yield loaded
      test.either.map(_.isLeft shouldBe true)
    }

    "not fail on non-existent subscription" in ioTest { dao =>
      val user = CoreGenerators.userGen.next
      Ydb.runTx(dao.delete(user, "123"))
    }
  }

  "list" should {
    "return all user's subscriptions" in ioTest { dao =>
      val user = CoreGenerators.userGen.next
      val subs = CoreGenerators.subscriptions.next(15).map(_.toBuilder.setUser(user).build())

      for {
        _ <- Ydb.runTx(
          ZIO.collectAll(subs.map(dao.create))
        )
        loaded <- Ydb.runTx(dao.listByUser(user))
      } yield loaded should contain theSameElementsAs subs
    }

    "return empty for unknown user" in ioTest { dao =>
      val user = CoreGenerators.userGen.next
      for {
        loaded <- Ydb.runTx(dao.listByUser(user))
      } yield loaded shouldBe empty
    }

    //todo
    /* "support paging" in ioTest { (ydb, dao) =>
      val limit = 4
      val user = CoreGenerators.userGen.next
      val subs = CoreGenerators.subscriptions.next(10).map(_.toBuilder.setUser(user).build())
      val groups = subs.toSeq.sortBy(_.getId).grouped(limit).toIndexedSeq

      for {
        _ <- ydb.runTx(
          ZIO.sequence(subs.map(dao.create))
        )
        loaded1 <- ydb.runTx(dao.listByUser(user, limit = limit))
        loaded2 <- ydb.runTx(dao.listByUser(user, limit = limit, afterId = Some(loaded1.last.getId)))
        loaded3 <- ydb.runTx(dao.listByUser(user, limit = limit, afterId = Some(loaded2.last.getId)))
      } yield {
        loaded1.toSeq should contain theSameElementsInOrderAs groups(0)
        loaded2.toSeq should contain theSameElementsInOrderAs groups(1)
        loaded3.toSeq should contain theSameElementsInOrderAs groups(2)
      }
    }*/
  }

  "setState" should {
    "update state" in ioTest { dao =>
      val sub = CoreGenerators.emailSubscriptions.next
      val newState = CoreGenerators.stateGen.next
      for {
        _ <- Ydb.runTx(dao.create(sub))
        _ <- Ydb.runTx(dao.setState(sub.getUser, sub.getId, newState))
        loaded <- Ydb.runTx(dao.get(sub.getUser, sub.getId))
      } yield {
        loaded.getState shouldBe newState
        loaded shouldBe sub.toBuilder.setState(newState).build()
      }
    }
  }

  "all" should {
    "return all subscriptions" in ioTest { dao =>
      val someSubs = CoreGenerators.subscriptions.next(10)
      for {
        _ <- Ydb.runTx(
          ZIO.collectAll(someSubs.map(dao.create))
        )
        loaded <- dao.all().runCollect
      } yield loaded should contain allElementsOf someSubs
    }
  }

  "listByToken" should {
    "work" in ioTest { dao =>
      val someSubs = CoreGenerators.subscriptions.next(20)
      val checks = someSubs.groupBy(s => dao.getToken(UserKey(s.getUser))).map {
        case (token, subs) =>
          dao.listByToken(token).runCollect.map { loaded =>
            loaded should contain allElementsOf subs
          }
      }

      for {
        _ <- Ydb.runTx(
          ZIO.collectAll(someSubs.map(dao.create))
        )
        _ <- ZIO.collectAll(checks)
      } yield ()
    }
  }

  "getOwner" should {
    "return owner of subscription" in ioTest { dao =>
      val sub = CoreGenerators.emailSubscriptions.next
      for {
        _ <- Ydb.runTx(dao.create(sub))
        owner <- Ydb.runTx(dao.getOwner(sub.getId))
      } yield owner shouldBe sub.getUser
    }
  }

}
