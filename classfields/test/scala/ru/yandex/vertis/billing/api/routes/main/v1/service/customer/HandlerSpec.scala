package ru.yandex.vertis.billing.api.routes.main.v1.service.customer

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.Exceptions.ArtificialNoSuchElementException
import ru.yandex.vertis.billing.api.Exceptions.artificialAccessDenyException
import ru.yandex.vertis.billing.api.routes.main.v1.service.customer.Handler
import ru.yandex.vertis.billing.api.{ErrorResponse, RootHandlerSpecBase}
import ru.yandex.vertis.billing.api.view.CustomerView.modelUnmarshaller
import ru.yandex.vertis.billing.api.view.{ClientView, ResourceView}
import ru.yandex.vertis.billing.dao.CustomerDao
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.security.CustomerNotFoundException
import ru.yandex.vertis.billing.util.{OperatorContext, Page, Slice, SlicedResult}

import scala.annotation.nowarn
import scala.concurrent.Future

/**
  * Specs on customers handler - [[Handler]]
  */
@nowarn("msg=discarded non-Unit value")
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = s"/api/1.x/service/autoru/customer"

  val clientId = 1L
  val agencyId = 2L
  val directCustomerId = CustomerId(clientId, None)
  val agencyCustomerId = CustomerId(clientId, Some(agencyId))

  val directClient = gens.ClientGen.next.copy(id = clientId)
  val agencyClient = gens.ClientGen.next.copy(id = clientId)
  val directCustomer = Customer(directCustomerId, directClient, None, Seq(PartnerRef("100")))
  val agencyCustomer = Customer(agencyCustomerId, agencyClient, Some(agencyClient), Seq(PartnerRef("200")))

  "GET /client/{clientId}" should {
    val uri = url(s"/client/$clientId")
    "not try to find customer for client without operator" in {
      Get(uri) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }
    val request = Get(uri) ~> defaultHeaders
    "find direct customer" in {
      stub(backend.customerService.get(_: CustomerId)(_: OperatorContext)) { case (`directCustomerId`, `operator`) =>
        Future.successful(directCustomer)
      }
      request ~> route ~> check {
        status should be(StatusCodes.OK)
        val header = responseAs[Customer]
        header.id.clientId should be(clientId)
        header.id.agencyId should be(None)
        header.client shouldBe directClient
      }
    }
    "not find direct customer" in {
      stub(backend.customerService.get(_: CustomerId)(_: OperatorContext)) { case (`directCustomerId`, `operator`) =>
        Future.failed(ArtificialNoSuchElementException())
      }
      request ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }
    "not provide direct customer by safety reasons" in {
      stub(backend.customerService.get(_: CustomerId)(_: OperatorContext)) { case (`directCustomerId`, `operator`) =>
        Future.failed(artificialAccessDenyException(operator.operator))
      }
      request ~> route ~> check {
        status should be(StatusCodes.Forbidden)
      }
    }
  }

  "GET /agency/{agencyId}/client/{clientId}" should {
    val uri = url(s"/agency/$agencyId/client/$clientId")
    "not try to find customer for agency client without operator" in {
      Get(uri) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }
    val request = Get(uri) ~> defaultHeaders
    "find agency customer" in {
      stub(backend.customerService.get(_: CustomerId)(_: OperatorContext)) { case (`agencyCustomerId`, `operator`) =>
        Future.successful(agencyCustomer)
      }
      request ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
    "not find agency customer" in {
      stub(backend.customerService.get(_: CustomerId)(_: OperatorContext)) { case (`agencyCustomerId`, `operator`) =>
        Future.failed(ArtificialNoSuchElementException())
      }
      request ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }
    "not provide agency customer by safety reasons" in {
      stub(backend.customerService.get(_: CustomerId)(_: OperatorContext)) { case (`agencyCustomerId`, `operator`) =>
        Future.failed(artificialAccessDenyException(operator.operator))
      }

      request ~> route ~> check {
        status should be(StatusCodes.Forbidden)
      }
    }
  }

  "GET /agency/{agencyId}/clients" should {
    val agencyId = agencyCustomerId.agencyId.get
    val uri = url(s"/agency/$agencyId/clients")
    "not try to provide agency customers without operator" in {
      Get(uri) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }
    val request = Get(uri) ~> defaultHeaders
    "provide agency customers" in {
      stub(backend.customerService.find(_: CustomerDao.Query, _: Slice)(_: OperatorContext)) {
        case (CustomerDao.Query.ForAgency(`agencyId`), p, `operator`) =>
          Future.successful(SlicedResult(Iterable(agencyCustomer), 1, p))
      }
      request ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
    "not provide agency customers by safety reasons" in {
      stub(backend.customerService.find(_: CustomerDao.Query, _: Slice)(_: OperatorContext)) {
        case (CustomerDao.Query.ForAgency(`agencyId`), p, `operator`) =>
          Future.failed(artificialAccessDenyException(operator.operator))
      }
      request ~> route ~> check {
        status should be(StatusCodes.Forbidden)
      }
    }
    "not provide agency customers if agency does not exist" in {
      stub(backend.customerService.find(_: CustomerDao.Query, _: Slice)(_: OperatorContext)) {
        case (CustomerDao.Query.ForAgency(`agencyId`), p, `operator`) =>
          Future.failed(CustomerNotFoundException(agencyCustomerId.toString))
      }
      request ~> route ~> check {
        status should be(StatusCodes.NotFound)
        contentType shouldBe ContentTypes.`application/json`
        responseAs[ErrorResponse].code shouldBe CustomerNotFoundException.ResponseCode
      }
    }
    "provide correct slice of customers" in {
      val page = Page(1, 100)
      stub(backend.customerService.find(_: CustomerDao.Query, _: Slice)(_: OperatorContext)) {
        case (CustomerDao.Query.ForAgency(`agencyId`), p, `operator`) =>
          Future.successful(SlicedResult.empty(p))
      }
      Get(s"$uri?pageNum=${page.number}&pageSize=${page.size}") ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }
  }

  "POST /client/{clientId}" should {
    val uri = url(s"/client/$clientId")

    "not try to create customer without operator" in {
      Post(uri) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    def create(r: Resource): Unit = {
      createWithResource(r, uri, directCustomer)
    }

    "create direct customer with Site resource" in {
      create(Site("www.example.org"))
    }

    "create direct customer with XmlFeed resource" in {
      create(XmlFeed("http://www.example.org/feed.xml"))
    }

    "create direct customer with Partner resource" in {
      create(Partner("123"))
    }
  }

  "POST /agency/{agencyId}/client/{clientId}" should {
    val uri = url(s"/agency/$agencyId/client/$clientId")

    "not try to create agency customer without operator" in {
      Post(uri) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    def create(r: Resource): Unit = {
      createWithResource(r, uri, agencyCustomer)
    }

    "create agency customer with Site resource" in {
      create(Site("www.example.org"))
    }

    "create agency customer with XmlFeed resource" in {
      create(XmlFeed("http://www.example.org/feed.xml"))
    }

    "create agency customer with Partner resource" in {
      create(Partner("123"))
    }
  }

  "GET /?(clientId={clientId}|agencyId={agencyId}|isAgency={true|false})" should {
    "not try to find customers without operator" in {
      Get(url("/?clientId=1")) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    def find(uri: String, query: CustomerDao.Query, slice: Slice): Unit = {
      val request = Get(url(uri)) ~> defaultHeaders
      stub(backend.customerService.find(_: CustomerDao.Query, _: Slice)(_: OperatorContext)) {
        case (`query`, p, `operator`) =>
          Future.successful(SlicedResult.empty(slice))
      }
      request ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }

    "find customers by clientId" in {
      find("/?clientId=1", CustomerDao.Query.ForClient(1), Page(0, 10))
    }

    "find customers by agencyId" in {
      find("/?agencyId=2&pageNum=2&pageSize=10", CustomerDao.Query.ForAgency(2), Page(2, 10))
    }

    "find direct customers" in {
      find("/?isAgency=false&pageNum=2&pageSize=5", CustomerDao.Query.ByType(false), Page(2, 5))
    }

    "find agency customers" in {
      find("/?isAgency=true&pageNum=0&pageSize=5", CustomerDao.Query.ByType(true), Page(0, 5))
    }

    "find all customers" in {
      find("/?pageNum=0&pageSize=5", CustomerDao.Query.All, Page(0, 5))
    }
  }

  private def createWithResource(r: Resource, uri: String, customer: Customer): Unit = {
    val content = ResourceView.jsonFormat.write(ResourceView(r)).compactPrint
    val entity = HttpEntity(ContentTypes.`application/json`, content)
    val request = Post(uri, entity) ~> defaultHeaders

    stub(backend.customerService.create(_: CustomerId, _: Resource)(_: OperatorContext)) {
      case (customer.`id`, `r`, `operator`) =>
        Future.successful(customer)
    }

    request ~> sealRoute(route) ~> check {
      status should be(StatusCodes.OK)
    }
  }
}
