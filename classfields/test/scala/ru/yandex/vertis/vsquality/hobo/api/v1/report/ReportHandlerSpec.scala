package ru.yandex.vertis.vsquality.hobo.api.v1.report

import org.joda.time.DateTime

import ru.yandex.vertis.vsquality.hobo.SalaryStatisticsReportFilter
import ru.yandex.vertis.vsquality.hobo.model.{SalaryStatisticsReport, SummarySalaryStatistics}
import ru.yandex.vertis.vsquality.hobo.service.OperatorContext
import ru.yandex.vertis.vsquality.hobo.util.{HandlerSpecBase, TimeInterval}
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators.{DateTimeGen, TimeIntervalGen, UserIdGen}
import ru.yandex.vertis.vsquality.hobo.model.generators.DaoGenerators.SalaryStatisticsReportFilterGen

/**
  * @author semkagtn
  */

class ReportHandlerSpec extends HandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.{BadRequest, NotFound, OK}
  import ru.yandex.vertis.vsquality.hobo.view.DomainMarshalling.{
    SalaryStatisticsReportUnmarshaller,
    StartSummaryPeriodMarshaller,
    StartSummaryPeriodUnmarshaller,
    SummarySalaryStatisticsUnmarshaller
  }

  override def basePath: String = "/api/1.x/report"

  private val taskService = backend.taskService
  private val startSummaryPeriodService = backend.startSummaryPeriodService
  private val summarySalaryStatisticsService = backend.summarySalaryStatisticsService

  "getReport" should {

    def getReportUrl(period: TimeInterval, filter: SalaryStatisticsReportFilter): String = {
      val fromParam =
        period.from.map { value =>
          s"from=${value.getMillis}"
        }
      val toParam =
        period.to.map { value =>
          s"to=${value.getMillis}"
        }
      val userParams = filter.users.map(user => s"user=${user.key}")
      val queueParams = filter.queues.map(queue => s"queue=$queue")
      val params = (fromParam.toSeq ++ toParam.toSeq ++ userParams ++ queueParams).mkString("&")
      url(s"/?$params")
    }

    "invoke correct method" in {
      implicit val oc: OperatorContext = OperatorContextGen.next
      val period = TimeIntervalGen.next
      val filter = SalaryStatisticsReportFilterGen.next

      Get(getReportUrl(period, filter)) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(taskService).getSalaryStatistics(period, filter)(oc))
        responseAs[SalaryStatisticsReport]
      }
    }
  }

  "getSummarySalaryStatistics" should {

    "invoke correct method" in {
      implicit val oc: OperatorContext = OperatorContextGen.next
      val userId = UserIdGen.next

      val requestUrl = url(s"/summary?user=${userId.key}")
      Get(requestUrl) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(summarySalaryStatisticsService).get(userId)(oc))
        responseAs[SummarySalaryStatistics]
      }
    }

    "return 404 if no user parameter" in {
      implicit val oc: OperatorContext = OperatorContextGen.next

      val requestUrl = url(s"/summary")
      Get(requestUrl) ~> defaultHeaders ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "getSummaryStartPeriod" should {

    "invoke correct method" in {
      implicit val oc: OperatorContext = OperatorContextGen.next

      val requestUrl = url(s"/summary/start")
      Get(requestUrl) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(startSummaryPeriodService).get()(oc))
        responseAs[DateTime]
      }
    }
  }

  "updateSummaryStartPeriod" should {

    "invoke correct method" in {
      implicit val oc: OperatorContext = OperatorContextGen.next
      val timestamp = DateTimeGen.next

      val requestUrl = url(s"/summary/start")
      Put(requestUrl, timestamp) ~> defaultHeaders ~> route ~> check {
        status shouldBe OK
        there.was(one(startSummaryPeriodService).update(timestamp)(oc))
      }
    }

    "return 400 if no timestamp parameter" in {
      implicit val oc: OperatorContext = OperatorContextGen.next

      val requestUrl = url("/summary/start")
      Put(requestUrl) ~> defaultHeaders ~> route ~> check {
        status shouldBe BadRequest
      }
    }
  }
}
