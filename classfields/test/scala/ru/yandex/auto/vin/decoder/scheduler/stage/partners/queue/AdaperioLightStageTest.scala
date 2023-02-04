package ru.yandex.auto.vin.decoder.scheduler.stage.partners.queue

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.adaperio.AdaperioReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration._

class AdaperioLightStageTest
  extends AnyFunSuite
  with MockitoSupport
  with BeforeAndAfter
  with CompoundStageSupport[VinCode, AdaperioLightStage] {

  private val queue = mock[WorkersQueue[VinCode, CompoundState]]
  private val stage = createProcessingStage()
  private val vin = VinCode("X4X3D59430PS96744")
  private lazy val reportType = AdaperioReportType.Main

  before {
    reset(queue)
    when(queue.enqueue(?, ?)).thenReturn(true)
    when(queue.queueName).thenReturn("adaperio")
  }

  test("ignore if no adaperio") {
    val b = CompoundState.newBuilder().build()
    val state = createProcessingState(b)

    assert(!stage.shouldProcess(state))
    verify(queue, never()).enqueue(?, ?)
  }

  test("ignore if completed and no flags") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder.setRequestSent(5).setReportArrived(55)
    val state = createProcessingState(b.build())

    assert(!stage.shouldProcess(state))
    verify(queue, never()).enqueue(?, ?)
  }

  test("do nothing if invalid") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder.setInvalid(true)
    val state = createProcessingState(b.build())

    assert(!stage.shouldProcess(state))
    verify(queue, never()).enqueue(?, ?)
  }

  test("do nothing with expired report") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder.setNoOrder(true).setRequestSent(1)
    val state = createProcessingState(b.build())

    assert(!stage.shouldProcess(state))
    verify(queue, never()).enqueue(?, ?)
  }

  test("enqueue if should process") {
    val b = CompoundState.newBuilder()
    b.getAdaperioBuilder
      .getReportBuilder(reportType)
      .setShouldProcess(true)
    val state = createProcessingState(b.build())

    val res = stage.processWithAsync(vin, state)

    assert(res.delay.toDuration <= WatchingStateUpdate.AsyncMaxDelay)
    verify(queue, times(1)).enqueue(?, ?)
  }

  private def createProcessingState(compoundState: CompoundState): ProcessingState[CompoundState] = {
    ProcessingState(WatchingStateUpdate(compoundState, DefaultDelay(25.hours)))
  }

  override def createProcessingStage(): AdaperioLightStage = {
    new AdaperioLightStage(reportType, queue)
  }
}
