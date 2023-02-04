package ru.yandex.vertis.moderation.hobo.deciders

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.Inside
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.hobo.proto.Model.AutoruResellerCleanNameResolution.Value.{Verdict => ResolutionVerdict}
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.HoboResolutionDecider.Decision
import ru.yandex.vertis.moderation.hobo.ProtobufImplicits._
import ru.yandex.vertis.moderation.hobo.deciders.HoboGenerators.{HoboTaskGen, _}
import ru.yandex.vertis.moderation.model.ModerationRequest.UpsertMetadata
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{essentialsGen, instanceGen, stringGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.ExternalId
import ru.yandex.vertis.moderation.model.meta.Metadata
import ru.yandex.vertis.moderation.proto.Model.Metadata.ResellerCleanNameMetadata.{Verdict => MetaVerdict}
import ru.yandex.vertis.moderation.proto.Model.Service

@RunWith(classOf[JUnitRunner])
class UsersAutoruResellerCleanNameMetadataDeciderSpec extends SpecBase with Inside {
  private val decider = new UsersAutoruResellerCleanNameMetadataDecider

  private val offerId1 = "offer_1"
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

  private val offerId2 = "offer_2"
  private val value3 =
    AutoruResellerCleanNameResolutionValueGen.next
      .setVerdict(ResolutionVerdict.CLEAN_NAME_FAKE_DOCS)
      .setOfferId(offerId2)
      .clearVin() // No VIN

  private val instance = instanceGen(essentialsGen(Service.USERS_AUTORU)).next

  "UsersAutoruResellerCleanNameMetadataDecider.decide" should {

    "returns empty decision if resolution is empty" in {
      val resolution = AutoruResellerCleanNameResolutionGen.next.clearValues
      val hoboTask = HoboTaskGen.next.setResolution(resolution).build
      val timestamp = DateTime.now

      val decision = decider.decide(instance, hoboTask, timestamp, 1)
      decision shouldBe Decision.Empty
    }

    "happy path" in {
      // GIVEN
      val resolution =
        AutoruResellerCleanNameResolutionGen.next
          .clearValues()
          .addValues(value1)
          .addValues(value2)
          .addValues(value3)
      val hoboTask = HoboTaskGen.next.setResolution(resolution).build
      val timestamp = DateTime.now

      // WHEN
      val decision = decider.decide(instance, hoboTask, timestamp, 1)

      // THEN
      decision.requests shouldBe Seq.empty

      decision.offersRequests should have size 2

      val sorted = decision.offersRequests.sortBy(_.externalId.objectId)

      inside(sorted.head) {
        case UpsertMetadata(
               externalId,
               Metadata.ResellerCleanName(Some(vin), verdicts, `timestamp`, None, None, None),
               `timestamp`,
               1
             ) =>
          vin shouldBe vin1
          verdicts shouldBe Set(
            MetaVerdict.CLEAN_NAME_PROVEN_OWNER_OK,
            MetaVerdict.CLEAN_NAME_RETURN_QUOTA
          )
          externalId shouldBe ExternalId(instance.externalId.user, offerId1)
      }

      inside(sorted(1)) {
        case UpsertMetadata(
               externalId,
               Metadata.ResellerCleanName(None, verdicts, `timestamp`, None, None, None),
               `timestamp`,
               1
             ) =>
          verdicts shouldBe Set(MetaVerdict.CLEAN_NAME_FAKE_DOCS)
          externalId shouldBe ExternalId(instance.externalId.user, offerId2)
      }
    }
  }
}
