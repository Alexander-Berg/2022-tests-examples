package ru.yandex.vos2.watching.stages

import java.lang.System.currentTimeMillis
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vos2.BasicsModel.TrustLevel
import ru.yandex.vos2.OfferModel.IDXStatus.{Note, NoteType}
import ru.yandex.vos2.OfferModel._
import ru.yandex.vos2._
import ru.yandex.vos2.features.SimpleFeatures
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder
import ru.yandex.vos2.proto.ProtoMacro
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.services.idx.sync.IdxResponse
import ru.yandex.vos2.services.mds.{MdsPhotoUtils, RealtyMdsSettings}
import ru.yandex.vos2.util.StageUtils
import ru.yandex.vos2.watching.ProcessingState
import ru.yandex.vos2.watching.stages.IdxStage.InactiveInterval
import ru.yandex.vos2.watching.utils.ProbeIdxClient
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.vos2.realty.features.RealtyFeatures

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author Ilya Gerasimov (747mmhg@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class IdxStageSpec extends WordSpec with Matchers with StageUtils {

  private val realtyMdsUtils = MdsPhotoUtils("writeUrl", "readUrl", RealtyMdsSettings)
  private val realtyMdsUrlBuilder = new MdsUrlBuilder("//avatars.mdst.yandex.net")
  private val defaultFeatures = new SimpleFeatures() with RealtyFeatures

  private def createStage(client: ProbeIdxClient, features: RealtyFeatures = defaultFeatures): IdxStage = {
    new IdxStage(client, features, realtyMdsUtils, realtyMdsUrlBuilder, None) {

      override def resendProbability: Double = 0
    }
  }

  private def withStage(f: (IdxStage, ProbeIdxClient) ⇒ Unit): Unit = {
    val client = new ProbeIdxClient
    val stage = createStage(client)
    f(stage, client)
  }

  private def process(stage: IdxStage, offer: Offer) = {
    val state = ProcessingState(offer, offer)
    asyncProcess(stage, state)
  }

  "IdxStage" should {

    "send good offers to index as visible" in {
      withStage { (stage, client) ⇒
        process(stage, createEligible().build())
        assert(client.sentRequest != null)
        assert(ProbeIdxClient.hasPublishedShowStatus(client.sentRequest))
        assert(!ProbeIdxClient.hasHiddenReason(client.sentRequest))
      }
    }

    "send bad offers to index as hidden" in {
      withStage { (stage, client) ⇒
        process(stage, createNotEligible().setHashIDX("some").build())
        assert(client.sentRequest != null)
        assert(ProbeIdxClient.hasHiddenReason(client.sentRequest))
        assert(!ProbeIdxClient.hasPublishedShowStatus(client.sentRequest))
      }
    }

    "re-schedule failed submit" in {
      val client = new ProbeIdxClient {
        override def submit(offerId: OfferID, bytes: Array[Byte]): Future[IdxResponse] =
          Future.failed(new IllegalArgumentException)
      }
      val stage = createStage(client)
      val result = process(stage, createEligible().build())
      assert(result.delay.isFinite())
    }

    "send deleted offer" in {
      withStage { (stage, client) ⇒
        val builder = createEligible().addFlag(OfferFlag.OF_DELETED).setHashIDX("some")
        process(stage, builder.build())
        assert(client.sentRequest != null)
        assert(!ProbeIdxClient.hasPublishedShowStatus(client.sentRequest))
      }
    }

    "ignore IDX errors when building request" in {
      withStage { (stage, client) ⇒
        val builder = createEligible()
        val note = createNote(NoteType.ERROR)
        builder.getIDXStatusBuilder.addNote(note)
        process(stage, builder.build())
        assert(client.sentRequest != null)
        assert(!ProbeIdxClient.hasHiddenReason(client.sentRequest))
      }
    }

    "send offer expired not too long ago" in {
      withStage { (stage, client) ⇒
        val builder = createEligible()
        builder
          .putFlag(OfferFlag.OF_EXPIRED)
          .setTimestampWillExpire(System.currentTimeMillis() - InactiveInterval / 2)
        process(stage, builder.build())
        assert(client.sentRequest != null)
        assert(ProbeIdxClient.hasHiddenReason(client.sentRequest))
      }
    }

    "send offer expired too long ago" in {
      withStage { (stage, client) ⇒
        val builder = createEligible()
        builder
          .putFlag(OfferFlag.OF_EXPIRED)
          .setTimestampWillExpire(System.currentTimeMillis() - InactiveInterval)
          .setHashIDX("some")
        process(stage, builder.build())
        assert(client.sentRequest != null)
        assert(!ProbeIdxClient.hasPublishedShowStatus(client.sentRequest))
      }
    }

    "resend offer on idx generation inc" in {
      val features = new SimpleFeatures with RealtyFeatures
      val client = new ProbeIdxClient
      val stage = createStage(client, features)
      val offer = createEligible().build()

      //send
      val sentOffer = process(stage, offer).offer
      val builder = sentOffer.toBuilder
      builder.getIDXStatusBuilder.setHash(client.sentRequest.getCreate.getOfferHash.toInt)
      val ackedOffer = builder.build()
      assert(client.sentRequest != null)
      assert(ackedOffer.hasHashIDX)
      assert(ackedOffer.hasGenerationIDX)
      assert(ackedOffer.getGenerationIDX == features.Indexing.generation)

      //resent same
      client.clean()
      process(stage, ackedOffer).offer
      assert(client.sentRequest == null)

      //resend inc
      client.clean()
      features.Indexing.setGeneration(features.Indexing.generation + 1)
      val resentOffer = process(stage, ackedOffer).offer
      assert(client.sentRequest != null)
      assert(resentOffer.hasHashIDX)
      assert(resentOffer.hasGenerationIDX)
      assert(resentOffer.getHashIDX == ackedOffer.getHashIDX)
      assert(resentOffer.getGenerationIDX == features.Indexing.generation)

      //resend dec
      client.clean()
      features.Indexing.setGeneration(features.Indexing.generation - 1)
      process(stage, resentOffer).offer
      assert(client.sentRequest == null)
    }

    "re-send offers until acknowledged by Indexer" in {
      withStage { (stage, client) ⇒
        //send first time
        val offer = createEligible().build()
        val sentOffer = process(stage, offer).offer
        val request = client.sentRequest
        assert(request != null)
        assert(sentOffer.hasHashIDX)
        assert(!ProtoMacro.opt(sentOffer.getIDXStatus).exists(_.hasHash))
        //send second time
        val sentOffer2 = process(stage, sentOffer).offer
        assert(client.sentRequest == request)
        assert(sentOffer.getHashIDX == sentOffer2.getHashIDX)
        // imitate acknowledgment
        val builder = sentOffer2.toBuilder
        builder.getIDXStatusBuilder.setHash(client.sentRequest.getCreate.getOfferHash.toInt)
        val ackedOffer = builder.build()
        client.clean()
        process(stage, ackedOffer).offer
        assert(client.sentRequest == null)
      }
    }

    "send changed unbind offer in index" in {
      withStage { (stage, client) ⇒
        val builder = createEligible().addFlag(OfferFlag.OF_DELETED).setHashIDX("some")
        builder.getOfferRealtyBuilder.getUnifiedAddressBuilder.setRgid(RevokeStage.MOSCOW)
        builder.getOfferRealtyBuilder.setTimestampRevoke(currentTimeMillis() - 1.minute.toMillis)
        process(stage, builder.build())
        assert(client.sentRequest != null)
        assert(!client.removed)
      }
    }

    "remove unbind offer when unbind interval time exceeded" in {
      withStage { (stage, client) ⇒
        val builder = createEligible().addFlag(OfferFlag.OF_DELETED).setHashIDX("some")
        builder.getOfferRealtyBuilder.getUnifiedAddressBuilder.setRgid(RevokeStage.MOSCOW)
        builder.getOfferRealtyBuilder.setTimestampRevoke(currentTimeMillis() - 8.days.toMillis)
        process(stage, builder.build())
        assert(client.sentRequest != null)
        assert(!ProbeIdxClient.hasPublishedShowStatus(client.sentRequest))
      }

    }

  }

  def createNote(noteType: NoteType): Note = {
    IDXStatus.Note
      .newBuilder()
      .setNoteCode(177)
      .setNoteType(noteType)
      .build()
  }

  private def createEligible(): Offer.Builder = {
    TestUtils
      .createOffer()
      .clearFlag(OfferFlag.OF_EXPIRED)
  }

  private def createNotEligible(): Offer.Builder = {
    val check = ManualCheck
      .newBuilder()
      .setTrustLevel(TrustLevel.TL_ZERO)
      .setTimestamp(System.currentTimeMillis())
      .setOfferVer(0)
      .build()
    TestUtils
      .createOffer()
      .addManualCheck(check)
  }
}
