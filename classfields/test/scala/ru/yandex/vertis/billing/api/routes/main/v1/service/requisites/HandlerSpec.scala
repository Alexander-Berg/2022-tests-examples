package ru.yandex.vertis.billing.api.routes.main.v1.service.requisites

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.api.routes.main.v1.view.proto.RequisitesProtoConversions.RequisitesPropertiesConversion
import ru.yandex.vertis.billing.api.{ErrorResponse, RootHandlerSpecBase}
import ru.yandex.vertis.billing.exceptions.{AutoruClientNotFoundException, RequisitesApiException}
import ru.yandex.vertis.billing.model_core.gens.{Producer, RequisitesGen}
import ru.yandex.vertis.billing.model.proto.{RequisitesIdResponse, RequisitesResponse}
import ru.yandex.vertis.billing.model_core.requisites.Requisites
import ru.yandex.vertis.billing.model_core.{AutoruClientId, RequisitesId}
import ru.yandex.vertis.billing.util.OperatorContext
import ru.yandex.vertis.util.akka.http.protobuf.{Protobuf, ProtobufSupport}

import scala.concurrent.Future

class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase with ProtobufSupport {

  val requisitesService = backend.asyncRequisitesService
  val clientId = 111L
  val requisitesId = 1000L

  override def basePath: String = "/api/1.x/service/autoru/requisites"

