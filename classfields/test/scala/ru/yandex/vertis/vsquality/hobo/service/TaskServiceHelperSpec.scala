package ru.yandex.vertis.vsquality.hobo.service

import org.scalacheck.Gen

import ru.yandex.vertis.vsquality.hobo.model._
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer._
import ru.yandex.vertis.hobo.proto.Model
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.hobo.proto.Model.RealtyVisualResolution.Value._
import ru.yandex.vertis.vsquality.hobo.util.{DateTimeUtil, SpecBase}

/**
  * @author potseluev
  */

class TaskServiceHelperSpec extends SpecBase {

  import TaskServiceHelperSpec._

  private val testCases =
    Seq(
      TestCase(
        initialTask = TaskGen.next.copy(
          resolution = None,
          queue = nextCrosscheckableQueue
        ),
        checkTask = TaskGen.next.copy(
          resolution = Some(TrueFalseResolution(true, "")),
          queue = nextCrosscheckableQueue
        ),
        needValidation = false,
        description = "no validation if first task hasn't resolution, second has no one important resolution"
      ),
      TestCase(
        initialTask = TaskGen.next.copy(
          resolution = Some(RealtyVisualResolution(Set(USER_FRAUD), "")),
          queue = nextCrosscheckableQueue
        ),
        checkTask = TaskGen.next.copy(
          resolution = None,
          queue = nextCrosscheckableQueue
        ),
        needValidation = true,
        description = "need validation if first task has important resolution but second has no one"
      ),
      TestCase(
        initialTask = TaskGen.next.copy(resolution = None, queue = nextCrosscheckableQueue),
        checkTask = TaskGen.next.copy(resolution = None, queue = nextCrosscheckableQueue),
        needValidation = false,
        description = "no validation if both tasks have not resolution"
      ),
      TestCase(
        initialTask = TaskGen.next.copy(
          resolution = Some(RealtyVisualResolution(Set(USER_FRAUD, PRICE_ERROR), "")),
          queue = nextCrosscheckableQueue
        ),
        checkTask = TaskGen.next.copy(
          resolution = Some(RealtyVisualResolution(Set(USER_FRAUD, COMPLAINT_FROM_OWNER), "")),
          queue = nextCrosscheckableQueue
        ),
        needValidation = false,
        description = "no validation if both tasks have same important resolution"
      ),
      TestCase(
        initialTask = TaskGen.next.copy(
          resolution = Some(RealtyVisualResolution(Set(USER_FRAUD), "")),
          queue = nextNonCrosscheckableQueue
        ),
        checkTask = TaskGen.next.copy(resolution = None, queue = nextNonCrosscheckableQueue),
        needValidation = false,
        description = "no validation if there are no assigned crosscheck queue"
      ),
      TestCase(
        initialTask = TaskGen.next.copy(
          resolution = Some(RealtyVisualResolution(Set(USER_FRAUD, PRICE_ERROR), "")),
          queue = nextCrosscheckableQueue,
          derivedTask = derivedTask(QueueId.AUTO_RU_ACCOUNTS_RESELLERS_CALL)
        ),
        checkTask = TaskGen.next.copy(
          resolution = Some(RealtyVisualResolution(Set(USER_FRAUD, COMPLAINT_FROM_OWNER), "")),
          queue = nextCrosscheckableQueue,
          derivedTask = derivedTask(QueueId.TEST_QUEUE)
        ),
        needValidation = true,
        description =
          "need validation if both tasks have same important resolution but were redirected to different queues"
      ),
      TestCase(
        initialTask = TaskGen.next.copy(
          resolution = Some(RealtyVisualResolution(Set(USER_FRAUD, PRICE_ERROR), "")),
          queue = nextCrosscheckableQueue,
          derivedTask = None
        ),
        checkTask = TaskGen.next.copy(
          resolution = Some(RealtyVisualResolution(Set(USER_FRAUD, COMPLAINT_FROM_OWNER), "")),
          queue = nextCrosscheckableQueue,
          derivedTask = derivedTask(QueueId.TEST_QUEUE)
        ),
        needValidation = true,
        description = "need validation if both tasks have same important resolution but one of them was redirected"
      ),
      TestCase(
        initialTask = TaskGen.next.copy(
          resolution = Some(RealtyVisualResolution(Set(USER_FRAUD, PRICE_ERROR), "")),
          queue = nextCrosscheckableQueue
        ),
        checkTask = TaskGen.next.copy(
          prevResolution = Some(RealtyVisualResolution(Set(USER_FRAUD, PRICE_ERROR), "")),
          resolution = None,
          queue = nextCrosscheckableQueue
        ),
        needValidation = false,
        description = "compare by prevResolution first"
      )
    )

  "TaskServiceHelper.toValidationTask" should {

    testCases.foreach { case TestCase(initial, check, needValidation, description) =>
      description in {
        val result = TaskServiceHelper.toValidationTask(initial, check, DateTimeUtil.now())
        result.isDefined shouldBe needValidation
      }
    }

  }

  "TaskServiceHelper.toEvaluationTask" should {
    "create evaluation task if needed" in {
      val baseQueue = nextEvaluationQueue
      val evalQueue = getEvaluationQueue(baseQueue)
      val task = TaskGen.next.copy(queue = baseQueue)

      val result = TaskServiceHelper.toEvaluationTask(task, DateTimeUtil.now())

      evalQueue should not be empty
      result should not be empty
      result.get._2.queue shouldBe evalQueue.get
    }

    "skip task without evaluation queue configuration" in {
      val task = TaskGen.next.copy(queue = nextNonEvaluationQueue)
      val result = TaskServiceHelper.toEvaluationTask(task, DateTimeUtil.now())
      result shouldBe empty
    }
  }

  private def derivedTask(redirectedTo: QueueId): Some[TaskDescriptor] =
    Some(TaskDescriptor(redirectedTo, CoreGenerators.TaskKeyGen.next))
}

object TaskServiceHelperSpec {

  private val TaskGen: Gen[Task] =
    CoreGenerators.TaskGen
      .suchThat(!_.payload.isInstanceOf[Payload.Subtasks])
      .map(
        _.copy(
          prevResolution = None,
          derivedTask = None
        )
      )

  private case class TestCase(initialTask: Task, checkTask: Task, needValidation: Boolean, description: String)

  private def hasCrosscheckQueue(queue: QueueId): Boolean =
    QueueSettings.defaultForQueue(queue).const.crosscheckQueue.isDefined

  private def nextCrosscheckableQueue: Model.QueueId = {
    val queues =
      QueueId
        .values()
        .filter(_.name.startsWith("REALTY")) // queue domains for both initial and check tasks must be the same
        .filter(_.name.endsWith("VISUAL")) // same resolution to be able to compare
        .filter(hasCrosscheckQueue)
    Gen.oneOf(queues.toIndexedSeq).next
  }

  private def nextNonCrosscheckableQueue: Model.QueueId =
    Gen.oneOf(QueueId.values().toIndexedSeq.filterNot(hasCrosscheckQueue)).next

  private def getEvaluationQueue(queue: QueueId): Option[QueueId] =
    QueueSettings.defaultForQueue(queue).const.evaluationQueue

  private def nextEvaluationQueue: Model.QueueId =
    Gen.oneOf(QueueId.values().toIndexedSeq.filter(getEvaluationQueue(_).isDefined)).next

  private def nextNonEvaluationQueue: Model.QueueId =
    Gen.oneOf(QueueId.values().toIndexedSeq.filterNot(getEvaluationQueue(_).isDefined)).next
}
