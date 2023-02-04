package ru.yandex.vertis.feedprocessor.autoru.dao

import java.sql._
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.{BeforeAndAfter, Inside}
import org.springframework.jdbc.BadSqlGrammarException
import org.springframework.jdbc.core.{PreparedStatementCreator, RowMapper}
import org.springframework.jdbc.support.GeneratedKeyHolder
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model.{ServiceInfo, Task}
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.utils.OpsJdbc
import ru.yandex.vertis.feedprocessor.autoru.model.ServiceInfo.Autoru
import ru.yandex.vertis.feedprocessor.util.{DatabaseSpec, DummyOpsSupport}

import scala.jdk.CollectionConverters._
import scala.util.Try

/**
  * @author pnaydenov
  */
class TasksDaoSpec extends WordSpecBase with DatabaseSpec with BeforeAndAfter with Inside with DummyOpsSupport {
  implicit val opsJdbcMeters: OpsJdbc.Meters = new OpsJdbc.Meters(operationalSupport.prometheusRegistry)
  val taskDao: TasksDao = new TasksDaoImpl(tasksDb)

  before {
    tasksDb.master.jdbc.update("TRUNCATE tasks")
  }
  private val dummySi = newServiceInfoGen.next

  private def createNewFileTask(taskId: Long, taskResultId: Long): Long = {
    createTask(1, taskId, taskResultId, None, Task.Status.New, "http://mds/new", "http://clint/new", dummySi)
  }

  private def createTask(
      clientId: Int,
      feedloaderTaskId: Long,
      feedloaderTaskResultId: Long,
      xmlHostId: Option[Int],
      status: Task.Status.Value,
      url: String,
      clientUrl: String,
      serviceInfo: ServiceInfo,
      now: LocalDateTime = LocalDateTime.now()): Long =
    createTask(
      clientId,
      Some(feedloaderTaskId),
      Some(feedloaderTaskResultId),
      xmlHostId,
      status,
      url,
      clientUrl,
      serviceInfo,
      now
    )

  private def createTask(
      clientId: Int,
      feedloaderTaskId: Option[Long],
      feedloaderTaskResultId: Option[Long],
      xmlHostId: Option[Int],
      status: Task.Status.Value,
      url: String,
      clientUrl: String,
      serviceInfo: ServiceInfo,
      now: LocalDateTime): Long = {
    val createdAtTimestamp = Timestamp.valueOf(now)
    val sql =
      """
        INSERT INTO tasks (
          client_id,
          feedloader_task_id,
          feedloader_task_result_id,
          xml_host_id,
          status,
          url,
          client_url,
          service_info_json,
          created_at,
          updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """
    val keyHolder = new GeneratedKeyHolder()
    tasksDb.master.jdbc.update(
      new PreparedStatementCreator {
        override def createPreparedStatement(con: Connection) = {
          val ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
          ps.setInt(1, Int.box(clientId))
          feedloaderTaskId.map(Long.box).fold(ps.setNull(2, Types.BIGINT))((i: java.lang.Long) => ps.setLong(2, i))
          feedloaderTaskResultId
            .map(Long.box)
            .fold(ps.setNull(3, Types.BIGINT))((i: java.lang.Long) => ps.setLong(3, i))
          xmlHostId.map(Int.box).fold(ps.setNull(4, Types.INTEGER))((i: Integer) => ps.setInt(4, i))
          ps.setString(5, status.toString)
          ps.setString(6, url)
          ps.setString(7, clientUrl)
          ps.setString(8, serviceInfo.toJson())
          ps.setTimestamp(9, createdAtTimestamp)
          ps.setTimestamp(10, createdAtTimestamp)
          ps
        }
      },
      keyHolder
    )
    val key = keyHolder.getKey
    if (key eq null) throw new RuntimeException("Insert must affects one row")
    else key.longValue()
  }

