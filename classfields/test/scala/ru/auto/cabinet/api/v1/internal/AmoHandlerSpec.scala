package ru.auto.cabinet.api.v1.internal

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route.seal
import org.mockito.Mockito._
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import ru.auto.amoyak.InternalServiceModel.{AmoSyncRequest, AmoyakDto}
import ru.auto.cabinet.ApiModel.Company
import ru.auto.cabinet.api.v1.{HandlerSpecTemplate, SecurityMocks}
import ru.auto.cabinet.dao.jdbc.NotFoundError
import ru.auto.cabinet.service.ClientService
import ru.auto.cabinet.test.TestUtil.RichOngoingStub

import java.util.NoSuchElementException
import akka.http.scaladsl.server.Route
import org.scalatest.Outcome

class AmoHandlerSpec extends FixtureAnyFlatSpec with HandlerSpecTemplate {
  private val auth = new SecurityMocks

  case class FixtureParam(clientService: ClientService, route: Route)

  override protected def withFixture(test: OneArgTest): Outcome = {
    val clientService: ClientService = mock[ClientService]
    val route: Route = wrapRequestMock(new AmoHandler(clientService).route)

    test(FixtureParam(clientService, route))
  }

  "POST /amo/client/sync" should "respond with ok" in { f =>
    val ids = List(1L)
    when(f.clientService.syncClients(eq(ids))(any())).thenReturnF(())

    val entity =
      HttpEntity(
        AmoSyncRequest.newBuilder().addClientIds(1L).build().toByteArray)

    Post("/amo/client/sync").withEntity(
      entity) ~> auth.hdrRequest ~> f.route ~> check {
      status shouldBe OK
    }
  }

  "GET /amo/client/{client_id}" should "respond with ok/404" in { f =>
    val id = 1L
    val failedId = 2L
    when(f.clientService.getAmoClient(eq(id))(any()))
      .thenReturnF(AmoyakDto.getDefaultInstance)

    when(f.clientService.getAmoClient(eq(failedId))(any()))
      .thenThrowF(NotFoundError("No rows found"))

    Get(s"/amo/client/1") ~> auth.hdrRequest ~> seal(f.route) ~> check {
      status shouldBe OK
    }
    // no animal has been harmed during this test
    Get(s"/amo/client/2") ~> auth.hdrRequest ~> seal(f.route) ~> check {
      status shouldBe NotFound
    }
  }

  "GET /amo/company/{company_id}" should "respond with ok/404" in { f =>
    val id = 1L
    val failedId = 2L

    when(f.clientService.getAmoCompany(eq(id))(any()))
      .thenReturnF(Company.getDefaultInstance)
    when(f.clientService.getAmoCompany(eq(failedId))(any()))
      .thenThrowF(new NoSuchElementException(s"No company with id $failedId"))

    Get(s"/amo/company/1") ~> auth.hdrRequest ~> seal(f.route) ~> check {
      status shouldBe OK
    }
    Get(s"/amo/company/2") ~> auth.hdrRequest ~> seal(f.route) ~> check {
      status shouldBe NotFound
    }
  }
}
