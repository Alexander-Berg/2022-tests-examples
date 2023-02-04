package ru.yandex.vertis.billing.model_core.proto

import com.google.protobuf.Message
import org.scalacheck.Gen
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.gens.{
  extendedCampaignCallFactGen,
  CallCenterCallGen,
  OfferIdGen,
  ProductGen,
  ProductWithDurationGen
}
import ru.yandex.vertis.billing.model_core.{Product, TeleponyCallFact}
import ru.yandex.vertis.billing.{DefaultPropertyChecks, Model}

import scala.annotation.nowarn

class ConversionsSpec extends AnyWordSpec with Matchers with DefaultPropertyChecks {

  "ConversionsSpec" should {
    "convert product" in {
      checkConversion[Product, Model.Product](
        ProductGen,
        Conversions.toMessage,
        Conversions.productFromMessage(_).get
      )
    }

    "convert finite product" in {
      checkConversion[Product, Model.Product](
        ProductWithDurationGen,
        Conversions.toMessage,
        Conversions.productFromMessage(_).get
      )
    }

    "convert to BilledCallFact" in {
      forAll(extendedCampaignCallFactGen(), Gen.option(OfferIdGen), Gen.option(CallCenterCallGen)) {
        (ecf, offerId, callCenterCall) =>
          val msg = Conversions.toMessage(ecf, offerId, callCenterCall)

          val fact = ecf.fact.call.asInstanceOf[TeleponyCallFact]
          fact.tag.foreach(t => msg.getTag shouldBe t)
          fact.recordId.foreach(t => msg.getRecordId shouldBe t)
          msg.getResult shouldBe fact.result.toString
          ecf.fact.complaint.foreach(c => Conversions.callComplaintFromMessage(msg.getComplaint) shouldBe c)
          msg.getIncoming shouldBe fact.incoming
          fact.redirect.foreach(redirect => msg.getRedirect shouldBe redirect.value)
          msg.getRedirectId shouldBe fact.redirectId.orNull
          msg.getInternal shouldBe fact.internal.value
          msg.getDuration shouldBe fact.duration.toSeconds
          msg.getWaitDuration shouldBe fact.waitDuration.toSeconds
          msg.getObjectId shouldBe fact.objectId
          msg.getStatus shouldBe ecf.status.toString
          msg.getResolution shouldBe Conversions.toMessage(ecf.fact.resolutions)
          msg.getDetailedStatus shouldBe ecf.detailedStatus.toString
          offerId.foreach(id => Conversions.offerIdFromMessage(msg.getServiceObject) shouldBe id)
          fact.callbackOrderId.foreach(callbackOrderId => msg.getCallbackOrderId shouldBe callbackOrderId)
          callCenterCall.foreach { info =>
            val callCenterCallInfo = msg.getCallCenterInfo
            callCenterCallInfo.getCallCenterName shouldBe info.callCenterName.toString
            callCenterCallInfo.getCallCenterCallId shouldBe info.callCenterCallId
            info.callCenterCampaignId.foreach { id =>
              callCenterCallInfo.getCallCenterCampaignId shouldBe id
            }
          }
      }
    }
  }

  private def checkConversion[A, M <: Message](gen: Gen[A], toProto: A => M, fromProto: M => A): Assertion =
    forAll(gen) { m =>
      val proto = toProto(m)
      val model = fromProto(proto)
      model shouldBe m
    }

}