  "GET /client/{clientId}/requisites" should {

    "not try to find requisites without specified operator (via header)" in {
      Get(url(s"/client/$clientId/requisites")) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    "not find requisites by client id" in {
      backend.asyncRequisitesService.foreach { service =>
        stub(service.getPaymentRequisites(_: AutoruClientId)(_: OperatorContext)) { case (`clientId`, `operator`) =>
          Future.failed(AutoruClientNotFoundException(clientId))
        }
      }

      Get(url(s"/client/$clientId/requisites")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.NotFound)
        }
    }

    "find empty requisites by client id " in {
      backend.asyncRequisitesService.foreach { service =>
        stub(service.getPaymentRequisites(_: AutoruClientId)(_: OperatorContext)) { case (`clientId`, `operator`) =>
          Future.successful(List.empty)
        }
      }

      Get(url(s"/client/$clientId/requisites")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = responseAs[RequisitesResponse]
          response.getRequisitesListList should be(empty)
        }
    }

    "find non empty requisites by client id " in {
      val records = RequisitesGen.next(5)
      backend.asyncRequisitesService.foreach { service =>
        stub(service.getPaymentRequisites(_: AutoruClientId)(_: OperatorContext)) { case (`clientId`, `operator`) =>
          Future.successful(records)
        }
      }

      Get(url(s"/client/$clientId/requisites")) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val response = responseAs[RequisitesResponse]
          response.getRequisitesListList.size() should be(5)
        }
    }
  }

  "POST /client/{clientId}/requisites" should {

    "not try to create requisites without specified operator (via header)" in {
      Post(url(s"/client/$clientId/requisites")) ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }

    "create requisites in json" in {
      backend.asyncRequisitesService.foreach { service =>
        stub(service.addPaymentRequisites(_: AutoruClientId, _: Requisites.Properties)(_: OperatorContext)) {
          case (`clientId`, _, `operator`) => Future.successful(requisitesId)
        }
      }

      val content = Protobuf.toJson(RequisitesPropertiesConversion.to(RequisitesGen.next.properties))
      val entity = HttpEntity(ContentTypes.`application/json`, content)
      Post(url(s"/client/$clientId/requisites"), entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          responseAs[RequisitesIdResponse].getId should be(requisitesId)
        }
    }

    "create requisites in protobuf" in {
      backend.asyncRequisitesService.foreach { service =>
        stub(service.addPaymentRequisites(_: AutoruClientId, _: Requisites.Properties)(_: OperatorContext)) {
          case (`clientId`, _, `operator`) => Future.successful(requisitesId)
        }
      }

      val content = RequisitesPropertiesConversion.to(RequisitesGen.next.properties).toByteArray
      val entity = HttpEntity(Protobuf.contentType, content)
      Post(url(s"/client/$clientId/requisites"), entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          responseAs[RequisitesIdResponse].getId should be(requisitesId)
        }
    }

    "not create requisites for non existent client" in {
      val clientNotFoundException = AutoruClientNotFoundException(clientId)
      backend.asyncRequisitesService.foreach { service =>
        stub(service.addPaymentRequisites(_: AutoruClientId, _: Requisites.Properties)(_: OperatorContext)) {
          case (`clientId`, _, `operator`) => Future.failed(clientNotFoundException)
        }
      }

      val content = Protobuf.toJson(RequisitesPropertiesConversion.to(RequisitesGen.next.properties))
      val entity = HttpEntity(ContentTypes.`application/json`, content)
      Post(url(s"/client/$clientId/requisites"), entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.NotFound)
        }
    }

    "reject bad requests" in {
      val invalidParamCode = "INVALID_PARAM"
      val invalidParamMessage = "INN should be 10 or 12 digits"
      val invalidParamException = RequisitesApiException(invalidParamCode, invalidParamMessage, "")
      backend.asyncRequisitesService.foreach { service =>
        stub(service.addPaymentRequisites(_: AutoruClientId, _: Requisites.Properties)(_: OperatorContext)) {
          case (`clientId`, _, `operator`) => Future.failed(invalidParamException)
        }
      }

      val content = Protobuf.toJson(RequisitesPropertiesConversion.to(RequisitesGen.next.properties))
      val entity = HttpEntity(ContentTypes.`application/json`, content)
      Post(url(s"/client/$clientId/requisites"), entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
          val response = responseAs[ErrorResponse]
          response.code should be(invalidParamCode)
          response.message should be(invalidParamMessage)
        }
    }
  }

  "PUT /client/{clientId}/requisites/{requisitesId}" should {

    "not try to update requisites without specified operator (via header)" in {
      Put(url(s"/client/$clientId/requisites/$requisitesId")) ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }

    "update requisites in json" in {
      backend.asyncRequisitesService.foreach { service =>
        stub(
          service
            .updatePaymentRequisites(_: AutoruClientId, _: RequisitesId, _: Requisites.Properties)(_: OperatorContext)
        ) { case (`clientId`, `requisitesId`, _, `operator`) =>
          Future.successful(requisitesId)
        }
      }

      val content = Protobuf.toJson(RequisitesPropertiesConversion.to(RequisitesGen.next.properties))
      val entity = HttpEntity(ContentTypes.`application/json`, content)
      Put(url(s"/client/$clientId/requisites/$requisitesId"), entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          responseAs[RequisitesIdResponse].getId should be(requisitesId)
        }
    }

    "update requisites in protobuf" in {
      backend.asyncRequisitesService.foreach { service =>
        stub(
          service
            .updatePaymentRequisites(_: AutoruClientId, _: RequisitesId, _: Requisites.Properties)(_: OperatorContext)
        ) { case (`clientId`, `requisitesId`, _, `operator`) =>
          Future.successful(requisitesId)
        }
      }

      val content = RequisitesPropertiesConversion.to(RequisitesGen.next.properties).toByteArray
      val entity = HttpEntity(Protobuf.contentType, content)
      Put(url(s"/client/$clientId/requisites/$requisitesId"), entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          responseAs[RequisitesIdResponse].getId should be(requisitesId)
        }
    }

    "not update requisites for non existent client" in {
      val clientNotFoundException = AutoruClientNotFoundException(clientId)
      backend.asyncRequisitesService.foreach { service =>
        stub(
          service
            .updatePaymentRequisites(_: AutoruClientId, _: RequisitesId, _: Requisites.Properties)(_: OperatorContext)
        ) { case (`clientId`, `requisitesId`, _, `operator`) =>
          Future.failed(clientNotFoundException)
        }
      }

      val content = Protobuf.toJson(RequisitesPropertiesConversion.to(RequisitesGen.next.properties))
      val entity = HttpEntity(ContentTypes.`application/json`, content)
      Put(url(s"/client/$clientId/requisites/$requisitesId"), entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.NotFound)
        }
    }

    "reject mismatched requisites updates" in {
      val personTypeMismatchCode = "PERSON_TYPE_MISMATCH"
      val personTypeMismatchMessage = "Person type (ph) cannot be changed to ur"
      val personTypeMismatchException = RequisitesApiException(personTypeMismatchCode, personTypeMismatchMessage, "")
      backend.asyncRequisitesService.foreach { service =>
        stub(
          service
            .updatePaymentRequisites(_: AutoruClientId, _: RequisitesId, _: Requisites.Properties)(_: OperatorContext)
        ) { case (`clientId`, `requisitesId`, _, `operator`) =>
          Future.failed(personTypeMismatchException)
        }
      }

      val content = Protobuf.toJson(RequisitesPropertiesConversion.to(RequisitesGen.next.properties))
      val entity = HttpEntity(ContentTypes.`application/json`, content)
      Put(url(s"/client/$clientId/requisites/$requisitesId"), entity) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
          val response = responseAs[ErrorResponse]
          response.code should be(personTypeMismatchCode)
          response.message should be(personTypeMismatchMessage)
        }
    }
  }
}
