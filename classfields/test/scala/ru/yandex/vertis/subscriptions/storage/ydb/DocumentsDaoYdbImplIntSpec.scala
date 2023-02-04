package ru.yandex.vertis.subscriptions.storage.ydb

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import org.scalatest.junit.JUnitRunner
import ru.yandex.common.tokenization.IntTokens
import ru.yandex.vertis.subscriptions.SlowAsyncSpec
import ru.yandex.vertis.subscriptions.model.LegacyGenerators._
import ru.yandex.vertis.subscriptions.storage.ActiveSubscriptionsDaoSpec

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@RunWith(classOf[JUnitRunner])
class DocumentsDaoYdbImplIntSpec
  extends Matchers
  with WordSpecLike
  with BeforeAndAfter
  with ScalaFutures
  with TestYdb
  with SlowAsyncSpec {

  import ydbWrapper.ops._

  private def dao: DocumentsDaoYdbYml = new DocumentsDaoYdbYml(ydbWrapper, zioRuntime)

  override def afterStart(): Unit = {
    super.afterStart()
    zioRuntime.unsafeRun(dao.storage.initSchema())
  }

  before {
    cleanData()
  }

  "DocumentsDao" should {
    "store and load documents" in {
      val originalDocuments = documentGen.next(3)
      Await.result(dao.store(originalDocuments), 10.seconds)

      val loadedDocuments = Await.result(dao.load(originalDocuments.map(_.getId)), 10.seconds)
      loadedDocuments should contain theSameElementsAs originalDocuments
    }
  }

  def cleanData(): Unit =
    zioRuntime.unsafeRun {
      ydbWrapper.runTx {
        ydbWrapper.execute(s"DELETE FROM document").ignoreResult
      }
    }
}
