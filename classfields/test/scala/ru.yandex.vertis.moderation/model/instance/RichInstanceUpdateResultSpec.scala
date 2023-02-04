package ru.yandex.vertis.moderation.model.instance

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer

/**
  * Specs for [[RichInstanceUpdateResult]]
  *
  * @author semkagtn
  */
@Ignore // TODO VSMODERATION-3711
@RunWith(classOf[JUnitRunner])
class RichInstanceUpdateResultSpec extends SpecBase {

  "toMap" should {

    "not fail for Touch" in {
      val instance = InstanceGen.next
      val map = InstanceUpdateResult(instance, instance, Diff.empty(instance.service)).toMap
      (map.keySet should contain).allOf("externalId", "record_type")
    }

    "not fail for Snapshot" in {
      val instance = InstanceGen.next
      val map = InstanceUpdateResult(instance, None, Diff.all(instance.service)).toMap
      (map.keySet should contain).allOf("id", "externalId", "record_type", "createTime", "updateTime")
      map.keySet.find(_.startsWith("opinions.")) should not be empty
    }

    "contain id and external id fields for Difference" in {
      val updateJournalResult = InstanceUpdateResultGen.suchThat(i => i.prev.nonEmpty && i.diff.nonEmpty).next
      val map = updateJournalResult.toMap
      (map.keySet should contain).allOf("id", "externalId", "record_type", "old.updateTime", "new.updateTime")
    }
  }
}