  "TasksDao" should {
    "find by id" in {
      val id1 = createNewFileTask(1, 2)
      val id2 = createNewFileTask(1, 3)
      taskDao.findById(id1).get.id shouldEqual id1
      taskDao.findById(id1 + id2 + 1) shouldBe empty
    }

    "create" in {
      type TaskRow = (Int, Long, Long, String, String, String, String)
      val serviceInfo = ServiceInfo(
        ServiceInfo.Autoru(
          saleCategoryId = 3,
          sectionId = 2,
          leaveServices = false,
          leaveAddedImages = false,
          maxDiscountEnabled = false,
          deleteSale = true,
          poiId = 1,
          fileId = Some(1237),
          imagesCacheBreaker = Some(1)
        )
      )
      createTask(134, 321, 1, None, Task.Status.New, "http://test/test", "http://file.xml", serviceInfo)
      val tasks = tasksDb.slave.jdbc.query(
        "SELECT * FROM tasks WHERE client_id = ?",
        new RowMapper[TaskRow] {
          override def mapRow(rs: ResultSet, rowNum: Int): TaskRow =
            (
              rs.getInt("client_id"),
              rs.getLong("feedloader_task_id"),
              rs.getLong("feedloader_task_result_id"),
              rs.getString("status"),
              rs.getString("url"),
              rs.getString("client_url"),
              rs.getString("service_info_json")
            )
        },
        Int.box(134)
      )

      tasks should have size (1)
      val task = tasks.get(0)
      inside(task) {
        case (134, 321, 1, "new", "http://test/test", "http://file.xml", serviceInfoJson) =>
          serviceInfo shouldEqual ServiceInfo.fromJson(serviceInfoJson)
      }
    }

    "pick new tasks and move to processing state" in {
      createTask(1, 1, 1, None, Task.Status.New, "http://new", "file1.xml", dummySi)
      Thread.sleep(1) // for different "created_at"
      createTask(1, 2, 3, None, Task.Status.New, "http://new2", "file2.xml", dummySi)
      createTask(1, 2, 2, None, Task.Status.Processing, "http://processing", "file3.xml", dummySi)
      Thread.sleep(1)
      createTask(1, 4, 4, None, Task.Status.New, "http://another-task", "file4.xml", dummySi)
      Thread.sleep(1)
      createTask(1, 1, 2, None, Task.Status.New, "http://new1-fresh", "file5.xml", dummySi)

      info("should pick 2 oldest tasks")
      val tasks = taskDao.pickLast(2, Seq(0), 1)
      tasks should have size (2)
      val first :: second :: _ = tasks.sortBy(_.clientId).toList
      first.url shouldEqual "http://new"
      second.url shouldEqual "http://new2"
      first.status shouldEqual Task.Status.Processing
      second.status shouldEqual Task.Status.Processing

      info("check actual status values")
      val taskStatuses = tasksDb.master.jdbc
        .queryForList(s"SELECT status FROM tasks WHERE id IN (${tasks.map(_.id).mkString(", ")})", classOf[String])
      taskStatuses.asScala.toList shouldEqual List(Task.Status.Processing.toString, Task.Status.Processing.toString)

      info("should find next suitable tasks")
      val tasks2 = taskDao.pickLast(5, Seq(0), 1)
      tasks2 should have size (2)
      tasks2(0).url shouldEqual "http://another-task"
      tasks2(1).url shouldEqual "http://new1-fresh"
    }

    "pick correct bucket" in {
      createTask(2, 1, 1, None, Task.Status.New, "http://1", "", dummySi)
      createTask(4, 2, 2, None, Task.Status.New, "http://2", "", dummySi)
      createTask(6, 3, 3, None, Task.Status.New, "http://3", "", dummySi)

      val bucket1 = taskDao.pickLast(3, Seq(1), 2)
      val bucket0 = taskDao.pickLast(3, Seq(0), 2)

      bucket1 should have size (2)
      bucket0 should have size (1)

      bucket1.map(_.url).toList shouldEqual List("http://1", "http://3")
      bucket0.map(_.url).toList shouldEqual List("http://2")
    }

    "pick multiple buckets" in {
      createTask(1, 1, 1, None, Task.Status.New, "http://1", "", dummySi)
      createTask(2, 2, 2, None, Task.Status.New, "http://2", "", dummySi)
      createTask(3, 3, 3, None, Task.Status.New, "http://3", "", dummySi)
      val bucket0_and_1 = taskDao.pickLast(3, Seq(0, 1), 2)
      bucket0_and_1 should have size (3)
      bucket0_and_1.map(_.url).toList shouldEqual List("http://1", "http://2", "http://3")
    }

    "write only service_info_json column" in {
      val id = createNewFileTask(1, 2)
      val serviceInfoJson = tasksDb.master.jdbc.queryForObject(
        "SELECT service_info_json FROM tasks WHERE id = ?",
        classOf[String],
        Long.box(id)
      )
      serviceInfoJson shouldEqual dummySi.toJson()
    }

  }

