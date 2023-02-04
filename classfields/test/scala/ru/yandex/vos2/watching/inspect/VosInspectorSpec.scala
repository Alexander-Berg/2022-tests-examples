package ru.yandex.vos2.watching.inspect

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.offer.IndexerErrors
import ru.yandex.vertis.vos2.model.realty.RealtyOffer.RealtyPropertyType
import ru.yandex.vos2.BasicsModel.CompositeStatus._
import ru.yandex.vos2.BasicsModel.TrustLevel._
import ru.yandex.vos2.OfferModel.IDXStatus
import ru.yandex.vos2.OfferModel.IDXStatus.NoteType._
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.watching.stages.inspect.{Verdict, VosInspector}

import scala.collection.JavaConverters._

/**
  * @author Nataila Ratskevich (reimai@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class VosInspectorSpec extends WordSpec with Matchers {

  val inspector = new VosInspector

  "Inspector" should {
    "ban for errors" in {
      val offer = TestUtils
        .createOffer()
        .setIDXStatus(genStatus(VALID_CODE → ERROR))
        .build()
      val verdict = inspector.inspect(offer)
      assert(verdict.resolution.level.contains(TL_ZERO))
    }

    "not ban unpaid realty" in {
      val offer = TestUtils
        .createOffer()
        .setIDXStatus(genStatus(IndexerErrors.NO_PLACEMENT_VALUE → ERROR))
        .build()
      assertNotBanned(inspector.inspect(offer))
    }

    "not ban unpaid suspicious realty" in {
      val offer = TestUtils
        .createOffer()
        .setIDXStatus(genStatus(IndexerErrors.NO_PLACEMENT_VALUE → ERROR, VALID_CODE → SUSPICIOUS))
        .clearStatusHistory()
        .build()
      val verdict = inspector.inspect(offer)
      assertNotBanned(verdict)
      val inspected = verdict.applyAndRecord(offer)
      assert(inspected.getStatusHistoryList.asScala.last.getOfferStatus == CS_SUSPICIOUS)
    }

    "not ban unpaid suspicious realty with ignored code" in {
      val offer = TestUtils
        .createOffer()
        .setIDXStatus(genStatus(IndexerErrors.NO_PLACEMENT_VALUE → ERROR, IGNORED_CODE → SUSPICIOUS))
        .build()
      val verdict = inspector.inspect(offer)
      assertNotBanned(verdict)
      val inspected = verdict.applyAndRecord(offer)
      assert(inspected.getStatusHistoryList.asScala.last.getOfferStatus == CS_ACTIVE)
    }

    "not ban unpaid commercial realty" in {
      val builder = TestUtils
        .createOffer()
        .setIDXStatus(genStatus(IndexerErrors.NO_PLACEMENT_VALUE → ERROR, VALID_CODE → SUSPICIOUS))
      builder.getOfferRealtyBuilder.setPropertyType(RealtyPropertyType.PT_COMMERCIAL)
      val offer = builder.build()
      val verdict = inspector.inspect(offer)
      assertNotBanned(verdict)
    }
  }

  def assertNotBanned(verdict: Verdict): Unit = {
    assert(verdict.resolution.level.isDefined)
    assert(!verdict.resolution.level.contains(TL_ZERO))
  }

  val IGNORED_CODE = 3
  val VALID_CODE = 4

  def genStatus(notes: (Int, IDXStatus.NoteType)*): IDXStatus = {
    val status = IDXStatus.newBuilder()
    notes.foreach { case (code, noteType) ⇒ status.addNoteBuilder().setNoteCode(code).setNoteType(noteType) }
    status.setCorrect(status.findLastIdxError.isEmpty).build()
  }
}
