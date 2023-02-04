package ru.yandex.vertis.moderation.hobo.deciders

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.Inside
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.hobo.proto.Model.AutoruResellerCleanNameResolution.Value.{Verdict => ResolutionVerdict}
import ru.yandex.vertis.hobo.proto.Model.OwnerInfo
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.HoboResolutionDecider.Decision
import ru.yandex.vertis.moderation.hobo.ProtobufImplicits._
import ru.yandex.vertis.moderation.hobo.deciders.HoboGenerators.{HoboTaskGen, _}
import ru.yandex.vertis.moderation.model.ModerationRequest.UpsertMetadata
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{essentialsGen, instanceGen, stringGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.meta.Metadata
import ru.yandex.vertis.moderation.proto.Model.Metadata.ResellerCleanNameMetadata.{Verdict => MetaVerdict}
import ru.yandex.vertis.moderation.proto.Model.Service

@RunWith(classOf[JUnitRunner])
class AutoruResellerCleanNameMetadataDeciderSpec extends SpecBase with Inside {
  private val decider = new AutoruResellerCleanNameMetadataDecider

  private val offerId1 = stringGen(5, 8).next
  private val vin1 = stringGen(7, 8).next
  private val value1 =
    AutoruResellerCleanNameResolutionValueGen.next
      .setVerdict(ResolutionVerdict.CLEAN_NAME_PROVEN_OWNER_OK)
      .setOfferId(offerId1)
      .setVin(vin1)
  private val value2 =
    AutoruResellerCleanNameResolutionValueGen.next
      .setVerdict(ResolutionVerdict.CLEAN_NAME_RETURN_QUOTA)
      .setOfferId(offerId1)
      .setVin(vin1)
  private val value3 =
    AutoruResellerCleanNameResolutionValueGen.next
      .setVerdict(ResolutionVerdict.CLEAN_NAME_ADDITIONAL_DOCS)
      .setOfferId(offerId1)
      .clearVin()

  private val instance = instanceGen(essentialsGen(Service.AUTORU)).next

  "AutoruResellerCleanNameMetadataDecider.decide" should {

    "returns empty decision if resolution is empty" in {
      val resolution = AutoruResellerCleanNameResolutionGen.next.clearValues
      val hoboTask = HoboTaskGen.next.setResolution(resolution).build
      val timestamp = DateTime.now

      val decision = decider.decide(instance, hoboTask, timestamp, 1)
      decision shouldBe Decision.Empty
    }

    "accepts empty VIN" in {
      // GIVEN
      val expectedComment = stringGen(5, 10).next
      val expectedLogin = stringGen(5, 10).next
      val resolution =
        AutoruResellerCleanNameResolutionGen.next
          .clearValues()
          .addValues(value3)
          .setComment(expectedComment)

      val ownerInfo = OwnerInfo.newBuilder().setStaffLogin(expectedLogin).build()
      val hoboTask = HoboTaskGen.next.setResolution(resolution).setOwnerInfo(ownerInfo).build
      val timestamp = DateTime.now

      // WHEN
      val decision = decider.decide(instance, hoboTask, timestamp, 1)

      // THEN
      decision.requests should have size 1

      inside(decision.requests.head) {
        case UpsertMetadata(
               externalId,
               Metadata.ResellerCleanName(None, verdicts, `timestamp`, None, actualComment, actualLogin),
               `timestamp`,
               1
             ) =>
          verdicts shouldBe Set(MetaVerdict.CLEAN_NAME_ADDITIONAL_DOCS)
          externalId shouldBe instance.externalId
          actualComment shouldBe Some(expectedComment)
          actualLogin shouldBe Some(expectedLogin)
      }

      decision.offersRequests shouldBe Seq.empty
    }

    "happy path" in {
      // GIVEN
      val expectedComment = stringGen(5, 10).next
      val expectedLogin = stringGen(5, 10).next
      val resolution =
        AutoruResellerCleanNameResolutionGen.next
          .clearValues()
          .addValues(value1)
          .addValues(value2)
          .setComment(expectedComment)
      val ownerInfo = OwnerInfo.newBuilder().setStaffLogin(expectedLogin).build()
      val hoboTask = HoboTaskGen.next.setResolution(resolution).setOwnerInfo(ownerInfo).build
      val timestamp = DateTime.now

      // WHEN
      val decision = decider.decide(instance, hoboTask, timestamp, 1)

      // THEN
      decision.requests should have size 1

      inside(decision.requests.head) {
        case UpsertMetadata(
               externalId,
               Metadata.ResellerCleanName(Some(vin), verdicts, `timestamp`, None, actualComment, actualLogin),
               `timestamp`,
               1
             ) =>
          vin shouldBe vin1
          verdicts shouldBe Set(MetaVerdict.CLEAN_NAME_PROVEN_OWNER_OK, MetaVerdict.CLEAN_NAME_RETURN_QUOTA)
          externalId shouldBe instance.externalId
          actualComment shouldBe Some(expectedComment)
          actualLogin shouldBe Some(expectedLogin)
      }

      decision.offersRequests shouldBe Seq.empty
    }
  }
}
