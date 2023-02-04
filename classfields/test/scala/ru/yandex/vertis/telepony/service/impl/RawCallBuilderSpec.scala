package ru.yandex.vertis.telepony.service.impl

import org.joda.time.DateTime
import ru.yandex.vertis.telepony.Specbase
import ru.yandex.vertis.telepony.component.impl.PhoneMappingHelper
import ru.yandex.vertis.telepony.generator.Generator.PhoneGen
import ru.yandex.vertis.telepony.model.Action.{BlockAction, NoAction}
import ru.yandex.vertis.telepony.model.Event._
import ru.yandex.vertis.telepony.model.RawCall.Origins
import ru.yandex.vertis.telepony.model.{EventAction, Operators, _}
import ru.yandex.vertis.telepony.service.tskv.FallbackCallLog
import ru.yandex.vertis.telepony.util.Component

import scala.concurrent.duration._

/**
  * Describes expected behaviour of raw call builder.
  * Text formatted behaviour could be found in task VSINFR-1853
  *
  * @author evans
  */
class RawCallBuilderSpec extends Specbase {

  private val callId = "1234"
  private val source = Some(RefinedSource.from("1234"))
  private val phone = Phone("+79312320032")
  private val target = Phone("+79312330032")
  private val startTime = DateTime.parse("2016-08-23T16:26:00+03:00")
  private val duration = 2.minutes
  private val talkDuration = 1.minutes
  private val recordedTalkDuration = 0.5.minutes
  private val callEndTime = startTime.plusSeconds(duration.toSeconds.toInt)
  private val transferTime = startTime.plusSeconds(1)
  private val answerTime = callEndTime.minusSeconds(talkDuration.toSeconds.toInt)
  private val recordEndTime = callEndTime
  private val recordStartTime = recordEndTime.minusSeconds(recordedTalkDuration.toSeconds.toInt)

  private val fallbackCallLog: FallbackCallLog = new FallbackCallLog {
    override def domain: Domain = "autoru_def"

    override def component: Component = new Component {
      override def name: String = "test"
      override def hostName: String = "test"
      override def formatName: String = "test"
    }
  }

  val mtsBuilderWithRecord =
    new RawCallBuilderImpl[MtsCallEventAction](
      PhoneMappingHelper.phoneMappingService,
      fallbackCallLog,
      Operators.Mts
    )

  val voxBuilderWithRecord =
    new RawCallBuilderImpl[VoxCallEventAction](
      PhoneMappingHelper.phoneMappingService,
      fallbackCallLog,
      Operators.Vox
    )

  val acceptedEvent = CallAcceptedEvent(
    callId,
    startTime,
    source,
    phone
  )

  val goToTransferEvent = GoToTransferEvent(
    callId,
    startTime,
    source,
    phone,
    target
  )

  val transferEvent = TransferEvent(
    callId,
    transferTime,
    source,
    phone,
    target,
    TransferEventType.ValidCall.Success("1280")
  )

  val answeredEvent = CallAnsweredEvent(
    callId,
    answerTime,
    source,
    phone
  )

  val recordStartEvent = RecordStartEvent(
    callId,
    recordStartTime,
    source,
    phone,
    None
  )

  val recordStopEvent = RecordStopEvent(
    callId,
    recordEndTime,
    source,
    phone
  )

  val endEvent = CallEndEvent(
    callId,
    callEndTime,
    source,
    phone,
    None
  )

  val endEventDefaultResult = CallEndEvent(
    callId,
    callEndTime,
    source,
    phone,
    Some(EndCallEventResults.Default)
  )

  val endEventNoDNnResult = CallEndEvent(
    callId,
    callEndTime,
    source,
    phone,
    Some(EndCallEventResults.NoDn)
  )

  def asWrappedMtsEvent(e: Event): MtsCallEventAction = MtsCallEventAction(e, NoAction(e.externalId))

  def asWrappedVoxEvent(e: Event): VoxCallEventAction = VoxCallEventAction(e, NoAction(e.externalId))

  private case class BuilderAndEventActionFunc[E <: EventAction](
      operator: Operators.Value,
      buildRawCall: Function[Seq[Event], Option[RawCall]])

  private val operatorRawCallFuncSeq: Seq[BuilderAndEventActionFunc[EventAction]] =
    Seq(
      BuilderAndEventActionFunc(Operators.Mts, events => mtsBuilderWithRecord.build(events.map(asWrappedMtsEvent))),
      BuilderAndEventActionFunc(Operators.Vox, events => voxBuilderWithRecord.build(events.map(asWrappedVoxEvent)))
    )

