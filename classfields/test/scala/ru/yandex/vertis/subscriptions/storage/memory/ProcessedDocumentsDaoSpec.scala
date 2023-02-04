package ru.yandex.vertis.subscriptions.storage.memory

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.storage

/**
  * Tests in-memory [[ru.yandex.vertis.subscriptions.storage.ProcessedDocumentsDao]] implementation.
  */
@RunWith(classOf[JUnitRunner])
class ProcessedDocumentsDaoSpec extends storage.ProcessedDocumentsDaoSpec {

  protected val dao = new ProcessedDocumentsDao
}
