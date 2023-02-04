package ru.yandex.vos2.autoru.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit._
import org.joda.time.DateTime
import org.scalactic.source
import org.scalatest.SuiteMixin
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsValue, Json, Reads}
import ru.auto.api.ApiOfferModel.{Category, Offer}
import ru.auto.api.CommonModel.PaidService
import ru.auto.api.RequestModel.AddServicesRequest
import ru.auto.api.ResponseModel.{MarkModelsResponse, MotoCategoriesResponse, OfferListingResponse, TruckCategoriesResponse}
import ru.auto.api.{ApiOfferModel, RequestModel}
import ru.yandex.vertis.application.deploy.Deploys
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.application.runtime.RuntimeConfigImpl
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.autoru.config.TestAutoruApiComponents
import ru.yandex.vos2.autoru.model.VosDraft
import ru.yandex.vos2.autoru.utils.Vos2ApiHandlerResponses.{validationError, _}
import ru.yandex.vos2.autoru.utils.testforms.TestFormsGenerator
import ru.yandex.vos2.autoru.utils.validators.ValidationErrors.ValidationError
import ru.yandex.vos2.autoru.{ApiHandlersSystemBuilder, InitTestDbs}
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.util.{Protobuf, RandomUtil}

import scala.jdk.CollectionConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.ClassTag

trait Vos2ApiSuite extends SuiteMixin with InitTestDbs with ScalatestRouteTest with Matchers with MockitoSupport {
  initDbs()

  lazy val apiComponents = new TestAutoruApiComponents {
    override lazy val coreComponents = components
    override lazy val actorSystem = system
  }

  when(components.resellerDeactivationParams.startDate)
    .thenReturn(None)
  when(components.resellerDeactivationParams.endDate)
    .thenReturn(None)
  when(components.resellerDeactivationParams.regionIds)
    .thenReturn(Set.empty[Long])
  when(components.resellerDeactivationParams.ifExpRuns)
    .thenReturn(false)

  implicit protected val td = TildeArrow.injectIntoRoute

  lazy val testFormGenerator = new TestFormsGenerator(components)

  // вот так можно увеличить таймаут, если требуется
  implicit def default(implicit system: ActorSystem): RouteTestTimeout = {
    RouteTestTimeout(new DurationInt(6000).second.dilated(system))
  }

  val route = new ApiHandlersSystemBuilder(
    apiComponents,
    RuntimeConfigImpl(Environments.Testing, "localhost", "localhost", Deploys.Debian, None)
  )(
    apiComponents.coreComponents.ec
  ).rootHandler.route

  protected def createRequest(
      req: HttpRequest,
      formContentType: ContentType = ContentTypes.`application/json`
  )(func: ApiOfferModel.Offer.Builder => Unit = builder => ()): HttpRequest = {
    val formBuilder = ApiOfferModel.Offer.newBuilder
    func(formBuilder)
    val form = formBuilder.build()
    val json = Protobuf.toJson(form)
    req.withEntity(HttpEntity(formContentType, json.getBytes("UTF-8")))
  }

  case class AddServiceRequest(service: String,
                               badge: Option[String] = None,
                               active: Boolean = false,
                               expired: Boolean = false,
                               absentDays: Boolean = false)

  protected def createAddServicesRequest(service: String,
                                         badge: Option[String] = None,
                                         active: Boolean = false,
                                         expired: Boolean = false,
                                         absentDays: Boolean = false): RequestModel.AddServicesRequest = {
    createMultipleAddServicesRequests(AddServiceRequest(service, badge, active, expired, absentDays))
  }

  protected def createMultipleAddServicesRequests(info: AddServiceRequest*): RequestModel.AddServicesRequest = {
    val builder: AddServicesRequest.Builder = AddServicesRequest.newBuilder()
    info.foreach {
      case AddServiceRequest(service, badge, active, expired, absentDays) =>
        val serviceBuilder: PaidService.Builder = PaidService.newBuilder().setIsActive(active).setService(service)
        if (!absentDays) {
          val createDate = DateTime.now.minusDays(RandomUtil.nextInt(3, 7)).getMillis
          val expireDate = if (expired) {
            DateTime.now.minusDays(1).getMillis
          } else {
            DateTime.now.plusDays(RandomUtil.nextInt(3, 7)).getMillis
          }
          serviceBuilder.setCreateDate(createDate).setExpireDate(expireDate)
        }
        badge.foreach(serviceBuilder.setBadge)
        builder.addServices(serviceBuilder)
    }
    builder.build()
  }

  protected def checkErrorRequest(
      req: HttpRequest,
      needStatus: StatusCode,
      needResponse: ErrorResponse
  )(implicit pos: source.Position): Unit = {
    req ~> route ~> check {
      val response: String = responseAs[String]
      withClue(req + "\n" + response + "\n") {
        status shouldBe needStatus
        val errorResponse = Json.parse(response).validate[ErrorResponse].asOpt.value
        assert(errorResponse == needResponse)
      }
    }
  }

