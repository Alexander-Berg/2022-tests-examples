package ru.yandex.vertis.moderation.proven.owner.external

import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.AutoruEssentials
import ru.yandex.vertis.moderation.model.meta.{Metadata, MetadataSet}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.DateTimeUtil

class ExternalProvenOwnerVerdictDeciderSpec extends SpecBase {

  private val hourAgo = DateTimeUtil.now().minusHours(1)
  private val dayAgo = DateTimeUtil.now().minusDays(1)
  private val now = DateTimeUtil.now()
  private val verdict = ExternalProvenOwnerVerdictGen.next.copy(updateTime = hourAgo)
  private val basicInstance = instanceGen(essentialsGen(Service.AUTORU)).next
  private val essentials: AutoruEssentials = AutoruEssentialsGen.next
  private val decider = new ExternalProvenOwnerVerdictDeciderImpl

  "ProvenOwnerDeciderImpl" should {
    "decide to update metadata if current proven owner meta is stale" in {
      val provenOwnerMetadata: Metadata.ProvenOwner = ProvenOwnerMetadataGen.next.copy(timestamp = dayAgo)
      val instance =
        basicInstance.copy(
          essentials =
            essentials.copy(
              externalProvenOwnerVerdict = Some(verdict)
            ),
          metadata = MetadataSet(provenOwnerMetadata)
        )
      val record = UpdateJournalRecordGen.next.copy(instance = instance)
      val res = decider.decide(record)
      res.nonEmpty shouldBe true
      res.get.metadata.timestamp shouldBe hourAgo
    }

    "decide not to update metadata" in {
      val provenOwnerMetadata = ProvenOwnerMetadataGen.next.copy(timestamp = now)
      val instance =
        basicInstance.copy(
          essentials =
            essentials.copy(
              externalProvenOwnerVerdict = Some(verdict)
            ),
          metadata = MetadataSet(provenOwnerMetadata)
        )
      val record = UpdateJournalRecordGen.next.copy(instance = instance)
      decider.decide(record) shouldBe None
    }

    "decide to update metadata if current proven owner meta is absent" in {
      val instance =
        basicInstance.copy(
          essentials =
            essentials.copy(
              externalProvenOwnerVerdict = Some(verdict)
            ),
          metadata = MetadataSet()
        )
      val record = UpdateJournalRecordGen.next.copy(instance = instance)
      val res = decider.decide(record)
      res.nonEmpty shouldBe true
      res.get.metadata.timestamp shouldBe hourAgo
    }
  }
}
