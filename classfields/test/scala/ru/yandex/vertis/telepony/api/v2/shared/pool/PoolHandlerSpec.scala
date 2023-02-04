package ru.yandex.vertis.telepony.api.v2.shared.pool

import akka.http.scaladsl.marshalling.GenericMarshallers
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import org.mockito.Mockito
import org.scalacheck.Gen
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.api.RouteTest
import ru.yandex.vertis.telepony.api.v2.view.json.pool.{AssignRequestView, CreateRequestView, SharedOperatorNumberView}
import ru.yandex.vertis.telepony.api.v2.view.proto.ApiProtoConversions
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.{OperatorAccounts, Operators, PhoneTypes, SharedOperatorNumber, TypedDomains}
import ru.yandex.vertis.telepony.service.SharedPoolService
import ru.yandex.vertis.telepony.service.SharedPoolService.{AssignRequest, CreateRequest, PhoneFilter}
import ru.yandex.vertis.telepony.util.OperatorUtils.PossibleVoxOriginOperators
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport

import scala.concurrent.Future

/**
  * @author neron
  */
class PoolHandlerSpec
  extends RouteTest
  with MockitoSupport
  with ApiProtoConversions
  with GenericMarshallers
  with ProtobufSupport {

  val RequestContext = RawHeader("X-Yandex-Operator-Uid", "123")

  private def createHandler(poolService: SharedPoolService): Route = seal(
    new PoolHandler {
      override protected def sharedPoolService: SharedPoolService = poolService
    }.route
  )

  private def createRequestFrom(sharedOperatorNumber: SharedOperatorNumber): CreateRequest = {
    CreateRequest(
      number = sharedOperatorNumber.number,
      account = sharedOperatorNumber.account,
      originOperator = sharedOperatorNumber.originOperator,
      domain = sharedOperatorNumber.domain
    )
  }

  private def testCreateSuccess(sharedOperatorNumber: SharedOperatorNumber) = {
    val ps = mock[SharedPoolService]
    val route = createHandler(ps)
    val request = createRequestFrom(withExpectedOriginOperator(sharedOperatorNumber))
    when(ps.create(?)(?)).thenReturn(Future.successful(sharedOperatorNumber))
    import CreateRequestView.modelMarshaller
    import SharedOperatorNumberView.asView
    Post("/phones", request).withHeaders(RequestContext) ~> route ~> check {
      response.status shouldEqual StatusCodes.OK
      responseAs[SharedOperatorNumberView] should ===(asView(sharedOperatorNumber))
    }
    Mockito.verify(ps).create(eq(request))(?)
  }

  private def withExpectedOriginOperator(sharedOperatorNumber: SharedOperatorNumber): SharedOperatorNumber = {
    val expected = sharedOperatorNumber.account.operator match {
      case Operators.Vox => sharedOperatorNumber.originOperator
      case other => Some(other)
    }
    sharedOperatorNumber.copy(originOperator = expected)
  }

  private def testCreateFailure(sharedOperatorNumber: SharedOperatorNumber) = {
    val ps = mock[SharedPoolService]
    val route = createHandler(ps)
    val request = createRequestFrom(sharedOperatorNumber)
    import CreateRequestView.modelMarshaller
    import SharedOperatorNumberView.asView
    Post("/phones", request).withHeaders(RequestContext) ~> route ~> check {
      response.status shouldEqual StatusCodes.BadRequest
    }
  }

  "PoolHandler" should {
    "assign" in {
      val ps = mock[SharedPoolService]
      val route = createHandler(ps)
      val request = AssignRequest(
        filter = PhoneFilter(PhoneTypes.Mobile, 2, OperatorAccounts.BillingRealty),
        targetDomain = TypedDomains.autoru_def,
        count = 100
      )
      when(ps.assign(?)(?)).thenReturn(Future.successful(100))
      import AssignRequestView.modelMarshaller
      Post("/assign", request).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(ps).assign(eq(request))(?)
    }

    "create" when {
      "Vox account with Vox origin operator" in {
        val sharedNumber = SharedOperatorNumberGen.next.copy(
          account = OperatorAccountGen.filter(_.operator == Operators.Vox).next,
          originOperator = Some(Operators.Vox)
        )
        testCreateSuccess(sharedNumber)
      }
      "Vox account with possible non Vox origin operator" in {
        val sharedNumber = SharedOperatorNumberGen.next.copy(
          account = OperatorAccountGen.filter(_.operator == Operators.Vox).next,
          originOperator = Some(Gen.oneOf(PossibleVoxOriginOperators.filter(_ != Operators.Vox)).next)
        )
        testCreateSuccess(sharedNumber)
      }
      "non Vox account with same origin operator" in {
        val account = OperatorAccountGen.filter(_.operator != Operators.Vox).next
        val sharedNumber = SharedOperatorNumberGen.next.copy(
          account = account,
          originOperator = Some(account.operator)
        )
        testCreateSuccess(sharedNumber)
      }
      "non Vox account without origin operator" in {
        val sharedNumber = SharedOperatorNumberGen.next.copy(
          account = OperatorAccountGen.filter(_.operator != Operators.Vox).next,
          originOperator = None
        )
        testCreateSuccess(sharedNumber)
      }
    }

    "fail create" when {
      "Vox account without origin operator" in {
        val sharedNumber = SharedOperatorNumberGen.next.copy(
          account = OperatorAccountGen.filter(_.operator == Operators.Vox).next,
          originOperator = None
        )
        testCreateFailure(sharedNumber)
      }
      "Vox account with not possible non Vox origin operator" in {
        val sharedNumber = SharedOperatorNumberGen.next.copy(
          account = OperatorAccountGen.filterNot(PossibleVoxOriginOperators.contains).next,
          originOperator = None
        )
        testCreateFailure(sharedNumber)
      }
      "non Vox account with non equal origin operator" in {
        val account = OperatorAccountGen.filter(_.operator != Operators.Vox).next
        val originOperator = OperatorGen.filter(_ != account.operator).next
        val sharedNumber = SharedOperatorNumberGen.next.copy(
          account = account,
          originOperator = Some(originOperator)
        )
        testCreateFailure(sharedNumber)
      }
      "use not supported operator" in {
        val account = OperatorAccountGen.next.copy(operator = Operators.Tele2)
        val sharedNumber = SharedOperatorNumberGen.next.copy(
          account = account,
          originOperator = None
        )
        testCreateFailure(sharedNumber)
      }
    }

    "delete" in {
      val ps = mock[SharedPoolService]
      val route = createHandler(ps)
      val phone = PhoneGen.next
      when(ps.delete(?)(?)).thenReturn(Future.unit)
      Delete(s"/phones/${phone.value}").withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(ps).delete(eq(phone))(?)
    }

    "set origin operator" in {
      val ps = mock[SharedPoolService]
      val route = createHandler(ps)
      val expectedRequest = UpdateOriginOperatorRequestGen.next
      val protoRequest = UpdateOriginOperatorRequestConversion.to(expectedRequest)
      when(ps.updateOriginOperator(eq(expectedRequest))(?)).thenReturn(Future.unit)
      Post("/phones/originOperator", protoRequest).withHeaders(RequestContext) ~> route ~> check {
        response.status shouldEqual StatusCodes.OK
      }
      Mockito.verify(ps).updateOriginOperator(eq(expectedRequest))(?)
    }

  }

}
