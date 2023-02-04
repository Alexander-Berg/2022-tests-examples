package ru.yandex.vertis.billing.api.routes.main.v1.service.customer.campaign

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.Exceptions.ArtificialInternalException
import ru.yandex.vertis.billing.api.Exceptions.artificialAccessDenyException
import ru.yandex.vertis.billing.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.api.routes.main.v1.view.CampaignPatchView
import ru.yandex.vertis.billing.api.view.CampaignHeaderView.modelUnmarshaller
import ru.yandex.vertis.billing.api.view.{CampaignHeaderView, CampaignSourceView}
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{Producer, ProductGen}
import ru.yandex.vertis.billing.service.CampaignService
import ru.yandex.vertis.billing.util.{OperatorContext, Slice, SlicedResult}

import scala.concurrent.Future

/**
  * Specs campaign handler -
  * [[Handler]]
  *
  * @author ruslansd
  * @author alesavin
  * @author dimas
  */
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = s"/api/1.x/service/autoru/customer/agency/$agencyId/client/$clientId/campaign"

  lazy val agencyId = 1L
  lazy val clientId = 2L

  val customerId = CustomerId(clientId, Some(agencyId))

  val resourceRef = PartnerRef("foo")

  val customerHeader = CustomerHeader(customerId, resourceRef)

  val campaignId = "Id_1"
  val campaignName = "Test name"
  val orderId = 3
  val orderProperties = OrderProperties("Order Properties", None)
  val product = Product(Raising(CostPerMille(100L)))
  val orderBalance = OrderBalance2(9, 10)

  val order = Order(orderId, customerId, orderProperties, orderBalance)

  val campaignHeader =
    CampaignHeader(
      campaignId,
      Some(campaignName),
      customerHeader,
      order,
      product,
      CampaignSettings.Default.copy(isEnabled = false)
    )

  val source = CampaignService.Source(
    Some(campaignName),
    orderId,
    product,
    CampaignSettings.Default.copy(isEnabled = false),
    None,
    List.empty
  )

  "GET /" should {
    val uri = url("/")

    "not provide agency customer campaigns" in {
      Get(uri) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    val request = Get(uri) ~> defaultHeaders

    "provide agency customer campaigns" in {
      stub(backend.campaignService.list(_: CampaignService.Filter, _: Slice)(_: OperatorContext)) {
        case (CampaignService.Filter.ForCustomer(`customerId`), p, `operator`) =>
          Future.successful(SlicedResult(List(campaignHeader), 1, p))
      }
      request ~> route ~> check {
        status should be(StatusCodes.OK)
        val views = responseAs[SlicedResult[CampaignHeaderView]]
        views.size should be(1)
        views.head.asModel should be(campaignHeader)
      }
    }

    "respond InternalServerError in case of backend problems" in {
      stub(backend.campaignService.list(_: CampaignService.Filter, _: Slice)(_: OperatorContext)) {
        case (CampaignService.Filter.ForCustomer(`customerId`), p, `operator`) =>
          Future.failed(ArtificialInternalException())
      }
      request ~> route ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }
  }

  "POST /" should {
    val uri = url("/")

    "not create agency customer campaing without operator" in {
      Post(uri) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    val content = CampaignSourceView.jsonFormat.write(CampaignSourceView(source)).compactPrint
    val entity = HttpEntity(ContentTypes.`application/json`, content)
    val request = Post(uri, entity) ~> defaultHeaders

    "create agency customer campaign" in {
      stub(backend.campaignService.create(_: CustomerId, _: CampaignService.Source)(_: OperatorContext)) {
        case (`customerId`, `source`, `operator`) =>
          Future.successful(campaignHeader)
      }
      request ~> route ~> check {
        status should be(StatusCodes.OK)
        val view = responseAs[CampaignHeader]
        view should be(campaignHeader)
      }
    }

    "respond InternalServerError in case of backend problems" in {
      stub(backend.campaignService.create(_: CustomerId, _: CampaignService.Source)(_: OperatorContext)) {
        case (`customerId`, `source`, `operator`) =>
          Future.failed(ArtificialInternalException())
      }
      request ~> route ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }

    "not create agency customer campaign by safety reasons" in {
      stub(backend.campaignService.create(_: CustomerId, _: CampaignService.Source)(_: OperatorContext)) {
        case (`customerId`, `source`, `operator`) =>
          Future.failed(artificialAccessDenyException(operator.operator))
      }
      request ~> route ~> check {
        status should be(StatusCodes.Forbidden)
      }
    }

    // TODO move to akka http this test case
//    val complaintUri = s"/$campaignId/complain/call/123"
//
//    def complaintRequest(email: String): HttpRequest = {
//      val complaintContent = CallComplaintView.jsonFormat.
//        write(CallComplaintView("placeholder", Some(email))).compactPrint
//      val complaintEntity = HttpEntity(ContentTypes.`application/json`, complaintContent)
//      Post(complaintUri, complaintEntity).withHeaders(requiredHeaders)
//    }
//
//    "success complaint request with good email" in {
//      complaintRequest("good@boy.com") ~> sealRoute(agencyRoute) ~> check {
//        campaignProbe.expectMsgPF() {
//          case req@CampaignActor.Request.Complaint(_, _, _, _, _) =>
//            campaignProbe reply CampaignActor.Response.Complaint(req, Success(()))
//        }
//        status shouldBe StatusCodes.OK
//      }
//    }
//
//    "fail complaint request with bad email" in {
//      complaintRequest("b..a..d@boy.com") ~> sealRoute(agencyRoute) ~> check {
//        status shouldBe StatusCodes.BadRequest
//        responseAs[String] == "Email bad format."
//      }
//    }
  }

  "PUT /{id}" should {
    val uri = url(s"/$campaignId")

    "not update agency customer campaign without operator" in {
      Put(uri) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    val newProduct = ProductGen.next
    val patch = CampaignService.Patch(product = Some(newProduct))

    val content = CampaignPatchView.jsonFormat.write(CampaignPatchView(patch)).compactPrint
    val entity = HttpEntity(ContentTypes.`application/json`, content)
    val request = Put(uri, entity) ~> defaultHeaders
    val updated = campaignHeader.copy(product = newProduct)

    "update agency customer campaign" in {
      stub(backend.campaignService.update(_: CustomerId, _: CampaignId, _: CampaignService.Patch)(_: OperatorContext)) {
        case (`customerId`, `campaignId`, `patch`, `operator`) =>
          Future.successful(updated)
      }
      request ~> route ~> check {
        status should be(StatusCodes.OK)
        val header = responseAs[CampaignHeader]
        header.product should be(newProduct)
      }
    }

    "update agency customer campaign's resetable properties" in {
      val platforms = EnabledPlatforms(Set(Platforms.Desktop))
      val pPatch = CampaignService.Patch(platforms = Some(Update(Some(platforms))), attachRule = Some(Update(None)))
      val pContent = CampaignPatchView.jsonFormat
        .write(CampaignPatchView(pPatch))
        .compactPrint
      val pEntity = HttpEntity(ContentTypes.`application/json`, pContent)
      val pRequest = Put(uri, pEntity) ~> defaultHeaders
      val updated = campaignHeader.copy(settings = CampaignSettings.Default.copy(platforms = Some(platforms)))
      stub(backend.campaignService.update(_: CustomerId, _: CampaignId, _: CampaignService.Patch)(_: OperatorContext)) {
        case (`customerId`, `campaignId`, `pPatch`, `operator`) =>
          Future.successful(updated)
      }
      pRequest ~> route ~> check {
        status should be(StatusCodes.OK)
        val header = responseAs[CampaignHeader]
        header.settings.platforms match {
          case Some(values) =>
            values shouldBe platforms
          case other => fail(s"Unexpected result: $other")
        }
        header.settings.attachRule should be(None)
      }
    }

    "respond InternalServerError in case of backend problems" in {
      stub(backend.campaignService.update(_: CustomerId, _: CampaignId, _: CampaignService.Patch)(_: OperatorContext)) {
        case (`customerId`, `campaignId`, _, `operator`) =>
          Future.failed(ArtificialInternalException())
      }
      request ~> route ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }

    "not update agency customer campaign by safety reasons" in {
      stub(backend.campaignService.update(_: CustomerId, _: CampaignId, _: CampaignService.Patch)(_: OperatorContext)) {
        case (`customerId`, `campaignId`, _, _) =>
          Future.failed(artificialAccessDenyException(operator.operator))
      }
      request ~> route ~> check {
        status should be(StatusCodes.Forbidden)
      }
    }
  }

  "GET /{id}" should {
    val uri = url(s"/$campaignId")

    "not provide agency customer campaigns" in {
      Get(uri) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    val request = Get(uri) ~> defaultHeaders

    "respond InternalServerError in case of backend problems" in {
      stub(backend.campaignService.get(_: CustomerId, _: CampaignId)(_: OperatorContext)) {
        case (`customerId`, `campaignId`, `operator`) =>
          Future.failed(ArtificialInternalException())
      }

      request ~> route ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }

    "not find agency customer campaign by ID" in {
      stub(backend.campaignService.get(_: CustomerId, _: CampaignId)(_: OperatorContext)) {
        case (`customerId`, `campaignId`, `operator`) =>
          Future.failed(new NoSuchElementException())
      }

      request ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }

    "get agency customer campaign" in {
      stub(backend.campaignService.get(_: CustomerId, _: CampaignId)(_: OperatorContext)) {
        case (`customerId`, `campaignId`, `operator`) =>
          Future.successful(campaignHeader)
      }

      request ~> route ~> check {
        status should be(StatusCodes.OK)
        val view = responseAs[CampaignHeader]
        view should be(campaignHeader)
      }
    }
  }
}
