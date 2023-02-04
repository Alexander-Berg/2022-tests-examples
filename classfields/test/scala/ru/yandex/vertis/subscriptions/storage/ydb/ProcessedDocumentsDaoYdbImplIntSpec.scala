package ru.yandex.vertis.subscriptions.storage.ydb

import ru.yandex.vertis.subscriptions.storage

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProcessedDocumentsDaoYdbImplIntSpec extends storage.ProcessedDocumentsDaoSpec with TestYdb {

  protected val dao = new ProcessedDocumentsDaoYdbImpl(ydbWrapper, zioRuntime, 10, 10)

  override def afterStart(): Unit = {
    super.afterStart()
    zioRuntime.unsafeRun(dao.storage.initSchema())
  }
}
