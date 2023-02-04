package ru.yandex.vertis.moderation.hobo.deciders

import com.google.protobuf.StringValue
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.Inside
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.hobo.proto.Common.AutoruProvenOwnerResolution.Value.{Verdict => ResolutionVerdict}
import ru.yandex.vertis.hobo.proto.Model.OwnerInfo
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.deciders.HoboGenerators._
import ru.yandex.vertis.moderation.model.ModerationRequest.UpsertMetadata
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{essentialsGen, instanceGen, stringGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.ExternalId
import ru.yandex.vertis.moderation.model.meta.Metadata
import ru.yandex.vertis.moderation.proto.Model.Metadata.ProvenOwnerMetadata.{Verdict => MetadataVerdict}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.OptionHelper

@RunWith(classOf[JUnitRunner])
class UsersAutoruProvenOwnerMetadataDeciderSpec extends SpecBase with Inside {
  private val decider = new UsersAutoruProvenOwnerMetadataDecider

  "UsersAutoruProvenOwnerDecider.decide" should {
    "happy path" in {
      // GIVEN
      val instance = instanceGen(essentialsGen(Service.AUTORU)).next
      val value1 = AutoruProvenOwnerResolutionValueGen.next.setVerdict(ResolutionVerdict.PROVEN_OWNER_OK)
      val value2 = AutoruProvenOwnerResolutionValueGen.next.setVerdict(ResolutionVerdict.PROVEN_OWNER_FAILED)
      val value3 = AutoruProvenOwnerResolutionValueGen.next.setVerdict(ResolutionVerdict.PROVEN_OWNER_ALREADY_HAS_BADGE)
      val expectedComment = stringGen(5, 10).next
      val expectedLogin = stringGen(5, 10).next
      val ownerInfo = OwnerInfo.newBuilder().setStaffLogin(expectedLogin).build()
      val resolution =
        AutoruProvenOwnerResolutionGen.next
          .clearValues()
          .addValues(value1)
          .addValues(value2)
          .addValues(value3)
          .setComment(StringValue.newBuilder().setValue(expectedComment))
      val hoboTask =
        HoboTaskGen.next
          .setResolution(resolution)
          .setOwnerInfo(ownerInfo)
          .build
      val timestamp = DateTime.now

      // WHEN
      val decision = decider.decide(instance, hoboTask, timestamp, 1)

      // THEN
      decision.requests shouldBe Seq.empty

      decision.offersRequests should have size 3

      inside(decision.offersRequests(0)) {
        case UpsertMetadata(
               ExternalId(user, _),
               Metadata.ProvenOwner(vin, verdict, _, _, _, actualComment, actualLogin),
               `timestamp`,
               1
             ) =>
          user shouldBe instance.externalId.user
          vin shouldBe (value1.hasVin ? value1.getVin.getValue)
          verdict shouldBe MetadataVerdict.PROVEN_OWNER_OK
          actualComment shouldBe Some(expectedComment)
          actualLogin shouldBe Some(expectedLogin)
      }

      inside(decision.offersRequests(1)) {
        case UpsertMetadata(
               ExternalId(user, _),
               Metadata.ProvenOwner(vin, verdict, _, _, _, actualComment, actualLogin),
               `timestamp`,
               1
             ) =>
          user shouldBe instance.externalId.user
          vin shouldBe (value2.hasVin ? value2.getVin.getValue)
          verdict shouldBe MetadataVerdict.PROVEN_OWNER_FAILED
          actualComment shouldBe Some(expectedComment)
          actualLogin shouldBe Some(expectedLogin)
      }

      inside(decision.offersRequests(2)) {
        case UpsertMetadata(
               ExternalId(user, _),
               Metadata.ProvenOwner(vin, verdict, _, _, _, actualComment, actualLogin),
               `timestamp`,
               1
             ) =>
          user shouldBe instance.externalId.user
          vin shouldBe (value3.hasVin ? value3.getVin.getValue)
          verdict shouldBe MetadataVerdict.PROVEN_OWNER_OK
          actualComment shouldBe Some(expectedComment)
          actualLogin shouldBe Some(expectedLogin)
      }
    }
  }
}
