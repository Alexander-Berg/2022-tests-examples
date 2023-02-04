package ru.yandex.vertis.moderation.hobo

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.hobo.decider.HoboDecider
import ru.yandex.vertis.moderation.model.{DetailedReason, Opinion, Opinions}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, Diff, Essentials, RealtyEssentials, User}
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.model.Opinion.Failed
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Visibility}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.util.DateTimeUtil

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class ComplaintsHoboTriggerSpec extends HoboTriggerSpecBase {

  "toCreate" should {
    case class TestCase(description: String,
                        trigger: ComplaintsHoboTrigger,
                        source: HoboDecider.Source,
                        expectedCheckType: Option[HoboCheckType]
                       )

    val testCases: Seq[TestCase] =
      Seq(
        // REALTY
        TestCase(
          description = "return COMPLAINTS (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold)
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedCheckType = Some(HoboCheckType.COMPLAINTS)
        ),
        TestCase(
          description = "return COMPLAINTS_CALL (REALTY) for WrongRooms",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS_CALL),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.WrongRooms)
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedCheckType = Some(HoboCheckType.COMPLAINTS_CALL)
        ),
        TestCase(
          description = "return specified check type (REALTY) for PeopleOnPhoto and Sold",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS_CALL),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.PeopleOnPhoto),
                  complaintsSignal(DetailedReason.Sold)
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedCheckType = Some(HoboCheckType.COMPLAINTS_CALL)
        ),
        TestCase(
          description = "return None if already exist but switched off hobo signal (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold),
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS).withSwitchOff(Some(SignalSwitchOffGen.next))
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None for wrong visibility (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.BLOCKED,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold)
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if reason is USER_RESELLER (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.UserReseller(None, Seq.empty))
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if no complaints signal (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals = Seq.empty,
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if switched off complaints signal (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold).withSwitchOff(Some(SignalSwitchOffGen.next))
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if already exist hobo signal (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold),
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS)
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if wrong diff (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold)
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.AGENCY_ID),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if failed opinion (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold)
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = FailedOpinion
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if reason does not match check type (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.PeopleOnPhoto)
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedCheckType = None
        ),
        // AUTORU
        TestCase(
          description = "return COMPLAINTS (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedCheckType = Some(HoboCheckType.COMPLAINTS)
        ),
        TestCase(
          description = "return COMPLAINTS_VISUAL (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS_VISUAL),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Another)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedCheckType = Some(HoboCheckType.COMPLAINTS_VISUAL)
        ),
        TestCase(
          description = "return COMPLAINTS_VISUAL_DEALER (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS_VISUAL_DEALER),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Another)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = DealerUserGen.next
            ),
          expectedCheckType = Some(HoboCheckType.COMPLAINTS_VISUAL_DEALER)
        ),
        TestCase(
          description = "return specified check type (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS_VISUAL),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Another),
                  complaintsSignal(DetailedReason.Sold)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedCheckType = Some(HoboCheckType.COMPLAINTS_VISUAL)
        ),
        TestCase(
          description = "return None if already exist but switched off hobo signal (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold),
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS).withSwitchOff(Some(SignalSwitchOffGen.next))
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None for wrong visibility (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if reason is USER_RESELLER (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.UserReseller(None, Seq.empty))
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if no complaints signal (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals = Seq.empty,
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if switched off complaints signal (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold).withSwitchOff(Some(SignalSwitchOffGen.next))
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if already exist hobo signal (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold),
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return None if wrong diff (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.ADDRESS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedCheckType = None
        ),
        TestCase(
          description = "return COMPLAINTS even if failed opinion (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = FailedOpinion,
              user = AutoruUserGen.next
            ),
          expectedCheckType = Some(HoboCheckType.COMPLAINTS)
        ),
        TestCase(
          description = "return None if reason does not match check type (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Another)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedCheckType = None
        )
      )

    testCases.foreach { case TestCase(description, trigger, source, expectedCheckType) =>
      s"$description" in {
        val actualCheckType = trigger.toCreate(source).map(_.`type`)
        actualCheckType shouldBe expectedCheckType
      }
    }
  }

  "toCancel" should {
    case class TestCase(description: String,
                        trigger: ComplaintsHoboTrigger,
                        source: HoboDecider.Source,
                        expectedIsTombstoneDefined: Boolean
                       )

    val testCases: Seq[TestCase] =
      Seq(
        // REALTY
        TestCase(
          description = "accept DELETED (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.DELETED,
              essentials = realtyEssentials,
              signals =
                Seq(
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS)
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedIsTombstoneDefined = true
        ),
        TestCase(
          description = "decline DELETED if no hobo signal (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.DELETED,
              essentials = realtyEssentials,
              signals = Seq.empty,
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedIsTombstoneDefined = false
        ),
        TestCase(
          description = "decline DELETED if switched off hobo signal (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.DELETED,
              essentials = realtyEssentials,
              signals =
                Seq(
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS).withSwitchOff(Some(SignalSwitchOffGen.next))
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedIsTombstoneDefined = false
        ),
        TestCase(
          description = "decline DELETED if wrong diff (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.DELETED,
              essentials = realtyEssentials,
              signals =
                Seq(
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS)
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.AGENCY_ID),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedIsTombstoneDefined = false
        ),
        TestCase(
          description = "accept VISIBLE (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold, ts = DateTimeUtil.now().plusDays(1)),
                  goodHoboSignal(HoboCheckType.COMPLAINTS, ts = DateTimeUtil.now())
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedIsTombstoneDefined = true
        ),
        TestCase(
          description = "decline VISIBLE if complaints signal is older than hobo (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold, ts = DateTimeUtil.now().minusDays(1)),
                  goodHoboSignal(HoboCheckType.COMPLAINTS, ts = DateTimeUtil.now())
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedIsTombstoneDefined = false
        ),
        TestCase(
          description = "decline DELETED if check type not match check type of the signal (REALTY)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS_VISUAL),
          source =
            createSource(
              visibility = Visibility.DELETED,
              essentials = realtyEssentials,
              signals =
                Seq(
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS)
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedIsTombstoneDefined = false
        ),
        // AUTORU
        TestCase(
          description = "accept DELETED (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.DELETED,
              essentials = autoruEssentials,
              signals =
                Seq(
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedIsTombstoneDefined = true
        ),
        TestCase(
          description = "decline DELETED if no hobo signal (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.DELETED,
              essentials = autoruEssentials,
              signals = Seq.empty,
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedIsTombstoneDefined = false
        ),
        TestCase(
          description = "decline DELETED if switched off hobo signal (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.DELETED,
              essentials = autoruEssentials,
              signals =
                Seq(
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS).withSwitchOff(Some(SignalSwitchOffGen.next))
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedIsTombstoneDefined = false
        ),
        TestCase(
          description = "decline DELETED if wrong diff (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.DELETED,
              essentials = autoruEssentials,
              signals =
                Seq(
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.ACTUALIZE_TIME),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedIsTombstoneDefined = false
        ),
        TestCase(
          description = "accept VISIBLE (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold, ts = DateTimeUtil.now().plusDays(1)),
                  goodHoboSignal(HoboCheckType.COMPLAINTS, ts = DateTimeUtil.now())
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedIsTombstoneDefined = true
        ),
        TestCase(
          description = "decline VISIBLE if complaints signal is older than hobo (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS),
          source =
            createSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals =
                Seq(
                  complaintsSignal(DetailedReason.Sold, ts = DateTimeUtil.now().minusDays(1)),
                  goodHoboSignal(HoboCheckType.COMPLAINTS, ts = DateTimeUtil.now())
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedIsTombstoneDefined = false
        ),
        TestCase(
          description = "decline DELETED if check type not match check type of the signal (AUTORU)",
          trigger = new ComplaintsHoboTrigger(HoboCheckType.COMPLAINTS_VISUAL),
          source =
            createSource(
              visibility = Visibility.DELETED,
              essentials = autoruEssentials,
              signals =
                Seq(
                  uncompleteHoboSignal(HoboCheckType.COMPLAINTS)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.EFFECTIVE_SIGNALS),
              opinion = Opinion.Unknown(Set.empty),
              user = AutoruUserGen.next
            ),
          expectedIsTombstoneDefined = false
        )
      )

    testCases.foreach { case TestCase(description, trigger, source, expectedIsTombstoneDefined) =>
      s"$description" in {
        val actualIsTombstoneDefined = trigger.toCancel(source).isDefined
        actualIsTombstoneDefined shouldBe expectedIsTombstoneDefined
      }
    }
  }

  private def realtyEssentials: RealtyEssentials = RealtyEssentialsGen.next

  private def realtyDiff(values: Model.Diff.Realty.Value*): Diff.Realty = Diff.Realty(values.toSet)

  private def autoruEssentials: AutoruEssentials = AutoruEssentialsGen.next.copy(isCallCenter = false)

  private def autoruDiff(values: Model.Diff.Autoru.Value*): Diff.Autoru = Diff.Autoru(values.toSet)

  private def createSource(visibility: Visibility,
                           essentials: Essentials,
                           signals: Seq[Signal],
                           diff: Diff,
                           opinion: Opinion,
                           user: User = UserGen.next
                          ): HoboDecider.Source = {
    val externalId = ExternalIdGen.next.copy(user = user)
    val context = ContextGen.next.copy(visibility = visibility)
    val instance =
      instanceGen(externalId).next.copy(context = context, essentials = essentials, signals = SignalSet(signals))
    val opinions = Opinions(Opinions.unknown(instance.service).mapValues(_ => opinion))
    HoboDecider.Source(
      instance,
      None,
      diff,
      opinions,
      DateTimeUtil.now(),
      0
    )
  }

  private def complaintsSignal(detailedReason: DetailedReason, ts: DateTime = DateTimeUtil.now()): WarnSignal =
    WarnSignalGen.next.copy(
      source = AutomaticSourceGen.next.copy(application = Application.COMPLAINTS, marker = NoMarker),
      detailedReason = detailedReason,
      timestamp = ts,
      switchOff = None,
      ttl = None
    )

  private lazy val FailedOpinion = Failed(Set(DetailedReason.Another), Set.empty)
}