  "TasksDao.tryCreateFeedTask" should {
    "create task by params" in {
      val now = LocalDateTime.now().withNano(999999999)
      val task = taskDao.tryCreateTaskAndRemovePrevious(1, 2, 3, None, "http://foo", "file.xml", dummySi, now).get
      task.clientId shouldEqual 1
      task.feedloaderTaskId shouldEqual Some(2)
      task.feedloaderTaskResultId shouldEqual Some(3)
      task.status shouldEqual Task.Status.New
      task.url shouldEqual "http://foo"
      task.serviceInfo shouldEqual dummySi

      val savedCreatedAt = tasksDb.master.jdbc.queryForObject("SELECT created_at FROM tasks", classOf[LocalDateTime])
      savedCreatedAt shouldEqual now.withNano(0) // MySQL works with seconds precision by default
    }

    "create missing feed task and remove previous" in {
      val first = taskDao.tryCreateTaskAndRemovePrevious(1, 2, 3, None, "http://foo", "file.xml", dummySi).get

      info("can create task for more fresh feed loading")
      val second = taskDao.tryCreateTaskAndRemovePrevious(1, 2, 4, None, "http://foo", "file.xml", dummySi).get

      first.id shouldNot equal(second.id)
      first.clientId shouldEqual 1
      first.feedloaderTaskResultId shouldEqual Some(3)
      second.clientId shouldEqual 1
      second.feedloaderTaskResultId shouldEqual Some(4)

      info("previous task disabled")
      taskDao.findById(first.id).get.status shouldEqual Task.Status.Failure
      taskDao.findById(second.id).get.status shouldEqual Task.Status.New
    }

    "not duplicate feed tasks" in {
      val initial = taskDao.tryCreateTaskAndRemovePrevious(1, 2, 2, None, "http://foo", "file.xml", dummySi).get

      info("can't create feed task with same result")
      taskDao.tryCreateTaskAndRemovePrevious(1, 2, 2, None, "http://foo", "file.xml", dummySi) shouldBe empty

      info("can't create offer from previous result")
      taskDao.tryCreateTaskAndRemovePrevious(1, 2, 1, None, "http://foo", "file.xml", dummySi) shouldBe empty

      info("even after completion of initial task")
      taskDao.failTask(initial.id, "test")
      taskDao.tryCreateTaskAndRemovePrevious(1, 2, 2, None, "http://foo", "file.xml", dummySi) shouldBe empty

      info("previous task is not removed")
      taskDao.findById(initial.id) shouldNot be(empty)
    }

    "pick new tasks correctly w/o locks" in {
      pending // too heavy for running on a regular basis
      val N = 100
      (0 until N).foreach { clientId =>
        createTask(clientId, clientId * 10, 1, None, Task.Status.New, "sdf", "file.xml", dummySi)
      }
      import scala.concurrent._
      import scala.concurrent.duration._

      def pickInParallel(): Future[scala.Seq[Task]] =
        Future {
          taskDao.pickLast(N, Seq(0), 1)
        }

      val tasksCount = tasksDb.master.jdbc.queryForObject("SELECT COUNT(*) FROM tasks", classOf[Int])
      tasksCount shouldEqual N

      val tasksByWorkers =
        (0 to 5).map(i => i -> pickInParallel()).map { case (k, v) => k -> Await.result(v, 50 seconds) }.toMap
      assert(tasksByWorkers.count(_._2.nonEmpty) > 1, "Can't partition tasks between workers")
      val tasks = tasksByWorkers.values.flatten
      tasks should have size (N)
      tasks.map(_.clientId).toSet should have size (N)
      val tasksInProcessing = tasksDb.master.jdbc.queryForObject(
        "SELECT COUNT(*) FROM tasks WHERE status = ?",
        classOf[Int],
        Task.Status.Processing.toString
      )
      tasksInProcessing shouldEqual N
    }

    "fill service_info_json column" in {
      val si = serviceInfoGen(leaveServices = true, leaveAddedImages = true).next
      assert(si.toJson() != "{}", "ServiceInfo not in degenerate case")
      val now = LocalDateTime.now()
      val task = taskDao.tryCreateTaskAndRemovePrevious(1, 2, 3, None, "http://foo", "file.xml", si, now).get
      task.serviceInfo shouldEqual si

      val serviceInfoJson = tasksDb.master.jdbc.queryForObject(
        "SELECT service_info_json FROM tasks WHERE id = ?",
        classOf[String],
        Long.box(task.id)
      )

      serviceInfoJson shouldEqual task.serviceInfo.toJson()
    }
  }

