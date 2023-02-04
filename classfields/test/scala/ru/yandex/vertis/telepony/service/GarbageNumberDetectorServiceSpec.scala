package ru.yandex.vertis.telepony.service

import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.SampleHelper._
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.Status.{Downtimed, Ready}
import ru.yandex.vertis.telepony.model.{OperatorNumber, Phone, RawCall, Status}
import ru.yandex.vertis.telepony.service.impl.GarbageNumberDetectorServiceImpl
import ru.yandex.vertis.telepony.settings.StatusSettings
import ru.yandex.vertis.telepony.time._
import ru.yandex.vertis.telepony.util.Threads.lightWeightTasksEc
import ru.yandex.vertis.telepony.util.sliced.SlicedResult
import ru.yandex.vertis.telepony.util.{Page, Slice}

import scala.concurrent.Future

/**
  * Created by neron on 02.02.17.
  */
class GarbageNumberDetectorServiceSpec extends SpecBase with MockitoSupport {

  private def createOperatorNumber(status: Status): OperatorNumber =
    OperatorNumberGen.next.copy(status = status)

  private def createRawCall(phone: Phone, callTime: DateTime): RawCall =
    RawCallGen.next.copy(proxy = phone, startTime = callTime)

  private val config = ConfigFactory.parseResources("service.conf").resolve()

  trait Test {
    val operatorNumberService = mock[OperatorNumberServiceV2]
    val unmatchedCallService = mock[UnmatchedCallService]
    val statusSettings = StatusSettings(config.getConfig("telepony.domain.default.status"))

    val service = new GarbageNumberDetectorServiceImpl(
      operatorNumberService,
      statusSettings,
      unmatchedCallService
    )
  }

  "GarbageNumberDetector" should {
    "filter unwanted numbers" when {
      "maxCount is exceeded" in new Test {
        val opn = createOperatorNumber(Ready(None, now().minusDays(3)))
        when(operatorNumberService.find(opn.number)).thenReturn(Future.successful(Some(opn)))
        val slice = Page(0, 1)
        when(unmatchedCallService.list(?, equ(slice): Slice)(?))
          .thenReturn(Future.successful(SlicedResult(Nil, 2, slice)))
        when(operatorNumberService.compareStatusAndSet(equ(opn.status.value), ?)(?))
          .thenReturn(Future.successful(true))

        val rawCall = createRawCall(opn.number, now().minusHours(2))
        val many = service.transitToGarbageIfManyUnmatchedCalls(rawCall).futureValue
        many should ===(true)

        Mockito.verify(operatorNumberService).compareStatusAndSet(equ(opn.status.value), ?)(?)
      }
    }

    "not filter good numbers" when {
      "call not in interval" in new Test {
        val opn = createOperatorNumber(Ready(None, now().minusDays(30)))
        when(operatorNumberService.find(opn.number)).thenReturn(Future.successful(Some(opn)))
        val slice = Page(0, 1)
        when(unmatchedCallService.list(?, equ(slice): Slice)(?))
          .thenReturn(Future.successful(SlicedResult(Nil, 10, slice)))
        val rawCall = createRawCall(opn.number, now().minusDays(20))
        val many = service.transitToGarbageIfManyUnmatchedCalls(rawCall).futureValue
        many should ===(false)
      }

      "maxCount is not exceeded" in new Test {
        val opn = createOperatorNumber(Ready(None, now().minusDays(3)))
        when(operatorNumberService.find(opn.number)).thenReturn(Future.successful(Some(opn)))
        val slice = Page(0, 1)
        when(unmatchedCallService.list(?, equ(slice): Slice)(?))
          .thenReturn(Future.successful(SlicedResult(Nil, 1, slice)))
        val rawCall = createRawCall(opn.number, now().minusHours(2))
        val many = service.transitToGarbageIfManyUnmatchedCalls(rawCall).futureValue
        many should ===(false)
      }
    }

    "not filter numbers" when {
      "updateTime gt deadline" in new Test {
        val opn = createOperatorNumber(Downtimed(Some(now().minusDays(1)), now()))
        when(operatorNumberService.find(opn.number)).thenReturn(Future.successful(Some(opn)))
        val slice = Page(0, 1)
        when(unmatchedCallService.list(?, equ(slice): Slice)(?))
          .thenReturn(Future.successful(SlicedResult(Nil, 2, slice)))
        val rawCall = createRawCall(opn.number, now().minusHours(2))
        val many = service.transitToGarbageIfManyUnmatchedCalls(rawCall).futureValue
        many should ===(false)
      }
    }
  }
}
