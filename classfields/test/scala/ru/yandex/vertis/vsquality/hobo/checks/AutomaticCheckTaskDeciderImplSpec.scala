package ru.yandex.vertis.vsquality.hobo.checks

import ru.yandex.vertis.vsquality.hobo.checks.TaskDecider.{Source, Verdict}
import ru.yandex.vertis.vsquality.hobo.model.UserQueueSettings
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer._
import ru.yandex.vertis.hobo.proto.Model.Task.State
import ru.yandex.vertis.vsquality.hobo.service.{AutomatedContext, RequestContext, TaskService, UserService}
import ru.yandex.vertis.vsquality.hobo.util.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Spec for [[CheckTaskDeciderImpl]]
  *
  * @author potseluev
  */

class AutomaticCheckTaskDeciderImplSpec extends SpecBase {

  implicit private val requestContext: RequestContext = AutomatedContext("test")

  private val taskService: TaskService = mock[TaskService]
  private val userService: UserService = mock[UserService]
  private val decider: TaskDecider = new CheckTaskDeciderImpl(userService, taskService)

  case class OrdinaryTaskTestCase(
      userQueueSettings: UserQueueSettings,
      taskState: State,
      expectedVerdict: Verdict,
      description: String)

  val ordinaryTestCases =
    Seq(
      OrdinaryTaskTestCase(
        userQueueSettings = UserQueueSettings(
          checkProbability = 1,
          numberOfChecks = 1,
          checkTaskTakeProbability = 0,
          evaluationTaskSettings = None
        ),
        taskState = State.COMPLETED,
        expectedVerdict = Verdict(needCheck = true),
        description = "return Verdict(needCheck = true) for ordinary task and checkProbability = 1, numberOfChecks > 0"
      ),
      OrdinaryTaskTestCase(
        userQueueSettings = UserQueueSettings(
          checkProbability = 0,
          numberOfChecks = 0,
          checkTaskTakeProbability = 0,
          evaluationTaskSettings = None
        ),
        taskState = State.COMPLETED,
        expectedVerdict = Verdict(needCheck = false),
        description = "return Verdict(needCheck = false) for ordinary task and checkProbability = 0, numberOfChecks = 0"
      ),
      OrdinaryTaskTestCase(
        userQueueSettings = UserQueueSettings(
          checkProbability = 0,
          numberOfChecks = 0,
          checkTaskTakeProbability = 0,
          evaluationTaskSettings = None
        ),
        taskState = State.NEED_VERIFICATION,
        expectedVerdict = Verdict(needCheck = true),
        description =
          "return Verdict(needCheck = true) if ordinaryTask.state = NEED_VERIFICATION even if checkProbability = 0, numberOfChecks = 0"
      )
    )

  "Decider" should {
    ordinaryTestCases.foreach { case OrdinaryTaskTestCase(userQueueSettings, taskState, expectedVerdict, description) =>
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
