package ru.yandex.vertis.vsquality.hobo.checks

import org.junit.runner.RunWith
import ru.yandex.vertis.vsquality.hobo.checks.TaskDecider.{Source, Verdict}
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer._
import ru.yandex.vertis.vsquality.hobo.model.{EvaluationTaskSettings, UserQueueSettings}
import ru.yandex.vertis.hobo.proto.Model.Task.State
import ru.yandex.vertis.vsquality.hobo.service.{AutomatedContext, RequestContext, UserService}
import ru.yandex.vertis.vsquality.hobo.util.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EvaluationTaskDeciderImplSpec extends SpecBase {

  implicit private val requestContext: RequestContext = AutomatedContext("test")

  private val userService: UserService = mock[UserService]
  private val decider: TaskDecider = new EvaluationTaskDeciderImpl(userService)

  private def queueSettings(probability: Option[Double]): UserQueueSettings =
    UserQueueSettings(0, 0, 0, probability.map(EvaluationTaskSettings))

  case class TaskTestCase(
      userQueueSettings: UserQueueSettings,
      taskState: State,
      expectedVerdict: Verdict,
      description: String)

  val testCases =
    Seq(
      TaskTestCase(
        userQueueSettings = queueSettings(Some(1)),
        taskState = State.COMPLETED,
        expectedVerdict = Verdict(needCheck = true),
        description = "return Verdict(needCheck = true) for a task with creationProbability = 1"
      ),
      TaskTestCase(
        userQueueSettings = queueSettings(Some(0)),
        taskState = State.COMPLETED,
        expectedVerdict = Verdict(needCheck = false),
        description = "return Verdict(needCheck = false) for a task with creationProbability = 0"
      ),
      TaskTestCase(
        userQueueSettings = queueSettings(None),
        taskState = State.NEED_VERIFICATION,
        expectedVerdict = Verdict(needCheck = false),
        description = "return Verdict(needCheck = false) if evaluationTaskSettings is None"
      )
    )

  "Decider" should {
    testCases.foreach { case TaskTestCase(userQueueSettings, taskState, expectedVerdict, description) =>
      description in {
        val queue = QueueIdGen.next
        val owner = UserGen.next.copy(queuesSettings = Map(queue -> userQueueSettings))
        val task =
          TaskGen.next.copy(
            queue = queue,
            initialTaskKey = None,
            checkCount = 0,
            owner = Some(owner.key),
            state = taskState
          )
        doReturn(Future.successful(owner)).when(userService).get(owner.key)
        val result = decider(Source(task)).futureValue
        result shouldBe expectedVerdict
      }
    }
  }
}
