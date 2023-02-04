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
class SequentialStageRunnerSpec extends AsyncSpecBase {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "SequentialStageRunner" should {
    "correctly work when everything succeeded" in {

      val stage1 = new AbstractStage() {
        override protected def doRun(): Source[StageState, _] = {
          Source(StageState(StageStatus.Succeeded) :: Nil)
        }
      }

      val stage2 = new AbstractStage() {
        override protected def doRun(): Source[StageState, _] = {
          Source(StageState(StageStatus.Succeeded) :: Nil)
        }
      }

      val runner = new SequentialStageRunner(Seq("STAGE1" -> stage1, "STAGE2" -> stage2))

      val states = runner.run().runWith(Sink.seq).futureValue.toList
      states should be(
        List(
          FlowState(
            List(
              StageInfo("STAGE1", StageState(StageStatus.Pending)),
              StageInfo("STAGE2", StageState(StageStatus.Pending))
            )
          ),
          FlowState(
            List(
              StageInfo("STAGE1", StageState(StageStatus.InProgress)),
              StageInfo("STAGE2", StageState(StageStatus.Pending))
            )
          ),
          FlowState(
            List(
              StageInfo("STAGE1", StageState(StageStatus.Succeeded)),
              StageInfo("STAGE2", StageState(StageStatus.Pending))
            )
          ),
          FlowState(
            List(
              StageInfo("STAGE1", StageState(StageStatus.Succeeded)),
              StageInfo("STAGE2", StageState(StageStatus.InProgress))
            )
          ),
          FlowState(
            List(
              StageInfo("STAGE1", StageState(StageStatus.Succeeded)),
              StageInfo("STAGE2", StageState(StageStatus.Succeeded))
            )
          )
        )
      )
    }

    "correctly work when something failed" in {

      val stage1 = new AbstractStage() {
        override protected def doRun(): Source[StageState, _] = {
          Source(StageState(StageStatus.Failed) :: Nil)
        }
      }

      val stage2 = new AbstractStage() {
        override protected def doRun(): Source[StageState, _] = {
          Source(StageState(StageStatus.Succeeded) :: Nil)
        }
      }

      val runner = new SequentialStageRunner(Seq("STAGE1" -> stage1, "STAGE2" -> stage2))

      val states = runner.run().runWith(Sink.seq).futureValue.toList
      states should be(
        List(
          FlowState(
            List(
              StageInfo("STAGE1", StageState(StageStatus.Pending)),
              StageInfo("STAGE2", StageState(StageStatus.Pending))
            )
          ),
          FlowState(
            List(
              StageInfo("STAGE1", StageState(StageStatus.InProgress)),
              StageInfo("STAGE2", StageState(StageStatus.Pending))
            )
          ),
          FlowState(
            List(
              StageInfo("STAGE1", StageState(StageStatus.Failed)),
              StageInfo("STAGE2", StageState(StageStatus.Pending))
            )
          ),
          FlowState(
            List(
              StageInfo("STAGE1", StageState(StageStatus.Failed)),
              StageInfo("STAGE2", StageState(StageStatus.Skipped))
            )
          )
        )
      )
    }
  }

}
