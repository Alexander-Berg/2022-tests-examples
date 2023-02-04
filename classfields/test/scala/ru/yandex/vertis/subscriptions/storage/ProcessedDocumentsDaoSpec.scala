package ru.yandex.vertis.subscriptions.storage

import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.util.Success
import scala.concurrent.duration.DurationInt

/**
  * Specs on [[ru.yandex.vertis.subscriptions.storage.ProcessedDocumentsDao]]
  */
trait ProcessedDocumentsDaoSpec extends Matchers with WordSpecLike {

  protected val dao: ProcessedDocumentsDao

  implicit class HandyProcessedDocumentDao(dao: ProcessedDocumentsDao) {
    val timeout = 10.seconds

    def notProcessedFrom(values: String*) =
      Await.result(dao.notProcessedFrom(values.toIterable), timeout)

    def markProcessed(values: String*) =
      Await.result(dao.markProcessed(values.toIterable), timeout)
  }

  "ProcessedDocumentsDao" should {
    "filter processed documents" in {
      dao.notProcessedFrom("foo", "bar", "baz").toSet should be(Set("foo", "bar", "baz"))
      dao.markProcessed("foo", "bar")
      dao.notProcessedFrom("foo", "bar", "baz").toSet should be(Set("baz"))
      dao.notProcessedFrom("foo").toSet should be(Set.empty)
    }
    "idempotemptly mark processed documents" in {
      dao.markProcessed("foo", "bar")
      dao.markProcessed("foo", "bar")
    }
  }
}