  protected def checkValidationErrorRequest(
      req: HttpRequest,
      needResponse: ValidationErrorResponse
  )(implicit pos: source.Position): Unit = {
    req ~> route ~> check {
      val response: String = responseAs[String]
      withClue(req + "\n" + response) {
        status shouldBe StatusCodes.BadRequest
        val errorResponse = Json.parse(response).validate[ValidationErrorResponse].asOpt.value
        val errors: List[ValidationErrorDescription] = errorResponse.errors
        val needErrors: List[ValidationErrorDescription] = needResponse.errors
        checkErrors(errors, needErrors)
      }
    }
  }

  protected def checkValidationErrorRequestContains(
      req: HttpRequest,
      needValidationErrors: ValidationError*
  )(implicit pos: source.Position): Unit = {
    req ~> route ~> check {
      val response: String = responseAs[String]
      withClue(req + "\n" + response) {
        status shouldBe StatusCodes.BadRequest
        val errorResponse = Json.parse(response).validate[ValidationErrorResponse].asOpt.value
        val resultErrors: List[ValidationErrorDescription] = errorResponse.errors
        val needErrors: List[ValidationErrorDescription] = validationError(needValidationErrors: _*).errors
        assert(
          needErrors.forall(resultErrors.contains),
          s"validation must fail with messages" +
            s"\n${needErrors.map(_.error_code).mkString(", ")},\nbut some or all of these messages wasn't " +
            s"found:\n${needErrors.diff(resultErrors).map(_.error_code).mkString(", ")}\n" +
            s"these messages found: ${resultErrors.map(_.error_code).mkString(", ")}"
        )
      }
    }
  }

  protected def checkValidationErrorRequestNotContains(
      req: HttpRequest,
      needValidationErrors: ValidationError*
  )(implicit pos: source.Position): Unit = {
    req ~> route ~> check {
      val response: String = responseAs[String]
      withClue(req + "\n" + response) {
        status shouldBe StatusCodes.BadRequest
        val errorResponse = Json.parse(response).validate[ValidationErrorResponse].asOpt.value
        val resultErrors: List[ValidationErrorDescription] = errorResponse.errors
        val needErrors: List[ValidationErrorDescription] = validationError(needValidationErrors: _*).errors
        assert(
          needErrors.forall(x => !resultErrors.contains(x)),
          s"validation must not fail with messages" +
            s"\n${needErrors.map(_.error_code).mkString(", ")},\nbut some or all of these messages were " +
            s"found:\n${needErrors.filter(resultErrors.contains).map(_.error_code).mkString(", ")}"
        )
      }
    }
  }

  private def checkErrors(
      resultErrors: List[ValidationErrorDescription],
      needErrors: List[ValidationErrorDescription]
  )(implicit pos: source.Position): Unit = {
    val check1: Boolean = needErrors.forall(resultErrors.contains)
    val check2: Boolean = resultErrors.forall(needErrors.contains)
    val message = new StringBuilder
    if (!check1)
      message.append(
        s"validation must fail with messages" +
          s"\n${needErrors.map(_.error_code).mkString(", ")},\nbut some or all of these messages wasn't " +
          s"found:\n${needErrors.diff(resultErrors).map(_.error_code).mkString(", ")}\n" +
          s"these messages found: ${resultErrors.map(_.error_code).mkString(", ")}\n"
      )
    if (!check2)
      message.append(
        s"unexpected messages found:" +
          s"\n${resultErrors.diff(needErrors).map(_.error_code).mkString(", ")}"
      )
    assert(check1 && check2, message.toString())
  }

  protected def checkSuccessStringRequest(req: HttpRequest)(implicit pos: source.Position): String = {
    req ~> route ~> check {
      val response: String = responseAs[String]
      withClue(req + "\n" + response) {
        status shouldBe StatusCodes.OK
        response
      }
    }
  }

  protected def checkSuccessJsonRequest[T](req: HttpRequest)(implicit reads: Reads[T], pos: source.Position): T = {
    checkSuccessJsonValueRequest(req).validate(reads).asOpt.value
  }

  protected def checkSuccessJsonValueRequest[T](req: HttpRequest)(implicit pos: source.Position): JsValue = {
    Json.parse(checkSuccessStringRequest(req))
  }

  protected def checkSuccessProtoFromJsonRequest[T: ClassTag](req: HttpRequest)(implicit pos: source.Position): T = {
    Protobuf.fromJson[T](checkSuccessStringRequest(req))
  }

  protected def checkSuccessProtobufRequest[T: ClassTag](req: HttpRequest)(implicit pos: source.Position): T = {
    req ~> route ~> check {
      val response: Array[Byte] = responseAs[Array[Byte]]
      withClue(response) {
        status shouldBe StatusCodes.OK
        val successResponse = Protobuf.fromBytes[T](response)
        successResponse
      }
    }
  }

  protected def checkSimpleSuccessRequest(req: HttpRequest)(implicit pos: source.Position): Unit = {
    req ~> route ~> check {
      val response: String = responseAs[String]
      withClue(response) {
        status shouldBe StatusCodes.OK
      }
    }
  }

