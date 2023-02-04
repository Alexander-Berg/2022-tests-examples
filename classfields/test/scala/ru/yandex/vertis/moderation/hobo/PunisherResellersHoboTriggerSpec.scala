package ru.yandex.vertis.moderation.hobo

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.hobo.decider.HoboDecider
import ru.yandex.vertis.moderation.model.ModerationRequest.InitialDepth
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, Opinion, Opinions}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Diff, Essentials}
import ru.yandex.vertis.moderation.model.instance.Essentials.RegionId
import ru.yandex.vertis.moderation.model.meta.Metadata.{GeoInfoStatistics, OffersStatistics}
import ru.yandex.vertis.moderation.model.meta.OffersSummary.AutoruQualifier
import ru.yandex.vertis.moderation.model.meta.{GeoInfoSummary, MetadataSet, OffersSummary, TimedCounter}
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service, Visibility}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.util.{DateTimeUtil, Interval}
import ru.yandex.vertis.moderation.util.DateTimeUtil.OrderedDateTime
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials

/**
  * Specs for [[PunisherResellersHoboTrigger]]
  *
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class PunisherResellersHoboTriggerSpec extends HoboTriggerSpecBase {

  case class TestCase(description: String,
                      regionIds: Seq[RegionId] = Seq.empty,
                      visibility: Visibility,
                      opinion: Opinion,
                      signals: Iterable[Signal],
                      diff: Diff,
                      expectedIsDefined: Boolean,
                      metadata: Option[MetadataSet] = None
                     )

  import PunisherResellersHoboTriggerSpec._

  "toCreate" should {

    val testCases: Seq[TestCase] =
      Seq(
        TestCase(
          description = "wrong visibility",
          regionIds = Seq.empty,
          visibility = VisibilityGen.suchThat(_ != Visibility.VISIBLE).next,
          opinion = Opinion.Unknown(Set.empty),
          signals =
            Seq(
              nextWarnSignal()
            ),
          diff = newMeaningfullDiff(),
          expectedIsDefined = false
        ),
        TestCase(
          description = "failed opinion",
          regionIds = Seq.empty,
          visibility = Visibility.VISIBLE,
          opinion = FailedOpinionGen.next,
          signals =
            Seq(
              nextWarnSignal()
            ),
          diff = newMeaningfullDiff(),
          expectedIsDefined = false
        ),
        TestCase(
          description = "empty signals",
          regionIds = Seq.empty,
          visibility = Visibility.VISIBLE,
          opinion = Opinion.Unknown(Set.empty),
          signals = Seq.empty,
          diff = newMeaningfullDiff(),
          expectedIsDefined = false
        ),
        TestCase(
          description = "already exist hobo signal",
          regionIds = Seq.empty,
          visibility = Visibility.VISIBLE,
          opinion = Opinion.Unknown(Set.empty),
          signals =
            Seq(
              nextWarnSignal(),
              nextHoboSignal()
            ),
          diff = newMeaningfullDiff(),
          expectedIsDefined = false
        ),
        TestCase(
          description = "switched off warn signal",
          regionIds = Seq.empty,
          visibility = Visibility.VISIBLE,
          opinion = Opinion.Unknown(Set.empty),
          signals =
            Seq(
              nextWarnSignal().copy(switchOff = Some(SignalSwitchOffGen.next))
            ),
          diff = newMeaningfullDiff(),
          expectedIsDefined = false
        ),
        TestCase(
          description = "contains USER_RESELLER ban",
          regionIds = Seq.empty,
          visibility = Visibility.VISIBLE,
          opinion = Opinion.Failed(Set.empty, Set.empty),
          signals =
            Seq(
              nextWarnSignal(),
              nextUserResellerBan()
            ),
          diff = newMeaningfullDiff(),
          expectedIsDefined = false
        ),
        TestCase(
          description = "right source with unknown opinion",
          regionIds = Seq.empty,
          visibility = Visibility.VISIBLE,
          opinion = Opinion.Unknown(Set.empty),
          signals =
            Seq(
              nextWarnSignal()
            ),
          diff = newMeaningfullDiff(),
          expectedIsDefined = true,
          metadata = Some(freshOffersMetadata)
        ),
        TestCase(
          description = "right source with ok opinion",
          regionIds = Seq.empty,
          visibility = Visibility.VISIBLE,
          opinion = Opinion.Ok(Set.empty),
          signals =
            Seq(
              nextWarnSignal()
            ),
          diff = newMeaningfullDiff(),
          expectedIsDefined = true,
          metadata = Some(freshOffersMetadata)
        ),
        TestCase(
          description = "no recent offers",
          regionIds = Seq.empty,
          visibility = Visibility.VISIBLE,
          opinion = Opinion.Unknown(Set.empty),
          signals =
            Seq(
              nextWarnSignal()
            ),
          diff = newMeaningfullDiff(),
          expectedIsDefined = false,
          metadata = Some(staleOffersMetadata)
        )
      )

    testCases.foreach {
      case TestCase(description, regionIds, visibility, opinion, signals, diff, expectedIsDefined, meta) =>
        s"return isDefined=$expectedIsDefined for case '$description'" in {
          val context = ContextGen.next.copy(visibility = visibility)
          val essentials: Essentials = AutoruEssentialsGen.next.copy(geobaseId = regionIds)
          val generated = InstanceGen.next
          val instance =
            generated.copy(
              essentials = essentials,
              context = context,
              signals = SignalSet(signals),
              metadata = meta.getOrElse(generated.metadata)
            )

          val opinionsMap =
            Domain
              .forServiceOrDefault(instance.service)
              .map { domain =>
                domain -> opinion
              }
              .toMap
          val opinions = Opinions(opinionsMap)

          val source = HoboDecider.Source(instance, None, diff, opinions, DateTimeUtil.now(), InitialDepth)

          val actualIsDefined = PunisherResellersHoboTrigger.toCreate(source).isDefined
          actualIsDefined shouldBe expectedIsDefined
        }
    }
  }

  "toCancel" should {

    val testCases: Seq[TestCase] =
      Seq(
        TestCase(
          description = "positive case",
          regionIds = Seq.empty,
          visibility = Visibility.BLOCKED,
          opinion = Opinion.Failed(Set.empty, Set.empty),
          signals =
            Seq(
              nextUncompletedHoboSignal(),
              nextUserResellerBan()
            ),
          diff = newMeaningfullDiff(),
          expectedIsDefined = true
        ),
        TestCase(
          description = "completed hobo signal",
          regionIds = Seq.empty,
          visibility = Visibility.BLOCKED,
          opinion = Opinion.Failed(Set.empty, Set.empty),
          signals =
            Seq(
              nextCompletedHoboSignal(),
              nextUserResellerBan()
            ),
          diff = newMeaningfullDiff(),
          expectedIsDefined = false
        ),
        TestCase(
          description = "no hobo signal",
          regionIds = Seq.empty,
          visibility = Visibility.BLOCKED,
          opinion = Opinion.Failed(Set.empty, Set.empty),
          signals =
            Seq(
              nextUserResellerBan()
            ),
          diff = newMeaningfullDiff(),
          expectedIsDefined = false
        ),
        TestCase(
          description = "diff not contains effective signals",
          regionIds = Seq.empty,
          visibility = Visibility.BLOCKED,
          opinion = Opinion.Failed(Set.empty, Set.empty),
          signals =
            Seq(
              nextUncompletedHoboSignal(),
              nextUserResellerBan()
            ),
          diff = Diff.Realty(Set.empty),
          expectedIsDefined = false
        ),
        TestCase(
          description = "diff contains context",
          regionIds = Seq.empty,
          visibility = Visibility.PAYMENT_REQUIRED,
          opinion = Opinion.Failed(Set.empty, Set.empty),
          signals =
            Seq(
              nextUncompletedHoboSignal(),
              nextUserResellerBan()
            ),
          diff = Diff.context(Service.AUTORU),
          expectedIsDefined = true
        )
      )

    testCases.foreach { case TestCase(description, _, visibility, opinion, signals, diff, expectedIsDefined, meta) =>
      s"return isDefined=$expectedIsDefined for case '$description" in {
        val context = ContextGen.next.copy(visibility = visibility)
        val instance = InstanceGen.next.copy(context = context, signals = SignalSet(signals))

        val opinionsMap =
          Domain
            .forServiceOrDefault(instance.service)
            .map { domain =>
              domain -> opinion
            }
            .toMap
        val opinions = Opinions(opinionsMap)

        val source = HoboDecider.Source(instance, None, diff, opinions, DateTimeUtil.now(), InitialDepth)

        val actualIsDefined = PunisherResellersHoboTrigger.toCancel(source).isDefined
        actualIsDefined shouldBe expectedIsDefined
      }
    }
  }

  private def nextWarnSignal(): WarnSignal =
    WarnSignalGen.next.copy(
      source = AutomaticSource(application = Application.PUNISHER),
      detailedReason = DetailedReason.UserReseller(None, Seq.empty),
      switchOff = None
    )

  private def nextHoboSignal(): HoboSignal =
    HoboSignalGen.next.copy(
      source = AutomaticSource(Application.MODERATION),
      `type` = HoboCheckType.PUNISHER_RESELLERS,
      switchOff = None
    )

  private def nextUncompletedHoboSignal(): HoboSignal =
    nextHoboSignal().copy(
      task = Some(HoboSignalTaskGen.next),
      result = HoboSignal.Result.Undefined
    )

  private def nextCompletedHoboSignal(): HoboSignal =
    nextHoboSignal().copy(
      task = Some(HoboSignalTaskGen.next),
      result = HoboSignal.Result.Good(None)
    )

  private def newMeaningfullDiff(): Diff =
    Diff.Realty(
      Set(
        Model.Diff.Realty.Value.SIGNALS,
        Model.Diff.Realty.Value.EFFECTIVE_SIGNALS,
        Model.Diff.Realty.Value.CONTEXT
      )
    )

  private def nextUserResellerBan(): BanSignal =
    BanSignalGen.next.copy(
      detailedReason = DetailedReason.UserReseller(None, Seq.empty),
      switchOff = None
    )
}

object PunisherResellersHoboTriggerSpec {
  val freshOffersMetadata = {
    val offerStat =
      OffersStatistics(
        timestamp = DateTimeGen.next,
        statistics =
          OffersSummary(
            Map(AutoruQualifier(Visibility.VISIBLE, AutoruEssentials.Category.CARS) -> 1),
            false
          )
      )

    val counter =
      TimedCounter(
        1,
        Some(
          Interval(
            DateTime.now().minusMonths(5),
            DateTime.now().minusMonths(2)
          )
        )
      )

    val geoStat =
      GeoInfoStatistics(
        timestamp = DateTimeGen.next,
        statistics =
          GeoInfoSummary(
            Map(1 -> counter),
            false
          )
      )

    MetadataSet(
      offerStat,
      geoStat
    )
  }

  val staleOffersMetadata = {
    val offerStat =
      OffersStatistics(
        timestamp = DateTimeGen.next,
        statistics =
          OffersSummary(
            Map(AutoruQualifier(Visibility.DELETED, AutoruEssentials.Category.CARS) -> 1),
            false
          )
      )

    val counter =
      TimedCounter(
        1,
        Some(
          Interval(
            DateTime.now().minusMonths(5),
            DateTime.now().minusMonths(4)
          )
        )
      )

    val geoStat =
      GeoInfoStatistics(
        timestamp = DateTimeGen.next,
        statistics =
          GeoInfoSummary(
            Map(1 -> counter),
            false
          )
      )

    MetadataSet(
      offerStat,
      geoStat
    )
  }
}
