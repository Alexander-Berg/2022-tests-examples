package ru.yandex.realty.rent.stage.flat

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.dao.{FlatQuestionnaireDao, FlatShowingDao, OwnerRequestDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.{FlatShowingStatus, OwnerRequestStatus}
import ru.yandex.realty.rent.model.{Flat, FlatQuestionnaire, FlatShowing, OwnerRequest}
import ru.yandex.realty.rent.proto.api.moderation.ClassifiedTypeNamespace
import ru.yandex.realty.rent.proto.model.flat.ClassifiedsPubStatusInternal
import ru.yandex.realty.rent.stage.flat.AutoPublishStage.AutoPublishStageConfig
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.realty.util.protobuf.ProtobufFormats.DateTimeFormat

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class AutoPublishStageSpec extends AsyncSpecBase with RentModelsGen {

  private val flatShowingDao = mock[FlatShowingDao]
  private val ownerRequestDao = mock[OwnerRequestDao]
  private val flatQuestionnaireDao = mock[FlatQuestionnaireDao]

  private val config = new AutoPublishStageConfig(
    unpublishThreshold = 5,
    publishThreshold = 0,
    firstPublishEverywhereDelayInHours = 24,
    daysBeforePossibleCheckInToPublishEverywhere = 14
  )

  private val enabledClassifiedPubStatuses = ClassifiedTypeNamespace.ClassifiedType
    .values()
    .toSeq
    .filterNot(_ == ClassifiedTypeNamespace.ClassifiedType.UNKNOWN)
    .filterNot(_ == ClassifiedTypeNamespace.ClassifiedType.UNRECOGNIZED)
    .map(
      classifiedType =>
        ClassifiedsPubStatusInternal
          .newBuilder()
          .setEnabled(true)
          .setClassifiedType(classifiedType)
          .build()
    )

  private val disabledClassifiedPubStatuses = enabledClassifiedPubStatuses.map(_.toBuilder.setEnabled(false).build())

  private val onlyYandexEnabled = enabledClassifiedPubStatuses
    .map { c =>
      c.toBuilder
        .setEnabled(c.getClassifiedType == ClassifiedTypeNamespace.ClassifiedType.YANDEX_REALTY)
        .build()
    }

  implicit val traced: Traced = Traced.empty

  private def invokeStage(flat: Flat): ProcessingState[Flat] = {
    val state = ProcessingState(flat)
    val stage = new AutoPublishStage(flatShowingDao, ownerRequestDao, flatQuestionnaireDao, config)
    stage.process(state).futureValue
  }

  "AutoPublishStage" should {
    "unpublish from all classifieds if number of showings is less or equal to threshold" in {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
      val pubilshedFlat = flat.copy(
        data = flat.data.toBuilder
          .clearClassifiedsPubStatuses()
          .addAllClassifiedsPubStatuses(enabledClassifiedPubStatuses.asJava)
          .build()
      )
      val ownerRequest = ownerRequestGen.next
      val activeShowings = flatShowingGen
        .next(config.unpublishThreshold)
        .map(
          _.copy(
            ownerRequestId = ownerRequest.ownerRequestId,
            status = FlatShowingStatus.NewShowing // probably not equal to loss or finalized
          )
        )

      handleMocks(pubilshedFlat.flatId, ownerRequest, activeShowings)
      val state = invokeStage(pubilshedFlat)

      state.entry.data.getClassifiedsPubStatusesList.asScala should contain theSameElementsAs
        disabledClassifiedPubStatuses
    }

    "unpublish from all classifieds if exists showing with SentContract, SigningAppointed, SignContract" in {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
      val pubilshedFlat = flat.copy(
        data = flat.data.toBuilder
          .clearClassifiedsPubStatuses()
          .addAllClassifiedsPubStatuses(enabledClassifiedPubStatuses.asJava)
          .build()
      )
      val ownerRequest = ownerRequestGen.next
      val activeShowings = flatShowingGen
        .next(config.unpublishThreshold)
        .map(
          _.copy(
            ownerRequestId = ownerRequest.ownerRequestId,
            status = FlatShowingStatus.SentContract
          )
        )

      handleMocks(pubilshedFlat.flatId, ownerRequest, activeShowings)
      val state = invokeStage(pubilshedFlat)

      state.entry.data.getClassifiedsPubStatusesList.asScala should contain theSameElementsAs
        disabledClassifiedPubStatuses
    }

    "publish to all classifieds if number of showings is greater or equal to threshold" in {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
      val unpublishedFlat = flat.copy(
        data = flat.data.toBuilder
          .clearClassifiedsPubStatuses()
          .addAllClassifiedsPubStatuses(disabledClassifiedPubStatuses.asJava)
          .build()
      )
      val ownerRequest = ownerRequestGen.next
      val nonActiveShowings = flatShowingGen
        .next(config.unpublishThreshold)
        .map(
          _.copy(
            ownerRequestId = ownerRequest.ownerRequestId,
            status = FlatShowingStatus.CloseWithoutRelease
          )
        )

      handleMocks(unpublishedFlat.flatId, ownerRequest, nonActiveShowings)
      val state = invokeStage(unpublishedFlat)

      state.entry.data.getClassifiedsPubStatusesList.asScala should contain theSameElementsAs
        enabledClassifiedPubStatuses
    }

    "publish to yandex realty only if possible check in date is 15 days later" in {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
      val unpublishedFlat = flat.copy(
        data = flat.data.toBuilder
          .clearClassifiedsPubStatuses()
          .addAllClassifiedsPubStatuses(onlyYandexEnabled.asJava)
          .build()
      )
      val ownerRequest = ownerRequestGen.next
      val nonActiveShowings = flatShowingGen
        .next(config.unpublishThreshold - 1)
        .map(
          _.copy(
            ownerRequestId = ownerRequest.ownerRequestId,
            status = FlatShowingStatus.CloseWithoutRelease
          )
        )
      val tmpFlatQuestionnaire = flatQuestionnaireGen.next
      val flatQuestionnaire = tmpFlatQuestionnaire.copy(
        data = tmpFlatQuestionnaire.data.toBuilder
          .setPossibleCheckInDate(DateTimeFormat.write(DateTime.now().plusDays(15)))
          .build()
      )

      handleMocks(unpublishedFlat.flatId, ownerRequest, nonActiveShowings, Some(flatQuestionnaire))
      val state = invokeStage(unpublishedFlat)

      state.entry.data.getClassifiedsPubStatusesList.asScala should contain theSameElementsAs
        onlyYandexEnabled
    }

    "publish to all classifieds if possible check in date is 13 days later" in {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
      val unpublishedFlat = flat.copy(
        data = flat.data.toBuilder
          .clearClassifiedsPubStatuses()
          .addAllClassifiedsPubStatuses(onlyYandexEnabled.asJava)
          .build()
      )
      val ownerRequest = ownerRequestGen.next
      val nonActiveShowings = flatShowingGen
        .next(config.unpublishThreshold - 1)
        .map(
          _.copy(
            ownerRequestId = ownerRequest.ownerRequestId,
            status = FlatShowingStatus.CloseWithoutRelease
          )
        )
      val tmpFlatQuestionnaire = flatQuestionnaireGen.next
      val flatQuestionnaire = tmpFlatQuestionnaire.copy(
        data = tmpFlatQuestionnaire.data.toBuilder
          .setPossibleCheckInDate(DateTimeFormat.write(DateTime.now().plusDays(13)))
          .build()
      )

      handleMocks(unpublishedFlat.flatId, ownerRequest, nonActiveShowings, Some(flatQuestionnaire))
      val state = invokeStage(unpublishedFlat)

      state.entry.data.getClassifiedsPubStatusesList.asScala should contain theSameElementsAs
        enabledClassifiedPubStatuses
    }

    "publish to all classifieds if number of showings is greater or equal to threshold and possible check in date is 13 days later" in {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
      val unpublishedFlat = flat.copy(
        data = flat.data.toBuilder
          .clearClassifiedsPubStatuses()
          .addAllClassifiedsPubStatuses(onlyYandexEnabled.asJava)
          .build()
      )
      val ownerRequest = ownerRequestGen.next
      val nonActiveShowings = flatShowingGen
        .next(config.unpublishThreshold)
        .map(
          _.copy(
            ownerRequestId = ownerRequest.ownerRequestId,
            status = FlatShowingStatus.CloseWithoutRelease
          )
        )
      val tmpFlatQuestionnaire = flatQuestionnaireGen.next
      val flatQuestionnaire = tmpFlatQuestionnaire.copy(
        data = tmpFlatQuestionnaire.data.toBuilder
          .setPossibleCheckInDate(DateTimeFormat.write(DateTime.now().plusDays(13)))
          .build()
      )

      handleMocks(unpublishedFlat.flatId, ownerRequest, nonActiveShowings, Some(flatQuestionnaire))
      val state = invokeStage(unpublishedFlat)

      state.entry.data.getClassifiedsPubStatusesList.asScala should contain theSameElementsAs
        enabledClassifiedPubStatuses
    }

    "NOT publish to all classifieds if number of all lost showings is less than unpublish-threshold" in {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
      val unpublishedFlat = flat.copy(
        data = flat.data.toBuilder
          .clearClassifiedsPubStatuses()
          .addAllClassifiedsPubStatuses(disabledClassifiedPubStatuses.asJava)
          .build()
      )
      val ownerRequest = ownerRequestGen.next
      val nonActiveShowings = flatShowingGen
        .next(config.unpublishThreshold - 1)
        .map(
          _.copy(
            ownerRequestId = ownerRequest.ownerRequestId,
            status = FlatShowingStatus.CloseWithoutRelease
          )
        )

      handleMocks(unpublishedFlat.flatId, ownerRequest, nonActiveShowings)
      val state = invokeStage(unpublishedFlat)

      state.entry.data.getClassifiedsPubStatusesList.asScala should contain theSameElementsAs
        disabledClassifiedPubStatuses
    }
  }

  private def handleMocks(
    flatId: String,
    ownerRequest: OwnerRequest,
    flatShowings: Iterable[FlatShowing],
    flatQuestionnaire: Option[FlatQuestionnaire] = None
  ): Unit = {
    (ownerRequestDao
      .findLastByFlatId(_: String)(_: Traced))
      .expects(flatId, *)
      .returning(Future.successful(Some(ownerRequest)))

    (flatShowingDao
      .findActiveByOwnerRequest(_: String, _: Boolean)(_: Traced))
      .expects(ownerRequest.ownerRequestId, *, *)
      .returning(Future.successful(flatShowings.toSeq))

    (flatQuestionnaireDao
      .findByFlatId(_: String)(_: Traced))
      .expects(flatId, *)
      .returning(Future.successful(flatQuestionnaire))
  }
}
