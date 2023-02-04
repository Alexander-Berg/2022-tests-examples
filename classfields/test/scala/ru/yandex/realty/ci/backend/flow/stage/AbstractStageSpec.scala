package ru.yandex.realty.ci.backend.flow.stage

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase

/**
  * @author abulychev
  */
@RunWith(classOf[JUnitRunner])
class AbstractStageSpec extends AsyncSpecBase {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val runningStates =
    List(
      StageState(StageStatus.InProgress),
      StageState(StageStatus.InProgress, msg = Some("some message")),
      StageState(StageStatus.InProgress, msg = Some("some message"), link = Some("https://somelink.ru")),
      StageState(StageStatus.Succeeded, msg = Some("completed"), link = Some("https://somelink.ru"))
    )

  "AbstractStage" should {
    "correctly run" in {

      val stage = new AbstractStage() {
        override protected def doRun(): Source[StageState, _] = {
          Source(runningStates)
        }
      }

      stage.in0 <<= StageTrigger.Run

      val states = stage.state.runWith(Sink.seq).futureValue.toList

      states.length should be(2 + runningStates.length)
      states.head.status should be(StageStatus.Pending)
      states(1).status should be(StageStatus.InProgress)
      states.drop(2) should be(runningStates)

      stage.out0.getOrElse(???) should be(StageStatus.Succeeded)
    }

    "correctly skip" in {
      val stage = new AbstractStage() {
        override protected def doRun(): Source[StageState, _] = {
          Source(runningStates)
        }
      }

      stage.in0 <<= StageTrigger.Skip

      val states = stage.state.runWith(Sink.seq).futureValue.toList

      states.length should be(2)
      states.head.status should be(StageStatus.Pending)
      states.last.status should be(StageStatus.Skipped)

      stage.out0.getOrElse(???) should be(StageStatus.Skipped)
    }
  }

}
