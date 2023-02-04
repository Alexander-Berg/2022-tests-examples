package ru.yandex.vertis.billing.api.routes.main.v1.service.archive

import akka.http.scaladsl.model.StatusCodes
import org.mockito.Mockito.verify
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.api.routes.main.v1.service.archive.Handler
import ru.yandex.vertis.billing.model_core.CustomerId
import ru.yandex.vertis.billing.service.async.AsyncArchiveService

import scala.concurrent.Future

/**
  * Specs on archive handler [[Handler]]
  *
  * @author alex-kovalenko
  */
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = "/api/1.x/service/autoru/archive"

  private val campaignId = "Id_1"
  private val agencyId = 1L
  private val clientId = 2L
  private val directCustomerId = CustomerId(clientId, None)
  private val agencyCustomerId = CustomerId(clientId, Some(agencyId))
  private val comment = "test"

  when(backend.asyncArchiveService.archiveCampaign(?, ?, ?)(?))
    .thenReturn(Future.successful(()))

  "POST /client/{clientId}/campaign/{id}" should {
    val uri = s"/client/$clientId/campaign/$campaignId?comment=$comment"

    "not archive campaign without operator" in {
      Post(url(uri)) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "archive direct campaign" in {
      Post(url(uri)) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(backend.asyncArchiveService)
            .archiveCampaign(directCustomerId, campaignId, comment)(operator)
        }
    }
  }

  "POST /agency/{agencyId}/client/{clientId}/campaign/{id}/archive" should {
    val uri = s"/agency/$agencyId/client/$clientId/campaign/$campaignId?comment=$comment"

    "not archive campaign without operator" in {
      Post(url(uri)) ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "archive agency campaign" in {
      Post(url(uri)) ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(backend.asyncArchiveService)
            .archiveCampaign(agencyCustomerId, campaignId, comment)(operator)
        }
    }
  }

}
