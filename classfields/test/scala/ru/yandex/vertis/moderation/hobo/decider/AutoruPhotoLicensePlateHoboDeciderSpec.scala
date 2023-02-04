package ru.yandex.vertis.moderation.hobo.decider

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.ModerationRequest.InitialDepth
import ru.yandex.vertis.moderation.model._
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.meta.MetadataSet
import ru.yandex.vertis.moderation.model.signal.{HoboSignal, SignalSet}
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Metadata.AutoruPhotoLicensePlate
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Visibility}
import ru.yandex.vertis.moderation.util.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class AutoruPhotoLicensePlateHoboDeciderSpec extends SpecBase {

  implicit private val featureRegistry: FeatureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)
  private val decider: HoboDecider = new AutoruPhotoLicensePlateHoboDecider

  private case class TestCase(description: String,
                              decider: HoboDecider,
                              source: HoboDecider.Source,
                              isActiveFeature: Boolean,
                              check: Option[HoboDecider.Verdict] => Boolean
                             )

  private val validSourceWithWaitingForPhoto: HoboDecider.Source =
    newSource(
      visibility = Visibility.VISIBLE,
      opinion = UnknownOpinionGen.next,
      metaValue = Some(AutoruPhotoLicensePlate.Value.WAITING_FOR_PHOTO),
      signals = SignalSet.Empty
    )
  private val validSourceWithCheckFailed: HoboDecider.Source =
    newSource(
      visibility = Visibility.VISIBLE,
      opinion = UnknownOpinionGen.next,
      metaValue = Some(AutoruPhotoLicensePlate.Value.CHECK_FAILED),
      signals = SignalSet.Empty
    )

  private val invalidSource: HoboDecider.Source =
    newSource(
      visibility = Visibility.VISIBLE,
      opinion = FailedOpinionGen.next,
      metaValue = Some(AutoruPhotoLicensePlate.Value.CHECK_OK),
      signals = SignalSet.Empty
    )

  private val invalidSourceNeedCancel: HoboDecider.Source =
    newSource(
      visibility = Gen.oneOf(HoboDecider.NotVisibleStatuses.toList).next,
      opinion = UnknownOpinionGen.next,
      metaValue = Some(AutoruPhotoLicensePlate.Value.WAITING_FOR_PHOTO),
      signals = signalSetWithUncompleteHoboSignal(AutoruPhotoLicensePlateHoboDecider.CheckType)
    )

  private val invalidSourceNotNeedCancel: HoboDecider.Source =
    newSource(
      visibility = Visibility.VISIBLE,
      opinion = FailedOpinionGen.next,
      metaValue = Some(AutoruPhotoLicensePlate.Value.WAITING_FOR_PHOTO),
      signals = signalSetWithUncompleteHoboSignal(AutoruPhotoLicensePlateHoboDecider.CheckType)
    )

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "Disabled by feature-toggle",
        decider = decider,
        source = validSourceWithWaitingForPhoto,
        isActiveFeature = false,
        check = isEmpty
      ),
      TestCase(
        description = "Valid source with WAITING_FOR_PHOTO",
        decider = decider,
        source = validSourceWithWaitingForPhoto,
        isActiveFeature = true,
        check = isCreate
      ),
      TestCase(
        description = "Valid source with WAITING_FOR_PHOTO, but without prev Instance",
        decider = decider,
        source = validSourceWithWaitingForPhoto.copy(prev = None),
        isActiveFeature = true,
        check = isEmpty
      ),
      TestCase(
        description = "Valid source with WAITING_FOR_PHOTO, but with not changed essentialsUpdateTime",
        decider = decider,
        source = validSourceWithWaitingForPhoto.copy(prev = Some(validSourceWithWaitingForPhoto.instance)),
        isActiveFeature = true,
        check = isEmpty
      ),
      TestCase(
        description = "Valid source with CHECK_FAILED",
        decider = decider,
        source = validSourceWithCheckFailed,
        isActiveFeature = true,
        check = isCreate
      ),
      TestCase(
        description = "Invalid source",
        decider = decider,
        source = invalidSource,
        isActiveFeature = true,
        check = isEmpty
      ),
      /* @see VSMODERATION-4189
    TestCase(
      description = "Invalid source with need cancel",
      decider = decider,
      source = invalidSourceNeedCancel,
      isActiveFeature = true,
      check = isCancel
    ),
       */
      TestCase(
        description = "Invalid source that does not need to be canceled",
        decider = decider,
        source = invalidSourceNotNeedCancel,
        isActiveFeature = true,
        check = isEmpty
      )
    )

  private def newSource(visibility: Visibility,
                        opinion: Opinion,
                        metaValue: Option[AutoruPhotoLicensePlate.Value],
                        signals: SignalSet
                       ): HoboDecider.Source = {
    val service = Model.Service.AUTORU
    val context = ContextGen.next.copy(visibility = visibility)
    val metadataSet =
      metaValue
        .map { value =>
          val metadata = AutoruPhotoLicensePlateMetadataGen.next.copy(value = value)
          MetadataSet(metadata)
        }
        .getOrElse(MetadataSet.Empty)
    val instance =
      InstanceGen.next.copy(
        context = context,
        essentials = AutoruEssentialsGen.next,
        signals = signals,
        metadata = metadataSet
      )
    val opinions = Opinions(map = Map(DomainAutoruGen.next -> opinion))
    val diff = diffGen(service).next
    val prev = Some(instance.copy(essentialsUpdateTime = DateTimeGen.next))
    HoboDecider.Source(instance, prev, diff, opinions, DateTimeUtil.now(), InitialDepth)
  }

  private def isCreate(verdict: Option[HoboDecider.Verdict]): Boolean =
    verdict match {
      case Some(v: HoboDecider.NeedCreate) =>
        v.request.hoboSignalSource.`type` == AutoruPhotoLicensePlateHoboDecider.CheckType
      case _ => false
    }

  private def isCancel(verdict: Option[HoboDecider.Verdict]): Boolean =
    verdict.exists(_.isInstanceOf[HoboDecider.NeedCancel])

  private def isEmpty(verdict: Option[HoboDecider.Verdict]): Boolean = verdict.isEmpty

  private def signalSetWithUncompleteHoboSignal(checkType: HoboCheckType): SignalSet = {
    val hoboSignal =
      HoboSignalGen.withoutMarker.withoutSwitchOff.next.copy(
        domain = DomainAutoruGen.next,
        `type` = checkType,
        result = HoboSignal.Result.Undefined,
        task = Some(HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_WARNED_REVALIDATION_VISUAL.toString))
      )
    SignalSet(hoboSignal)
  }

  "AutoruPhotoLicensePlateHoboDecider.decide" should {

    testCases.foreach { case TestCase(description, decider, source, isActiveFeature, check) =>
      description in {
        featureRegistry
          .updateFeature(AutoruPhotoLicensePlateHoboDecider.IsActiveFeatureName, isActiveFeature)
          .futureValue
        check(decider.decide(source).futureValue) shouldBe true
      }
    }
  }
}
