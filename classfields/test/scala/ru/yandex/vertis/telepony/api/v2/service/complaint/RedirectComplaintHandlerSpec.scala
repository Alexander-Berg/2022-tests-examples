package ru.yandex.vertis.telepony.api.v2.service.complaint

import akka.http.scaladsl.marshalling.GenericMarshallers
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import org.mockito.Mockito
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eql}
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.generator.Generator.{ComplaintCreateRequestGen, ComplaintGen, RedirectIdGen}
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.Complaint
import ru.yandex.vertis.telepony.model.proto.ComplaintsModel.ComplaintCreateRequest
import ru.yandex.vertis.telepony.proto.ProtoConversions._
import ru.yandex.vertis.telepony.service.ComplaintService
import ru.yandex.vertis.telepony.service.ComplaintService.RedirectNotFound
import ru.yandex.vertis.telepony.service.SharedComplaintService.{ComplaintDuplicate, ComplaintNotFound}
import ru.yandex.vertis.telepony.util.AuthorizedContext
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

import scala.concurrent.Future

class RedirectComplaintHandlerSpec
  extends RouteTest
  with MockitoSupport
  with ScalaCheckDrivenPropertyChecks
  with GenericMarshallers
  with ProtobufSupport {

  val loginHeader = RawHeader("X-Yandex-Internal-Login", "ponydev")

  def createHandler(cds: ComplaintService): Route = {
    seal(
      new RedirectComplaintHandler {
        override def complaintService: ComplaintService = cds
      }.route
    )
  }

  class TestEnv {
    val mockCS: ComplaintService = mock[ComplaintService]
    val handler: Route = createHandler(mockCS)

    implicit val ac: AuthorizedContext = AuthorizedContext(
      id = "1",
      login = "testLogin",
      trace = Traced.empty
    )
  }

  "Complaint handler" should {
    "create complaint" in new TestEnv {
      import ComplaintCreateRequestProtoConversion._
      private val complaint = ComplaintGen.next
      private val createRequest = ComplaintCreateRequestGen.next
      private val complaintCreateRequest: ComplaintCreateRequest = createRequest
      when(mockCS.create(eql(complaint.redirectId), eql(createRequest))(?))
        .thenReturn(Future.successful(complaint))
      Post(s"/redirect/${complaint.redirectId.value}", complaintCreateRequest).withHeaders(loginHeader) ~>
        handler ~> check {
        responseAs[Complaint] shouldEqual complaint
        status shouldEqual StatusCodes.OK
      }
      Mockito.verify(mockCS).create(eql(complaint.redirectId), eql(createRequest))(?)
    }
    "fail to create complaint for non existent redirect" in new TestEnv {
      import ComplaintCreateRequestProtoConversion._
      private val createRequest = ComplaintCreateRequestGen.next
      private val redirectId = RedirectIdGen.next
      private val complaintCreateRequest: ComplaintCreateRequest = createRequest
      when(mockCS.create(eql(redirectId), eql(createRequest))(?))
        .thenReturn(Future.failed(RedirectNotFound(redirectId)))
      Post(s"/redirect/${redirectId.value}", complaintCreateRequest).withHeaders(loginHeader) ~>
        handler ~> check {
        status shouldEqual StatusCodes.NotFound
      }
      Mockito.verify(mockCS).create(eql(redirectId), eql(createRequest))(?)
    }
    "fail to create duplicate complaint" in new TestEnv {
      import ComplaintCreateRequestProtoConversion._
      private val createRequest = ComplaintCreateRequestGen.next
      private val redirectId = RedirectIdGen.next
      private val complaintCreateRequest: ComplaintCreateRequest = createRequest
      when(mockCS.create(eql(redirectId), eql(createRequest))(?))
        .thenReturn(Future.failed(ComplaintDuplicate(redirectId)))
      Post(s"/redirect/${redirectId.value}", complaintCreateRequest).withHeaders(loginHeader) ~>
        handler ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
      Mockito.verify(mockCS).create(eql(redirectId), eql(createRequest))(?)
    }
    "Something bad happened during creation" in new TestEnv {
      import ComplaintCreateRequestProtoConversion._
      private val createRequest = ComplaintCreateRequestGen.next
      private val redirectId = RedirectIdGen.next
      private val complaintCreateRequest: ComplaintCreateRequest = createRequest
      when(mockCS.create(eql(redirectId), eql(createRequest))(?))
        .thenReturn(Future.failed(ComplaintNotFound(redirectId)))
      Post(s"/redirect/${redirectId.value}", complaintCreateRequest).withHeaders(loginHeader) ~>
        handler ~> check {
        status shouldEqual StatusCodes.NotFound
      }
      Mockito.verify(mockCS).create(eql(redirectId), eql(createRequest))(?)
    }
  }
}
