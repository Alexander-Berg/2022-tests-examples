package ru.yandex.vertis.vsquality.hobo.api.v1.queue

import akka.http.scaladsl.model.StatusCodes.Forbidden
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.testkit._
import org.joda.time.DateTime
import org.mockito.{ArgumentMatchers => m}
import ru.yandex.vertis.vsquality.hobo.model._
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.DaoGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.vsquality.hobo.service.OperatorContext
import ru.yandex.vertis.vsquality.hobo.util.{DateTimeUtil, HandlerSpecBase, Ignore, Page, TimeInterval}
import ru.yandex.vertis.vsquality.hobo.{TaskCreateOptions, TaskEventFilter, TaskFilter}

import scala.concurrent.duration._

/**
  * @author semkagtn
  */

class QueueHandlerSpec extends HandlerSpecBase {

  override def basePath: String = "/api/1.x/queue"

  private val taskService = backend.taskService
  private val viewService = backend.validateViewService
  // VSMODERATION-5452
  implicit private val timeout: RouteTestTimeout = RouteTestTimeout(5.seconds.dilated)

  before {
    taskService.clear().futureValue
  }

  import akka.http.scaladsl.model.StatusCodes.{BadRequest, NotFound, OK}
  import ru.yandex.vertis.vsquality.hobo.view.DomainMarshalling.{
    CompletedTaskSourceMarshaller,
    CountUnmarshaller,
    ResolutionMarshaller,
    SeqTaskUnmarshaller,
    TaskHistoryUnmarshaller,
    TaskSourceMarshaller,
    TaskUnmarshaller,
    TaskUpdateRequestMarshaller,
    ViewMarshaller,
    ViewUnmarshaller
  }

  "createSingleTask" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      implicit val oc: OperatorContext = OperatorContextGen.next
      val allowResultAfter = DateTimeGen.next
      val options = TaskCreateOptions(allowResultAfter = Some(allowResultAfter))