  "TasksDao.failTask" should {
    "change to failed status with error message" in {
      val taskId = createNewFileTask(1, 1)
      taskDao.failTask(taskId, "Test error")
      val task = taskDao.findById(taskId).get
      task.status shouldEqual Task.Status.Failure
      val message = tasksDb.master.jdbc.queryForObject(
        "SELECT error_message FROM tasks WHERE id = ?",
        classOf[String],
        Long.box(taskId)
      )
      message shouldEqual "Test error"
    }
  }

  "TasksDao.successTask" should {
    "change to success status with report url" in {
      val taskId = createNewFileTask(1, 1)
      taskDao.successTask(taskId)
      val task = taskDao.findById(taskId).get
      task.status shouldEqual Task.Status.Success
      val status =
        tasksDb.master.jdbc.queryForObject("SELECT status FROM tasks WHERE id = ?", classOf[String], Long.box(taskId))
      status shouldEqual Task.Status.Success.toString
    }
  }

  "TasksDao.findProcessedById" should {
    def createAndStartTask(startAt: LocalDateTime = LocalDateTime.now()): Long = {
      val taskId = createNewFileTask(1, 1)
      tasksDb.master.jdbc.update(
        "UPDATE tasks SET status = ?, started_at = ? WHERE id = ?",
        Task.Status.Processing.toString(),
        startAt,
        Long.box(taskId)
      )
      taskId
    }

    "return processed task" in {
      val taskId = createAndStartTask()

      taskDao.findProcessedById(taskId, LocalDateTime.now().minusMinutes(1)).get.id shouldEqual taskId
    }

    "not return too old task" in {
      val taskId = createAndStartTask(startAt = LocalDateTime.now().minusDays(1))

      taskDao.findProcessedById(taskId, LocalDateTime.now().minusMinutes(1)) shouldBe empty
    }

    "not return already completed task" in {
      val taskId = createAndStartTask()
      taskDao.successTask(taskId)

      taskDao.findProcessedById(taskId, LocalDateTime.now().minusMinutes(1)) shouldBe empty
    }

    "not return alreasy failed task" in {
      val taskId = createAndStartTask()
      taskDao.failTask(taskId, "Test fail")

      taskDao.findProcessedById(taskId, LocalDateTime.now().minusMinutes(1)) shouldBe empty
    }

    "not return not existing task" in {
      createAndStartTask()
      val taskId = tasksDb.master.jdbc.queryForObject("SELECT max(id) FROM tasks", classOf[Long]) + 1
      taskDao.findProcessedById(taskId, LocalDateTime.now().minusYears(1)) shouldBe empty
    }
  }

