package ru.yandex.vertis.subscriptions.storage.ydb

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.subscriptions.SpecBase
import ru.yandex.vertis.ydb.Ydb
import zio.RIO

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class ProcessedDocumentsYdbStorageIntSpec extends SpecBase with TestYdb with ProducerProvider {

  def ioTest(action: (ProcessedDocumentsYdbStorage) => RIO[YEnv, _]): Unit = {
    val dao = new ProcessedDocumentsYdbStorage(ydbWrapper)
    zioRuntime.unsafeRun(action(dao))
  }

  override def afterStart(): Unit = {
    super.afterStart()
    ioTest { dao =>
      dao.initSchema()
    }
  }

  "ProcessedDocumentsYdbStorage" should {
    "return nothing if nothing to return" in ioTest { dao =>
      Ydb.runTx(dao.getProcessed(Seq("1", "2", "3"))).map { res =>
        res shouldBe empty
      }
    }

    "upsert and return it" in ioTest { dao =>
      val ids = Seq("a", "b", "c")
      for {
        _ <- Ydb.runTx(dao.upsert(ids))
        loaded <- Ydb.runTx(dao.getProcessed(ids ++ Seq("some", "else")))
      } yield loaded should contain theSameElementsAs ids
    }

  }

}