  protected def checkSuccessRequest(req: HttpRequest)(implicit pos: source.Position): String = {
    req ~> route ~> check {
      val response: String = responseAs[String]
      withClue(req + "\n" + response) {
        status shouldBe StatusCodes.OK
        val successResponse = Json.parse(response).validate[SuccessResponse].asOpt.value
        successResponse.offerId
      }
    }
  }

  protected def checkSuccessRequestWithOffer(req: HttpRequest)(implicit pos: source.Position): ApiOfferModel.Offer = {
    req ~> route ~> check {
      val response: String = responseAs[String]
      withClue(req + "\n" + response) {
        status shouldBe StatusCodes.OK
        val jsonResponse = Json.parse(response)
        val successResponse = jsonResponse.validate[SuccessResponse].asOpt.value

        val jsonOffer: String = Json.stringify((jsonResponse \ "offer").getOrElse(sys.error("error!")))
        val offer = Protobuf.fromJson[Offer](jsonOffer)

        successResponse.offerId shouldBe offer.getId

        offer
      }
    }
  }

  protected def checkSuccessListingRequest(req: HttpRequest)(implicit pos: source.Position): OfferListingResponse = {
    checkSuccessProtoFromJsonRequest[OfferListingResponse](req)
  }

  protected def checkSuccessMarkModelsRequest(req: HttpRequest)(implicit pos: source.Position): MarkModelsResponse = {
    checkSuccessProtoFromJsonRequest[MarkModelsResponse](req)
  }

  protected def checkSuccessTruckCategoriesRequest(
      req: HttpRequest
  )(implicit pos: source.Position): TruckCategoriesResponse = {
    checkSuccessProtoFromJsonRequest[TruckCategoriesResponse](req)
  }

  protected def checkSuccessMotoCategoriesRequest(
      req: HttpRequest
  )(implicit pos: source.Position): MotoCategoriesResponse = {
    checkSuccessProtoFromJsonRequest[MotoCategoriesResponse](req)
  }

  protected def checkSuccessReadRequest(req: HttpRequest)(implicit pos: source.Position): ApiOfferModel.Offer = {
    req ~> route ~> check {
      val response: String = responseAs[String]
      withClue(response) {
        status shouldBe StatusCodes.OK
        Protobuf.fromJson[ApiOfferModel.Offer](response)
      }
    }
  }

  protected def checkSuccessOfferRequest(req: HttpRequest)(implicit pos: source.Position): OfferModel.Offer = {
    req ~> route ~> check {
      val response: String = responseAs[String]
      withClue(req + "\n" + response) {
        status shouldBe StatusCodes.OK
        Protobuf.fromJson[OfferModel.Offer](response)
      }
    }
  }

  protected def checkStatusHistory(
      category: String,
      offerId: String,
      statuses: (String, String)*
  )(implicit pos: source.Position): Unit = {
    val req: String = s"/api/v1/offer/$category/$offerId/status/history?fromvos=1&include_removed=1"
    val statusHistory = checkSuccessJsonRequest[StatusHistory](Get(req))
    assert(statusHistory.status_history.length == statuses.length)
    statusHistory.status_history.zip(statuses).zipWithIndex.foreach {
      case ((elem, status), idx) =>
        assert(elem.status == status._1, s"unexpected status for $idx elem $elem")
        assert(elem.comment == status._2, s"unexpected comment for $idx elem $elem")
    }
  }

  protected def checkDraftStatusHistory(
      userRef: UserRef,
      category: String,
      draftId: String,
      statuses: (String, String)*
  )(implicit pos: source.Position, trace: Traced): Unit = {
    val draft = readDraft(userRef, category, draftId)
    val statusHistory = draft.getStatusHistoryList.asScala
    assert(statusHistory.length == statuses.length)
    statusHistory.zip(statuses).zipWithIndex.foreach {
      case ((elem, status), idx) =>
        assert(elem.getOfferStatus.name() == status._1, s"unexpected status for $idx elem $elem")
        assert(elem.getComment == status._2, s"unexpected comment for $idx elem $elem")
    }
  }

  protected def checkUserChangeActionHistory(
      category: String,
      offerId: String,
      statuses: List[String]
  )(implicit pos: source.Position): Unit = {
    val req: String = s"/api/v1/offer/$category/$offerId/user_change_action_history"
    val resp = checkSuccessJsonRequest[UserChangeActionResponse](Get(req))
    resp.items.map(_.status).zip(statuses).foreach {
      case (actual, expected) => assert(expected == actual)
    }
  }

  protected def readDraft(
      userRef: UserRef,
      category: String,
      draftId: String
  )(implicit trace: Traced, pos: source.Position): VosDraft = {
    val result =
      Await
        .result(
          components.draftDao
            .getDraft(userRef, Category.valueOf(category.toUpperCase), draftId, includeRemoved = true)
            .value,
          Duration.Inf
        )
        .toOption

    assert(result.isDefined, s"Expected draft here $category/$userRef/$draftId")
    result.get
  }
}