  "TasksDao.isPreviousTaskExists" should {
    val outdatedMysqlServer = Try(tasksDb.master.jdbc.execute("select JSON_EXTRACT('{}', '$.foo');"))
      .map(_ => false)
      .recover { case ex: BadSqlGrammarException => true }
      .get
    if (outdatedMysqlServer) {
      info("can't process some tests if MySQL server does not support JSON_EXTRACT")
      pending
    }
    val feedloaderResultIdCounter = new AtomicInteger(0)

    def serviceInfo(category: Int, section: Int): ServiceInfo =
      ServiceInfo(Autoru(category, section, false, false, false, 0, None, None))

    def createTask2(clientId: Int, status: Task.Status.Value, serviceInfo: ServiceInfo, now: LocalDateTime): Long = {
      createTask(
        clientId,
        1,
        feedloaderResultIdCounter.incrementAndGet(),
        None,
        status,
        "url",
        "client_url",
        serviceInfo,
        now
      )
    }

    val now = LocalDateTime.now()
    val checkSince = now.minusMinutes(5)

    "find very closer analogous task" in {
      createTask2(1, Task.Status.New, serviceInfo(15, 2), now.minusMinutes(1))
      val taskId = createTask2(1, Task.Status.New, serviceInfo(15, 2), now)

      taskDao.isPreviousTaskExists(taskId, checkSince, 1, 15, 2) shouldBe true
    }

    "not count already completed tasks" in {
      createTask2(1, Task.Status.Failure, serviceInfo(15, 2), now.minusMinutes(1))
      createTask2(1, Task.Status.Success, serviceInfo(15, 2), now.minusMinutes(1))
      val taskId = createTask2(1, Task.Status.New, serviceInfo(15, 2), now)

      taskDao.isPreviousTaskExists(taskId, checkSince, 1, 15, 2) shouldBe false
    }

    "not count tasks from another feed" in {
      createTask2(1, Task.Status.New, serviceInfo(31, 2), now.minusMinutes(1))
      createTask2(1, Task.Status.New, serviceInfo(15, 1), now.minusMinutes(1))
      val taskId = createTask2(1, Task.Status.New, serviceInfo(15, 2), now)

      taskDao.isPreviousTaskExists(taskId, checkSince, 1, 15, 2) shouldBe false
    }

    "not count tasks of another client" in {
      createTask2(2, Task.Status.New, serviceInfo(15, 2), now.minusMinutes(1))
      val taskId = createTask2(1, Task.Status.New, serviceInfo(15, 2), now)

      taskDao.isPreviousTaskExists(taskId, checkSince, 1, 15, 2) shouldBe false
    }

    "not count subsequent tasks" in {
      val taskId = createTask2(1, Task.Status.New, serviceInfo(15, 2), now)
      createTask2(1, Task.Status.New, serviceInfo(15, 2), now.minusMinutes(1))

      taskDao.isPreviousTaskExists(taskId, checkSince, 1, 15, 2) shouldBe false
    }

    "not count tasks too remoted in time" in {
      createTask2(1, Task.Status.New, serviceInfo(15, 2), now.minusMinutes(100))
      val taskId = createTask2(1, Task.Status.New, serviceInfo(15, 2), now)

      taskDao.isPreviousTaskExists(taskId, checkSince, 1, 15, 2) shouldBe false
    }
  }

