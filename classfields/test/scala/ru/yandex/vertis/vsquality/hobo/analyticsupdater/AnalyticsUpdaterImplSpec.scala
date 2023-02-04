package ru.yandex.vertis.vsquality.hobo.analyticsupdater

import org.joda.time.DateTime
import ru.yandex.vertis.vsquality.hobo.TaskUpdateRequest
import ru.yandex.vertis.vsquality.hobo.dao.impl.AnalyticsUpdaterWaterlineDao
import ru.yandex.vertis.vsquality.hobo.dao.impl.mysql.{MySqlAnalyticsTaskDao, MySqlWaterlineKeyValueDao}
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.model.{PayloadSource, Task, TaskKey}
import ru.yandex.vertis.vsquality.hobo.service.impl.mysql.MySqlTaskService
import ru.yandex.vertis.vsquality.hobo.service.impl.stub.{StubQueueSettingsService, StubUserService}
import ru.yandex.vertis.vsquality.hobo.service.{
  AutomatedContext,
  OnCompleteActionsFactory,
  OnCompleteActionsRegistry,
  QueueSettingsService,
  TaskFactory,
  TaskService
}
import ru.yandex.vertis.vsquality.hobo.util.{DateTimeUtil, MySqlSpecBase, SpecBase}

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Base specs for [[AnalyticsUpdaterImpl]]
  *
  * @author semkagtn
  */

class AnalyticsUpdaterImplSpec extends SpecBase with MySqlSpecBase {

  implicit private val rc: AutomatedContext = AutomatedContext("test")

  private val mainDb = database
  private val analyticsDb = database
  private val settingsService: QueueSettingsService = StubQueueSettingsService()
  private val taskFactory = new TaskFactory(settingsService)
  private val onCompleteActionsRegistry = OnCompleteActionsRegistry.Stub
  private val onCompleteActionsFactory = new OnCompleteActionsFactory(settingsService, onCompleteActionsRegistry)

  val waterlineKeyValueDao = new MySqlWaterlineKeyValueDao(mainDb)

  val taskService: TaskService =
    new MySqlTaskService(
      mainDb,
      maxBatchSize = 1000,
      needSaveHistory = true,
      taskFactory = taskFactory,
      onCompleteActionsFactory = onCompleteActionsFactory,
      readUserSupport = new StubUserService
    )
  val analyticsTaskDao: MySqlAnalyticsTaskDao = new MySqlAnalyticsTaskDao(analyticsDb, upsertBatchSize = 1000)
  val waterlineDao: AnalyticsUpdaterWaterlineDao = new AnalyticsUpdaterWaterlineDao(waterlineKeyValueDao)
  val brokerClient = new StubBrokerClient
  val analyticsUpdater = new AnalyticsUpdaterImpl(taskService, analyticsTaskDao, waterlineDao, brokerClient)

  before {
    taskService.clear().futureValue
    analyticsTaskDao.clear().futureValue
    waterlineDao.delete().futureValue
  }

  "update" should {

    "correctly add one new task to analytics storage" in {
      val oldTaskKey = createTask(DateTimeUtil.now().minusDays(2)).key
      val newTaskKey = createTask(DateTimeUtil.now()).key

      val oldWaterline = DateTimeUtil.now().minusDays(1)
      setWaterline(oldWaterline)

      analyticsUpdater.update(limit = 1).futureValue
      val newWaterline = getWaterline()

      taskExistInTheStorage(oldTaskKey) should smartEqual(false)
      taskExistInTheStorage(newTaskKey) should smartEqual(true)

      newWaterline.isAfter(oldWaterline) should smartEqual(true)
    }

    "correctly add two new tasks to analytics storage" in {
      val taskKey1 = createTask(DateTimeUtil.now()).key
      val taskKey2 = createTask(DateTimeUtil.now()).key

      val oldWaterline = DateTimeUtil.now().minusDays(1)
      setWaterline(oldWaterline)

      analyticsUpdater.update(limit = 3).futureValue
      val newWaterline = getWaterline()

      taskExistInTheStorage(taskKey1) should smartEqual(true)
      taskExistInTheStorage(taskKey2) should smartEqual(true)

      newWaterline.isAfter(oldWaterline) should smartEqual(true)
    }

    "updates no more than specified limit" in {
      val now = DateTimeUtil.now()
      val task1 = createTask(now.minusDays(3))
      val task2 = createTask(now.minusDays(2))
      val task3 = createTask(now.minusDays(1))

      setWaterline(now.minusDays(3).minusMillis(1))

      analyticsUpdater.update(limit = 2).futureValue
      taskExistInTheStorage(task1.key) should smartEqual(true)
      taskExistInTheStorage(task2.key) should smartEqual(true)
      taskExistInTheStorage(task3.key) should smartEqual(false)

      val newWaterline = getWaterline()
      newWaterline should smartEqual(task2.updateTime)
    }

    "correctly update task in the analytics storage" in {
      val oldTask = createTask(DateTimeUtil.now())
      analyticsUpdater.update(limit = 1).futureValue
      setWaterline(DateTimeUtil.now().minusDays(1))
      val newTask = updateTask(oldTask)

      analyticsUpdater.update(limit = 1).futureValue

      analyticsTaskDao.exists(newTask.key, Some(newTask.updateTime)).futureValue shouldBe true
    }

    "correctly works if waterline not set" in {
      val key = createTask(DateTimeUtil.now()).key
      analyticsUpdater.update(limit = 1).futureValue

      taskExistInTheStorage(key) should smartEqual(true)
      getWaterline()
    }
  }

  private def createTask(updateTime: DateTime): Task = {
    val taskSource = taskSourceGen[PayloadSource.External]().next
    val queue = QueueIdGen.next
    taskService.create(queue, taskSource, createTime = updateTime).futureValue
  }

  @nowarn("cat=w-flag-value-discard")
  private def setWaterline(dateTime: DateTime): Unit = waterlineDao.putOrReplace(dateTime).futureValue

  private def getWaterline(): DateTime = waterlineDao.get().futureValue

  private def updateTask(task: Task): Task = {
    taskService.update(task.queue, task.key, TaskUpdateRequest()).futureValue
    taskService.get(task.queue, task.key).futureValue
  }

  private def taskExistInTheStorage(key: TaskKey): Boolean = analyticsTaskDao.exists(key, None).futureValue
}
