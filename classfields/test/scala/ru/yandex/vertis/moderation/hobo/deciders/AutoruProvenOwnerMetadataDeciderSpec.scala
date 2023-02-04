package ru.yandex.vertis.moderation.hobo.deciders

import com.google.protobuf.StringValue
import org.joda.time.DateTime
import org.scalatest.Inside
import ru.yandex.vertis.hobo.proto.Common.AutoruProvenOwnerResolution.Value.{Verdict => ResolutionVerdict}
import ru.yandex.vertis.hobo.proto.Model.OwnerInfo
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.HoboResolutionDecider.Decision
import ru.yandex.vertis.moderation.hobo.deciders.HoboGenerators._
import ru.yandex.vertis.moderation.model.ModerationRequest.UpsertMetadata
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  essentialsGen,
  instanceGen,
  stringGen,
  ProvenOwnerMetadataGen
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.meta.{Metadata, MetadataSet}
import ru.yandex.vertis.moderation.proto.Model.Metadata.ProvenOwnerMetadata.{Verdict => MetadataVerdict}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.OptionHelper

class AutoruProvenOwnerMetadataDeciderSpec extends SpecBase with Inside {
  private val decider = new AutoruProvenOwnerMetadataDecider

  "AutoruProvenOwnerDecider.decide" should {
    "happy path" in {
      // GIVEN
      val expectedLogin = stringGen(5, 10).next
      val expectedComment = stringGen(5, 10).next
      val instance = instanceGen(essentialsGen(Service.AUTORU)).next
      val value = AutoruProvenOwnerResolutionValueGen.next.setVerdict(ResolutionVerdict.PROVEN_OWNER_OK)
      val resolution =
        AutoruProvenOwnerResolutionGen.next
          .clearValues()
          .setComment(StringValue.newBuilder().setValue(expectedComment))
          .addValues(value)
      val ownerInfo = OwnerInfo.newBuilder().setStaffLogin(expectedLogin)
      val hoboTask =
        HoboTaskGen.next
          .setResolution(resolution)
          .setOwnerInfo(ownerInfo)
          .build
      val timestamp = DateTime.now

      // WHEN
      val decision = decider.decide(instance, hoboTask, timestamp, 1)

      // THEN
      decision.offersRequests shouldBe Seq.empty

      decision.requests should have size 1

      inside(decision.requests.head) {
        case UpsertMetadata(
               externalId,
               Metadata.ProvenOwner(vin, verdict, _, _, _, actualComment, actualLogin),
               `timestamp`,
               1
             ) =>
          externalId shouldBe instance.externalId
          vin shouldBe value.getVin.getValue
          verdict shouldBe MetadataVerdict.PROVEN_OWNER_OK
          actualComment shouldBe Some(expectedComment)
          actualLogin shouldBe Some(expectedLogin)

      }
    }

    "consider existing metadata update time" in {
      val value = AutoruProvenOwnerResolutionValueGen.next.setVerdict(ResolutionVerdict.PROVEN_OWNER_OK)
      val resolution = AutoruProvenOwnerResolutionGen.next.clearValues().addValues(value)
      val timestamp = DateTime.now.minusHours(1)
      val hoboTask = HoboTaskGen.next.setResolution(resolution).setFinishTime(timestamp.getMillis).build
      val provenOwnerMetadata = ProvenOwnerMetadataGen.next.copy(timestamp = DateTime.now)
      val instance =
        instanceGen(essentialsGen(Service.AUTORU)).next
          .copy(metadata = MetadataSet(provenOwnerMetadata))
      val decision = decider.decide(instance, hoboTask, timestamp, 1)

      decision shouldBe Decision.Empty
    }

    "consider absence of metadata" in {
      val value = AutoruProvenOwnerResolutionValueGen.next.setVerdict(ResolutionVerdict.PROVEN_OWNER_OK)
      val timestamp = DateTime.now.minusHours(1)
      val resolution = AutoruProvenOwnerResolutionGen.next.clearValues().addValues(value)
      val hoboTask = HoboTaskGen.next.setResolution(resolution).setFinishTime(timestamp.getMillis).build
      val instance =
        instanceGen(essentialsGen(Service.AUTORU)).next
          .copy(metadata = MetadataSet())
      val decision = decider.decide(instance, hoboTask, timestamp, 1)

      decision.requests.size shouldBe 1
    }

  }
}
