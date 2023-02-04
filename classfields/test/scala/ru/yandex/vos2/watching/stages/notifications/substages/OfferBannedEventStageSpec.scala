package ru.yandex.vos2.watching.stages.notifications.substages

import org.joda.time.DateTime
import org.scalacheck.Arbitrary
import org.scalatest.prop.{Checkers, PropertyChecks}
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.mockito.MockitoSupport
import org.mockito.Mockito.{times, verify, when}
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferID
import ru.yandex.vos2.OfferModel.IDXStatus.NoteType
import ru.yandex.vos2.OfferModel.{IDXStatus, IDXStatusOrBuilder, Offer, OfferStatusHistoryItem, OfferStatusSource}
import ru.yandex.vos2.features.SimpleFeatures
import ru.yandex.vos2.realty.model.offer.RealtyOfferGenerator
import ru.yandex.vos2.services.idx.sync.{IdxClient, Ok}
import ru.yandex.vos2.watching.stages.notifications.EventProcessingState
import ru.yandex.vos2.watching.stages.notifications.substages.OfferBannedEventStage._
import OfferBannedEventStageSpec.RichDt
import ru.yandex.vos2.realty.features.RealtyFeatures
import ru.yandex.vos2.watching.stages.notifications.substages.OfferBannedEventStageSpec.RichHistoryOffer

import scala.concurrent.Future

class OfferBannedEventStageSpec
  extends WordSpecLike
  with Matchers
  with Checkers
  with MockitoSupport
  with PropertyChecks {

  implicit val arbOffer = Arbitrary(RealtyOfferGenerator.offerGen())
  val features = new SimpleFeatures with RealtyFeatures
  features.SendBanOfferNotifications.setNewState(true)

  "no ban message history" should {
    "dont send email with NotFoundStatus" in {
      forAll(RealtyOfferGenerator.offerGen()) { offer: Offer ⇒
        val idxClient = mock[IdxClient[OfferID]]
        val stage = new OfferBannedEventStage(idxClient, features)
        val dt = new DateTime()
        val o = offer.toBuilder
          .addModeration(dt.plus1SecEachCall)
          .addActive(dt.plus1SecEachCall)

        val verdict = OfferBannedEventStage.getVerdict(o.build())
        verdict shouldBe NotFoundStatus

        stage.process(o.toState)
        verify(idxClient, times(0)).submit(any(), any())
      }
    }
  }

  "ban message history" should {
    "send email once with SendNewBanReason" in {
      forAll(RealtyOfferGenerator.offerGen(), minSuccessful(1)) { offer: Offer ⇒
        val idxClient = mock[IdxClient[OfferID]]
        val stage = new OfferBannedEventStage(idxClient, features)
        val dt = new DateTime()
        val o = offer.toBuilder
          .addModeration(dt.plus1SecEachCall)
          .addActive(dt.plus1SecEachCall)
          .addBan(dt.plus1SecEachCall, OfferStatusSource.OSS_IDX, 82)

        val verdict = OfferBannedEventStage.getVerdict(o.build())
        verdict shouldBe SendNewBanReason

        when(idxClient.submit(any(), any())).thenReturn(Future.successful(Ok))
        stage.process(o.toState)
        verify(idxClient, times(1)).submit(any(), any())
      }
    }
  }

  "ban message history, but user was edit offer (ban stay)" should {
    "don't send email with DontSendUserEdited" in {
      forAll(RealtyOfferGenerator.offerGen(), minSuccessful(1)) { offer: Offer ⇒
        val idxClient = mock[IdxClient[OfferID]]
        val stage = new OfferBannedEventStage(idxClient, features)
        val dt = new DateTime()
        val o = offer.toBuilder
          .addModeration(dt.plus1SecEachCall)
          .addActive(dt.plus1SecEachCall)
          .addBan(dt.plus1SecEachCall, OfferStatusSource.OSS_IDX, 82)
          .addModeration(dt.plus1SecEachCall)
          .addActive(dt.plus1SecEachCall)
          .addBan(dt.plus1SecEachCall, OfferStatusSource.OSS_IDX, 82)

        val verdict = OfferBannedEventStage.getVerdict(o.build())
        verdict shouldBe DontSendUserEdited

        stage.process(o.toState)
        verify(idxClient, times(0)).submit(any(), any())
      }
    }
  }

  "ban message history, but user was banned too" should {
    "don't send email with DontSendSameBanReason" in {
      forAll(RealtyOfferGenerator.offerGen(), minSuccessful(1)) { offer: Offer ⇒
        val idxClient = mock[IdxClient[OfferID]]
        val stage = new OfferBannedEventStage(idxClient, features)
        val dt = new DateTime()
        val o = offer.toBuilder
          .addModeration(dt.plus1SecEachCall)
          .addActive(dt.plus1SecEachCall)
          .addBan(dt.plus1SecEachCall, OfferStatusSource.OSS_USER, 82)
          .addBan(dt.plus1SecEachCall, OfferStatusSource.OSS_IDX, 82) // inherit from user

        val verdict = OfferBannedEventStage.getVerdict(o.build())
        verdict shouldBe DontSendSameBanReason

        when(idxClient.submit(any(), any())).thenReturn(Future.successful(Ok))
        stage.process(o.toState)
        verify(idxClient, times(0)).submit(any(), any())
      }
    }
  }
}

object OfferBannedEventStageSpec {

  implicit class RichDt(dt: DateTime) {
    var prevSeconds = 0
    private def getPrevSeconds = {
      prevSeconds += 1
      prevSeconds
    }
    def plus1SecEachCall = dt.plusSeconds(getPrevSeconds).getMillis
  }

  implicit class RichHistoryOffer(o: Offer.Builder) {

    def toState() = EventProcessingState.apply(o.build())

    def addBan(dt: Long, oss: OfferStatusSource, idxError: Int) =
      o.setIDXStatus(
          IDXStatus
            .newBuilder()
            .addNote(
              IDXStatus.Note.newBuilder().setNoteType(NoteType.ERROR).setNoteCode(idxError).build()
            )
            .build()
        )
        .addStatusHistory(buildHistoryItem(dt, CompositeStatus.CS_BANNED, Some(oss), Some(idxError)))

    def addActive(dt: Long) =
      o.addStatusHistory(buildHistoryItem(dt, CompositeStatus.CS_ACTIVE, Some(OfferStatusSource.OSS_USER)))

    def addModeration(dt: Long) =
      o.addStatusHistory(buildHistoryItem(dt, CompositeStatus.CS_MODERATION))

    def buildHistoryItem(
      dt: Long,
      cs: CompositeStatus,
      oss: Option[OfferStatusSource] = None,
      idxError: Option[Int] = None
    ) = {
      val b = OfferStatusHistoryItem
        .newBuilder()
      b.setTimestamp(dt)
        .setOfferStatus(cs)
        .build()
      oss.foreach { x =>
        b.setSource(x)
      }
      idxError.foreach { x =>
        b.setIdxCode(x)
      }
      b
    }
  }
}
