package ru.yandex.vertis.vsquality.hobo.service

import org.joda.time.{DateTime, Interval}
import org.scalacheck.Gen
import ru.yandex.vertis.vsquality.hobo.TaskSort.ByCreateTime
import ru.yandex.vertis.vsquality.hobo._
import ru.yandex.vertis.vsquality.hobo.exception.{AccessDenyException, AlreadyExistException, NotExistException}
import ru.yandex.vertis.vsquality.hobo.model.PayloadSource.Subtasks
import ru.yandex.vertis.vsquality.hobo.model._
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators.{resolutionGen, _}
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.oncomplete.OnCompleteAction
import ru.yandex.vertis.hobo.proto.Common.ActionId.InternalActionId
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.hobo.proto.Model.RealtyVisualResolution.Value._
import ru.yandex.vertis.hobo.proto.Model.Task.{State, TaskType}
import ru.yandex.vertis.vsquality.hobo.service.TaskFactory.{getSimilarityHash, CreateOptions}
import ru.yandex.vertis.vsquality.hobo.service.impl.stub.StubQueueSettingsService
import ru.yandex.vertis.vsquality.hobo.util._

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random

/**
  * Base specs on [[TaskService]]
  *
  * @author semkagtn
  */
@nowarn("cat=w-flag-value-discard")
trait TaskServiceSpecBase extends SpecBase {

  import TaskServiceSpecBase._

  def taskService: TaskService

  private val settingsService = StubQueueSettingsService()
  protected val taskFactory = new TaskFactory(settingsService)
  protected val onCompleteActionsFactory = new OnCompleteActionsFactory(settingsService, TestOnCompleteActionsRegistry)

  implicit val requestContext: AutomatedContext = AutomatedContext("")

  before {
    clearTasks()
  }

  "create one task" should {

    "successful create new snapshot task" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      val task = taskService.create(queue, taskSource).futureValue

      compareTaskWithTaskSource(queue, task, taskSource)
      checkTaskHistoryWasSaved(queue, task.key, TaskAction.Create)
    }

