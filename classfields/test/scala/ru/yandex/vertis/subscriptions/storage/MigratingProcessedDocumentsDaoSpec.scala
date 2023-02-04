package ru.yandex.vertis.subscriptions.storage

import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.subscriptions.model.LegacyGenerators
import ru.yandex.vertis.subscriptions.{Mocking, SpecBase, TestExecutionContext}
import ru.yandex.vertis.generators.ProducerProvider._

import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatest.junit.JUnitRunner

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class MigratingProcessedDocumentsDaoSpec extends SpecBase with Mocking with TestExecutionContext with Eventually {

  private trait Test {
    val main = mock[ProcessedDocumentsDao]
    val slave = mock[ProcessedDocumentsDao]

    val dao = new MigratingProcessedDocumentsDao(main, slave, "test")(ec, TestOperationalSupport)
  }

  "markProcessed" should {
    "replicate to slave" in new Test {
      val ids = LegacyGenerators.idGen.next(10)

      (main.markProcessed _).expects(ids).returnsF(())
      (slave.markProcessed _).expects(ids).returnsF(())

      dao.markProcessed(ids).futureValue
      waitEffectsReady()
    }
  }

  "notProcessedFrom" should {
    "filter by slave first" in new Test {
      val ids = LegacyGenerators.idGen.next(10)
      (slave.notProcessedFrom _).expects(ids).returnsF(Nil)

      dao.notProcessedFrom(ids).futureValue shouldBe Nil
      waitEffectsReady()
    }

    "sync diff to slave" in new Test {
      val ids = Seq("1", "2", "3")
      (slave.notProcessedFrom _).expects(ids).returnsF(Seq("2", "3"))
      (main.notProcessedFrom _).expects(Seq("2", "3")).returnsF(Seq("3"))
      (slave.markProcessed _).expects(Seq("2")).returnsF(())

      dao.notProcessedFrom(ids).futureValue shouldBe Seq("3")
      waitEffectsReady()
    }
  }

  private def waitEffectsReady(): Unit = {
    eventually {
      (1 to 10).foreach { _ =>
        ecExecutor.getActiveCount shouldBe 0
        Thread.sleep(10)
      }
    }
  }
}
