package ru.yandex.vertis.moderation.hobo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.feature.EmptyFeatureRegistry
import ru.yandex.vertis.moderation.hobo.decider.HoboDecider
import ru.yandex.vertis.moderation.model.ModerationRequest.InitialDepth
import ru.yandex.vertis.moderation.model.{DetailedReason, Opinion, Opinions}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, Diff, Essentials, RealtyEssentials}
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.model.Opinion.Failed
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.SellerType
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service, Visibility}
import ru.yandex.vertis.moderation.util.DateTimeUtil

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class RecheckBannedInstanceHoboTriggerSpec extends HoboTriggerSpecBase {

  implicit private val featureRegistry: FeatureRegistry = EmptyFeatureRegistry
  private val trigger = new RecheckBannedInstanceHoboTrigger

  case class TestCase(description: String, source: HoboDecider.Source, expectedIsDefined: Boolean)

  "toCreate" should {
    val toCreateTestCases: Seq[TestCase] =
      Seq(
        // REALTY
        TestCase(
          description = "accept INACTIVE with ban for REALTY",
          source =
            newSource(
              visibility = Visibility.INACTIVE,
              essentials = realtyEssentials,
              signals = Seq(banSignal(DetailedReason.WrongAdParameters)),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "accept INACTIVE with hobo for REALTY",
          source =
            newSource(
              visibility = Visibility.INACTIVE,
              essentials = realtyEssentials,
              signals = Seq(hoboSignal(HoboCheckType.CALL_CENTER, DetailedReason.WrongAdParameters)),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "accept INVALID with ban for REALTY",
          source =
            newSource(
              visibility = Visibility.INVALID,
              essentials = realtyEssentials,
              signals = Seq(banSignal(DetailedReason.WrongAdParameters)),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "accept INVALID with hobo for REALTY",
          source =
            newSource(
              visibility = Visibility.INVALID,
              essentials = realtyEssentials,
              signals = Seq(hoboSignal(HoboCheckType.CALL_CENTER, DetailedReason.WrongAdParameters)),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "decline VISIBLE for REALTY",
          source =
            newSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals = Seq(banSignal(DetailedReason.WrongAdParameters)),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline not from vos for REALTY",
          source =
            newSource(
              visibility = Visibility.INACTIVE,
              essentials = realtyEssentials.copy(isVos = Some(false)),
              signals = Seq(banSignal(DetailedReason.WrongAdParameters)),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline not editable for REALTY",
          source =
            newSource(
              visibility = Visibility.INACTIVE,
              essentials = realtyEssentials.copy(isEditable = Some(false)),
              signals = Seq(banSignal(DetailedReason.WrongAdParameters)),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline ban with switchOff for REALTY",
          source =
            newSource(
              visibility = Visibility.INACTIVE,
              essentials = realtyEssentials,
              signals =
                Seq(banSignal(DetailedReason.WrongAdParameters).copy(switchOff = Some(SignalSwitchOffGen.next))),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline wrong diff for REALTY",
          source =
            newSource(
              visibility = Visibility.INACTIVE,
              essentials = realtyEssentials,
              signals = Seq(banSignal(DetailedReason.WrongAdParameters)),
              diff = realtyDiff(Model.Diff.Realty.Value.IS_COMMERCIAL),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline Unknown opinion for REALTY",
          source =
            newSource(
              visibility = Visibility.INACTIVE,
              essentials = realtyEssentials,
              signals = Seq(banSignal(DetailedReason.WrongAdParameters)),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline signal REVALIDATION_VISUAL already exist for REALTY",
          source =
            newSource(
              visibility = Visibility.INACTIVE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  banSignal(DetailedReason.WrongAdParameters),
                  uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)
                ),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline signal REVALIDATION already exist for REALTY",
          source =
            newSource(
              visibility = Visibility.INACTIVE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  banSignal(DetailedReason.WrongAdParameters),
                  uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION)
                ),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline if contains Another reason", // VSMODERATION-3855
          source =
            newSource(
              visibility = Visibility.INACTIVE,
              essentials = realtyEssentials,
              signals = Seq(banSignal(DetailedReason.WrongAdParameters), banSignal(DetailedReason.Another)),
              diff =
                realtyDiff(
                  Model.Diff.Realty.Value.RAW_OFFER_USER_CONTROLLED_FIELDS_HASH,
                  Model.Diff.Realty.Value.OFFER_TYPE
                ),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        // AUTORU
        TestCase(
          description = "accept BLOCKED with ban for AUTORU",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals = Seq(banSignal(DetailedReason.WrongPhoto)),
              diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "accept BLOCKED with hobo for AUTORU",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals = Seq(hoboSignal(HoboCheckType.CALL_CENTER, DetailedReason.WrongPhoto)),
              diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "decline VISIBLE for AUTORU",
          source =
            newSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals = Seq(banSignal(DetailedReason.WrongPhoto)),
              diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline empty phones for AUTORU",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials.copy(phones = Map.empty),
              signals = Seq(banSignal(DetailedReason.WrongPhoto)),
              diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline not private seller type for AUTORU",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials.copy(sellerType = Some(SellerType.COMMERCIAL)),
              signals = Seq(banSignal(DetailedReason.WrongPhoto)),
              diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline ban with switchOff for AUTORU",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals = Seq(banSignal(DetailedReason.WrongPhoto).copy(switchOff = Some(SignalSwitchOffGen.next))),
              diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline wrong diff and reason for AUTORU",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals = Seq(banSignal(DetailedReason.WrongPhoto)),
              diff = autoruDiff(Model.Diff.Autoru.Value.CUSTOM_HOUSE_STATE),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline Unknown opinion for AUTORU",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals = Seq(banSignal(DetailedReason.WrongPhoto)),
              diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
              opinion = Opinion.Unknown(Set.empty)
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline signal REVALIDATION_VISUAL already exist for AUTORU",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals =
                Seq(
                  banSignal(DetailedReason.WrongPhoto),
                  uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline signal REVALIDATION already exist for AUTORU",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals =
                Seq(banSignal(DetailedReason.WrongPhoto), uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION)),
              diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "accept changed moderation photos for AUTORU, when photos were wrong",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals = Seq(banSignal(DetailedReason.WrongPhoto)),
              diff = autoruDiff(Model.Diff.Autoru.Value.MODERATION_PHOTOS_ADDED),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "accept changed moderation photos for AUTORU, when grz was hidden",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals = Seq(banSignal(DetailedReason.HiddenGrz)),
              diff = autoruDiff(Model.Diff.Autoru.Value.MODERATION_PHOTOS_ADDED),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "accept changed photos for AUTORU in case of previous contacts spam",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals = Seq(banSignal(DetailedReason.ContactsSpam)),
              diff = autoruDiff(Model.Diff.Autoru.Value.PHOTOS_UNORDERED),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        )
      )

    toCreateTestCases.foreach { case TestCase(description, source, expectedIsDefined) =>
      s"$description" in {
        val actualIsDefined = trigger.toCreate(source).isDefined
        actualIsDefined shouldBe expectedIsDefined
      }
    }
  }

  "toCancel" should {
    val toCancelTestCases: Seq[TestCase] =
      Seq(
        // REALTY
        TestCase(
          description = "accept DELETED for REALTY",
          source =
            newSource(
              visibility = Visibility.DELETED,
              essentials = realtyEssentials,
              signals = Seq(uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)),
              diff = realtyDiff(Model.Diff.Realty.Value.SIGNALS),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "decline VISIBLE for REALTY",
          source =
            newSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals = Seq(uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)),
              diff = realtyDiff(Model.Diff.Realty.Value.SIGNALS),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline with turned off signal for REALTY",
          source =
            newSource(
              visibility = Visibility.VISIBLE,
              essentials = realtyEssentials,
              signals =
                Seq(
                  uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)
                    .withSwitchOff(Some(SignalSwitchOffGen.next))
                ),
              diff = realtyDiff(Model.Diff.Realty.Value.SIGNALS),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        // AUTORU
        TestCase(
          description = "accept DELETED for AUTORU",
          source =
            newSource(
              visibility = Visibility.DELETED,
              essentials = autoruEssentials,
              signals = Seq(uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)),
              diff = autoruDiff(Model.Diff.Autoru.Value.SIGNALS),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "accept INACTIVE for AUTORU",
          source =
            newSource(
              visibility = Visibility.INACTIVE,
              essentials = autoruEssentials,
              signals = Seq(uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)),
              diff = autoruDiff(Model.Diff.Autoru.Value.SIGNALS),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "accept INVALID for AUTORU",
          source =
            newSource(
              visibility = Visibility.INVALID,
              essentials = autoruEssentials,
              signals = Seq(uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)),
              diff = autoruDiff(Model.Diff.Autoru.Value.SIGNALS),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "accept VISIBLE for AUTORU",
          source =
            newSource(
              visibility = Visibility.VISIBLE,
              essentials = autoruEssentials,
              signals = Seq(uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)),
              diff = autoruDiff(Model.Diff.Autoru.Value.SIGNALS),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "decline BLOCKED for AUTORU",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials,
              signals = Seq(uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)),
              diff = autoruDiff(Model.Diff.Autoru.Value.SIGNALS),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "accept no phones for AUTORU",
          source =
            newSource(
              visibility = Visibility.BLOCKED,
              essentials = autoruEssentials.copy(phones = Map.empty),
              signals = Seq(uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)),
              diff = autoruDiff(Model.Diff.Autoru.Value.SIGNALS),
              opinion = FailedOpinion
            ),
          expectedIsDefined = true
        ),
        TestCase(
          description = "decline with turned off signal for AUTORU",
          source =
            newSource(
              visibility = Visibility.DELETED,
              essentials = autoruEssentials,
              signals =
                Seq(
                  uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)
                    .withSwitchOff(Some(SignalSwitchOffGen.next))
                ),
              diff = autoruDiff(Model.Diff.Autoru.Value.SIGNALS),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        ),
        TestCase(
          description = "decline with wrong diff for AUTORU",
          source =
            newSource(
              visibility = Visibility.DELETED,
              essentials = autoruEssentials,
              signals = Seq(uncompleteHoboSignal(HoboCheckType.BANNED_REVALIDATION_VISUAL)),
              diff = autoruDiff(Model.Diff.Autoru.Value.DOORS_COUNT),
              opinion = FailedOpinion
            ),
          expectedIsDefined = false
        )
      )

    toCancelTestCases.foreach { case TestCase(description, source, expectedIsDefined) =>
      s"$description" in {
        val actualIsDefined = trigger.toCancel(source).isDefined
        actualIsDefined shouldBe expectedIsDefined
      }
    }
  }

  private def newSource(visibility: Visibility,
                        essentials: Essentials,
                        signals: Seq[Signal],
                        diff: Diff,
                        opinion: Opinion
                       ): HoboDecider.Source = {
    val context = ContextGen.next.copy(visibility = visibility)
    val instance = InstanceGen.next.copy(context = context, essentials = essentials, signals = SignalSet(signals))
    val opinions = Opinions(Opinions.unknown(instance.service).mapValues(_ => opinion))
    HoboDecider.Source(
      instance,
      None,
      diff,
      opinions,
      DateTimeUtil.now(),
      InitialDepth
    )
  }

  private def realtyEssentials: RealtyEssentials =
    RealtyEssentialsGen.next.copy(
      isVos = Some(true),
      isEditable = Some(true)
    )

  private def autoruEssentials: AutoruEssentials =
    AutoruEssentialsGen.next.copy(
      phones = AutoruPhoneGen.next(1).toMap,
      sellerType = Some(SellerType.PRIVATE)
    )

  private def realtyDiff(values: Model.Diff.Realty.Value*): Diff.Realty = Diff.Realty(values.toSet)

  private def autoruDiff(values: Model.Diff.Autoru.Value*): Diff.Autoru = Diff.Autoru(values.toSet)

  private def banSignal(detailedReason: DetailedReason, marker: SourceMarker = NoMarker): BanSignal =
    BanSignalGen.next.copy(
      source = ManualSourceGen.next.copy(marker = marker),
      detailedReason = detailedReason,
      switchOff = None,
      ttl = None
    )

  private def hoboSignal(checkType: HoboCheckType, detailedReason: DetailedReason): HoboSignal =
    uncompleteHoboSignal(checkType).copy(
      result = HoboSignal.Result.Bad(Set(detailedReason), None)
    )

  private def uncompleteHoboSignal(checkType: HoboCheckType): HoboSignal =
    HoboSignalGen.next.copy(
      source = SourceGen.next.withMarker(NoMarker),
      `type` = checkType,
      task = Some(HoboSignalTaskGen.next),
      switchOff = None,
      result = HoboSignal.Result.Undefined,
      ttl = None
    )

  private lazy val FailedOpinion = Failed(Set(DetailedReason.Another), Set.empty)
}
