package ru.auto.salesman.api.v1.service.amoyak

import java.util

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import ru.auto.amoyak.InternalServiceModel.AmoSyncRequest
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.api.v1.service.amoyak.sync_requests.AmoyakSyncRequestsHandler
import ru.auto.salesman.service.AmoyakSyncRequestsService
import ru.auto.salesman.service.AmoyakSyncRequestsService.AmoyakSyncRequestsError
import zio.IO

class AmoyakSyncRequestsHandlerSpec extends RoutingSpec {

  val amoyakSyncRequestsService: AmoyakSyncRequestsService =
    mock[AmoyakSyncRequestsService]

  private val route = new AmoyakSyncRequestsHandler(
    amoyakSyncRequestsService
  ).route

  "GET /amoyak/sync-requests" should {
    "mark clients for synchronization" in {
      val entity = HttpEntity(
        ContentTypes.`application/json`,
        """{"clientIds": [1, 2, 3]}"""
      )

      val amoSyncRequest =
        AmoSyncRequest
          .newBuilder()
          .addAllClientIds(util.Arrays.asList(1L, 2L, 3L))
          .build()

      (amoyakSyncRequestsService.create _)
        .expects(amoSyncRequest)
        .returningZ(IO.succeed(()))

      Post("/")
        .withHeaders(RequestIdentityHeaders)
        .withEntity(entity) ~> seal(route) ~> check {
        status shouldBe StatusCodes.Created
      }
    }

    "return error if request is invalid" in {
      val entity =
        HttpEntity(ContentTypes.`application/json`, """{"clientIds": []}""")

      (amoyakSyncRequestsService.create _)
        .expects(*)
        .returning(IO.fail(AmoyakSyncRequestsError.NoClientsProvided))

      Post("/")
        .withHeaders(RequestIdentityHeaders)
        .withEntity(entity) ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }
  }
}