    "successful create new snapshot task with on complete action id" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot](onCompleteActionIds = Set(StubActionId0)).next

      val task = taskService.create(queue, taskSource).futureValue

      compareTaskWithTaskSource(queue, task, taskSource)
      checkOnCompleteAction(task.key, StubActionId0)
    }

    "successful create new external task" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next

      val task = taskService.create(queue, taskSource).futureValue

      compareTaskWithTaskSource(queue, task, taskSource)
      checkTaskHistoryWasSaved(queue, task.key, TaskAction.Create)
    }

    "successful create new subtasks task" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[Subtasks]().next

      val task = taskService.create(queue, taskSource).futureValue

      compareTaskWithTaskSource(queue, task, taskSource)
      checkTaskHistoryWasSaved(queue, task.key, TaskAction.Create)
    }

    "successful on creating duplicate tasks in different queues" in {
      val queue1 = QueueId.TEST_QUEUE
      val queue2 = QueueId.AUTO_RU_CALL_CENTER
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      taskService.create(queue1, taskSource).futureValue
      taskService.create(queue2, taskSource).futureValue
    }

    "result task contains createdBy if OperatorContext used" in {
      val operator = UserIdGen.next
      val operatorContext = OperatorContext("test", operator)
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val task = taskService.create(queue, taskSource)(operatorContext).futureValue

      task.createdBy shouldBe Some(operator)
    }

    "fail on creating duplicate tasks with same timestamp in same queue" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      val createTime = DateTimeGen.next

      taskService.create(queue, taskSource, createTime = createTime).futureValue
      taskService.create(queue, taskSource, createTime = createTime).shouldCompleteWithException[AlreadyExistException]
    }

    "succeed on creating duplicate tasks within several millis in same queue" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      val createTimeFirst = DateTimeGen.next
      val createTimeSecond = createTimeFirst.plusMillis(1)

      taskService.create(queue, taskSource, createTime = createTimeFirst).futureValue
      taskService.create(queue, taskSource, createTime = createTimeSecond).futureValue
    }

    "successfully create task from taskSource with similarityHashSource" in {
      val queue = QueueIdGen.next
      val taskSource =
        taskSourceGen[PayloadSource.Snapshot]().next
          .copy(
            similarityHashSource = Seq("similarity", "hash", "source")
          )

      val task = taskService.create(queue, taskSource).futureValue

      compareTaskWithTaskSource(queue, task, taskSource)
      checkTaskHistoryWasSaved(queue, task.key, TaskAction.Create)
    }
  }

  "createCompletedTask" should {

    "correctly create task" in {
      val queue = QueueIdGen.next
      val completedTaskSource = CompletedTaskSourceGen.next
      val createTime = DateTimeGen.next

      val CompletedTaskSource(usersWithCost, text) = completedTaskSource

      val actualResult = taskService.createCompletedTask(queue, completedTaskSource, createTime).futureValue
      val childrenTaskSources =
        usersWithCost.map { case (user, cost) =>
          TaskSource(
            priority = None,
            expireTime = None,
            payload = PayloadSource.External(Set.empty),
            comment = None,
            response = None,
            takeableAfter = None,
            deferCount = None,
            cost = Some(cost),
            qualifier = Some(user.key),
            labels = Set.empty,
            excludeUsers = Set.empty,
            includeUsers = Set.empty,
            externalKey = None,
            onCompleteActionIds = Set.empty,
            similarityHashSource = Seq.empty
          )
        }.toSeq

      val parentTaskSource =
        TaskSource(
          priority = None,
          expireTime = None,
          payload = PayloadSource.Subtasks(childrenTaskSources),
          comment = Some(text),
          response = None,
          takeableAfter = None,
          deferCount = None,
          cost = None,
          qualifier = None,
          labels = Set.empty,
          excludeUsers = Set.empty,
          includeUsers = Set.empty,
          externalKey = None,
          onCompleteActionIds = Set.empty,
          similarityHashSource = Seq.empty
        )
      val newTask =
        taskFactory.newTask(CreateOptions.OrdinaryTask(queue, createTime))(None, parentTaskSource).futureValue
      val expectedResult = newTask.copy(state = State.COMPLETED, finishTime = Some(createTime))

      actualResult should smartEqual(expectedResult)

      val filter = TaskFilter.Children(actualResult.key)
      val sort = TaskSort.ByCreateTime(asc = true)
      val slice = Range(0, 10)

      val actualChildren = taskService.find(filter, sort, slice).futureValue
      val users = usersWithCost.keySet
      actualChildren.length shouldBe users.size
      actualChildren.foreach { task =>
        task.owner.isDefined shouldBe true
        users should contain(task.owner.get)
      }
    }

    "return error on duplicate task" in {
      val queue = QueueIdGen.next
      val completedTaskSource = CompletedTaskSourceGen.next
      val createTime = DateTimeGen.next

      taskService.createCompletedTask(queue, completedTaskSource, createTime).futureValue
      taskService
        .createCompletedTask(queue, completedTaskSource, createTime)
        .shouldCompleteWithException[AlreadyExistException]
    }
  }

  "findDuplicates" should {

    def createDuplicates(queue: QueueId, qualifier: Qualifier, nDuplicates: Int): Seq[Task] = {
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next.copy(qualifier = Some(qualifier))
      val baseCreateTime = DateTimeGen.next

      (1 to nDuplicates).map { i =>
        taskService.create(queue, taskSource, createTime = baseCreateTime.plusMillis(i)).futureValue
      }
    }

    "return duplicate tasks in same queue" in {
      val queue = QueueIdGen.next
      val qualifier1 = QualifierGen.next
      val qualifier2 = QualifierGen.next

      createDuplicates(queue, qualifier1, nDuplicates = 2)
      createDuplicates(queue, qualifier2, nDuplicates = 1)

      val expectedDuplicates = Seq((qualifier1, 2))

      val createdSince = DateTimeUtil.fromMillis(0)
      val threshold = DuplicatesThreshold(2, 2, 2)
      val actual = taskService.findDuplicates(queue, createdSince, threshold, Range(0, 10)).futureValue

      actual shouldBe expectedDuplicates
    }

    "return duplicates with least qualifier when using paging" in {
      val queue = QueueIdGen.next
      val qualifierMin = QualifierGen.next
      val qualifierMax = QualifierGen.suchThat(_ > qualifierMin).next

      createDuplicates(queue, qualifierMin, nDuplicates = 2)
      createDuplicates(queue, qualifierMax, nDuplicates = 2)

      val expectedDuplicates = Seq((qualifierMin, 2))

      val createdSince = DateTimeUtil.fromMillis(0)
      val threshold = DuplicatesThreshold(2, 2, 2)
      val slice = Range(0, 1)
      val actual = taskService.findDuplicates(queue, createdSince, threshold, slice).futureValue

      actual shouldBe expectedDuplicates
    }

    "return correct total number of records when full result does not fit in a single page" in {
      val queue = QueueIdGen.next
      val qualifierMin = QualifierGen.next
      val qualifierMax = QualifierGen.suchThat(_ > qualifierMin).next

      createDuplicates(queue, qualifierMin, nDuplicates = 2)
      createDuplicates(queue, qualifierMax, nDuplicates = 2)

      val createdSince = DateTimeUtil.fromMillis(0)
      val slice = Range(0, 1)
      val threshold = DuplicatesThreshold(2, 2, 2)
      val actual = taskService.findDuplicates(queue, createdSince, threshold, slice).futureValue

      actual.total shouldBe 2
    }

    "return empty result if duplicatesThreshold is too big" in {
      val queue = QueueIdGen.next
      createDuplicates(queue, QualifierGen.next, nDuplicates = 2)

      val createdSince = DateTimeUtil.fromMillis(0)
      val threshold = DuplicatesThreshold(3, 3, 3)
      val actual = taskService.findDuplicates(queue, createdSince, threshold, Range(0, 10)).futureValue
      actual shouldBe empty
    }

    "return empty result if duplicates were created before `createdTime`" in {
      val queue = QueueIdGen.next
      val duplicates = createDuplicates(queue, QualifierGen.next, nDuplicates = 2)

      val createdSinceTooBig = DateTimeUtil.fromMillis(duplicates.map(_.createTime.getMillis).max + 1)
      val threshold = DuplicatesThreshold(2, 2, 2)
      val actual = taskService.findDuplicates(queue, createdSinceTooBig, threshold, Range(0, 10)).futureValue
      actual shouldBe empty
    }

    "return empty result for duplicates in different queues" in {
      val queueWithDuplicates = QueueIdGen.next
      val queueWithoutDuplicates = QueueIdGen.suchThat(_ != queueWithDuplicates).next
      createDuplicates(queueWithDuplicates, QualifierGen.next, nDuplicates = 2)

      val createdSince = DateTimeUtil.fromMillis(0)
      val threshold = DuplicatesThreshold(2, 2, 2)
      val actual = taskService.findDuplicates(queueWithoutDuplicates, createdSince, threshold, Range(0, 10)).futureValue
      actual shouldBe empty
    }
  }

  "take" should {

    "return correct task" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSources =
        List(
          taskSourceGen[PayloadSource.Snapshot]().next.copy(priority = Some(0)),
          taskSourceGen[PayloadSource.Snapshot]().next.copy(priority = Some(1))
        )

      val tasks = createTasks(queue, taskSources)
      val maxPriorityTask = tasks.maxBy(task => (task.priority, -task.createTime.getMillis))
      Thread.sleep(2)

      val actualTask = taskService.take(queue, user).futureValue
      val expectedTask =
        maxPriorityTask.copy(
          owner = Some(user),
          state = State.IN_PROGRESS,
          startTime = actualTask.startTime,
          updateTime = actualTask.updateTime
        )

      actualTask.startTime.filter(_.isAfter(maxPriorityTask.createTime)) should not be None
      actualTask.updateTime.isAfter(maxPriorityTask.updateTime) should smartEqual(true)
      actualTask should smartEqual(expectedTask)
      checkTaskHistoryWasSaved(queue, actualTask.key, TaskAction.Take(user))
    }

    "return task if include_users contains user" in {
      val user1 = UserIdGen.next
      val user2 = UserIdGen.suchThat(_ != user1).next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next.copy(includeUsers = Set(user1, user2))
      val task = taskService.create(queue, taskSource).futureValue

      val actualTaskKey = taskService.take(queue, user1).futureValue.key
      val expectedTaskKey = task.key

      actualTaskKey shouldBe expectedTaskKey
    }

    "not return task if include_users does not contain user" in {
      val user = UserIdGen.next
      val otherUser = UserIdGen.suchThat(_ != user).next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next.copy(includeUsers = Set(user))
      taskService.create(queue, taskSource).futureValue

      taskService.take(queue, otherUser).shouldCompleteWithException[NotExistException]
    }

    "not save history if action failed" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val resolution = resolutionGen[TrueFalseResolution].next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val task = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(queue, task.key, user).futureValue
      taskService.complete(queue, task.key, resolution, isResolutionVerified = true).futureValue

      taskService.take(queue, user).failed.futureValue
      checkTaskHistoryWasNotSaved(queue, task.key, TaskAction.Take(user))
    }

    "not return COMPLETED task" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val resolution = resolutionGen[TrueFalseResolution].next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val task = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(queue, task.key, user).futureValue
      taskService.complete(queue, task.key, resolution, isResolutionVerified = true).futureValue

      whenReady(taskService.take(queue, user).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "not return task if user is excluded" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next

      val taskSource =
        taskSourceGen[PayloadSource.Snapshot]().next.copy(
          excludeUsers = Set(user)
        )
      taskService.create(queue, taskSource).futureValue

      whenReady(taskService.take(queue, user).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "fail if queue is empty" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next

      whenReady(taskService.take(queue, user).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "not to return parent task" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[Subtasks]().next

      val numChildren = taskSource.payload.asInstanceOf[Subtasks].tasks.size

      taskService.create(queue, taskSource).futureValue

      for (i <- 0 until numChildren) taskService.take(queue, user).futureValue

      whenReady(taskService.take(queue, user).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "not to return tasks with takeableAfter > now" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource =
        taskSourceGen[PayloadSource.Snapshot]().next.copy(takeableAfter = Some(DateTimeUtil.now().plusDays(1)))
      taskService.create(queue, taskSource).futureValue

      whenReady(taskService.take(queue, user).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "not return task with type = MANUAL_CHECK" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val initialTask = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(initialTask.queue, initialTask.key, user).futureValue
      taskService.complete(initialTask.queue, initialTask.key, resolution, isResolutionVerified = true).futureValue

      taskService.createManualCheckTasks(initialTask.descriptor, Set(user)).futureValue

      taskService.take(queue, user).shouldCompleteWithException[NotExistException]
    }
  }

  "takeByKey" should {

    "return correctly updated task" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val createTime = DateTimeGen.next
      val takeTime = dateTimeGen(after = createTime).next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val task = taskService.create(queue, taskSource, createTime = createTime).futureValue
      val key = task.key

      val actualTask = taskService.takeByKey(queue, key, user, takeTime).futureValue
      val expectedTask =
        task.copy(
          owner = Some(user),
          state = State.IN_PROGRESS,
          startTime = Some(takeTime),
          updateTime = takeTime
        )
      actualTask should smartEqual(expectedTask)
      checkTaskHistoryWasSaved(queue, actualTask.key, TaskAction.TakeByKey(user))
    }

    "not save history if action failed" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val resolution = resolutionGen[TrueFalseResolution].next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val task = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(queue, task.key, user).futureValue
      taskService.complete(queue, task.key, resolution, isResolutionVerified = true).futureValue

      taskService.takeByKey(queue, task.key, user).failed.futureValue
      checkTaskHistoryWasNotSaved(queue, task.key, TaskAction.TakeByKey(user))
    }

    "not return COMPLETED task" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val resolution = resolutionGen[TrueFalseResolution].next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val task = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(queue, task.key, user).futureValue
      taskService.complete(queue, task.key, resolution, isResolutionVerified = true).futureValue

      whenReady(taskService.takeByKey(queue, task.key, user).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "not return task if user is excluded" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next

      val taskSource =
        taskSourceGen[PayloadSource.Snapshot]().next.copy(
          excludeUsers = Set(user)
        )
      val task = taskService.create(queue, taskSource).futureValue

      whenReady(taskService.takeByKey(queue, task.key, user).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "return error if no such task" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val key = TaskKeyGen.next

      whenReady(taskService.takeByKey(queue, key, user).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "not return parent task" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[Subtasks]().next

      val key = taskService.create(queue, taskSource).futureValue.key

      whenReady(taskService.takeByKey(queue, key, user).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "not to return tasks with takeableAfter > now" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource =
        taskSourceGen[PayloadSource.Snapshot]().next.copy(takeableAfter = Some(DateTimeUtil.now().plusDays(1)))
      val key = taskService.create(queue, taskSource).futureValue.key

      whenReady(taskService.takeByKey(queue, key, user).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }

  "get" should {

    "return correct created snapshot task" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      val expectedTask = taskService.create(queue, taskSource).futureValue
      val actualTask = taskService.get(queue, expectedTask.key).futureValue

      actualTask should smartEqual(expectedTask)
    }

    "return correct created external task" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next

      val expectedTask = taskService.create(queue, taskSource).futureValue
      val actualTask = taskService.get(queue, expectedTask.key).futureValue

      actualTask should smartEqual(expectedTask)
    }

    "return correct created subtasks task" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[Subtasks]().next

      val expectedTask = taskService.create(queue, taskSource).futureValue
      val actualTask = taskService.get(queue, expectedTask.key).futureValue

      actualTask should smartEqual(expectedTask)
    }

    "fail for nonexistent key" in {
      val queue = QueueIdGen.next
      val nonExistentKey = TaskKeyGen.next

      whenReady(taskService.get(queue, nonExistentKey).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "fail for wrong queue" in {
      val queue = QueueId.TEST_QUEUE
      val wrongQueue = QueueId.AUTO_RU_CALL_CENTER
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      val key = taskService.create(queue, taskSource).futureValue.key

      whenReady(taskService.get(wrongQueue, key).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }

  "find/count" should {

    "return slice of tasks" in {
      val queue = QueueIdGen.next

      val taskSources = taskSourceGen[Subtasks]().next(3).toList

      createTasks(queue, taskSources)

      val tasks = taskService.find(queue, TaskFilter.No, Range(0, 10)).futureValue
      val count = taskService.count(queue, TaskFilter.No).futureValue
      count should smartEqual(Count(tasks.size))

      for (slice <- List(Range(0, 3), Range(0, 2), Range(1, 3))) {
        val actualTasks = taskService.find(queue, TaskFilter.No, slice).futureValue.toList
        val expectedTasks = tasks.slice(slice.from, slice.to).toList
        actualTasks should smartEqual(expectedTasks)
        assert(actualTasks.toSet == expectedTasks.toSet, s"wrong result with slice $slice")
      }
    }

    "return tasks from different queues" in {
      val queue1 = QueueId.TEST_QUEUE
      val taskSource1 = taskSourceGen[PayloadSource.Snapshot]().next
      val task1 = taskService.create(queue1, taskSource1).futureValue

      val queue2 = QueueId.REALTY_CALL_CENTER
      val taskSource2 = taskSourceGen[PayloadSource.Snapshot]().next
      val task2 = taskService.create(queue2, taskSource2).futureValue

      val filter = TaskFilter.No
      val sort = TaskSort.ByUpdateTime(asc = true)
      val slice = Range(0, 2)
      val actualTasks = taskService.find(filter, sort, slice).futureValue.values.toSet
      val expectedTasks = Set(task1, task2)

      actualTasks should smartEqual(expectedTasks)
    }

    "return correctly sorted tasks" in {
      val now = DateTimeUtil.now()
      val queue = QueueIdGen.next
      val taskSource1 = taskSourceGen[PayloadSource.External]().next
      val taskSource2 = taskSourceGen[PayloadSource.External]().next
      val task1 = taskService.create(queue, taskSource1, createTime = now.minusDays(2)).futureValue
      val task2 = taskService.create(queue, taskSource2, createTime = now.minusDays(1)).futureValue

      val filter = TaskFilter.No
      val sort = TaskSort.ByUpdateTime(asc = true)
      val slice = Range(0, 2)
      val actualResult = taskService.find(filter, sort, slice).futureValue.toList
      val expectedResult = List(task1, task2)

      actualResult should smartEqual(expectedResult)
    }

    "return correct tasks with Free filter (without createTime and takeableBy)" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next

      createInProgressTask(queue, user)
      createCompletedTask(queue, user)
      createCanceledTask(queue)
      createExpiredTask(queue)

      val sources = taskSourceGen[PayloadSource.External]().next(2).toList
      val expectedTasks = createTasks(queue, sources).toSet

      val filter =
        TaskFilter.Free(
          createTime = TimeInterval(None, None),
          takeableBy = Ignore,
          allowManualCheckTasks = true,
          similarityHash = Ignore
        )

      val actualTasks = taskService.find(queue, filter, Range(0, 5)).futureValue.toSet
      actualTasks should smartEqual(expectedTasks)

      val actualCount = taskService.count(queue, filter).futureValue
      actualCount should smartEqual(Count(2))
    }

    "return correct tasks with Free filter (with createTime and takeableBy)" in {
      val queue = QueueIdGen.next
      val userId = UserIdGen.next
      createTaskWithUpdateTime(queue, DateTimeUtil.now().minusDays(2))
      val expectedTask = createTaskWithUpdateTime(queue, DateTimeUtil.now())
      createTaskWithUpdateTime(
        queue,
        DateTimeUtil.now(),
        taskGen = taskSourceGen[PayloadSource.External]().map(
          _.copy(
            excludeUsers = Set(userId)
          )
        )
      )
      createTaskWithUpdateTime(queue, DateTimeUtil.now().plusDays(2))
      val expectedTasks = Set(expectedTask)

      val filter =
        TaskFilter.Free(
          createTime = TimeInterval(
            from = Some(DateTimeUtil.now().minusDays(1)),
            to = Some(DateTimeUtil.now().plusDays(1))
          ),
          takeableBy = Use(userId),
          allowManualCheckTasks = true,
          similarityHash = Ignore
        )

      val actualTasks = taskService.find(queue, filter, Range(0, 3)).futureValue.toSet
      actualTasks should smartEqual(expectedTasks)

      val actualCount = taskService.count(queue, filter).futureValue
      actualCount should smartEqual(Count(1))
    }

    "return correct tasks with Free filter (with takeableBy) for tasks with includeUsers" in {
      val queue = QueueIdGen.next
      val userId = UserIdGen.next
      val otherUserId = UserIdGen.suchThat(_ != userId).next
      val expectedTask1 =
        createTaskWithUpdateTime(
          queue,
          DateTimeUtil.now(),
          taskSourceGen[PayloadSource.External]().map(
            _.copy(
              includeUsers = Set.empty
            )
          )
        )
      val expectedTask2 =
        createTaskWithUpdateTime(
          queue,
          DateTimeUtil.now(),
          taskSourceGen[PayloadSource.External]().map(
            _.copy(
              includeUsers = Set(userId)
            )
          )
        )
      createTaskWithUpdateTime(
        queue,
        DateTimeUtil.now(),
        taskSourceGen[PayloadSource.External]().map(
          _.copy(
            includeUsers = Set(otherUserId)
          )
        )
      )

      val filter =
        TaskFilter.Free(
          createTime = TimeInterval(None, None),
          takeableBy = Use(userId),
          allowManualCheckTasks = true,
          similarityHash = Ignore
        )

      val expectedTasks = List(expectedTask1, expectedTask2).sortBy(_.key)
      val actualTasks = taskService.find(queue, filter, Range(0, 3)).futureValue.toList.sortBy(_.key)
      actualTasks should smartEqual(expectedTasks)

      val actualCount = taskService.count(queue, filter).futureValue
      val expectedCount = Count(expectedTasks.size)
      actualCount shouldBe expectedCount
    }

    "return correct tasks with Free filter (allowManualCheckTasks=false)" in {
      val user = UserIdGen.next
      val user2 = UserIdGen.suchThat(_ != user).next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val descriptor = taskService.create(queue, taskSource).futureValue.descriptor
      taskService.takeByKey(descriptor.queue, descriptor.key, user).futureValue
      taskService.complete(descriptor.queue, descriptor.key, resolution, isResolutionVerified = true).futureValue

      val ordinaryTaskKey = taskService.create(queue, taskSourceGen[PayloadSource.External]().next).futureValue.key
      taskService.createManualCheckTasks(descriptor, Set(user2)).futureValue.head.descriptor

      val filter =
        TaskFilter.Free(
          createTime = TimeInterval(None, None),
          takeableBy = Ignore,
          allowManualCheckTasks = false,
          similarityHash = Ignore
        )

      val actualResult = taskService.find(queue, filter, Range(0, 2)).futureValue.values.map(_.key).toSet
      val expectedResult = Set(ordinaryTaskKey)

      actualResult shouldBe expectedResult
    }

    "correctly work in case of Free filter with specified similarityHash" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next

      createInProgressTask(queue, user)
      createCompletedTask(queue, user)
      createCanceledTask(queue)
      createExpiredTask(queue)

      val taskSources =
        taskSourceGen[PayloadSource.External]()
          .next(5)
          .map(_.copy(similarityHashSource = Seq("this", "is", "hash", "ok?")))
          .toSeq
      val expectedTasks = createTasks(queue, taskSources).toSet
      val actualSimilarityHash = expectedTasks.head.similarityHash.get
      val filter =
        TaskFilter.Free(
          createTime = TimeInterval(None, None),
          takeableBy = Ignore,
          allowManualCheckTasks = true,
          similarityHash = Use(actualSimilarityHash)
        )

      val actualTasks = taskService.find(queue, filter, Range(0, 10)).futureValue.toSet
      actualTasks should smartEqual(expectedTasks)

      val actualCount = taskService.count(queue, filter).futureValue
      actualCount should smartEqual(Count(5))
    }

    "correctly work in case of Free filter with specified createTime, takeableBy and similarityHash" in {
      val queue = QueueIdGen.next
      val userId = UserIdGen.next
      val similarityHashSource = Seq("this", "is", "hash", "ok?")
      val taskGen1 =
        taskSourceGen[PayloadSource.External]().next
          .copy(similarityHashSource = similarityHashSource)
      val taskGen2 =
        taskSourceGen[PayloadSource.External]().next
          .copy(
            excludeUsers = Set(userId),
            similarityHashSource = similarityHashSource
          )

      createTaskWithUpdateTime(queue, DateTimeUtil.now().minusDays(2), taskGen1)
      createTaskWithUpdateTime(queue, DateTimeUtil.now(), taskGen = taskGen2)
      createTaskWithUpdateTime(queue, DateTimeUtil.now().plusDays(2))

      val expectedTask = createTaskWithUpdateTime(queue, DateTimeUtil.now(), taskGen1)
      val expectedTasks = Set(expectedTask)
      val actualSimilarityHash = expectedTask.similarityHash.get
      val filter =
        TaskFilter.Free(
          createTime = TimeInterval(
            from = Some(DateTimeUtil.now().minusDays(1)),
            to = Some(DateTimeUtil.now().plusDays(1))
          ),
          takeableBy = Use(userId),
          allowManualCheckTasks = true,
          similarityHash = Use(actualSimilarityHash)
        )

      val actualTasks = taskService.find(queue, filter, Range(0, 3)).futureValue.toSet
      actualTasks should smartEqual(expectedTasks)

      val actualCount = taskService.count(queue, filter).futureValue
      actualCount should smartEqual(Count(1))
    }

    "return correct tasks with InProgress filter" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val anotherUser = UserIdGen.next
      assume(user != anotherUser, "users must be different")

      createInProgressTask(queue, anotherUser)
      createCompletedTask(queue, user)
      createCanceledTask(queue)
      createExpiredTask(queue)

      val task1 = createInProgressTask(queue, user)
      val task2 = createInProgressTask(queue, user)
      val expectedTasks = Set(task1, task2)

      taskService.create(queue, taskSourceGen[PayloadSource.External]().next).futureValue

      val actualTasks = taskService.find(queue, TaskFilter.InProgress(user, Ignore), Range(0, 7)).futureValue.toSet
      actualTasks should smartEqual(expectedTasks)

      val actualCount = taskService.count(queue, TaskFilter.InProgress(user, Ignore)).futureValue
      actualCount should smartEqual(Count(2))
    }

    "correctly work in case of InProgress filter with specified similarityHash" in {
      val queue = QueueIdGen.next
      val anotherQueue = QueueIdGen.next
      val user = UserIdGen.next
      val anotherUser = UserIdGen.next
      assume(user != anotherUser, "users must be different")
      assume(queue != anotherQueue, "queues must be different")

      val similarityHashSource = Seq("this", "is", "hash", "ok?")

      createInProgressTask(queue, anotherUser)
      createCompletedTask(queue, user)
      createCanceledTask(queue)
      createExpiredTask(queue)
      createInProgressTask(queue, user)
      createInProgressTask(anotherQueue, user)

      val task1 = createInProgressTask(queue, user, similarityHashSource = similarityHashSource)
      val task2 = createInProgressTask(queue, user, similarityHashSource = similarityHashSource)
      val expectedTasks = Set(task1, task2)
      val actualSimilarityHash = task1.similarityHash.get
      val filter = TaskFilter.InProgress(user, Use(actualSimilarityHash))

      taskService.create(queue, taskSourceGen[PayloadSource.External]().next).futureValue

      val actualTasks = taskService.find(queue, filter, Range(0, 7)).futureValue.toSet
      actualTasks should smartEqual(expectedTasks)

      val actualCount = taskService.count(queue, filter).futureValue
      actualCount should smartEqual(Count(2))
    }

    "return correct tasks with ReadyToSend filter" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next

      createInProgressTask(queue, user)
      createSendingFailedTask(queue, user)

      val task1 = createReadyToSendTask(queue, user)
      val task2 = createReadyToSendTask(queue, user)
      createReadyToSendTaskWaitingForSendAfter(queue, user)
      val expectedTasks = Set(task1, task2)

      taskService.create(queue, taskSourceGen[PayloadSource.External]().next).futureValue

      val now = DateTimeUtil.now()
      val actualTasks = taskService.find(queue, TaskFilter.ReadyToSend(now), Range(0, 5)).futureValue.toSet
      actualTasks should smartEqual(expectedTasks)

      val actualCount = taskService.count(queue, TaskFilter.ReadyToSend(now)).futureValue
      actualCount should smartEqual(Count(2))
    }

    "return correct tasks with SendingFailed filter" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next

      createInProgressTask(queue, user)
      createReadyToSendTask(queue, user)

      val task1 = createSendingFailedTask(queue, user)
      val task2 = createSendingFailedTask(queue, user)
      val expectedTasks = Set(task1, task2)

      taskService.create(queue, taskSourceGen[PayloadSource.External]().next).futureValue

      val actualTasks = taskService.find(queue, TaskFilter.SendingFailed, Range(0, 5)).futureValue.toSet
      actualTasks should smartEqual(expectedTasks)

      val actualCount = taskService.count(queue, TaskFilter.SendingFailed).futureValue
      actualCount should smartEqual(Count(2))
    }

    "return correct tasks with UpdatedAfter filter" in {
      val queue = QueueIdGen.next

      createTaskWithUpdateTime(queue, DateTimeUtil.now().minusDays(1))
      val expectedTasks = Set(createTaskWithUpdateTime(queue, DateTimeUtil.now().plusDays(1)))

      val filter = TaskFilter.UpdatedAfter(DateTimeUtil.now())

      val actualTasks = taskService.find(queue, filter, Range(0, 2)).futureValue.toSet
      actualTasks should smartEqual(expectedTasks)

      val actualCount = taskService.count(queue, filter).futureValue
      actualCount should smartEqual(Count(1))
    }

    "return correct tasks with Parent filter" in {
      val queue = QueueIdGen.next

      val source1 = taskSourceGen[Subtasks]().next
      val source2 = taskSourceGen[Subtasks]().next
      val expectedTasks = createTasks(queue, Seq(source1, source2)).toSet

      val actualTasks = taskService.find(queue, TaskFilter.Parents, Range(0, 100)).futureValue.toSet
      actualTasks should smartEqual(expectedTasks)

      val actualCount = taskService.count(queue, TaskFilter.Parents).futureValue
      actualCount should smartEqual(Count(2))
    }

    "return correct tasks with Children filter" in {
      val queue = QueueIdGen.next

      val expectedCount = Count(2)
      val childSources = taskSourceGen[PayloadSource.External]().next(expectedCount).toList
      val source = TaskSourceGen.next.copy(payload = Subtasks(childSources))
      val parentKey = taskService.create(queue, source).futureValue.key
      taskService.create(queue, taskSourceGen[Subtasks]().next).futureValue

      val actualCount = taskService.count(queue, TaskFilter.Children(parentKey)).futureValue
      actualCount should smartEqual(expectedCount)
    }

    "correctly work in case of WithSimilarityHash filter" in {
      val queue = QueueIdGen.next
      val taskSources =
        taskSourceGen[PayloadSource.Snapshot]()
          .next(5)
          .map(_.copy(similarityHashSource = Seq("this", "is", "hash", "ok?")))
          .toSeq
      val anotherTaskSources = taskSourceGen[PayloadSource.Snapshot]().next(10).toSeq
      val similarTasks = createTasks(queue, taskSources).toList
      val actualSimilarityHash = similarTasks.head.similarityHash.get
      createTasks(queue, anotherTaskSources)
      val slice = Page(0, 10)

      val filter = TaskFilter.WithSimilarityHash(actualSimilarityHash)
      taskService.count(queue, filter).futureValue shouldBe Count(5)
      taskService.find(queue, filter, slice).futureValue shouldBe SlicedResult(similarTasks, 5, slice)
    }
  }

  "findWithOnCompleteActions" should {
    "find correct actions" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next

      val inProgressTask = createInProgressTask(queue, user, Set(StubActionId0))
      makeOnCompleteActionReadyToBePerformed(queue, inProgressTask.key, StubActionId0)
      val cancelledTask = createCanceledTask(queue, Set(StubActionId0))
      makeOnCompleteActionReadyToBePerformed(queue, cancelledTask.key, StubActionId0)
      val expiredTask = createExpiredTask(queue, Set(StubActionId0))
      makeOnCompleteActionReadyToBePerformed(queue, expiredTask.key, StubActionId0)
      val completedTask = createCompletedTask(queue, user, onCompleteActionIds = Set(StubActionId0, StubActionId1))
      makeOnCompleteActionReadyToBePerformed(queue, completedTask.key, StubActionId0)
      makeOnCompleteActionNotReadyToBePerformed(queue, completedTask.key, StubActionId1)

      val found =
        taskService
          .findWithOnCompleteActions(
            TaskWithOnCompleteActionsFilter.ReadyToPerformOnCompleteAction(DateTimeUtil.now()),
            TaskSort.ByUpdateTime(asc = true),
            Range(0, 100)
          )
          .futureValue

      found.size shouldBe 1
      val first = found.head
      first.task.key shouldBe completedTask.key
      first.actionsInfo.keySet shouldBe Set(StubActionId0)
    }
  }

  "putBack" should {

    "correctly return to queue taken task" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      Thread.sleep(1)
      taskService.putBack(queue, task.key).futureValue

      val actualTask = taskService.get(queue, task.key).futureValue
      val expectedTask =
        task.copy(
          owner = None,
          state = State.NEW,
          startTime = None,
          updateTime = actualTask.updateTime
        )

      actualTask.updateTime.isAfter(task.updateTime) should smartEqual(true)
      actualTask should smartEqual(expectedTask)
      checkTaskHistoryWasSaved(queue, actualTask.key, TaskAction.PutBack)
    }

    "not save history if action failed" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      val task = taskService.create(queue, taskSource).futureValue
      assume(task.state == State.NEW, "task state is not new")

      taskService.putBack(queue, task.key).failed.futureValue
      checkTaskHistoryWasNotSaved(queue, task.key, TaskAction.PutBack)
    }

    "error on task that already in queue" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      val task = taskService.create(queue, taskSource).futureValue
      assume(task.state == State.NEW, "task state is not new")

      whenReady(taskService.putBack(queue, task.key).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "error on completed task" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      taskService.complete(queue, task.key, resolution, isResolutionVerified = true).futureValue
      val completedTask = taskService.get(queue, task.key).futureValue
      assume(completedTask.state == State.COMPLETED, "task is not completed")

      whenReady(taskService.putBack(queue, task.key).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "error for wrong user" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val anotherUser = UserIdGen.next
      assume(user != anotherUser, "users are not different")
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue

      val operatorContext = OperatorContext("", anotherUser)
      whenReady(taskService.putBack(queue, task.key)(operatorContext).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "no error if ifUserOwner = false" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val anotherUser = UserIdGen.next
      assume(user != anotherUser, "users are not different")
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue

      taskService.putBack(queue, task.key, ifUserOwner = false).futureValue
    }

    "error for wrong queue" in {
      val queue = QueueId.TEST_QUEUE
      val wrongQueue = QueueId.AUTO_RU_CALL_CENTER
      val user = UserIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue

      whenReady(taskService.putBack(wrongQueue, task.key).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }
  }

  "redirect" should {

    "correctly redirect task" in {
      val originalQueue = QueueIdGen.next
      val user = UserIdGen.next
      val originalSource = TaskSourceGen.next
      taskService.create(originalQueue, originalSource).futureValue
      val originalKey = taskService.take(originalQueue, user).futureValue.key

      val redirectQueue = QueueIdGen.suchThat(_ != originalQueue).next
      val oc = OperatorContext("", user)
      val derivedTask = taskService.redirect(originalQueue, originalKey, redirectQueue)(oc).futureValue
      val originalTask = taskService.get(originalQueue, originalKey).futureValue

      derivedTask.payload should smartEqual(originalTask.payload)
      derivedTask.queue should smartEqual(redirectQueue)
      derivedTask.comment should smartEqual(originalTask.comment)
      derivedTask.originalTask should smartEqual(Some(originalTask.descriptor))

      originalTask.state should smartEqual(State.REDIRECTED)
      originalTask.derivedTask should smartEqual(Some(derivedTask.descriptor))
      originalTask.finishTime.isDefined should smartEqual(true)

      checkTaskHistoryWasSaved(originalQueue, originalKey, TaskAction.Redirect)
      checkTaskHistoryWasSaved(redirectQueue, derivedTask.key, TaskAction.Create)
    }

    "not save history if action failed" in {
      val originalQueue = QueueIdGen.next
      val originalSource = TaskSourceGen.suchThat(!_.payload.isInstanceOf[PayloadSource.Subtasks]).next
      val originalKey = taskService.create(originalQueue, originalSource).futureValue.key

      val redirectQueue = QueueIdGen.next

      taskService.redirect(originalQueue, originalKey, redirectQueue).failed.futureValue
      checkTaskHistoryWasNotSaved(originalQueue, originalKey, TaskAction.Redirect)
    }

    "return error if original task state is not IN_PROGRESS" in {
      val originalQueue = QueueIdGen.next
      val originalSource = TaskSourceGen.suchThat(!_.payload.isInstanceOf[Subtasks]).next
      val originalKey = taskService.create(originalQueue, originalSource).futureValue.key

      val redirectQueue = QueueIdGen.next

      whenReady(taskService.redirect(originalQueue, originalKey, redirectQueue).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "return error if user is not an owner of the task" in {
      val originalQueue = QueueIdGen.next
      val originalSource = TaskSourceGen.next
      val user = UserIdGen.next
      val otherUser = UserIdGen.suchThat(_ != user).next

      taskService.create(originalQueue, originalSource).futureValue
      val originalKey = taskService.take(originalQueue, user).futureValue.key

      val redirectQueue = QueueIdGen.next

      val oc = OperatorContext("", otherUser)
      whenReady(taskService.redirect(originalQueue, originalKey, redirectQueue)(oc).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "return error if redirect twice" in {
      val originalQueue = QueueIdGen.next
      val user = UserIdGen.next
      val originalSource = TaskSourceGen.next
      taskService.create(originalQueue, originalSource).futureValue
      val originalKey = taskService.take(originalQueue, user).futureValue.key

      val redirectQueue = QueueIdGen.suchThat(_ != originalQueue).next

      taskService.redirect(originalQueue, originalKey, redirectQueue).futureValue
      whenReady(taskService.redirect(originalQueue, originalKey, redirectQueue).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "return fake redirected task for check tasks" in {
      val originalQueue = QueueIdGen.next
      val redirectQueue = QueueIdGen.suchThat(_ != originalQueue).next
      val user = UserIdGen.next
      val originalSource = TaskSourceGen.suchThat(!_.payload.isInstanceOf[Subtasks]).next
      val resolution = resolutionGen[TrueFalseResolution].next

      val originalTask = taskService.create(originalQueue, originalSource).futureValue
      taskService.takeByKey(originalQueue, originalTask.key, user).futureValue
      taskService.complete(originalQueue, originalTask.key, resolution, isResolutionVerified = true).futureValue

      val checkTaskKey = taskService.createManualCheckTasks(originalTask.descriptor, Set(user)).futureValue.head.key
      taskService.takeByKey(originalQueue, checkTaskKey, user).futureValue
      val derivedCheckTask = taskService.redirect(originalQueue, checkTaskKey, redirectQueue).futureValue

      val checkTask = taskService.get(originalQueue, checkTaskKey).futureValue

      val fakeDescriptor = TaskDescriptor(redirectQueue, Task.FakeKey)

      derivedCheckTask.descriptor shouldBe fakeDescriptor
      checkTask.state shouldBe State.COMPLETED
      checkTask.derivedTask shouldBe Some(fakeDescriptor)
    }
  }

  "complete" should {

    "correct set task resolution" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val expectedResolution = resolutionGen[TrueFalseResolution].next

      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      assume(task.state == State.IN_PROGRESS, "task not in progress")

      taskService.complete(queue, task.key, expectedResolution, isResolutionVerified = true).futureValue

      val actualResolution = taskService.get(queue, task.key).futureValue.resolution

      actualResolution should smartEqual(Some(expectedResolution))

      checkTaskHistoryWasSaved(queue, task.key, TaskAction.Complete)
    }

    "not save history if action failed" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val task = taskService.create(queue, taskSource).futureValue
      assume(task.state == State.NEW, "task is not new")

      taskService.complete(queue, task.key, resolution, isResolutionVerified = true).failed.futureValue
      checkTaskHistoryWasNotSaved(queue, task.key, TaskAction.Complete)
    }

    "error on already completed task" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      taskService.complete(queue, task.key, resolution, isResolutionVerified = true).futureValue

      whenReady(taskService.complete(queue, task.key, resolution, isResolutionVerified = true).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "error on new task" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val task = taskService.create(queue, taskSource).futureValue
      assume(task.state == State.NEW, "task is not new")

      whenReady(taskService.complete(queue, task.key, resolution, isResolutionVerified = true).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "error for wrong user" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val anotherUser = UserIdGen.next
      assume(user != anotherUser, "users are not different")
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      assume(task.state == State.IN_PROGRESS, "task is not in progress")

      val oc = OperatorContext("", anotherUser)
      whenReady(taskService.complete(queue, task.key, resolution, isResolutionVerified = true)(oc).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "no error if ifUserOwner = false" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val anotherUser = UserIdGen.next
      assume(user != anotherUser, "users are not different")
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      assume(task.state == State.IN_PROGRESS, "task is not in progress")

      val oc = OperatorContext("", anotherUser)
      taskService
        .complete(queue, task.key, resolution, ifUserOwner = false, isResolutionVerified = true)(oc)
        .futureValue
    }

    "error for wrong queue" in {
      val queue = QueueId.TEST_QUEUE
      val wrongQueue = QueueId.AUTO_RU_CALL_CENTER
      val user = UserIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      assume(task.state == State.IN_PROGRESS, "task is not in progress")

      whenReady(taskService.complete(wrongQueue, task.key, resolution, isResolutionVerified = true).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "save previous resolution to prevResolution" in {
      val queue = QueueId.TELEPONY_AUTORU_PAID_CALL
      val user = UserIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val firstResolution = resolutionGen[TrueFalseResolution].next
      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      assume(task.state == State.IN_PROGRESS, "task not in progress")
      val newResolution = resolutionGen[TrueFalseResolution].suchThat(_ != firstResolution).next
      taskService.complete(queue, task.key, firstResolution, isResolutionVerified = false).futureValue
      val task1 = taskService.get(queue, task.key).futureValue
      task1.state shouldBe State.NEED_VERIFICATION
      taskService.complete(queue, task.key, newResolution, isResolutionVerified = true).futureValue
      val actualTask = taskService.get(queue, task.key).futureValue
      actualTask.resolution should smartEqual(Some(newResolution))
      actualTask.prevResolution should smartEqual(Some(firstResolution))
      actualTask.state shouldBe State.COMPLETED
    }

    "fail to save twice not verified resolution" in {
      val queue = QueueId.TELEPONY_AUTORU_PAID_CALL
      val user = UserIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val firstResolution = resolutionGen[TrueFalseResolution].next
      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      assume(task.state == State.IN_PROGRESS, "task not in progress")
      val newResolution = resolutionGen[TrueFalseResolution].suchThat(_ != firstResolution).next
      taskService.complete(queue, task.key, firstResolution, isResolutionVerified = false).futureValue
      taskService
        .complete(queue, task.key, newResolution, isResolutionVerified = false)
        .shouldCompleteWithException[NotExistException]
    }

    "not save previous resolution if new resolution equal to previous" in {
      val queue = QueueId.TELEPONY_AUTORU_PAID_CALL
      val user = UserIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val firstResolution = resolutionGen[TrueFalseResolution].next
      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      assume(task.state == State.IN_PROGRESS, "task not in progress")
      val newResolution = firstResolution
      taskService.complete(queue, task.key, firstResolution, isResolutionVerified = false).futureValue
      taskService.complete(queue, task.key, newResolution, isResolutionVerified = true).futureValue
      val actualTask = taskService.get(queue, task.key).futureValue
      actualTask.resolution should smartEqual(Some(newResolution))
      actualTask.prevResolution shouldBe None
      actualTask.state shouldBe State.COMPLETED
    }
  }

  "update" should {

    "correctly updates all allowable fields" in {
      val queue = QueueIdGen.next
      val oldPriority = 10
      val oldExpire = DateTimeUtil.now().plusDays(10)
      val taskSource =
        taskSourceGen[PayloadSource.Snapshot]().next.copy(
          priority = Some(oldPriority)
        )

      val task = taskService.create(queue, taskSource).futureValue
      val newPriority = oldPriority + 1
      val newExpireTime = oldExpire.plusDays(1)
      val newNotificationSent = !task.notificationInfo.notificationSent
      val newTryCountLeft = task.notificationInfo.tryCountLeft - 1
      val newSendAfter = task.notificationInfo.sendAfter.plusMinutes(5)
      val newComment = task.comment + "abc"
      val newMinTakeTime = task.takeableAfter.plusDays(1)
      val newDeferCountLeft = task.deferCountLeft + 1
      val newTag = task.tag + "abc"

      Thread.sleep(1)
      taskService
        .update(
          queue = queue,
          key = task.key,
          updateRequest = TaskUpdateRequest(
            priority = Use(newPriority),
            expireTime = Use(newExpireTime),
            notificationSent = Use(newNotificationSent),
            tryCountLeft = Use(newTryCountLeft),
            sendAfter = Use(newSendAfter),
            comment = Use(newComment),
            takeableAfter = Use(newMinTakeTime),
            deferCountLeft = Use(newDeferCountLeft),
            tag = Use(newTag)
          )
        )
        .futureValue

      val actualTask = taskService.get(queue, task.key).futureValue
      val expectedTask =
        task.copy(
          priority = newPriority,
          expireTime = newExpireTime,
          notificationInfo = task.notificationInfo.copy(
            notificationSent = newNotificationSent,
            tryCountLeft = newTryCountLeft,
            sendAfter = newSendAfter
          ),
          comment = newComment,
          takeableAfter = newMinTakeTime,
          deferCountLeft = newDeferCountLeft,
          tag = newTag,
          updateTime = actualTask.updateTime
        )

      actualTask.updateTime.isAfter(task.updateTime) should smartEqual(true)

      actualTask should smartEqual(expectedTask)
    }

    "correctly updates by filter" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val oldUpdateTime = DateTimeUtil.now().minusDays(1)
      val freeTask = taskService.create(queue, taskSource, createTime = oldUpdateTime).futureValue
      createCanceledTask(queue)

      val filter =
        TaskFilter.Free(
          createTime = TimeInterval(None, None),
          takeableBy = Ignore,
          allowManualCheckTasks = true,
          similarityHash = Ignore
        )
      val newPriority = freeTask.priority + 1
      val updateRequest = TaskUpdateRequest(priority = Use(newPriority))

      val actualCount = taskService.update(queue, filter, updateRequest).futureValue
      val expectedCount = Count(1)
      actualCount shouldBe expectedCount

      val updatedTask = taskService.get(queue, freeTask.key).futureValue
      val updatedPriority = updatedTask.priority
      val updatedUpdateTime = updatedTask.updateTime
      updatedPriority shouldBe newPriority
      (updatedUpdateTime.isAfter(oldUpdateTime)) shouldBe true
    }

    "return error if user is not owner" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val task = taskService.create(queue, taskSource).futureValue
      val updateRequest = TaskUpdateRequest()
      val rc = OperatorContext("", UserIdGen.next)
      whenReady(taskService.update(queue, task.key, updateRequest)(rc).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "not return error if ifUserOwner = false" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val task = taskService.create(queue, taskSource).futureValue
      val updateRequest = TaskUpdateRequest()
      val rc = OperatorContext("", UserIdGen.next)
      taskService.update(queue, task.key, updateRequest, ifUserOwner = false)(rc).futureValue
    }
  }

  "cancel" should {

    "correct cancel task" in {
      val queue = QueueIdGen.next
      val subtasksTaskSource = taskSourceGen[Subtasks]().next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val slice = Range(0, 1000)

      val tasks = createTasks(queue, Seq(subtasksTaskSource, taskSource))
      val parentTask = tasks.head
      assume(
        parentTask.payload match {
          case _: Payload.Subtasks => true
          case _                   => false
        },
        "task is not a parent"
      )
      val children =
        taskService.find(queue, TaskFilter.No, slice).futureValue.filter {
          _.parent contains parentTask.key
        }

      taskService.cancel(queue, parentTask.key).futureValue

      val actualTasks =
        taskService.find(queue, TaskFilter.No, slice).futureValue.filter {
          _.state == State.CANCELED
        }
      val actualKeys = actualTasks.map(_.key).toSet
      val expectedKeys = (children.toSet + parentTask).map(_.key)

      actualKeys should smartEqual(expectedKeys)
      actualKeys.foreach(key => checkTaskHistoryWasSaved(queue, key, TaskAction.Cancel))
    }

    "not save history if action failed" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val derivedQueue = QueueIdGen.suchThat(_ != queue).next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      taskService.create(queue, taskSource).futureValue
      val key = taskService.take(queue, user).futureValue.key
      taskService.redirect(queue, key, derivedQueue).futureValue

      taskService.cancel(queue, key).failed.futureValue
      checkTaskHistoryWasNotSaved(queue, key, TaskAction.Cancel)
    }

    "return error if task in REDIRECT state" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val derivedQueue = QueueIdGen.suchThat(_ != queue).next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      taskService.create(queue, taskSource).futureValue
      val key = taskService.take(queue, user).futureValue.key
      taskService.redirect(queue, key, derivedQueue).futureValue

      whenReady(taskService.cancel(queue, key).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "return error if derived in terminal state" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val derivedQueue = QueueIdGen.suchThat(_ != queue).next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      taskService.create(queue, taskSource).futureValue
      val key = taskService.take(queue, user).futureValue.key
      val derivedKey = taskService.redirect(queue, key, derivedQueue).futureValue.key

      taskService.cancel(derivedQueue, derivedKey).futureValue

      whenReady(taskService.cancel(queue, key).failed) { e =>
        e shouldBe a[NotExistException]
      }

      val task = taskService.get(queue, key).futureValue
      val derivedTask = taskService.get(derivedQueue, derivedKey).futureValue

      task.state should smartEqual(State.REDIRECTED)
      derivedTask.state should smartEqual(State.CANCELED)
    }

    "return error on nonexistent task" in {
      val queue = QueueIdGen.next
      val key = TaskKeyGen.next

      whenReady(taskService.cancel(queue, key).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "return error on completed task" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val resolution = resolutionGen[TrueFalseResolution].next
      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      taskService.complete(queue, task.key, resolution, isResolutionVerified = true).futureValue
      whenReady(taskService.cancel(queue, task.key).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "return error on canceled task" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val task = taskService.create(queue, taskSource).futureValue
      taskService.cancel(queue, task.key).futureValue
      whenReady(taskService.cancel(queue, task.key).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "return error for wrong user" in {
      val user = UserIdGen.next
      val wrongUser = UserIdGen.next
      assume(user != wrongUser, "users must be different")
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      val rc = OperatorContext("", wrongUser)
      whenReady(taskService.cancel(queue, task.key)(rc).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "not return error if ifUserOwner = false" in {
      val user = UserIdGen.next
      val wrongUser = UserIdGen.next
      assume(user != wrongUser, "users must be different")
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      val rc = OperatorContext("", wrongUser)
      taskService.cancel(queue, task.key, ifUserOwner = false)(rc).futureValue
    }

    "return error if task in_progress for AutomatedContext" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      whenReady(taskService.cancel(queue, task.key).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "cancel task if task in_progress for OperatorContext" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      taskService.create(queue, taskSource).futureValue
      val task = taskService.take(queue, user).futureValue
      val rc = OperatorContext("", user)
      taskService.cancel(queue, task.key)(rc).futureValue
    }
  }

  "finishOriginal" should {

    "finish task if derived is completed" in {
      val user = UserIdGen.next
      val originalQueue = QueueIdGen.next
      val originalSource = taskSourceGen[PayloadSource.Snapshot]().next
      val originalKey = taskService.create(originalQueue, originalSource).futureValue.key

      val takenOriginalKey = taskService.take(originalQueue, user).futureValue.key
      assume(originalKey == takenOriginalKey, "key of the taken task must be originalKey")

      val redirectQueue = QueueIdGen.suchThat(_ != originalQueue).next
      val redirectKey = taskService.redirect(originalQueue, originalKey, redirectQueue).futureValue.key

      val takenRedirectKey = taskService.take(redirectQueue, user).futureValue.key
      assume(takenRedirectKey == redirectKey, "key of the taken task must be redirectKey")

      val resolution = resolutionGen[TrueFalseResolution].next
      taskService.complete(redirectQueue, redirectKey, resolution, isResolutionVerified = true).futureValue

      val derived = taskService.get(redirectQueue, redirectKey).futureValue
      taskService.finishOriginal(derived).futureValue

      val original = taskService.get(originalQueue, originalKey).futureValue
      original.state should smartEqual(State.COMPLETED)
      original.resolution should smartEqual(Some(resolution))

      checkTaskHistoryWasSaved(originalQueue, originalKey, TaskAction.FinishOriginal(State.COMPLETED))
    }

    "finish task if derived is canceled" in {
      val user = UserIdGen.next
      val originalQueue = QueueIdGen.next
      val originalSource = taskSourceGen[PayloadSource.Snapshot]().next
      val originalKey = taskService.create(originalQueue, originalSource).futureValue.key

      val takenOriginalKey = taskService.take(originalQueue, user).futureValue.key
      assume(originalKey == takenOriginalKey, "key of the taken task must be originalKey")

      val redirectQueue = QueueIdGen.suchThat(_ != originalQueue).next
      val redirectKey = taskService.redirect(originalQueue, originalKey, redirectQueue).futureValue.key

      taskService.cancel(redirectQueue, redirectKey).futureValue

      val derived = taskService.get(redirectQueue, redirectKey).futureValue
      taskService.finishOriginal(derived).futureValue

      val original = taskService.get(originalQueue, originalKey).futureValue
      original.state should smartEqual(State.CANCELED)
      original.resolution should smartEqual(None)

      checkTaskHistoryWasSaved(originalQueue, originalKey, TaskAction.FinishOriginal(State.CANCELED))
    }

    "return error if original doesn't exist" in {
      val derived =
        TaskGen.next.copy(
          originalTask = Some(TaskDescriptorGen.next),
          notificationInfo = NotificationInfoGen.suchThat(_.response.isEmpty).next,
          state = State.CANCELED
        )

      whenReady(taskService.finishOriginal(derived).failed) { e =>
        e shouldBe a[NotExistException]
      }
    }

    "throw exception if derived task not in the terminal state" in {
      val derived =
        TaskGen.next.copy(
          originalTask = Some(TaskDescriptorGen.next),
          notificationInfo = NotificationInfoGen.suchThat(_.response.isEmpty).next,
          state = StateGen.suchThat(NotTerminalStates.contains).next
        )
      whenReady(taskService.finishOriginal(derived).failed) { e =>
        e shouldBe a[IllegalStateException]
      }
    }

    "throw exception if derived task doesn't have original task" in {
      val derived =
        TaskGen.next.copy(
          originalTask = None,
          state = State.CANCELED
        )
      whenReady(taskService.finishOriginal(derived).failed) { e =>
        e shouldBe a[IllegalStateException]
      }
    }
  }

  "getSalaryStatistics" should {

    "return empty result if no salary statistics" in {
      val period = TimeInterval(None, None)
      val actualResult = taskService.getSalaryStatistics(period).futureValue
      val expectedResult = SalaryStatisticsReport(period, Map.empty)
      actualResult shouldBe expectedResult
    }

    "return correct salary statistics" in {
      val period = TimeInterval(None, None)
      val queue = QueueIdGen.next
      val redirectQueue = QueueIdGen.suchThat(_ != queue).next
      val user1 = UserIdGen.next
      val user2 = UserIdGen.next
      val resolution = resolutionGen[TrueFalseResolution].next

      val taskSources = taskSourceGen[PayloadSource.External]().next(4).toList
      createTasks(queue, taskSources)

      val costs1 =
        for (_ <- 1 to 2) yield {
          val task = taskService.take(queue, user1).futureValue
          taskService.complete(queue, task.key, resolution, isResolutionVerified = true).futureValue
          task.cost
        }
      val costs2 =
        for (_ <- 1 to 2) yield {
          val task = taskService.take(queue, user2).futureValue
          taskService.redirect(queue, task.key, redirectQueue).futureValue
          task.cost
        }

      val actualResult = taskService.getSalaryStatistics(period).futureValue

      val expectedStatistics1 = Map(queue -> CountSalary(2, costs1.sum))
      val expectedStatistics2 = Map(queue -> CountSalary(2, costs2.sum))
      val expectedMap = Map(user1 -> expectedStatistics1, user2 -> expectedStatistics2)
      val expectedResult = SalaryStatisticsReport(period, expectedMap)

      actualResult should smartEqual(expectedResult)
    }

    "return salary statistics only in the specified interval" in {
      val period =
        TimeInterval(
          from = Some(DateTimeUtil.now().minusDays(5)),
          to = Some(DateTimeUtil.now().plusDays(5))
        )
      val beforePeriod = DateTimeUtil.now().minusDays(10)
      val afterPeriod = DateTimeUtil.now().plusDays(10)

      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val resolution = resolutionGen[TrueFalseResolution].next

      val taskSourcesNotInPeriod = taskSourceGen[PayloadSource.External]().map(_.copy(cost = Some(1))).next(2).toList
      createTasks(queue, taskSourcesNotInPeriod)

      val keyBefore = taskService.take(queue, user).futureValue.key
      taskService
        .complete(queue, keyBefore, resolution, finishTime = beforePeriod, isResolutionVerified = true)
        .futureValue

      val keyAfter = taskService.take(queue, user).futureValue.key
      taskService
        .complete(queue, keyAfter, resolution, finishTime = afterPeriod, isResolutionVerified = true)
        .futureValue

      val taskSources = taskSourceGen[PayloadSource.External]().map(_.copy(cost = Some(1))).next(2).toList
      createTasks(queue, taskSources)

      for (_ <- 1 to 2) {
        val key = taskService.take(queue, user).futureValue.key
        taskService.complete(queue, key, resolution, isResolutionVerified = true).futureValue
      }

      val actualResult = taskService.getSalaryStatistics(period).futureValue

      val expectedStatistics = Map(queue -> CountSalary(2, 2))
      val expectedMap = Map(user -> expectedStatistics)
      val expectedResult = SalaryStatisticsReport(period, expectedMap)

      actualResult should smartEqual(expectedResult)
    }

    "return salary statistics filtered by user" in {
      val queue = QueueIdGen.next

      val user1 = UserIdGen.next
      val cost = createCompletedTask(queue, user1).cost

      val user2 = UserIdGen.suchThat(_ != user1).next
      createCompletedTask(queue, user2)

      val period = TimeInterval(None, None)
      val filter = SalaryStatisticsReportFilter(users = Set(user1), queues = Set.empty)

      val actualResult = taskService.getSalaryStatistics(period, filter).futureValue
      val expectedResult =
        SalaryStatisticsReport(
          period = period,
          statistics = Map(
            user1 -> Map(queue -> CountSalary(1, cost))
          )
        )

      actualResult shouldBe expectedResult
    }

    "return salary statistics filtered by queue" in {
      val user = UserIdGen.next

      val queue1 = QueueIdGen.next
      val cost = createCompletedTask(queue1, user).cost

      val queue2 = QueueIdGen.suchThat(_ != queue1).next
      createCompletedTask(queue2, user)

      val period = TimeInterval(None, None)
      val filter = SalaryStatisticsReportFilter(users = Set.empty, queues = Set(queue1))

      val actualResult = taskService.getSalaryStatistics(period, filter).futureValue
      val expectedResult =
        SalaryStatisticsReport(
          period = period,
          statistics = Map(
            user -> Map(queue1 -> CountSalary(1, cost))
          )
        )

      actualResult shouldBe expectedResult
    }
  }

  "delete" should {

    "correct delete tasks" in {
      val numTasks = 3
      val queue = QueueIdGen.next
      val taskSources = taskSourceGen[PayloadSource.Snapshot]().next(numTasks).toList
      val taskSourceWithAction = taskSourceGen[PayloadSource.Snapshot](onCompleteActionIds = Set(StubActionId0)).next

      val tasks = createTasks(queue, taskSources :+ taskSourceWithAction)

      val task = tasks.head
      val tasksToDelete = tasks.tail

      taskService.delete(tasksToDelete.map(_.key)).futureValue

      val slice = Range(0, numTasks + 1)

      val allTasks = taskService.find(queue, TaskFilter.No, slice).futureValue
      allTasks should contain(task)
      allTasks should not contain tasksToDelete.head
      allTasks should not contain tasksToDelete.last

      tasksToDelete.foreach(task => checkTaskHistoryWasNotSaved(queue, task.key, TaskAction.Create))
      checkOnCompleteActionDoesNotExist(tasks.last.key, StubActionId0)
    }
  }

  "markExpired" should {

    "mark as expired correctly" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val task = taskService.create(queue, taskSource).futureValue
      val currentTime = DateTimeUtil.now()
      taskService.markAsExpired(task.descriptor, currentTime).futureValue
      val actualTask = taskService.get(task.queue, task.key).futureValue
      actualTask.state shouldBe State.EXPIRED
      actualTask.finishTime shouldBe Some(currentTime)
      checkTaskHistoryWasSaved(queue, task.key, TaskAction.Expire)
    }
  }

  "unlock" should {

    "unlock task correctly" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val maxLockDuration = QueueSettings.defaultForQueue(queue).mutable.defaultMaxLockDuration

      taskService.create(queue, taskSourceGen[PayloadSource.External]().next).futureValue

      val currentTime = DateTimeUtil.now()

      val task1StartTime = currentTime.minusSeconds(maxLockDuration.toSeconds.toInt + 1)
      val task1Key = taskService.create(queue, taskSourceGen[PayloadSource.External]().next).futureValue.key
      taskService.takeByKey(queue, task1Key, user, takeTime = task1StartTime).futureValue

      val task2StartTime = currentTime
      val task2Key = taskService.create(queue, taskSourceGen[PayloadSource.External]().next).futureValue.key
      taskService.takeByKey(queue, task2Key, user, takeTime = task2StartTime).futureValue

      val unlockableTasks =
        taskService
          .find(
            TaskFilter.TooLongLocked(currentTime),
            ByCreateTime(asc = true),
            Range(0, 10)
          )
          .futureValue

      unlockableTasks.foreach { task =>
        taskService.unlock(task.descriptor).futureValue
      }

      val unlockedCount = unlockableTasks.size
      unlockedCount should smartEqual(1)

      val task1 = taskService.get(queue, task1Key).futureValue
      val task2 = taskService.get(queue, task2Key).futureValue

      task1.state should smartEqual(State.NEW)
      task2.state should smartEqual(State.IN_PROGRESS)

      checkTaskHistoryWasSaved(task1.queue, task1.key, TaskAction.Unlock)
    }
  }

  "findReadyToDeleteTasks" should {

    "return correct tasks" in {
      val queue = QueueId.TEST_QUEUE
      val user = UserIdGen.next
      val resolution = resolutionGen[TrueFalseResolution].next
      val notSubtasksTaskSources = taskSourceGen[PayloadSource.Snapshot]().next(25).toList
      val subtasksTaskSources = taskSourceGen[Subtasks]().next(7).toList
      val taskSources = (notSubtasksTaskSources ++ subtasksTaskSources).toSet

      val notifiedTaskSources = Random.shuffle(taskSources).take(12)
      val notifiedTasks = createTasks(queue, notifiedTaskSources.toSeq)
      for (task <- notifiedTasks)
        taskService.update(queue, task.key, TaskUpdateRequest(notificationSent = Use(true))).futureValue

      for (_ <- 0 until 12) {
        val task = taskService.take(queue, user).futureValue
        taskService.complete(queue, task.key, resolution, isResolutionVerified = true).futureValue
      }

      val limit = 7
      val storageDurationPerQueue = notifiedTasks.map(t => t.queue -> 0.seconds).toMap
      val readyToDeleteTasks =
        storageDurationPerQueue.foldLeft(Iterable.empty[(TaskKey, QueueId)]) { case (acc, (queue, duration)) =>
          val res = taskService.findReadyToDelete(queue, duration, limit, DateTimeUtil.now()).futureValue
          res ++ acc
        }
      readyToDeleteTasks.size should be <= limit

      val slice = Range(0, 1000)
      readyToDeleteTasks.foreach { case (key, queueId) =>
        val task = taskService.get(queueId, key).futureValue
        assert(TerminalStates.contains(task.state), s"task not in terminal state: $task")
        assert(task.notificationInfo.notificationSent, s"notification not sent: $task")
        task.payload match {
          case _: Payload.Subtasks =>
            val children =
              taskService.find(queue, TaskFilter.No, slice).futureValue.filter {
                _.parent contains task.key
              }
            assert(children.isEmpty, s"children was not deleted. task: $task, children: $children")
          case _: Payload.Snapshot | _: Payload.External =>
            task.parent match {
              case Some(parentKey) =>
                val parent = taskService.get(queue, parentKey).futureValue
                assert(
                  TerminalStates.contains(parent.state),
                  s"task parent not in terminal state. task: $task, parent: $parent"
                )
              case None =>
            }
        }
      }
    }

    "return tasks with different storage duration correctly " in {
      val queue1 = QueueId.TEST_QUEUE
      val queue2 = QueueId.AUTO_RU_TEST
      val user = UserIdGen.next

      val task1 = createCompletedTask(queue1, user, createTime = DateTimeUtil.now().minusSeconds(60))
      val task2 = createCompletedTask(queue1, user, createTime = DateTimeUtil.now().minusSeconds(360))
      val tasks = Seq(task1, task2)
      val otherTasks = Seq.fill(10)(createCompletedTask(queue2, user))

      val limit = (tasks ++ otherTasks).size
      val readyToDeleteTasks1 =
        taskService.findReadyToDelete(queue1, 120.seconds, limit, DateTimeUtil.now()).futureValue
      val readyToDeleteTasks2 = taskService.findReadyToDelete(queue2, 0.seconds, limit, DateTimeUtil.now()).futureValue
      val readyToDeleteTasks = readyToDeleteTasks1 ++ readyToDeleteTasks2
      readyToDeleteTasks.size shouldBe limit - 1
      readyToDeleteTasks.exists(_._1 == task1.key) shouldBe false
    }
  }

  "completeParents" should {

    "successfully complete parent task" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val resolution = resolutionGen[TrueFalseResolution].next

      val task = createParentWithCompletedChildren(queue, resolution, user)
      val updated = taskService.completeParents(limit = 1).futureValue
      updated should smartEqual(1)
      val completedParent = taskService.get(queue, task.key).futureValue

      completedParent.state should smartEqual(State.COMPLETED)
      checkTaskHistoryWasSaved(task.queue, task.key, TaskAction.Complete)
    }

    "complete no more than limit" in {
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val resolution = resolutionGen[TrueFalseResolution].next
      val limit = 2

      val key1 = createParentWithCompletedChildren(queue, resolution, user).key
      val key2 = createParentWithCompletedChildren(queue, resolution, user).key
      val key3 = createParentWithCompletedChildren(queue, resolution, user).key

      val updated = taskService.completeParents(limit = limit).futureValue
      updated should smartEqual(limit)
      val parent1 = taskService.get(queue, key1).futureValue
      val parent2 = taskService.get(queue, key2).futureValue
      val parent3 = taskService.get(queue, key3).futureValue
      val completedCount = Seq(parent1, parent2, parent3).count(_.state == State.COMPLETED)

      completedCount should smartEqual(limit)
    }
  }

  "getLastCreatedByQualifier" should {

    "successfully return correct task" in {
      val queue = QueueIdGen.next
      val qualifier = stringGen(5, 7).next

      val taskSource1 = taskSourceGen[PayloadSource.External]().next.copy(qualifier = Some(qualifier))
      taskService.create(queue, taskSource1, createTime = DateTimeUtil.now().minusDays(1)).futureValue

      val taskSource2 = taskSourceGen[PayloadSource.External]().next.copy(qualifier = Some(qualifier))
      val expectedTask = taskService.create(queue, taskSource2, createTime = DateTimeUtil.now()).futureValue

      val actualTask = taskService.getLastCreatedByQualifier(queue, qualifier).futureValue

      actualTask should smartEqual(Some(expectedTask))
    }

    "return None if no task with specified qualifier" in {
      val queue = QueueIdGen.next
      val qualifier = stringGen(5, 7).next

      val actualTask = taskService.getLastCreatedByQualifier(queue, qualifier).futureValue
      actualTask should smartEqual(None)
    }
  }

  "takeManualCheckTask" should {

    "successfully return correct task" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val initialCreateTime = DateTimeUtil.now().minusHours(10)
      val initialTakeTime = DateTimeUtil.now().minusHours(9)
      val initialFinishTime = DateTimeUtil.now().minusHours(8)

      val manualCheckCreateTime = DateTimeUtil.now().minusDays(7)
      val manualCheckTakeTime = DateTimeUtil.now().minusHours(6)

      val initialTask = taskService.create(queue, taskSource, TaskCreateOptions.Default, initialCreateTime).futureValue
      taskService.takeByKey(initialTask.queue, initialTask.key, user, initialTakeTime).futureValue
      taskService
        .complete(
          initialTask.queue,
          initialTask.key,
          resolution,
          finishTime = initialFinishTime,
          isResolutionVerified = true
        )
        .futureValue

      val expectedResult =
        taskService
          .createManualCheckTasks(
            initialTask = initialTask.descriptor,
            users = Set(user),
            createTime = manualCheckCreateTime
          )
          .futureValue
          .head
          .copy(
            state = State.IN_PROGRESS,
            startTime = Some(manualCheckTakeTime),
            updateTime = manualCheckTakeTime,
            owner = Some(user)
          )
      val actualResult = taskService.takeManualCheckTask(queue, user, manualCheckTakeTime).futureValue

      actualResult should smartEqual(Some(expectedResult))
      checkTaskHistoryWasSaved(queue, expectedResult.key, TaskAction.Take(user))
    }

    "returns None if no manual check task for specified user" in {
      val user = UserIdGen.next
      val otherUser = UserIdGen.suchThat(_ != user).next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next
      val createTime = DateTimeGen.next
      val takeTime = DateTimeGen.next

      val initialTask = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(initialTask.queue, initialTask.key, user).futureValue
      taskService.complete(initialTask.queue, initialTask.key, resolution, isResolutionVerified = true).futureValue

      taskService
        .createManualCheckTasks(
          initialTask = initialTask.descriptor,
          users = Set(otherUser),
          createTime = createTime
        )
        .futureValue
      val actualResult = taskService.takeManualCheckTask(queue, user, takeTime).futureValue

      actualResult shouldBe None
    }
  }

  "createAutomaticCheckTask" should {

    "successfully create check-task" in {
      val queue = QueueIdGen.next
      val taskSource = TaskSourceGen.suchThat(!_.payload.isInstanceOf[Subtasks]).next
      val initialTask = taskService.create(queue, taskSource).futureValue
      val checkTask = taskService.createAutomaticCheckTask(TaskDescriptor(queue, initialTask.key)).futureValue
      checkTask.excludeUsers shouldBe initialTask.excludeUsers ++ initialTask.owner.toSet
      checkTask.checkCount shouldBe initialTask.checkCount + 1
      checkTask.initialTaskKey shouldBe Some(initialTask.key)
      checkTaskHistoryWasSaved(checkTask.queue, checkTask.key, TaskAction.Create)
    }

    "successfully create several check-tasks" in {
      val n = 5
      val queues = QueueIdGen.next(n)
      val taskSources = TaskSourceGen.suchThat(!_.payload.isInstanceOf[Subtasks]).next(n)
      val initialTasks =
        Future
          .traverse(queues.zip(taskSources)) { case (queue, taskSource) =>
            taskService.create(queue, taskSource)
          }
          .futureValue
          .toSeq
          .map(_.descriptor)
      val checks = taskService.createAutomaticCheckTasks(initialTasks).futureValue
      checks.flatMap(_.initialTaskKey).toSet shouldBe initialTasks.map(_.key).toSet
    }
  }

  "createManualCheckTask" should {

    "successfully create one check-task" in {
      val operator = UserIdGen.next
      val operatorContext = OperatorContext("test", operator)
      val checkTaskCreateTime = DateTimeUtil.now()
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val initialTask = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(initialTask.queue, initialTask.key, user).futureValue
      taskService.complete(initialTask.queue, initialTask.key, resolution, isResolutionVerified = true).futureValue

      val actualCheckTask =
        taskService
          .createManualCheckTasks(
            initialTask = initialTask.descriptor,
            users = Set(user),
            createTime = checkTaskCreateTime
          )(operatorContext)
          .futureValue
          .head
      val expectedCheckTask =
        initialTask.copy(
          createTime = checkTaskCreateTime,
          updateTime = checkTaskCreateTime,
          includeUsers = Set(user),
          initialTaskKey = Some(initialTask.key),
          notificationInfo = initialTask.notificationInfo.copy(response = None, sendAfter = checkTaskCreateTime),
          `type` = TaskType.MANUAL_CHECK,
          // ignore key, qualifier and expireTime
          key = actualCheckTask.key,
          qualifier = actualCheckTask.qualifier,
          expireTime = actualCheckTask.expireTime,
          createdBy = Some(operator),
          similarityHash = None,
          similarityHashSource = Seq.empty
        )

      actualCheckTask should smartEqual(expectedCheckTask)
    }

    "successfully create two check-tasks" in {
      val checkTaskCreateTime = DateTimeUtil.now()
      val user1 = UserIdGen.next
      val user2 = UserIdGen.next
      val users = Seq(user1, user2).sortBy(_.key)
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val initialTask = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(initialTask.queue, initialTask.key, user1).futureValue
      taskService.complete(initialTask.queue, initialTask.key, resolution, isResolutionVerified = true).futureValue

      val actualCheckTasks =
        taskService
          .createManualCheckTasks(
            initialTask = initialTask.descriptor,
            users = users.toSet,
            createTime = checkTaskCreateTime
          )
          .futureValue
          .sortBy(_.includeUsers.headOption.map(_.key))
      val expectedCheckTask =
        initialTask.copy(
          createTime = checkTaskCreateTime,
          updateTime = checkTaskCreateTime,
          initialTaskKey = Some(initialTask.key),
          notificationInfo = initialTask.notificationInfo.copy(response = None, sendAfter = checkTaskCreateTime),
          `type` = TaskType.MANUAL_CHECK,
          similarityHash = None,
          similarityHashSource = Seq.empty
        )
      val expectedCheckTasks =
        users.map { user =>
          val actualCheckTask = actualCheckTasks.find(_.includeUsers.contains(user)).head
          expectedCheckTask.copy(
            includeUsers = Set(user),
            // ignore key, qualifier and expireTime
            key = actualCheckTask.key,
            qualifier = actualCheckTask.qualifier,
            expireTime = actualCheckTask.expireTime
          )
        }

      actualCheckTasks should smartEqual(expectedCheckTasks)
    }

    "successfully create zero check-tasks" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val initialTask = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(initialTask.queue, initialTask.key, user).futureValue
      taskService.complete(initialTask.queue, initialTask.key, resolution, isResolutionVerified = true).futureValue

      val actualCheckTasks = taskService.createManualCheckTasks(initialTask.descriptor, Set.empty).futureValue

      actualCheckTasks shouldBe Seq.empty
    }

    "fail for not completed initial task" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next

      val initialTask = taskService.create(queue, taskSource).futureValue

      taskService
        .createManualCheckTasks(initialTask.descriptor, Set(user))
        .shouldCompleteWithException[NotExistException]
    }

    "fail if initial task is check-task" in {
      val user1 = UserIdGen.next
      val user2 = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val ordinaryInitialTask = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(ordinaryInitialTask.queue, ordinaryInitialTask.key, user1).futureValue
      taskService
        .complete(ordinaryInitialTask.queue, ordinaryInitialTask.key, resolution, isResolutionVerified = true)
        .futureValue

      val checkInitialTask =
        taskService
          .createManualCheckTasks(
            initialTask = ordinaryInitialTask.descriptor,
            users = Set(user1)
          )
          .futureValue
          .head
      taskService.takeByKey(checkInitialTask.queue, checkInitialTask.key, user1).futureValue
      taskService
        .complete(checkInitialTask.queue, checkInitialTask.key, resolution, isResolutionVerified = true)
        .futureValue

      taskService
        .createManualCheckTasks(checkInitialTask.descriptor, users = Set(user2))
        .shouldCompleteWithException[NotExistException]
    }
  }

  "createAutomaticEvaluationTask" should {

    "successfully create evaluation-task" in {
      val queue = QueueId.REALTY_QUALITY_CONTROL
      val taskSource = TaskSourceGen.suchThat(!_.payload.isInstanceOf[Subtasks]).next
      val initialTask = taskService.create(queue, taskSource).futureValue

      val checkTask =
        taskService.createAutomaticEvaluationTasks(Seq(TaskDescriptor(queue, initialTask.key))).futureValue.head

      checkTask.excludeUsers shouldBe empty
      checkTask.initialTaskKey shouldBe empty
      checkTaskHistoryWasSaved(checkTask.queue, checkTask.key, TaskAction.Create)
    }
  }

  "generateValidationTask" should {
    def createTaskAndCheck(queue: QueueId, initialTaskResolution: Resolution, checkTaskResolution: Resolution): Unit = {
      val source = taskSourceGen[PayloadSource.External]().next
      val initialTask = taskService.create(queue, source).futureValue
      taskService.takeByKey(queue, initialTask.key, UserIdGen.next).futureValue
      taskService.complete(queue, initialTask.key, initialTaskResolution, isResolutionVerified = true).futureValue

      val checkTask = taskService.createAutomaticCheckTask(initialTask.descriptor).futureValue
      taskService.takeByKey(queue, checkTask.key, UserIdGen.next).futureValue
      taskService.complete(queue, checkTask.key, checkTaskResolution, isResolutionVerified = true).futureValue
    }

    "successfully generate some validation tasks" in {
      val limit = 10
      val numberOfValidationTasks = 15
      (1 to numberOfValidationTasks).foreach(_ =>
        createTaskAndCheck(
          queue = QueueId.REALTY_PREMODERATION_VISUAL,
          initialTaskResolution = RealtyVisualResolution(Set(USER_FRAUD), ""),
          checkTaskResolution = RealtyVisualResolution(Set(WRONG_ADDRESS), "")
        )
      )
      val crosschecksFilter = CrosschecksFilter.All
      val crosschecksSort = CrosschecksSort.ByCheckTask(ByCreateTime(asc = true))

      def getCrosschecks =
        taskService
          .getCrosschecks(TaskType.AUTOMATIC_CHECK, crosschecksFilter, crosschecksSort, Page(0, 100))
          .futureValue

      validateCrosschecks(getCrosschecks, expectedSize = 0, expectedValidationTasks = 0)

      taskService.generateValidationTasks(limit).futureValue.size shouldBe limit
      validateCrosschecks(getCrosschecks, expectedSize = limit, expectedValidationTasks = limit)

      taskService.generateValidationTasks(limit).futureValue.size shouldBe numberOfValidationTasks - limit
      validateCrosschecks(
        getCrosschecks,
        expectedSize = numberOfValidationTasks,
        expectedValidationTasks = numberOfValidationTasks
      )

      taskService.generateValidationTasks(100).futureValue.size shouldBe 0
      validateCrosschecks(
        getCrosschecks,
        expectedSize = numberOfValidationTasks,
        expectedValidationTasks = numberOfValidationTasks
      )
    }
  }

  "getCrosschecks" should {

    import TaskServiceHelper.CrosscheckTaskVerdictComparator

    "return crosschecks for MANUAL_CHECK type" in {
      val user = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val initialTask = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(initialTask.queue, initialTask.key, user).futureValue
      taskService.complete(initialTask.queue, initialTask.key, resolution, isResolutionVerified = true).futureValue
      val finishedInitialTask = taskService.get(initialTask.queue, initialTask.key).futureValue

      val manualCheckTask =
        taskService
          .createManualCheckTasks(
            initialTask = initialTask.descriptor,
            users = Set(user)
          )
          .futureValue
          .head

      val filter = CrosschecksFilter.All
      val sort = CrosschecksSort.ByCheckTask(TaskSort.ByCreateTime(asc = true))
      val slice = Page(0, 1)
      val actualResult = taskService.getCrosschecks(TaskType.MANUAL_CHECK, filter, sort, slice).futureValue

      val expectedCrosscheck = Crosscheck(finishedInitialTask, manualCheckTask, Some(finishedInitialTask))
      val expectedResult = SlicedResult(Seq(expectedCrosscheck), 1, slice)

      actualResult shouldBe expectedResult
    }

    "return crosschecks for MANUAL_CHECK for specified owner only" in {
      val initialUser = UserIdGen.next
      val user = UserIdGen.suchThat(_ != initialUser).next
      val otherUser = UserIdGen.suchThat(u => u != initialUser && u != user).next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val initialTask = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(initialTask.queue, initialTask.key, initialUser).futureValue
      taskService.complete(initialTask.queue, initialTask.key, resolution, isResolutionVerified = true).futureValue
      val finishedInitialTask = taskService.get(initialTask.queue, initialTask.key).futureValue

      val manualCheckTasks =
        taskService
          .createManualCheckTasks(
            initialTask = initialTask.descriptor,
            users = Set(user, otherUser)
          )
          .futureValue

      val manualCheckTask = manualCheckTasks.find(_.includeUsers contains user).get
      val takenManualCheckTask = taskService.takeByKey(manualCheckTask.queue, manualCheckTask.key, user).futureValue

      val otherManualCheckTask = manualCheckTasks.find(_.includeUsers contains otherUser).get
      taskService.takeByKey(otherManualCheckTask.queue, otherManualCheckTask.key, otherUser).futureValue

      val filter = CrosschecksFilter.Composite(user = Use(user))
      val sort = CrosschecksSort.ByCheckTask(TaskSort.ByCreateTime(asc = true))
      val slice = Page(0, 3)
      val actualResult = taskService.getCrosschecks(TaskType.MANUAL_CHECK, filter, sort, slice).futureValue

      val expectedCrosscheck = Crosscheck(finishedInitialTask, takenManualCheckTask, Some(finishedInitialTask))
      val expectedResult = SlicedResult(Seq(expectedCrosscheck), 1, slice)

      actualResult shouldBe expectedResult
    }

    "return crosschecks for MANUAL_CHECK for specified finishTime only" in {
      val user = UserIdGen.next
      val otherUser = UserIdGen.next
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val initialTask = taskService.create(queue, taskSource).futureValue
      taskService.takeByKey(initialTask.queue, initialTask.key, user).futureValue
      taskService.complete(initialTask.queue, initialTask.key, resolution, isResolutionVerified = true).futureValue
      val finishedInitialTask = taskService.get(initialTask.queue, initialTask.key).futureValue

      val beforeFinishTime = DateTimeUtil.now().minusDays(1)
      val finishTime = DateTimeUtil.now()
      val afterFinishTime = DateTimeUtil.now().plusDays(1)
      val otherFinishTime = DateTimeUtil.now().plusDays(2)

      val manualCheckTask =
        taskService
          .createManualCheckTasks(
            initialTask = initialTask.descriptor,
            users = Set(user)
          )
          .futureValue
          .head
      taskService.takeByKey(manualCheckTask.queue, manualCheckTask.key, user).futureValue
      taskService
        .complete(
          manualCheckTask.queue,
          manualCheckTask.key,
          resolution,
          finishTime = finishTime,
          isResolutionVerified = true
        )
        .futureValue
      val finishedManualCheckTask = taskService.get(manualCheckTask.queue, manualCheckTask.key).futureValue

      val otherManualCheckTask =
        taskService
          .createManualCheckTasks(
            initialTask = initialTask.descriptor,
            users = Set(otherUser)
          )
          .futureValue
          .head
      taskService.takeByKey(otherManualCheckTask.queue, otherManualCheckTask.key, otherUser).futureValue
      taskService
        .complete(
          otherManualCheckTask.queue,
          otherManualCheckTask.key,
          resolution,
          finishTime = otherFinishTime,
          isResolutionVerified = true
        )
        .futureValue

      val finishTimeInterval = Use(TimeInterval(Some(beforeFinishTime), Some(afterFinishTime)))
      val filter = CrosschecksFilter.Composite(checkTask = CrosschecksFilter.CompositeTaskFilter(finishTimeInterval))
      val sort = CrosschecksSort.ByCheckTask(TaskSort.ByCreateTime(asc = true))
      val slice = Page(0, 3)
      val actualResult = taskService.getCrosschecks(TaskType.MANUAL_CHECK, filter, sort, slice).futureValue

      val expectedCrosscheck = Crosscheck(finishedInitialTask, finishedManualCheckTask, Some(finishedInitialTask))
      val expectedResult = SlicedResult(Seq(expectedCrosscheck), 1, slice)

      actualResult shouldBe expectedResult
    }
  }

  private def validateCrosschecks(
      crosschecks: Seq[Crosscheck],
      expectedSize: Int,
      expectedValidationTasks: Int): Unit = {
    crosschecks.size shouldBe expectedSize
    crosschecks.count(_.validation.isDefined) shouldBe expectedValidationTasks
  }

  private def clearTasks(): Unit = taskService.clear().futureValue

  private def createTasks(queue: QueueId, taskSources: Seq[TaskSource]): Seq[Task] =
    for (taskSource <- taskSources) yield taskService.create(queue, taskSource).futureValue

  private def nowInterval: Interval = {
    val now = DateTimeUtil.now()
    val intervalRadiusMinutes = 5
    new Interval(now.minusMinutes(intervalRadiusMinutes), now.plusMinutes(intervalRadiusMinutes))
  }

  private def compareTaskWithTaskSource(queue: QueueId, task: Task, taskSource: TaskSource): Unit = {
    val approximatelyNow = nowInterval

    approximatelyNow.contains(task.createTime) should smartEqual(true)
    approximatelyNow.contains(task.updateTime) should smartEqual(true)
    task.key should not be empty
    task.queue should smartEqual(queue)
    task.owner should smartEqual(None)
    task.startTime should smartEqual(None)
    task.finishTime should smartEqual(None)
    task.priority should smartEqual(
      taskSource.priority.getOrElse(QueueSettings.defaultForQueue(queue).mutable.defaultPriority)
    )
    task.notificationInfo.response should smartEqual(taskSource.response)
    task.resolution should smartEqual(None)
    task.comment should smartEqual(taskSource.comment.getOrElse(""))
    task.deferCountLeft should smartEqual(taskSource.deferCount.getOrElse(0))

    val expectedHash = getSimilarityHash(taskSource.similarityHashSource)
    task.similarityHash should smartEqual(expectedHash)

    (task.payload, taskSource.payload) match {
      case (actualSnapshot: Payload.Snapshot, expectedSnapshot: PayloadSource.Snapshot) =>
        actualSnapshot.contentType should smartEqual(expectedSnapshot.contentType)
        actualSnapshot.value should smartEqual(expectedSnapshot.value)
      case (actualExternal: Payload.External, expectedExternal: PayloadSource.External) =>
        actualExternal.ids should smartEqual(expectedExternal.ids)
      case (actualSubtasks: Payload.Subtasks, expectedSubtasks: Subtasks) =>
        actualSubtasks.count should smartEqual(expectedSubtasks.tasks.size)
      case _ =>
        fail(s"Wrong payload type ${task.payload}")
    }
  }

  private def checkTaskHistoryWasSaved(taskQueue: QueueId, taskKey: TaskKey, lastActions: TaskAction*): Unit = {
    val taskHistoryList = taskService.getTaskHistory(taskQueue, taskKey).futureValue
    taskHistoryList should not be empty
    taskHistoryList.takeRight(lastActions.size).map(_.action) should smartEqual(lastActions.toSeq)
  }

  private def checkOnCompleteAction(taskKey: TaskKey, onCompleteActionId: OnCompleteActionId): Unit = {
    val action = taskService.getOnCompleteAction(taskKey, onCompleteActionId).futureValue
    action.performAfter.isBeforeNow shouldBe true
  }

  private def checkOnCompleteActionDoesNotExist(taskKey: TaskKey, onCompleteActionId: OnCompleteActionId): Unit = {
    assertThrows[NotExistException] {
      Await.result(taskService.getOnCompleteAction(taskKey, onCompleteActionId), 2.seconds)
    }
  }

  private def checkTaskHistoryWasNotSaved(taskQueue: QueueId, taskKey: TaskKey, taskAction: TaskAction): Unit = {
    val taskHistoryList = taskService.getTaskHistory(taskQueue, taskKey).futureValue
    if (taskHistoryList.nonEmpty) {
      taskHistoryList.last.action should not be smartEqual(taskAction)
    }
  }

  private def createParentWithCompletedChildren(queue: QueueId, resolution: Resolution, user: UserId): Task = {
    val slice = Range(0, 100)
    val taskSource = taskSourceGen[Subtasks]().next
    val task = taskService.create(queue, taskSource).futureValue
    val children =
      taskService.find(queue, TaskFilter.No, slice).futureValue.filter {
        _.parent.contains(task.key)
      }
    for (i <- children.indices) {
      val key = taskService.take(queue, user).futureValue.key
      taskService.complete(queue, key, resolution, isResolutionVerified = true).futureValue
    }
    task
  }

  private def createInProgressTask(
      queue: QueueId,
      user: UserId,
      onCompleteActionIds: Set[OnCompleteActionId] = Set.empty,
      similarityHashSource: Seq[String] = Seq.empty): Task = {
    val source =
      taskSourceGen[PayloadSource.External](onCompleteActionIds).next
        .copy(similarityHashSource = similarityHashSource)
    taskService.create(queue, source).futureValue
    val task = taskService.take(queue, user).futureValue
    assume(task.state == State.IN_PROGRESS, "task must be in progress")
    task
  }

  private def createCompletedTask(
      queue: QueueId,
      user: UserId,
      filter: TaskSource => Boolean = _ => true,
      onCompleteActionIds: Set[OnCompleteActionId] = Set.empty,
      createTime: DateTime = DateTimeUtil.now(),
      notificationSent: Boolean = true): Task = {
    val source = taskSourceGen[PayloadSource.External](onCompleteActionIds).suchThat(filter).next
    taskService.create(queue, source, createTime = createTime).futureValue
    val taskKey = taskService.take(queue, user).futureValue.key
    taskService
      .update(queue, taskKey, TaskUpdateRequest(notificationSent = Use(true)))
      .futureValue

    taskService
      .complete(queue, taskKey, resolutionGen[TrueFalseResolution].next, isResolutionVerified = true)
      .futureValue
    val task = taskService.get(queue, taskKey).futureValue
    assume(task.state == State.COMPLETED, "task must be completed")
    task
  }

  private def createCanceledTask(queue: QueueId, onCompleteActionIds: Set[OnCompleteActionId] = Set.empty): Task = {
    val source = taskSourceGen[PayloadSource.External](onCompleteActionIds).next
    val taskKey = taskService.create(queue, source).futureValue.key
    taskService.cancel(queue, taskKey).futureValue
    val task = taskService.get(queue, taskKey).futureValue
    assume(task.state == State.CANCELED, "task must be cancelled")
    task
  }

  private def createExpiredTask(queue: QueueId, onCompleteActionIds: Set[OnCompleteActionId] = Set.empty): Task = {
    val source =
      taskSourceGen[PayloadSource.External](onCompleteActionIds).next.copy(
        expireTime = Some(DateTimeUtil.now().minusDays(1))
      )
    val initialTask = taskService.create(queue, source).futureValue
    taskService.markAsExpired(initialTask.descriptor).futureValue
    val task = taskService.get(queue, initialTask.key).futureValue
    assume(task.state == State.EXPIRED, "task must be expired")
    task
  }

  private def createReadyToSendTask(queue: QueueId, user: UserId): Task = {
    val taskKey = createCompletedTask(queue, user).key
    taskService
      .update(
        queue,
        taskKey,
        TaskUpdateRequest(
          notificationSent = Use(false),
          tryCountLeft = Use(1),
          sendAfter = Use(DateTimeUtil.fromMillis(1234))
        )
      )
      .futureValue
    taskService.get(queue, taskKey).futureValue
  }

  private def makeOnCompleteActionReadyToBePerformed(
      queueId: QueueId,
      taskKey: TaskKey,
      actionId: OnCompleteActionId): Unit = {
    updateOnCompleteAction(queueId, taskKey, actionId, DateTimeUtil.fromMillis(1234))
  }

  private def makeOnCompleteActionNotReadyToBePerformed(
      queueId: QueueId,
      taskKey: TaskKey,
      actionId: OnCompleteActionId): Unit = {
    updateOnCompleteAction(queueId, taskKey, actionId, DateTimeUtil.now().plusDays(1))
  }

  private def updateOnCompleteAction(
      queueId: QueueId,
      taskKey: TaskKey,
      actionId: OnCompleteActionId,
      performAfter: DateTime): Unit = {
    taskService
      .updateOnCompleteActionInfo(
        queueId,
        taskKey,
        actionId,
        OnCompleteActionInfoUpdateRequest(
          actionCompleted = Use(false),
          tryCountLeft = Use(1),
          performAfter = Use(performAfter)
        )
      )
      .futureValue
  }

  private def createReadyToSendTaskWaitingForSendAfter(queue: QueueId, user: UserId): Task = {
    val taskKey = createCompletedTask(queue, user).key
    taskService
      .update(
        queue,
        taskKey,
        TaskUpdateRequest(
          notificationSent = Use(false),
          tryCountLeft = Use(1),
          sendAfter = Use(DateTimeUtil.now().plusDays(100))
        )
      )
      .futureValue
    taskService.get(queue, taskKey).futureValue
  }

  private def createSendingFailedTask(queue: QueueId, user: UserId): Task = {
    val taskKey = createCompletedTask(queue, user).key
    taskService
      .update(
        queue,
        taskKey,
        TaskUpdateRequest(
          notificationSent = Use(false),
          tryCountLeft = Use(0)
        )
      )
      .futureValue
    taskService.get(queue, taskKey).futureValue
  }

  private def createTaskWithUpdateTime(
      queue: QueueId,
      updateTime: DateTime,
      taskGen: Gen[TaskSource] = taskSourceGen[PayloadSource.External]()): Task = {
    val source = taskGen.next
    val task = taskService.create(queue, source, createTime = updateTime).futureValue
    assume(task.updateTime == updateTime, "wrong task update time")
    task
  }
}

object TaskServiceSpecBase {
  private val StubActionId0: OnCompleteActionId = OnCompleteActionId.Internal(InternalActionId.TEST)
  private val StubActionId1: OnCompleteActionId = OnCompleteActionId.Internal(InternalActionId.ISSUE_PROMO_CODE)

  object TestOnCompleteActionsRegistry extends OnCompleteActionsRegistry {

    override def get(queueId: QueueId): Future[Map[OnCompleteActionId, OnCompleteAction]] =
      Future.successful(
        Map(
          StubActionId0 -> new OnCompleteAction {
            override def id: OnCompleteActionId = StubActionId0
            override def isDefined(task: Task): Boolean = true
            override def doAction(task: Task): Future[Unit] = Future.unit
          },
          StubActionId1 -> new OnCompleteAction {
            override def id: OnCompleteActionId = StubActionId1
            override def isDefined(task: Task): Boolean = true
            override def doAction(task: Task): Future[Unit] = Future.unit
          }
        )
      )
  }
}
