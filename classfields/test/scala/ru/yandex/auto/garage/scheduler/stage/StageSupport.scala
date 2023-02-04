package ru.yandex.auto.garage.scheduler.stage

import org.scalatest.matchers.should.Matchers
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{State, WatchingStateUpdate}
import ru.yandex.auto.vin.decoder.scheduler.stage.Stage
import ru.yandex.vertis.mockito.{MockitoSupport, NotMockedException}
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Try}

trait StageSupport[A, S, B <: Stage[A, S]] extends Matchers with MockitoSupport {

  implicit val m = TestOperationalSupport

  implicit val stateS: State[S]

  def createProcessingStage(): B

  def checkIgnored(processingState: ProcessingState[S]): Unit = {
    (createProcessingStage().shouldProcess(processingState) shouldBe false): Unit
  }

  def checkProcessed(id: A, processingState: ProcessingState[S]): Unit = {
    val stage = createProcessingStage()
    stage.shouldProcess(processingState) shouldBe true

    intercept[NotMockedException] {
      // provided client was called
      stage.processWithAsync(id, processingState)
    }
    ()
  }

  def checkShouldProcess(processingState: ProcessingState[S]): Unit = {
    val stage = createProcessingStage()
    (stage.shouldProcess(processingState) shouldBe true): Unit
  }

  implicit class RichStage(stage: Stage[A, S]) {

    def processWithAsync(id: A, processingState: ProcessingState[S]): WatchingStateUpdate[S] = {
      stage.run(id, processingState).map(applyAsyncOperations).getOrElse(processingState.compoundStateUpdate)
    }
  }

  private def applyAsyncOperations(processingState: ProcessingState[S]): WatchingStateUpdate[S] = {
    val update = processingState.compoundStateUpdate
    val asyncOps = processingState.asyncOperations
    if (asyncOps.isEmpty) {
      processingState.compoundStateUpdate
    } else {
      Try {
        val updater = asyncOps
          .map(t => Await.result(t.calculateUpdate(update), 10.minutes).get)
          .combine
        updater.apply(update)
      }.recoverWith {
        // because calculateUpdate in asyncOp wraps mock exception in runtime
        case e: RuntimeException if e.getCause.isInstanceOf[NotMockedException] =>
          Failure(e.getCause)
      }.get
    }
  }
}

object StageSupport {

  def createDefaultProcessingState[S: State](compoundState: S): ProcessingState[S] = {
    ProcessingState[S](WatchingStateUpdate(compoundState, WatchingStateUpdate.defaultAsyncDelay))
  }
}
