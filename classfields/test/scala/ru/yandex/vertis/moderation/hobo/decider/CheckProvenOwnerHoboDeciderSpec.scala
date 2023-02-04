package ru.yandex.vertis.moderation.hobo.decider

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.auto.api.CommonModel.PhotoType
import ru.yandex.vertis.feature.model.{Feature, FeatureRegistry, FeatureType}
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.Generators.HoboDeciderSourceGen
import ru.yandex.vertis.moderation.hobo.decider.CheckProvenOwnerHoboDeciderSpec._
import ru.yandex.vertis.moderation.hobo.decider.HoboDecider.NeedCreate
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.instance.Diff
import ru.yandex.vertis.moderation.model.meta.{Metadata, MetadataSet}
import ru.yandex.vertis.moderation.model.signal.AutomaticSource
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.photos_check.PhotoTaskHelper
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Diff.Autoru.Value
import ru.yandex.vertis.moderation.proto.Model.HoboCheckType
import ru.yandex.vertis.moderation.proto.Model.Metadata.ProvenOwnerMetadata.Verdict
import ru.yandex.vertis.moderation.util.DateTimeUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class CheckProvenOwnerHoboDeciderSpec extends SpecBase {
  implicit private val featureRegistry: FeatureRegistry = mock[FeatureRegistry]
  implicit private val isEnabledFeature: Feature[Boolean] = mock[Feature[Boolean]]
  when(featureRegistry.register(any(), any(), any(), any())(any[FeatureType[Boolean]]())).thenReturn(isEnabledFeature)

  private val transparentPhotoTaskHelper: PhotoTaskHelper[Future] =
    (source: PhotoTaskHelper.Source) => Future.successful(source.taskView)

  private val decider = new CheckProvenOwnerHoboDecider(transparentPhotoTaskHelper, isFromCamera = _ => true)

  private val decideTestCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "ignore if there are no photos",
        additionalPhotos = Seq.empty,
        check = _.isEmpty
      ),
      TestCase(
        description = "decide to create if there is one photo with DRIVING_LICENSE type",
        additionalPhotos = Seq(Photo(Some(PhotoType.DRIVING_LICENSE))),
        check = VerdictIsNeedCreate
      ),
      TestCase(
        description = "ignore if conditions are met but feature is disabled",
        additionalPhotos = Seq(Photo(Some(PhotoType.DRIVING_LICENSE))),
        check = _.isEmpty,
        isEnabledFeature = false
      ),
      TestCase(
        description = "ignore if there is one photo with STS_FRONT type",
        additionalPhotos = Seq(Photo(Some(PhotoType.STS_FRONT))),
        check = _.isEmpty
      ),
      TestCase(
        description = "decide to create if there are two photos with STS_FRONT type",
        additionalPhotos = Seq(Photo(Some(PhotoType.STS_FRONT)), Photo(Some(PhotoType.STS_FRONT))),
        check = VerdictIsNeedCreate
      ),
      TestCase(
        description = "ignore if there is one photo without specified type",
        additionalPhotos = Seq(Photo(None)),
        check = _.isEmpty
      ),
      TestCase(
        description = "ignore if prev instance has the same additional photos",
        additionalPhotos = Seq(Photo(Some(PhotoType.DRIVING_LICENSE))),
        check = _.isEmpty,
        prevHasTheSamePhotos = true
      ),
      TestCase(
        description = "ignore if owner is already proved",
        additionalPhotos = Seq(Photo(Some(PhotoType.DRIVING_LICENSE))),
        check = _.isEmpty,
        metadata = MetadataSet(Metadata.ProvenOwner(None, Verdict.PROVEN_OWNER_OK, Set.empty, DateTimeUtil.now()))
      )
    )

  private val decideExpiredTestCases: Seq[ExpiredTestCase] =
    Seq(
      ExpiredTestCase(
        description = "decide to create if we have fresh data before signal",
        photoUpdate = DateTimeUtil.now().minusDays(2),
        signalUpdate = DateTimeUtil.now().minusDays(1),
        check = VerdictIsNeedCreate
      ),
      ExpiredTestCase(
        description = "decide to create if we have fresh data after signal",
        photoUpdate = DateTimeUtil.now().minusDays(2),
        signalUpdate = DateTimeUtil.now().minusDays(3),
        check = VerdictIsNeedCreate
      ),
      ExpiredTestCase(
        description = "decide to create if we have old data after signal",
        photoUpdate = DateTimeUtil.now().minusDays(200),
        signalUpdate = DateTimeUtil.now().minusDays(210),
        check = VerdictIsNeedCreate
      ),
      ExpiredTestCase(
        description = "ignore if we have old data before signal",
        photoUpdate = DateTimeUtil.now().minusDays(200),
        signalUpdate = DateTimeUtil.now().minusDays(190),
        check = _.isEmpty
      )
    )

  "CheckProvenOwnerHoboDecider" should {
    decideTestCases.foreach { case TestCase(description, photos, check, prevHasTheSamePhotos, isEnabled, metadata) =>
      description in {
        when(isEnabledFeature.apply()).thenReturn(isEnabled)
        val photoInfos =
          AutoPhotoInfoGen
            .next(photos.size)
            .toSeq
            .zip(photos)
            .map { case (photoInfo, Photo(photoType)) =>
              photoInfo.copy(photoType = photoType)
            }
        val essentialsGen =
          AutoruEssentialsGen.map(
            _.copy(
              additionalPhotos = photoInfos
            )
          )
        val prev = if (prevHasTheSamePhotos) Some(instanceGen(essentialsGen).next) else Gen.option(InstanceGen).next
        val source =
          HoboDeciderSourceGen.next.copy(
            instance = instanceGen(essentialsGen).next.copy(metadata = metadata, signals = SignalSet.Empty),
            prev = prev,
            timestamp = DateTimeUtil.now()
          )
        check(decider.decide(source).futureValue) shouldBe true
      }
    }

    decideExpiredTestCases.foreach { case ExpiredTestCase(description, photoUpdate, signalUpdate, check) =>
      description in {
        when(isEnabledFeature.apply()).thenReturn(true)
        val photoInfo = AutoPhotoInfoGen.next.copy(photoType = Some(PhotoType.DRIVING_LICENSE))
        val photoInfos =
          Seq(
            photoInfo.copy(uploadTime = Some(photoUpdate)),
            photoInfo.copy(uploadTime = Some(DateTimeUtil.Zero))
          )
        val essentialsGen = AutoruEssentialsGen.map(_.copy(additionalPhotos = photoInfos))
        val signals =
          SignalSet(
            HoboSignalGen.next.copy(
              `type` = HoboCheckType.PROVEN_OWNER,
              timestamp = signalUpdate,
              source = AutomaticSource(Application.HOBO)
            )
          )

        val hoboSource =
          HoboDeciderSourceGen.next.copy(
            instance = instanceGen(essentialsGen).next.copy(metadata = MetadataSet.Empty, signals = signals),
            prev = None,
            timestamp = DateTimeUtil.now()
          )
        check(decider.decide(hoboSource).futureValue) shouldBe true
      }
    }

    "not be defined at diff without ADDITIONAL_PHOTOS" in {
      val diff = AutoruDiffGen.suchThat(!_.values.contains(Value.ADDITIONAL_PHOTOS)).next
      decider.isDefinedAt(diff) shouldBe false
    }

    "be defined at ADDITIONAL_PHOTOS diff" in {
      val diff = Diff.Autoru(Set(Value.ADDITIONAL_PHOTOS))
      decider.isDefinedAt(diff) shouldBe true
    }
  }
}

object CheckProvenOwnerHoboDeciderSpec {
  private type CheckVerdict = Option[HoboDecider.Verdict] => Boolean

  private val VerdictIsNeedCreate: CheckVerdict = {
    case Some(NeedCreate(_)) => true
    case _                   => false
  }

  private case class Photo(photoType: Option[PhotoType])

  private case class TestCase(description: String,
                              additionalPhotos: Seq[Photo],
                              check: CheckVerdict,
                              prevHasTheSamePhotos: Boolean = false,
                              isEnabledFeature: Boolean = true,
                              metadata: MetadataSet = MetadataSet.Empty
                             )

  private case class ExpiredTestCase(description: String,
                                     photoUpdate: DateTime,
                                     signalUpdate: DateTime,
                                     check: CheckVerdict
                                    )

}
