package ru.yandex.auto.vin.decoder.scheduler.stage.partners.queue

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{CompoundState, PreparedDataState}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateUpdate
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import ru.yandex.auto.vin.decoder.scheduler.stage.StageSupport.createDefaultProcessingState
import ru.yandex.auto.vin.decoder.scheduler.workers.data.PreparedDataProcessorsEngine
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.vertis.mockito.MockitoSupport

class PreparedDataLightStageTest
  extends AnyWordSpecLike
  with Matchers
  with MockitoSupport
  with BeforeAndAfter
  with CompoundStageSupport[VinCode, PreparedDataLightStage[VinCode]] {

  override def createProcessingStage(): PreparedDataLightStage[VinCode] =
    new PreparedDataLightStage[VinCode](queue, processorsEngine)

  private val processorsEngine = mock[PreparedDataProcessorsEngine[VinCode]]
  private val queue = mock[WorkersQueue[VinCode, CompoundState]]
  private val stage = createProcessingStage()
  private val vin = VinCode("X4X3D59430PS96744")

  before {
    reset(queue)
    reset(processorsEngine)
    when(queue.enqueue(?, ?)).thenReturn(true)
    when(queue.queueName).thenReturn("name")
  }

  "PreparedDataLightStage.shouldProcess" should {
    "return false" when {
      "no prepared data states" in {
        val state = CompoundState.newBuilder().build()
        val processingState = createDefaultProcessingState(state)

        stage.shouldProcess(processingState) shouldBe false
      }
      "should process = false" in {
        val state = CompoundState.newBuilder()
        state.addPreparedDataState(createPreparedDataState(EventType.AAA_MOTORS_INSURANCE, false))
        state.addPreparedDataState(createPreparedDataState(EventType.ACAT_INFO, false))
        val processingState = createDefaultProcessingState(state.build())

        stage.shouldProcess(processingState) shouldBe false
      }
    }
    "return true" when {
      "should process = false" in {
        val state = CompoundState.newBuilder()
        state.addPreparedDataState(createPreparedDataState(EventType.AAA_MOTORS_INSURANCE, false))
        state.addPreparedDataState(createPreparedDataState(EventType.ACAT_INFO, true))
        val processingState = createDefaultProcessingState(state.build())

        stage.shouldProcess(processingState) shouldBe true
      }
    }
  }

  "PreparedDataLightStage.process" should {
    "add state to queue" when {
      "processors has async processor return true" in {
        when(processorsEngine.hasAsyncProcessors(?)).thenReturn(true)

        val state = {
          val builder = CompoundState.newBuilder()
          builder.addPreparedDataState(createPreparedDataState(EventType.ACAT_INFO, true))
          builder.build()
        }
        val processingState = createDefaultProcessingState(state)

        stage.shouldProcess(processingState) shouldBe true
        val update = stage.processWithAsync(vin, processingState)

        verify(queue, times(1)).enqueue(eq(vin), ?)
        verify(processorsEngine, times(1)).hasAsyncProcessors(eq(processingState.compoundStateUpdate.state))
        assert(update.delay.toDuration.toMillis <= WatchingStateUpdate.AsyncMaxDelay.toMillis)
      }
    }
  }

  "finish all states and dont add to queue" when {
    "processors has async processor return false" in {
      when(processorsEngine.hasAsyncProcessors(?)).thenReturn(false)

      val state = {
        val builder = CompoundState.newBuilder()
        builder.addPreparedDataState(createPreparedDataState(EventType.ACAT_INFO, true))
        builder.build()
      }
      val processingState = createDefaultProcessingState(state)

      stage.shouldProcess(processingState) shouldBe true
      val update = stage.processWithAsync(vin, processingState)

      verify(queue, never()).enqueue(eq(vin), ?)
      verify(processorsEngine, times(1)).hasAsyncProcessors(eq(processingState.compoundStateUpdate.state))

      update.state.getPreparedDataStateCount shouldBe 1
      update.state.getPreparedDataState(0).getShouldProcess shouldBe false
      update.state.getPreparedDataState(0).getEventType shouldBe EventType.ACAT_INFO
    }
  }

  private def createPreparedDataState(eventType: EventType, shouldProcess: Boolean): PreparedDataState = {
    PreparedDataState.newBuilder().setEventType(eventType).setShouldProcess(shouldProcess).build()
  }

}
