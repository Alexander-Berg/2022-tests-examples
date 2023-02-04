package ru.yandex.vertis.subscriptions.storage.ydb

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.subscriptions.SpecBase
import ru.yandex.vertis.subscriptions.storage.{ActiveSubscription, Generators}
import ru.yandex.vertis.ydb.Ydb
import zio.{RIO, ZIO}

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class ActiveSubscriptionsYdbStorageIntSpec extends SpecBase with TestYdb with ProducerProvider {

  def ioTest(action: ActiveSubscriptionsYdbStorage => RIO[YEnv, _]): Unit = {
    val dao = new ActiveSubscriptionsYdbStorage(ydbWrapper, new IntTokens(16))(ActiveSubscription.LightWeightFormat)
    zioRuntime.unsafeRun(action(dao))
  }

  override def afterStart(): Unit = {
    super.afterStart()
    ioTest { dao =>
      dao.initSchema()
    }
  }

  "upsert" should {
    "insert new entries" in ioTest { dao =>
      val subs = Generators.activeSubscriptionGen.next(5)

      for {
        _ <- Ydb.runTx(dao.upsert(subs))
        loaded <- Ydb.runTx(ZIO.foreach(subs.map(_.key))(dao.get)).map(_.flatten)
      } yield loaded should contain theSameElementsAs subs
    }

    "update some entries" in ioTest { dao =>
      val subs = Generators.activeSubscriptionGen.next(5).toIndexedSeq
      val subsUpdates = subs.take(4) :+ subs(4).copy(timestamp = System.currentTimeMillis() + 10)
      for {
        _ <- Ydb.runTx(dao.upsert(subs))
        _ <- Ydb.runTx(dao.upsert(subsUpdates))
        loaded <- Ydb.runTx(ZIO.foreach(subs.map(_.key))(dao.get)).map(_.flatten)
      } yield loaded should contain theSameElementsAs subsUpdates
    }
  }

  "delete" should {
    "delete entry" in ioTest { dao =>
      val sub = Generators.activeSubscriptionGen.next
      for {
        _ <- Ydb.runTx(dao.upsert(Seq(sub)))
        loaded1 <- Ydb.runTx(dao.get(sub.key))
        _ <- Ydb.runTx(dao.delete(sub.key))
        loaded2 <- Ydb.runTx(dao.get(sub.key))
      } yield {
        loaded1 shouldBe Some(sub)
        loaded2 shouldBe None
      }
    }

    "not fail on non-existent" in ioTest { dao =>
      val sub = Generators.activeSubscriptionGen.next
      Ydb.runTx(dao.delete(sub.key))
    }
  }

  "listWithToken" should {
    "work" in ioTest { dao =>
      val subs = Generators.activeSubscriptionGen.suchThat(v => dao.getToken(v.key) == 1).next(10)
      for {
        _ <- Ydb.runTx(dao.upsert(subs))
        loaded <- dao.listWithToken(1).runCollect
      } yield loaded should contain allElementsOf loaded
    }
  }

}
