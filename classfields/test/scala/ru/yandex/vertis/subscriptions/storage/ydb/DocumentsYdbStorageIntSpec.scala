package ru.yandex.vertis.subscriptions.storage.ydb

import java.time.Instant

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.subscriptions.SpecBase
import ru.yandex.vertis.subscriptions.model.LegacyGenerators._
import ru.yandex.vertis.ydb.Ydb
import zio.RIO

@RunWith(classOf[JUnitRunner])
class DocumentsYdbStorageIntSpec extends SpecBase with TestYdb with ProducerProvider {

  def ioTest(action: DocumentsYdbStorage => RIO[YEnv, _]): Unit = {
    val dao = new DocumentsYdbStorage(ydbWrapper)
    zioRuntime.unsafeRun(action(dao))
  }

  override def afterStart(): Unit = {
    super.afterStart()
    ioTest { dao =>
      dao.initSchema()
    }
  }

  "DocumentsYdbStorage" should {
    "return nothing if nothing to return" in ioTest { dao =>
      Ydb.runTx(dao.get(idGen.next(3))).map { res =>
        res shouldBe empty
      }
    }

    "upsert and return it" in ioTest { dao =>
      val documents = documentGen.next(3)
      for {
        _ <- Ydb.runTx(dao.upsert(documents))
        loaded <- Ydb.runTx(dao.get(documents.map(_.getId)))
      } yield loaded should contain theSameElementsAs documents
    }

    "deleteOlderThan" in ioTest { dao =>
      val documents = documentGen.next(3)
      val afterDocumentsCreatedTimestamp = Instant.now().plusMillis(1000)
      for {
        _ <- Ydb.runTx(dao.upsert(documents))
        deleted1 <- Ydb.runTx(dao.deleteOlderThan(afterDocumentsCreatedTimestamp))
        loaded <- Ydb.runTx(dao.get(documents.map(_.getId)))
        deleted2 <- Ydb.runTx(dao.deleteOlderThan(afterDocumentsCreatedTimestamp))
      } yield {
        loaded should be(Seq.empty)
        deleted1 should be >= 3
        deleted2 shouldBe 0
      }

    }
  }

}
