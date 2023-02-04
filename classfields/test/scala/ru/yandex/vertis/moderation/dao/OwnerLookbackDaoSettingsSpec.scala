package ru.yandex.vertis.moderation.dao

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.OffersProcessorParams
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Service

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class OwnerLookbackDaoSettingsSpec extends SpecBase {

  private val settings = new OwnerLookbackDaoSettings(Service.REALTY)

  "OwnerLookbackDaoSettings" should {

    "correctly deserialize OffersProcessorParams" in {
      val ownerJournalRecord = OwnerJournalRecordGen.next
      val externalId1 = ExternalIdGen.next.copy(user = ownerJournalRecord.owner.user)
      val externalId2 = ExternalIdGen.next.copy(user = ownerJournalRecord.owner.user)
      val externalIds = Seq(externalId1, externalId2)
      val params = OffersProcessorParams(externalIds, ownerJournalRecord)
      val serialized = settings.serialize(params)

      val actualResult = settings.deserialize(serialized)

      actualResult shouldBe params
    }
  }
}