  "Common call builder" should {
    "build call ignoring order" in {
      val events = Seq(
        goToTransferEvent,
        endEvent,
        acceptedEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val expected = Some(
          RawCall(
            callId,
            source,
            phone,
            Some(target),
            startTime,
            duration,
            0.minutes,
            None,
            CallResults.StopCaller,
            origin = Origins.Online,
            operator = op.operator
          )
        )

        val actual = op.buildRawCall(events)
        actual shouldEqual expected
      }
    }

    "build call with record and abc phone" in {
      val phone = Phone("+78129251490")
      val events = Seq(
        acceptedEvent.copy(proxyNumber = phone),
        answeredEvent.copy(proxyNumber = phone),
        goToTransferEvent.copy(proxyNumber = phone),
        transferEvent.copy(proxyNumber = phone),
        recordStartEvent.copy(proxyNumber = phone),
        recordStopEvent.copy(proxyNumber = phone),
        endEvent.copy(proxyNumber = phone)
      )

      operatorRawCallFuncSeq.foreach { op =>
        val expected = Some(
          RawCall(
            callId,
            source,
            phone,
            Some(target),
            startTime,
            2.minute,
            recordedTalkDuration,
            Some("https://aa.mts.ru/api/v4/record?phone=9119251490&id=1234"),
            CallResults.Success,
            origin = Origins.Online,
            operator = op.operator
          )
        )

        val actual = op.buildRawCall(events)
        actual shouldEqual expected
      }
    }

    "nothing build for random events" in {
      val events = Seq(
        recordStartEvent,
        acceptedEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual shouldEqual None
      }
    }
  }

  "Call builder" should {
    "build call success call with record" in {
      val events = Seq(
        acceptedEvent,
        answeredEvent,
        goToTransferEvent,
        transferEvent,
        recordStartEvent,
        recordStopEvent,
        endEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val expected = Some(
          RawCall(
            callId,
            source,
            phone,
            Some(target),
            startTime,
            duration,
            recordedTalkDuration,
            Some("https://aa.mts.ru/api/v4/record?phone=9312320032&id=1234"),
            CallResults.Success,
            origin = Origins.Online,
            operator = op.operator
          )
        )

        val actual = op.buildRawCall(events)
        actual shouldEqual expected
      }
    }

    "build call success call from minimum event set" in {
      val events = Seq(
        acceptedEvent,
        transferEvent,
        recordStartEvent,
        endEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val expected = Some(
          RawCall(
            callId,
            source,
            phone,
            Some(target),
            startTime,
            duration,
            recordedTalkDuration,
            Some("https://aa.mts.ru/api/v4/record?phone=9312320032&id=1234"),
            CallResults.Success,
            origin = Origins.Online,
            operator = op.operator
          )
        )

        val actual = op.buildRawCall(events)
        actual shouldEqual expected
      }
    }

    "build call stopped by caller" in {
      val events = Seq(
        acceptedEvent,
        goToTransferEvent,
        endEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val expected = Some(
          RawCall(
            callId,
            source,
            phone,
            Some(target),
            startTime,
            duration,
            0.minutes,
            None,
            CallResults.StopCaller,
            origin = Origins.Online,
            operator = op.operator
          )
        )
        val actual = op.buildRawCall(events)
        actual shouldEqual expected
      }
    }

    "build call for busy with only accepted and end events" in {
      val events = Seq(
        endEventDefaultResult,
        acceptedEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.BusyCallee
      }
    }

    "build call for no redirect" in {
      val events = Seq(
        endEventNoDNnResult,
        acceptedEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.NoRedirect
      }
    }

    "build call for unknown" in {
      val events = Seq(
        endEvent,
        acceptedEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.Unknown
      }
    }

    "build call for no answer" in {
      val events = Seq(
        acceptedEvent,
        goToTransferEvent,
        transferEvent.copy(transferEventType = TransferEventType.ValidCall.NoAnswer("1284")),
        endEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.NoAnswer
      }
    }

    "build call for unavailable callee" in {
      val events = Seq(
        acceptedEvent,
        goToTransferEvent,
        transferEvent.copy(transferEventType = TransferEventType.ValidCall.UnavailableCallee("480")),
        endEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.UnavailableCallee
      }
    }

    "build blocked call" in {
      val events = Seq(
        acceptedEvent,
        goToTransferEvent,
        transferEvent.copy(transferEventType = TransferEventType.ValidCall.UnavailableCallee("480")),
        endEvent
      )

      val mtsActual = mtsBuilderWithRecord.build(events.map(asWrappedMtsEvent).map {
        case MtsCallEventAction(e: CallAcceptedEvent, _) =>
          MtsCallEventAction(e, BlockAction(e.externalId, PhoneGen.sample.get))
        case we => we
      })
      mtsActual.get.callResult shouldEqual CallResults.Blocked

      val voxActual = voxBuilderWithRecord.build(events.map(asWrappedVoxEvent).map {
        case VoxCallEventAction(e: CallAcceptedEvent, _) =>
          VoxCallEventAction(e, BlockAction(e.externalId, PhoneGen.sample.get))
        case we => we
      })

      voxActual.get.callResult shouldEqual CallResults.Blocked
    }

    "build call for busy callee" in {
      val events = Seq(
        acceptedEvent,
        goToTransferEvent,
        transferEvent.copy(transferEventType = TransferEventType.ValidCall.BusyCallee("486")),
        endEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.BusyCallee
      }
    }

    "build call with callee error v1" in {
      val events = Seq(
        acceptedEvent,
        goToTransferEvent,
        transferEvent.copy(transferEventType = TransferEventType.CalleeError.NumberNotInService("1344")),
        endEvent
      )
      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.InvalidCallee
      }
    }