  "TasksDao.getOldHandTasks" should {
    val now = LocalDateTime.now()

    "return old hand task" in {
      val taskId =
        createTask(1, None, None, None, Task.Status.New, "http://foo", "foo", newServiceInfoGen.next, now.minusDays(5))
      val tasks = taskDao.getOldHandTasks(now.minusDays(2), limit = 10, minId = None)
      tasks should have size (1)
      tasks.head.id shouldEqual taskId
    }

    "not return old non-hand task" in {
      val tasks = taskDao.getOldHandTasks(now.minusDays(2), limit = 10, minId = None)
      tasks shouldBe empty
    }

    "not return fresh hand task" in {
      val tasks = taskDao.getOldHandTasks(now.minusDays(2), limit = 10, minId = None)
      tasks shouldBe empty
    }

    "start results from min_id exclusively" in {
      val taskId1 =
        createTask(1, None, None, None, Task.Status.New, "http://foo", "foo", newServiceInfoGen.next, now.minusDays(5))
      val taskId2 =
        createTask(1, None, None, None, Task.Status.New, "http://foo", "foo", newServiceInfoGen.next, now.minusDays(5))
      val tasks = taskDao.getOldHandTasks(now.minusDays(2), limit = 10, minId = Some(taskId1))
      tasks should have size (1)
      tasks.head.id shouldEqual taskId2
    }

    "limit task result, ordered by id" in {
      val taskId1 =
        createTask(1, None, None, None, Task.Status.New, "http://foo", "foo", newServiceInfoGen.next, now.minusDays(5))
      val taskId2 =
        createTask(1, None, None, None, Task.Status.New, "http://foo", "foo", newServiceInfoGen.next, now.minusDays(5))
      val taskId3 =
        createTask(1, None, None, None, Task.Status.New, "http://foo", "foo", newServiceInfoGen.next, now.minusDays(5))

      taskDao
        .getOldHandTasks(now.minusDays(2), limit = 2, minId = None)
        .map(_.id)
        .toSet shouldEqual Set(taskId1, taskId2)
      taskDao
        .getOldHandTasks(now.minusDays(2), limit = 2, minId = Some(taskId2))
        .map(_.id)
        .toSet shouldEqual Set(taskId3)
      taskDao.getOldHandTasks(now.minusDays(2), limit = 2, minId = Some(taskId3)) shouldBe empty
    }

    "TasksDao.findEarliestTaskIdByTime" in {
      val oldTaskTime = LocalDateTime.now().minusDays(30)
      val t1 = createTask(
        1,
        None,
        None,
        None,
        Task.Status.Success,
        "",
        "",
        newServiceInfoGen.next,
        now = oldTaskTime.minusSeconds(5)
      )
      val t2 = createTask(2, None, None, None, Task.Status.Success, "", "", newServiceInfoGen.next, now = oldTaskTime)
      val t3 = createTask(
        3,
        None,
        None,
        None,
        Task.Status.Success,
        "",
        "",
        newServiceInfoGen.next,
        now = oldTaskTime.plusSeconds(5)
      )

      taskDao.findEarliestTaskIdByTime(oldTaskTime).get should (be(t1).or(be(t2)))
      taskDao.findEarliestTaskIdByTime(oldTaskTime.minusSeconds(1)).get shouldEqual t1
      taskDao.findEarliestTaskIdByTime(oldTaskTime.plusSeconds(1)).get shouldEqual t2
      taskDao.findEarliestTaskIdByTime(oldTaskTime.minusSeconds(6)) shouldBe empty
      taskDao.findEarliestTaskIdByTime(oldTaskTime.plusDays(10)).get shouldEqual t3
    }

    "TasksDao.deleteTasksOlderThanTaskId" in {
      val oldTaskTime = LocalDateTime.now().minusDays(30)
      val t2 = createTask(2, None, None, None, Task.Status.Success, "", "", newServiceInfoGen.next, now = oldTaskTime)
      val t3 = createTask(
        3,
        None,
        None,
        None,
        Task.Status.Success,
        "",
        "",
        newServiceInfoGen.next,
        now = oldTaskTime.plusSeconds(5)
      )

      taskDao.deleteTasksOlderThanTaskId(t2)

      val taskIds = tasksDb.master.jdbc.queryForList("SELECT id FROM tasks", classOf[Int])
      taskIds.asScala.toSet shouldEqual Set(t2, t3)
    }
  }
}
