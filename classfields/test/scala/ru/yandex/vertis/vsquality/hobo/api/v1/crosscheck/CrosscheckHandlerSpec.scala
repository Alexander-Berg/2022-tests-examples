package ru.yandex.vertis.vsquality.hobo.api.v1.crosscheck

import ru.yandex.vertis.vsquality.hobo.CrosschecksFilter.CompositeTaskFilter
import ru.yandex.vertis.vsquality.hobo._
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.model.{Crosscheck, UserCheckReport}
import ru.yandex.vertis.hobo.proto.Model.Task.TaskType
import ru.yandex.vertis.vsquality.hobo.service.{OperatorContext, TaskService}
import ru.yandex.vertis.vsquality.hobo.util.{HandlerSpecBase, Ignore, Page, SlicedResult, Use}
import ru.yandex.vertis.vsquality.hobo.view.DomainMarshalling._

/**
  * @author semkagtn
  */

class CrosscheckHandlerSpec extends HandlerSpecBase {

  private val taskService: TaskService = backend.taskService

  override def basePath: String = "/api/1.x/crosscheck"

  import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK}

  "getCrosschecks" should {

    "invoke correct method" in {
      implicit val oc: OperatorContext = OperatorContextGen.next

      val page = Page(0, 100)
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val taskType = TaskTypeGen.suchThat(_ != TaskType.ORDINARY).next
      val filter =
        CrosschecksFilter.Composite(
          queue = Use(queue),
          user = Use(user),
          checkTask = CompositeTaskFilter.All
        )
      val taskSort = TaskSort.ByUpdateTime(asc = false)
      val crosscheckSort = CrosschecksSort.ByCheckTask(taskSort)
      val path =
        s"?queue=$queue&owner=${user.key}&page_number=${page.number}&" +
          s"page_size=${page.size}&sort=update_time&asc=${taskSort.asc}&check_type=$taskType"
      Get(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[SlicedResult[Crosscheck]]
        there.was(one(taskService).getCrosschecks(taskType, filter, crosscheckSort, page)(oc))
      }
    }

    "return 400 if check_type=ORDINARY" in {
      implicit val oc: OperatorContext = OperatorContextGen.next

      val page = Page(0, 50)
      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val taskType = TaskType.ORDINARY
      val path =
        s"?queue=$queue&owner=${user.key}&page_number=${page.number}&" +
          s"page_size=${page.size}&sort=update_time&asc=true&check_type=$taskType"
      Get(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe BadRequest
      }
    }
  }

  "getReport" should {

    "invoke correct method" in {
      implicit val oc: OperatorContext = OperatorContextGen.next

      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val taskType = TaskTypeGen.suchThat(_ != TaskType.ORDINARY).next
      val filter =
        UserCheckReportFilter.Composite(
          queue = queue,
          user = user,
          interval = Ignore
        )
      val path = s"/report?queue=$queue&owner=${user.key}&check_type=$taskType"
      Get(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        responseAs[UserCheckReport]
        there.was(one(taskService).getUserCheckReport(taskType, filter)(oc))
      }
    }

    "return 400 if check_type=ORDINARY" in {
      implicit val oc: OperatorContext = OperatorContextGen.next

      val queue = QueueIdGen.next
      val user = UserIdGen.next
      val taskType = TaskType.ORDINARY

      val path = s"/report?queue=$queue&owner=${user.key}&check_type=$taskType"
      Get(url(path)) ~> defaultHeaders ~> route ~> check {
        status shouldBe BadRequest
      }
    }
  }
}