    "build call with callee error v3" in {
      val events = Seq(
        acceptedEvent,
        goToTransferEvent,
        transferEvent.copy(transferEventType = TransferEventType.CalleeError.NotFound("404")),
        endEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.InvalidCallee
      }
    }

    "build call with error v0" in {
      val events = Seq(
        acceptedEvent,
        goToTransferEvent,
        transferEvent.copy(transferEventType = TransferEventType.Error.RequestTimeout("408")),
        endEvent
      )
      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.Error
      }
    }

    "build call with error v1" in {
      val events = Seq(
        acceptedEvent,
        goToTransferEvent,
        transferEvent.copy(transferEventType = TransferEventType.Error.Rejected("1352")),
        endEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.Error
      }
    }

    "build call with error v2" in {
      val events = Seq(
        acceptedEvent,
        goToTransferEvent,
        transferEvent.copy(transferEventType = TransferEventType.Error.BadRequestError("400")),
        endEvent
      )
      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.Error
      }
    }

    "build call with unknown" in {
      val unknown = TransferEventType.UnknownError.UnknownTransferEventType("1234")
      val events = Seq(
        acceptedEvent,
        goToTransferEvent,
        transferEvent.copy(transferEventType = unknown),
        endEvent
      )
      operatorRawCallFuncSeq.foreach { op =>
        val actual = op.buildRawCall(events)
        actual.get.callResult shouldEqual CallResults.Unknown
      }
    }

    "ignore error case" in {
      val events = Seq(
        acceptedEvent,
        answeredEvent,
        goToTransferEvent,
        transferEvent
      )
      val actual = mtsBuilderWithRecord.build(events.map(asWrappedMtsEvent))
      actual shouldEqual None
    }

    "set talk duration to 0 if the call ends before record starts and the call is short" in {
      val shortTalkDuration = RawCallBuilderImpl.CallDurationFallbackThreshold - 1.second
      val quickEndEvent = CallEndEvent(
        callId,
        answerTime.plusSeconds(shortTalkDuration.toSeconds.toInt),
        source,
        phone,
        None
      )
      val callDuration = (quickEndEvent.eventTime.getMillis - startTime.getMillis).millis
      val events = Seq(
        acceptedEvent,
        answeredEvent,
        goToTransferEvent,
        transferEvent,
        quickEndEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val expected = Some(
          RawCall(
            callId,
            source,
            phone,
            Some(target),
            startTime,
            callDuration,
            0.seconds,
            None,
            CallResults.Success,
            origin = Origins.Online,
            operator = op.operator
          )
        )

        val actual = op.buildRawCall(events)
        actual shouldEqual expected
      }
    }

    "set talk duration to call duration if the call is suspiciously long" in {
      val farEndTime =
        answerTime.plusSeconds((RawCallBuilderImpl.CallDurationFallbackThreshold + 1.second).toSeconds.toInt)

      val longCallDuration = (farEndTime.getMillis - startTime.getMillis).millis
      val longTalkDuration = (farEndTime.getMillis - answerTime.getMillis).millis

      val farEndEvent = CallEndEvent(
        callId,
        farEndTime,
        source,
        phone,
        None
      )
      val events = Seq(
        acceptedEvent,
        answeredEvent,
        goToTransferEvent,
        transferEvent,
        farEndEvent
      )

      operatorRawCallFuncSeq.foreach { op =>
        val expected = Some(
          RawCall(
            callId,
            source,
            phone,
            Some(target),
            startTime,
            longCallDuration,
            longTalkDuration,
            None,
            CallResults.Success,
            origin = Origins.Online,
            operator = op.operator
          )
        )
        val actual = op.buildRawCall(events)
        actual shouldEqual expected
      }
    }
  }
}
