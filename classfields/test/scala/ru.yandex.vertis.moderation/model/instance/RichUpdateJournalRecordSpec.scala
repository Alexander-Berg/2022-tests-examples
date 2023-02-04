package ru.yandex.vertis.moderation.model.instance

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer

/**
  * Specs for [[RichUpdateJournalRecord]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class RichUpdateJournalRecordSpec extends SpecBase {

  "toEvent" should {
    "generate equal events for similar data" in {
      val record = UpdateJournalRecordGen.next
      record.toEvent(Environments.Local) should be(record.toEvent(Environments.Local))
      record.toEvent(Environments.Development) should be(record.toEvent(Environments.Development))
      record.toEvent(Environments.Testing) should be(record.toEvent(Environments.Testing))
      record.toEvent(Environments.Stable) should be(record.toEvent(Environments.Stable))
    }
    "generate different ids for non-equal externalIds" in {
      val record = UpdateJournalRecordGen.next
      val firstEvent = record.toEvent(Environments.Testing)
      val secondEvent =
        record.copy(instance = record.instance.copy(id = InstanceIdGen.next)).toEvent(Environments.Testing)
      firstEvent.getId should not be secondEvent.getId
    }
    "generate different ids for non-equal env" in {
      val record = UpdateJournalRecordGen.next
      val firstEvent = record.toEvent(Environments.Development)
      val secondEvent = record.toEvent(Environments.Stable)
      firstEvent.getId should not be secondEvent.getId
    }
    "generate different ids for non-equal timestamp" in {
      val record = UpdateJournalRecordGen.next
      val firstEvent = record.toEvent(Environments.Testing)
      val secondEvent =
        record
          .copy(timestamp = record.timestamp.minus(1))
          .toEvent(Environments.Testing)
      firstEvent.getId should not be secondEvent.getId
    }
  }
}
