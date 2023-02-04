package ru.yandex.vertis.moderation.util

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.Checkers.check
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{HoboSignalGen, InstanceGen, SourceGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, EssentialsVersion}
import ru.yandex.vertis.moderation.model.instance.AgencyCardRealtyEssentials
import ru.yandex.vertis.moderation.proto.RealtyLight.AgencyCardRealtyEssentials.AgencyCardServiceResolution
import ru.yandex.vertis.moderation.proto.RealtyLight.UserRealtyEssentials.UserType
import ru.yandex.vertis.moderation.util.HoboUtil._
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.hobo.proto.{Model => HoboModel}
import ru.yandex.vertis.hobo.proto.Model.{Task => HoboTask}
import ru.yandex.vertis.moderation.model.signal.{HoboSignal, NoMarker}

import scala.jdk.CollectionConverters.asJavaIterableConverter

@RunWith(classOf[JUnitRunner])
class HoboUtilTest extends SpecBase {

  "HoboUtil" should {
    "correctly converts instanceId <-> externalKey" in {
      check(forAll(Gen.alphaNumStr)(s => parseTaskExternalKey(generateTaskExternalKey(s)).contains(s)))
    }

    "fail on another service external key" in {
      val key = "salesman_123"
      parseTaskExternalKey(key) shouldBe None
    }

    "parse moderation external key" in {
      val key = "moderation_123"
      parseTaskExternalKey(key) shouldBe Some("123")
    }

    "correctly create snapshot from AGENCY_CARD_REALTY instance" in {
      val result =
        """{"essentialsVersion":{"hash":"card_hash","timestamp":0},"name":"real_name","ogrn":"QWER123","phones":["79999999999","71034212105","78283639933"],"foundationDate":1000,"address":"derevnya_dedushki","description":"best_agency_ever","logoUrl":"http://logo.url","trademarkDocsUrls":["http://trademark1.url","http://trademark2.url"],"sparkDocsUrls":["http://spark1.url","http://spark2.url"],"userType":"AGENCY","serviceResolutions":["MINIMAL_RATE"]}"""
      val essentialsVersion = EssentialsVersion(Some("card_hash"), Some(DateTimeUtil.Zero))
      val essentials =
        AgencyCardRealtyEssentials(
          essentialsVersion = Some(essentialsVersion),
          name = Some("real_name"),
          ogrn = Some("QWER123"),
          phones = Seq("79999999999", "71034212105", "78283639933"),
          foundationDate = Some(DateTimeUtil.Zero.plusSeconds(1)),
          address = Some("derevnya_dedushki"),
          description = Some("best_agency_ever"),
          logoUrl = Some("http://logo.url"),
          trademarkDocsUrls = Seq("http://trademark1.url", "http://trademark2.url"),
          sparkDocsUrls = Seq("http://spark1.url", "http://spark2.url"),
          userType = Some(UserType.AGENCY),
          serviceResolutions = Set(AgencyCardServiceResolution.MINIMAL_RATE)
        )
      val instance = InstanceGen.next.copy(essentials = essentials)
      instance2SnapshotPayload(instance) shouldBe Some(result)
    }
  }

  "getUpdatedHoboSignalSourceInternal" should {
    def createHoboSignal(domain: Domain): HoboSignal = {
      HoboSignalGen.next.copy(
        domain = domain,
        source = SourceGen.next.withMarker(NoMarker),
        result = HoboSignal.Result.Undefined
      )
    }

    def createTaskForRealtyFeedValidation(values: Seq[HoboModel.RealtyFeedValidationResolution.Value]): HoboTask = {
      val realtyFeedResolution =
        HoboModel.RealtyFeedValidationResolution
          .newBuilder()
          .addAllValues(values.asJava)

      HoboTask
        .newBuilder()
        .setState(HoboTask.State.COMPLETED)
        .setQueue(HoboModel.QueueId.REALTY_FEED_VALIDATION)
        .setResolution(HoboModel.Resolution.newBuilder().setRealtyFeedValidation(realtyFeedResolution).setVersion(1))
        .setVersion(1)
        .build()
    }

    "for realtyFeedValidation return only hobo OK if ALL is OK" in {
      val hoboSignal = createHoboSignal(Domain.FeedsRealty(Model.Domain.FeedsRealty.ALL))
      val values =
        Seq(
          HoboModel.RealtyFeedValidationResolution.Value
            .newBuilder()
            .setSegment(HoboModel.RealtyFeedValidationResolution.Value.Segment.ALL)
            .addVerdicts(HoboModel.RealtyFeedValidationResolution.Value.Verdict.OK)
            .build()
        )
      val task = createTaskForRealtyFeedValidation(values)

      val result = getUpdatedHoboSignalSourceInternal(hoboSignal, task)
      result.hobo.result shouldBe HoboSignal.Result.Good(resolutionComment = None)
      result.additional shouldBe empty
    }

    "for realtyFeedValidation return a few segments with different verdicts" in {
      val hoboSignal = createHoboSignal(Domain.FeedsRealty(Model.Domain.FeedsRealty.ALL))
      val values =
        Seq(
          HoboModel.RealtyFeedValidationResolution.Value
            .newBuilder()
            .setSegment(HoboModel.RealtyFeedValidationResolution.Value.Segment.ALL)
            .addVerdicts(HoboModel.RealtyFeedValidationResolution.Value.Verdict.OK_WITH_RESTRICTIONS)
            .build(),
          HoboModel.RealtyFeedValidationResolution.Value
            .newBuilder()
            .setSegment(HoboModel.RealtyFeedValidationResolution.Value.Segment.RENT_APARTMENTS)
            .addVerdicts(HoboModel.RealtyFeedValidationResolution.Value.Verdict.BAD_PHOTO)
            .build(),
          HoboModel.RealtyFeedValidationResolution.Value
            .newBuilder()
            .setSegment(HoboModel.RealtyFeedValidationResolution.Value.Segment.RENT_HOUSES)
            .addVerdicts(HoboModel.RealtyFeedValidationResolution.Value.Verdict.COMMERCIAL_IN_LIVING)
            .build()
        )
      val task = createTaskForRealtyFeedValidation(values)

      val result = getUpdatedHoboSignalSourceInternal(hoboSignal, task)
      result.hobo.result shouldBe HoboSignal.Result.Warn(
        detailedReasons = Set(DetailedReason.OkWithRestrictions),
        resolutionComment = None
      )
      result.additional.size shouldBe 2
      result.additional.exists { s =>
        s.domain == Domain.FeedsRealty(Model.Domain.FeedsRealty.RENT_APARTMENTS) &&
        s.getDetailedReasons == Set(DetailedReason.BadPhoto)
      } shouldBe true
      result.additional.exists { s =>
        s.domain == Domain.FeedsRealty(Model.Domain.FeedsRealty.RENT_HOUSES) &&
        s.getDetailedReasons == Set(DetailedReason.CommercialInLiving)
      } shouldBe true
    }
  }

}
