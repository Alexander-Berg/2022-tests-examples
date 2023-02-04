package ru.yandex.vertis.billing.api.routes.main.v1.service.campaign

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.api.routes.main.v1.service.campaign.Handler
import ru.yandex.vertis.billing.api.routes.main.v1.view.CampaignStatisticsReportView
import ru.yandex.vertis.billing.model_core.gens.{CampaignHeaderGen, OrderGen, Producer}
import ru.yandex.vertis.billing.model_core.{
  CampaignHeader,
  CampaignId,
  CampaignStatisticsReport,
  ServiceStatisticsReport
}
import ru.yandex.vertis.billing.service.CampaignService
import ru.yandex.vertis.billing.service.CampaignService.Filter
import ru.yandex.vertis.billing.service.StatisticsService.CampaignQuery
import ru.yandex.vertis.billing.util.{OperatorContext, Slice, SlicedResult}

import scala.concurrent.Future

/**
  * Specs on [[Handler]].
  *
  * @author dimas
  */
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = "/api/1.x/service/autoru/campaign"

  "GET /" should {
    "provide campaigns list" in {
      val HeadersCount = 5
      stub(backend.campaignService.list(_: Filter, _: Slice)(_: OperatorContext)) {
        case (CampaignService.Filter.All, page, `operator`) =>
          val headers = CampaignHeaderGen.next(HeadersCount)
          Future.successful(SlicedResult(headers, headers.size, page))
      }
      Get(url("/")) ~>
        defaultHeaders ~>
        route ~>
        check {
          import ru.yandex.vertis.billing.api.view.CampaignHeaderView.slicedResultUnmarshaller
          response.status should be(StatusCodes.OK)
          val parsed = responseAs[SlicedResult[CampaignHeader]]
          parsed should have size HeadersCount
        }
    }
  }

  "GET /{id}" should {
    val header = CampaignHeaderGen.next
    val id = header.id
    stub(backend.campaignService.get(_: CampaignId)(_: OperatorContext)) { case (`id`, `operator`) =>
      Future.successful(header)
    }
    "provide campaign by its ID" in {
      Get(url(s"/$id")) ~>
        defaultHeaders ~>
        route ~>
        check {
          import ru.yandex.vertis.billing.api.view.CampaignHeaderView.modelUnmarshaller
          response.status should be(StatusCodes.OK)
          val parsed = responseAs[CampaignHeader]
          parsed should be(header)
        }
    }
  }

  "GET /{id}/stat" should {
    val header = CampaignHeaderGen.next
    val id = header.id
    val report = CampaignStatisticsReport(
      id,
      OrderGen.next,
      Iterable.empty
    )
    stub(backend.statisticService.get(_: CampaignQuery)(_: OperatorContext)) { case (_, `operator`) =>
      Future.successful(report)
    }
    "provide campaign stat" in {
      import CampaignStatisticsReportView.modelUnmarshaller

      Get(url(s"/$id/stat?from=2015-01-01&precision=Day")) ~>
        defaultHeaders ~>
        route ~>
        check {
          response.status should be(StatusCodes.OK)
          val parsed = responseAs[CampaignStatisticsReport]
          parsed shouldBe report
        }
    }
  }

}