      val requestUrl = s"/$queue/task?allow_result_after=${allowResultAfter.getMillis}"
      Post(url(requestUrl), taskSource) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(
          one(taskService)
            .create(m.eq(queue), m.eq(taskSource), m.eq(options), m.any[DateTime])(m.eq(oc))
        )
        responseAs[Task]
      }
    }

    "return 404 if no such queue" in {
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      implicit val oc: OperatorContext = OperatorContextGen.next

      Post(url("/unknown/task"), taskSource) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "createManualCheckTask" should {

    "invoke correct method" in {
      implicit val unmarshaller: FromEntityUnmarshaller[Seq[Task]] = SeqTaskUnmarshaller
      implicit val oc: OperatorContext = OperatorContextGen.next
      val user1 = UserIdGen.next
      val user2 = UserIdGen.next
      val queue = QueueIdGen.next
      val initialTaskSource = taskSourceGen[PayloadSource.External]().next
      val resolution = resolutionGen[TrueFalseResolution].next

      val initialTask = createTask(queue, initialTaskSource)
      takeTask(queue, user1)
      completeTask(queue, initialTask.key, resolution)

      val requestUrl = s"/$queue/task/${initialTask.key}/manual-check-task?user=${user2.key}"
      Post(url(requestUrl)) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(
          one(taskService)
            .createManualCheckTasks(m.eq(initialTask.descriptor), m.eq(Set(user2)), m.any[DateTime])(m.eq(oc))
        )
        responseAs[Seq[Task]]
      }
    }

    "return 404 if initial task doesn't exist" in {
      implicit val oc: OperatorContext = OperatorContextGen.next
      val user = UserIdGen.next
      val descriptor = TaskDescriptorGen.next
      val requestUrl = s"/${descriptor.queue}/task/${descriptor.key}/manual-check-task?user=${user.key}"
      Post(url(requestUrl)) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "createCompletedTask" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      val taskSource = CompletedTaskSourceGen.next
      implicit val oc: OperatorContext = OperatorContextGen.next

      val requestUrl = s"/$queue/completed-task"
      Post(url(requestUrl), taskSource) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).createCompletedTask(m.eq(queue), m.eq(taskSource), m.any[DateTime])(m.eq(oc)))
        responseAs[Task]
      }
    }

    "return 404 if no such queue" in {
      val taskSource = CompletedTaskSourceGen.next
      implicit val oc: OperatorContext = OperatorContextGen.next

      Post(url("/invalid/completed-task"), taskSource) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "takeTask" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next
      val user = UserIdGen.next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      createTask(queue, taskSource)

      Post(url(s"/$queue/task/take?user=${user.key}")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).take(m.eq(queue), m.eq(user), m.any[DateTime])(m.eq(oc)))
        responseAs[Task]
      }
    }

    "return 404 if queue is empty" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      taskService.clear().futureValue

      Post(url(s"/$queue/task/take?user=${oc.user.key}")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "return 404 if no such queue" in {
      implicit val oc: OperatorContext = OperatorContextGen.next
      val user = oc.user

      Post(s"/unknown/task/take?user=$user") ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "taskTaskByKey" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      implicit val oc: OperatorContext = OperatorContextGen.next
      val user = UserIdGen.next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val key = createTask(queue, taskSource).key

      Post(url(s"/$queue/task/$key/take?user=${user.key}")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).takeByKey(m.eq(queue), m.eq(key), m.eq(user), m.any[DateTime])(m.eq(oc)))
      }
    }

    "return 404 if no such task" in {
      val queue = QueueIdGen.next
      val key = TaskKeyGen.next
      implicit val oc: OperatorContext = OperatorContextGen.next

      taskService.clear().futureValue

      Post(url(s"/$queue/task/$key/take?user=${oc.user.key}")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "getTask" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val key = createTask(queue, taskSource).key

      Get(url(s"/$queue/task/$key")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).get(queue, key)(oc))
        responseAs[Task]
      }
    }
    "return 404 if no such task" in {
      val queue = QueueIdGen.next
      val key = TaskKeyGen.next
      implicit val oc = OperatorContextGen.next

      Get(url(s"/$queue/task/$key")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "return 404 if no such queue" in {
      val key = TaskKeyGen.next
      implicit val oc = OperatorContextGen.next

      Get(url(s"/unknown/task/$key")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "findTasks" should {

    "return 400 if invalid page_number" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val pn = -1
      val ps = 10
      Get(url(s"/$queue/task?page_number=$pn&page_size=$ps")) ~> defaultHeaders ~> route ~> check {
        status shouldBe BadRequest
      }
    }

    "return 400 if invalid page_size" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val pn = 11
      val ps = 0
      Get(s"/$queue/task?page_number=$pn&page_size=$ps") ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "invoke correct method with 'no' filter" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val pn = 12
      val ps = 12
      Get(url(s"/$queue/task?page_number=$pn&page_size=$ps&filter=no")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).find(queue, TaskFilter.No, Page(pn, ps))(oc))
      }
    }

    "invoke correct method with 'in_progress' filter" in {
      val queue = QueueIdGen.next
      val owner = UserIdGen.next
      implicit val oc = OperatorContextGen.next

      val pn = 13
      val ps = 13
      val path = s"/$queue/task?page_number=$pn&page_size=$ps&filter=in_progress&owner=${owner.key}"
      Get(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).find(queue, TaskFilter.InProgress(owner, Ignore), Page(pn, ps))(oc))
      }
    }

    "return 400 if not 'owner' with 'in_progress' filter" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val pn = 14
      val ps = 14
      Get(url(s"/$queue/task?page_number=$pn&page_size=$ps&filter=in_progress")) ~> defaultHeaders ~> route ~> check {
        status shouldBe BadRequest
      }
    }

    "invoke correct method with 'free' filter" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val pn = 15
      val ps = 15
      Get(url(s"/$queue/task?page_number=$pn&page_size=$ps&filter=free")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        val filter =
          TaskFilter.Free(
            createTime = TimeInterval(None, None),
            takeableBy = Ignore,
            allowManualCheckTasks = false,
            similarityHash = Ignore
          )
        val page = Page(pn, ps)
        there.was(one(taskService).find(queue, filter, page)(oc))
      }
    }

    "invoke correct method with 'parents' filter" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val pn = 16
      val ps = 16
      Get(url(s"/$queue/task?page_number=$pn&page_size=$ps&filter=parents")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).find(queue, TaskFilter.Parents, Page(pn, ps))(oc))
      }
    }

    "invoke correct method with 'children' filter" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next
      val parent = TaskKeyGen.next

      val pn = 17
      val ps = 17
      Get(
        url(s"/$queue/task?page_number=$pn&page_size=$ps&filter=children&parent=$parent")
      ) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).find(queue, TaskFilter.Children(parent), Page(pn, ps))(oc))
      }
    }

    "invoke correct method with 'sending_failed' filter" in {
      val queue = QueueIdGen.next
      implicit val oc: OperatorContext = OperatorContextGen.next

      val pn = 18
      val ps = 18
      Get(
        url(s"/$queue/task?page_number=$pn&page_size=$ps&filter=sending_failed")
      ) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).find(queue, TaskFilter.SendingFailed, Page(pn, ps))(oc))
      }
    }

    "return 400 if not 'parent' with 'children' filter" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val pn = 19
      val ps = 19
      Get(url(s"/$queue/task?page_number=$pn&page_size=$ps&filter=children")) ~> defaultHeaders ~> route ~> check {
        status shouldBe BadRequest
      }
    }

    "return 404 if no such queue" in {
      implicit val oc = OperatorContextGen.next

      Get(url(s"/unknown/task?page_number=0&page_size=0")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "putBackTask" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next
      val user = oc.user

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      createTask(queue, taskSource)
      val key = takeTask(queue, user).key

      Post(url(s"/$queue/task/$key/put_back")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).putBack(m.eq(queue), m.eq(key), m.anyBoolean)(m.eq(oc)))
      }
    }

    "return 404 if no such task" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next
      val key = TaskKeyGen.next

      Post(url(s"/$queue/task/$key/put_back")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "return 404 if no such queue" in {
      implicit val oc = OperatorContextGen.next
      val key = TaskKeyGen.next

      Post(url(s"/unknown/task/$key/put_back")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "completeTask" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next
      val user = oc.user
      val resolution = resolutionGen[TrueFalseResolution].next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      createTask(queue, taskSource)
      val key = takeTask(queue, user).key

      Post(url(s"/$queue/task/$key?user=$user"), resolution) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(
          one(taskService)
            .complete(m.eq(queue), m.eq(key), m.eq(resolution), m.anyBoolean, m.any[DateTime], m.anyBoolean)(m.eq(oc))
        )
      }
    }

    "return 404 if no such task" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next
      val resolution = resolutionGen[TrueFalseResolution].next
      val key = TaskKeyGen.next

      Post(url(s"/$queue/task/$key?user=${oc.user}"), resolution) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "return 404 if no such queue" in {
      implicit val oc = OperatorContextGen.next
      val resolution = resolutionGen[TrueFalseResolution].next
      val key = TaskKeyGen.next

      Post(url(s"/unknown/task/$key?user=${oc.user}"), resolution) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "updateTask" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next
      val updateRequest = TaskUpdateRequestGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next

      val key = createTask(queue, taskSource).key
      takeTask(queue, oc.user)

      Patch(url(s"/$queue/task/$key"), updateRequest) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).update(m.eq(queue), m.eq(key), m.eq(updateRequest), m.anyBoolean)(m.eq(oc)))
      }
    }

    "return 404 if no such task" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next
      val updateRequest = TaskUpdateRequestGen.next

      Patch(url(s"/$queue/task/unknown"), updateRequest) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "return 404 if no such queue" in {
      val queue = QueueIdGen.next
      val updateRequest = TaskUpdateRequestGen.next
      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      implicit val oc = OperatorContextGen.next

      val key = createTask(queue, taskSource).key

      Patch(url(s"/unknown/task/$key"), updateRequest) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "updateTasksByFilter" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      implicit val oc: OperatorContext = OperatorContextGen.next
      val updateRequest = TaskUpdateRequestGen.next
      val filter = TaskFilter.SendingFailed

      Patch(url(s"/$queue/task?filter=sending_failed"), updateRequest) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).update(queue, filter, updateRequest)(oc))
        responseAs[Count]
      }
    }

    "return 404 if no such queue" in {
      implicit val oc: OperatorContext = OperatorContextGen.next
      val updateRequest = TaskUpdateRequestGen.next

      Patch(url(s"/unknown/task"), updateRequest) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "cancelTask" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      createTask(queue, taskSource)
      val key = takeTask(queue, oc.user).key

      Delete(url(s"/$queue/task/$key")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).cancel(m.eq(queue), m.eq(key), m.anyBoolean)(m.eq(oc)))
      }
    }

    "return 404 if no such queue" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val key = createTask(queue, taskSource).key

      Delete(url(s"/unknown/task/$key")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "return 404 if no such key" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      Delete(url(s"/$queue/task/unknown")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "putView" should {

    "return 403 if user is not owner of the task" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val taskSource = TaskSourceGen.next
      val view = ViewGen.next
      val key = createTask(queue, taskSource).key

      Put(url(s"/$queue/task/$key/view"), view) ~> defaultHeaders ~> route ~> check {
        status shouldBe Forbidden
      }
    }
    "invoke correct method" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val taskSource = TaskSourceGen.next
      val view = ViewGen.next
      createTask(queue, taskSource)
      val key = takeTask(queue, oc.user).key
      Put(url(s"/$queue/task/$key/view"), view) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(viewService).putView(m.eq(queue), m.eq(key), m.eq(view), m.anyBoolean)(m.eq(oc)))
      }
    }

    "return 404 if no such queue" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val view = ViewGen.next
      val taskSource = TaskSourceGen.next
      val key = createTask(queue, taskSource).key

      Put(url(s"/unknown/task/$key/view"), view) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
    "return 404 if no such task" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next
      val key = TaskKeyGen.next
      val view = ViewGen.next

      Put(url(s"/$queue/task/$key/view"), view) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

  }

  "getView" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val taskSource = TaskSourceGen.next
      val view = ViewGen.next
      val key = createTask(queue, taskSource).key
      putView(queue, key, view)

      Get(url(s"/$queue/task/$key/view")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[View]
        there.was(one(viewService).getView(queue, key)(oc))
      }
    }

    "return 404 if no such queue" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val taskSource = TaskSourceGen.next
      val key = createTask(queue, taskSource).key

      Get(url(s"/unknown/task/$key/view")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "return 404 if no such task" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next
      val key = TaskKeyGen.next

      Get(url(s"/$queue/task/$key/view")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "return 404 if task doesn't have view" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val taskSource = TaskSourceGen.next
      val key = createTask(queue, taskSource).key

      Get(url(s"/$queue/task/$key/view")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "redirectTask" should {

    "invoke correct method" in {
      val queue = QueueIdGen.next
      val redirectQueue = QueueIdGen.suchThat(_ != queue).next
      implicit val oc = OperatorContextGen.next

      val taskSource = TaskSourceGen.next
      createTask(queue, taskSource)
      val key = takeTask(queue, oc.user).key

      Post(url(s"/$queue/task/$key/redirect?redirect_queue=$redirectQueue")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).redirect(queue, key, redirectQueue)(oc))
      }
    }

    "return 404 if task doesn't exist" in {
      val queue = QueueIdGen.next
      val key = TaskKeyGen.next
      val redirectQueue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      Post(url(s"/$queue/task/$key/redirect?redirect_queue=$redirectQueue")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "return 400 if nonexistent redirect queue" in {
      val queue = QueueIdGen.next
      val key = TaskKeyGen.next
      val redirectQueue = "nonexistent_queue"
      implicit val oc = OperatorContextGen.next

      Post(url(s"/$queue/task/$key/redirect?redirect_queue=$redirectQueue")) ~> defaultHeaders ~> route ~> check {
        status shouldBe BadRequest
      }
    }

    "return 400 if redirect queue not specified" in {
      val queue = QueueIdGen.next
      val key = TaskKeyGen.next
      implicit val oc = OperatorContextGen.next

      Post(url(s"/$queue/task/$key/redirect")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "getHistory" should {

    "use transparent filter if it is not specified" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val key = createTask(queue, taskSource).key

      Get(url(s"/$queue/task/$key/history")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).getTaskHistory(queue, key, TaskEventFilter.All)(oc))
        responseAs[TaskHistory]
      }
    }

    "use specified filter" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val key = createTask(queue, taskSource).key

      Get(url(s"/$queue/task/$key/history?filter=without_view")) ~>
        defaultHeaders ~> route ~> check {

          status shouldBe OK
          there.was(one(taskService).getTaskHistory(queue, key, TaskEventFilter.WithoutView)(oc))
          responseAs[TaskHistory]
        }
    }

    "return 400 if filter is incorrect" in {
      val queue = QueueIdGen.next
      implicit val oc = OperatorContextGen.next

      val taskSource = taskSourceGen[PayloadSource.Snapshot]().next
      val key = createTask(queue, taskSource).key

      Get(url(s"/$queue/task/$key/history?filter=unknown_filter")) ~>
        defaultHeaders ~> route ~> check {
          status shouldBe BadRequest
        }
    }

    "return 200 and empty history if no such task" in {
      val queue = QueueIdGen.next
      val key = TaskKeyGen.next
      implicit val oc = OperatorContextGen.next

      Get(url(s"/$queue/task/$key/history")) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[TaskHistory].size shouldBe 0
      }
    }

    "return 404 if no such queue" in {
      val key = TaskKeyGen.next
      implicit val oc = OperatorContextGen.next

      Get(url(s"/nonexistent_queue/task/$key/history")) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  private def createTask(queue: QueueId, taskSource: TaskSource): Task =
    taskService.create(queue, taskSource)(automatedContext).futureValue

  private def takeTask(queue: QueueId, user: UserId): Task =
    taskService.take(queue, user, DateTimeUtil.now())(automatedContext).futureValue

  private def completeTask(queue: QueueId, key: TaskKey, resolution: Resolution): Unit =
    taskService.complete(queue, key, resolution, isResolutionVerified = true)(automatedContext).futureValue

  private def putView(queue: QueueId, key: TaskKey, view: View): Unit =
    viewService.putView(queue, key, view)(automatedContext).futureValue

}
