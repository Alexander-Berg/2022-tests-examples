package ru.yandex.vertis.moderation.hobo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.decider.HoboDecider
import ru.yandex.vertis.moderation.model.ModerationRequest.InitialDepth
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Diff, Essentials}
import ru.yandex.vertis.moderation.model.signal.{HoboSignal, NoMarker, Signal, SignalSet}
import ru.yandex.vertis.moderation.model.{Opinion, Opinions}
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.{Category, SellerType}
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Visibility}
import ru.yandex.vertis.moderation.proto.{Autoru, Model}
import ru.yandex.vertis.moderation.util.DateTimeUtil

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class CallCenterPhotoHoboTriggerSpec extends SpecBase {

  case class TestCase(description: String,
                      trigger: CallCenterPhotoHoboTrigger,
                      source: HoboDecider.Source,
                      expectedIsDefined: Boolean
                     )

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "positive case: VISIBLE call center offer",
        trigger = newTrigger(enabled = true, additionalEnabled = true),
        source =
          newSource(
            visibility = Visibility.VISIBLE,
            essentials =
              AutoruEssentialsGen.next.copy(
                isCallCenter = true,
                source = Autoru.AutoruEssentials.Source.AVITO,
                timestampCreate = Some(CallCenterPhotoHoboTrigger.MinTimestampCreate.plus(1)),
                photos = Seq(AutoPhotoInfoOptGen.next)
              ),
            signals = Seq.empty,
            diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
            opinion = Opinion.Unknown(Set.empty, None)
          ),
        expectedIsDefined = true
      ),
      TestCase(
        description = "positive case: INACTIVE call center offer",
        trigger = newTrigger(enabled = true, additionalEnabled = true),
        source =
          newSource(
            visibility = Visibility.INACTIVE,
            essentials =
              AutoruEssentialsGen.next.copy(
                isCallCenter = true,
                source = Autoru.AutoruEssentials.Source.AVITO,
                timestampCreate = Some(CallCenterPhotoHoboTrigger.MinTimestampCreate.plus(1)),
                photos = Seq(AutoPhotoInfoOptGen.next)
              ),
            signals = Seq.empty,
            diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
            opinion = Opinion.Unknown(Set.empty, None)
          ),
        expectedIsDefined = true
      ),
      TestCase(
        description = "positive case: not call center offer",
        trigger = newTrigger(enabled = true, additionalEnabled = true),
        source =
          newSource(
            visibility = Visibility.VISIBLE,
            essentials =
              AutoruEssentialsGen.next.copy(
                isCallCenter = false,
                category = Some(Category.CARS),
                mark = Some("PEUGEOT"),
                sellerType = Some(SellerType.PRIVATE)
              ),
            signals = Seq.empty,
            diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
            opinion = Opinion.Unknown(Set.empty, None)
          ),
        expectedIsDefined = true
      ),
      TestCase(
        description = "negative case: not call center offer, trigger disabled",
        trigger = newTrigger(enabled = true, additionalEnabled = false),
        source =
          newSource(
            visibility = Visibility.VISIBLE,
            essentials =
              AutoruEssentialsGen.next.copy(
                isCallCenter = false,
                category = Some(Category.CARS),
                mark = Some("PEUGEOT"),
                sellerType = Some(SellerType.PRIVATE)
              ),
            signals = Seq.empty,
            diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
            opinion = Opinion.Unknown(Set.empty, None)
          ),
        expectedIsDefined = false
      ),
      TestCase(
        description = "negative case: call center, not from AVITO",
        trigger = newTrigger(enabled = true, additionalEnabled = true),
        source =
          newSource(
            visibility = Visibility.VISIBLE,
            essentials =
              AutoruEssentialsGen.next.copy(
                isCallCenter = true,
                source = Autoru.AutoruEssentials.Source.AUTO24,
                timestampCreate = Some(CallCenterPhotoHoboTrigger.MinTimestampCreate.plus(1)),
                photos = Seq(AutoPhotoInfoOptGen.next)
              ),
            signals = Seq.empty,
            diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
            opinion = Opinion.Unknown(Set.empty, None)
          ),
        expectedIsDefined = false
      ),
      TestCase(
        description = "negative case: call center, created before MinTimestampCreate",
        trigger = newTrigger(enabled = true, additionalEnabled = true),
        source =
          newSource(
            visibility = Visibility.VISIBLE,
            essentials =
              AutoruEssentialsGen.next.copy(
                isCallCenter = true,
                source = Autoru.AutoruEssentials.Source.AVITO,
                timestampCreate = Some(CallCenterPhotoHoboTrigger.MinTimestampCreate.minus(1)),
                photos = Seq(AutoPhotoInfoOptGen.next)
              ),
            signals = Seq.empty,
            diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
            opinion = Opinion.Unknown(Set.empty, None)
          ),
        expectedIsDefined = false
      ),
      TestCase(
        description = "negative case: call center, no photos",
        trigger = newTrigger(enabled = true, additionalEnabled = true),
        source =
          newSource(
            visibility = Visibility.VISIBLE,
            essentials =
              AutoruEssentialsGen.next.copy(
                isCallCenter = true,
                source = Autoru.AutoruEssentials.Source.AVITO,
                timestampCreate = Some(CallCenterPhotoHoboTrigger.MinTimestampCreate.plus(1)),
                photos = Seq.empty
              ),
            signals = Seq.empty,
            diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
            opinion = Opinion.Unknown(Set.empty, None)
          ),
        expectedIsDefined = false
      ),
      TestCase(
        description = "negative case: call center, wrong diff",
        trigger = newTrigger(enabled = true, additionalEnabled = true),
        source =
          newSource(
            visibility = Visibility.VISIBLE,
            essentials =
              AutoruEssentialsGen.next.copy(
                isCallCenter = true,
                source = Autoru.AutoruEssentials.Source.AVITO,
                timestampCreate = Some(CallCenterPhotoHoboTrigger.MinTimestampCreate.plus(1)),
                photos = Seq(AutoPhotoInfoOptGen.next)
              ),
            signals = Seq.empty,
            diff = autoruDiff(Model.Diff.Autoru.Value.ADDRESS),
            opinion = Opinion.Unknown(Set.empty, None)
          ),
        expectedIsDefined = false
      ),
      TestCase(
        description = "negative case: call center, trigger disabled",
        trigger = newTrigger(enabled = false, additionalEnabled = true),
        source =
          newSource(
            visibility = Visibility.VISIBLE,
            essentials =
              AutoruEssentialsGen.next.copy(
                isCallCenter = true,
                source = Autoru.AutoruEssentials.Source.AVITO,
                timestampCreate = Some(CallCenterPhotoHoboTrigger.MinTimestampCreate.plus(1)),
                photos = Seq(AutoPhotoInfoOptGen.next)
              ),
            signals = Seq.empty,
            diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
            opinion = Opinion.Unknown(Set.empty, None)
          ),
        expectedIsDefined = false
      ),
      TestCase(
        description = "negative case: hobo signal already exists",
        trigger = newTrigger(enabled = true, additionalEnabled = true),
        source =
          newSource(
            visibility = Visibility.VISIBLE,
            essentials =
              AutoruEssentialsGen.next.copy(
                isCallCenter = true,
                source = Autoru.AutoruEssentials.Source.AVITO,
                timestampCreate = Some(CallCenterPhotoHoboTrigger.MinTimestampCreate.plus(1)),
                photos = Seq(AutoPhotoInfoOptGen.next)
              ),
            signals = Seq(newHoboSignal(switchedOff = false)),
            diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
            opinion = Opinion.Unknown(Set.empty, None)
          ),
        expectedIsDefined = false
      ),
      TestCase(
        description = "negative case: switched off hobo signal already exists",
        trigger = newTrigger(enabled = true, additionalEnabled = true),
        source =
          newSource(
            visibility = Visibility.VISIBLE,
            essentials =
              AutoruEssentialsGen.next.copy(
                isCallCenter = true,
                source = Autoru.AutoruEssentials.Source.AVITO,
                timestampCreate = Some(CallCenterPhotoHoboTrigger.MinTimestampCreate.plus(1)),
                photos = Seq(AutoPhotoInfoOptGen.next)
              ),
            signals = Seq(newHoboSignal(switchedOff = true)),
            diff = autoruDiff(Model.Diff.Autoru.Value.PHOTO_IDS_UNORDERED),
            opinion = Opinion.Unknown(Set.empty, None)
          ),
        expectedIsDefined = false
      )
    )

  "toCreate" should {

    testCases.foreach { case TestCase(description, trigger, source, expectedIsDefined) =>
      description in {
        val actualIsDefined = trigger.toCreate(source).isDefined
        actualIsDefined shouldBe expectedIsDefined
      }
    }
  }

  private def newHoboSignal(switchedOff: Boolean): HoboSignal =
    HoboSignalGen.next
      .copy(
        `type` = HoboCheckType.CALL_CENTER_PHOTO,
        switchOff = if (switchedOff) Some(SignalSwitchOffGen.next) else None
      )
      .withMarker(NoMarker)

  private def newTrigger(enabled: Boolean, additionalEnabled: Boolean): CallCenterPhotoHoboTrigger = {
    val featureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)
    val trigger = new CallCenterPhotoHoboTrigger(featureRegistry)
    featureRegistry.updateFeature(CallCenterPhotoHoboTrigger.EnableFeatureName, enabled).futureValue
    featureRegistry
      .updateFeature(CallCenterPhotoHoboTrigger.EnableNonCallCenterFeatureName, additionalEnabled)
      .futureValue
    trigger
  }

  private def autoruDiff(values: Model.Diff.Autoru.Value*): Diff.Autoru = Diff.Autoru(values.toSet)

  private def newSource(visibility: Visibility,
                        essentials: Essentials,
                        signals: Seq[Signal],
                        diff: Diff,
                        opinion: Opinion
                       ): HoboDecider.Source = {
    val context = ContextGen.next.copy(visibility = visibility)
    val instance =
      InstanceGen.next.copy(
        context = context,
        essentials = essentials,
        signals = SignalSet(signals)
      )
    val opinions = Opinions(Opinions.unknown(instance.service).mapValues(_ => opinion))
    HoboDecider.Source(instance, None, diff, opinions, DateTimeUtil.now(), InitialDepth)
  }
}
