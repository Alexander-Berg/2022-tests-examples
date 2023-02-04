package ru.yandex.realty.rent.stage.flat

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.dao.OwnerRequestDao
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{Flat, OwnerRequest}
import ru.yandex.realty.rent.proto.model.owner_request.OwnerRequestData
import ru.yandex.realty.rent.proto.model.sms.confirmation.SmsConfirmation
import ru.yandex.realty.rent.proto.model.sms.confirmation.SmsConfirmation.Status
import ru.yandex.realty.rent.proto.model.sms.confirmation.SmsConfirmation.Status._
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.vertis.protobuf.BasicProtoFormats._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class CleanExpiredConfirmationsStageSpec extends AsyncSpecBase with RentModelsGen {

  private val ownerRequestDao = mock[OwnerRequestDao]
  implicit val traced: Traced = Traced.empty

  private def invokeStage(flat: Flat): ProcessingState[Flat] = {
    val state = ProcessingState(flat)
    val stage = new CleanExpiredConfirmationsStage(ownerRequestDao)
    stage.process(state).futureValue
  }

  "CleanExpiredConfirmationsStage" should {
    "clean few expired confirmations with revisit" in {
      val flat = flatGen().next
      val earliestExpiredDate = DateTimeUtil.now().minus(3.hour.toMillis)
      val ownerRequests =
        Seq(buildOwnerRequest(flat.flatId, earliestExpiredDate), buildOwnerRequest(flat.flatId, earliestExpiredDate))
      handleMocks(flat.flatId, ownerRequests)
      val state = invokeStage(flat)
      state.entry.ownerRequests.size shouldBe ownerRequests.size
      state.entry.ownerRequests
        .foreach(
          ownerRequest =>
            ownerRequest.getSmsConfirmations.exists(
              c => DateTimeFormat.read(c.getExpirationTime).plusHours(4).isBeforeNow
            ) shouldBe false
        )
      state.entry.visitTime.isDefined shouldBe true
      state.entry.visitTime.get shouldBe earliestExpiredDate.plus(4.hour.toMillis)
    }

    "clean few expired confirmations without revisit" in {
      val flat = flatGen().next
      val ownerRequests = Seq(buildOwnerRequest(flat.flatId, hasValid = false))
      handleMocks(flat.flatId, ownerRequests)
      val state = invokeStage(flat)
      state.entry.ownerRequests.size shouldBe ownerRequests.size
      state.entry.ownerRequests
        .foreach(ownerRequest => ownerRequest.data.getSmsConfirmationsCount shouldBe 0)
      state.entry.visitTime.isDefined shouldBe false
    }
  }

  private def handleMocks(
    flatId: String,
    validOwnerRequest: Seq[OwnerRequest]
  ): Unit = {
    (ownerRequestDao
      .findAllByFlatId(_: String)(_: Traced))
      .expects(flatId, *)
      .atLeastOnce()
      .returning(Future.successful(validOwnerRequest))
  }

  private def buildOwnerRequest(
    flatId: String,
    earliestExpiredDate: DateTime = DateTimeUtil.now(),
    hasValid: Boolean = true
  ): OwnerRequest = {
    val validConfirmations = if (hasValid) {
      Seq(
        buildSmsConfirmations(status = EXPIRED),
        buildSmsConfirmations(
          earliestExpiredDate.minus(30.minutes.toMillis),
          earliestExpiredDate,
          WAITING_FOR_CONFIRMATION
        ),
        buildSmsConfirmations(status = CONFIRMED)
      )
    } else Seq.empty
    val smsConfirmations = validConfirmations ++ Seq(
      buildSmsConfirmations(
        DateTimeUtil.now().minus(6.hour.toMillis),
        DateTimeUtil.now().minus(5.hour.toMillis),
        EXPIRED
      ),
      buildSmsConfirmations(
        DateTimeUtil.now().minus(6.hour.toMillis),
        DateTimeUtil.now().minus(5.hour.toMillis),
        WAITING_FOR_CONFIRMATION
      )
    )
    ownerRequestGen.next
      .copy(
        flatId = flatId,
        data = OwnerRequestData
          .newBuilder()
          .addAllSmsConfirmations(smsConfirmations.asJava)
          .build()
      )
  }

  private def buildSmsConfirmations(
    initiation: DateTime = DateTimeUtil.now(),
    expiration: DateTime = DateTimeUtil.now().plus(30.minutes.toMillis),
    status: Status
  ): SmsConfirmation = {
    SmsConfirmation
      .newBuilder()
      .setRequestId(readableString.next)
      .setConfirmationCode(posNum[Int].next.toString)
      .setInitiationTime(DateTimeFormat.write(initiation))
      .setExpirationTime(DateTimeFormat.write(expiration))
      .setStatus(status)
      .build()
  }
}
