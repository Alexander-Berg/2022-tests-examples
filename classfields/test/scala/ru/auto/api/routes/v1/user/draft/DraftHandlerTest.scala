package ru.auto.api.routes.v1.user.draft

import akka.http.scaladsl.model.headers.{Accept, RawHeader}
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, MediaTypes, StatusCodes}
import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsObject, Json}
import ru.auto.api.ApiOfferModel.{Offer, Seller}
import ru.auto.api.ResponseModel.{DraftResponse, OffersSaveSuccessResponse, ResponseStatus, SuccessResponse}
import ru.auto.api.exceptions.BannedDomainException
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.offers.DraftsManager
import ru.auto.api.managers.offers.DraftsManager.PublishResult
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.ModelGenerators.{OfferGen, OfferIDGen, SessionResultGen}
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.{AutoruUser, CategorySelector, ModelGenerators, OfferID}
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.{MockedClients, MockedPassport}
import ru.auto.api.util.{ManagerUtils, Protobuf}
import ru.auto.api.{ApiOfferModel, ApiSuite, CarsModel}
import ru.yandex.vertis.feature.model.Feature

import scala.concurrent.Future

/**
  * Created by andrey on 3/1/17.
  */
class DraftHandlerTest extends ApiSuite with ScalaCheckPropertyChecks with MockedClients with MockedPassport {

  override lazy val draftsManager: DraftsManager = mock[DraftsManager]
  override lazy val featureManager: FeatureManager = mock[FeatureManager]

  before {
    reset(passportManager, draftsManager)
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
    when(passportManager.getSessionFromUserTicket()(?)).thenReturnF( /*Some(SessionResultGen.next)*/ None)
    when(passportManager.getSession(?)(?)).thenReturnF( /*Some(SessionResultGen.next)*/ None)

    val chatsDontTextMeFeatureTrue: Feature[Boolean] = mock[Feature[Boolean]]
    when(chatsDontTextMeFeatureTrue.value).thenReturn(true)
    when(featureManager.toString).thenReturn("featureManager")
    when(featureManager.chatsDontTextMe).thenReturn {
      new Feature[Boolean] {
        override def name: String = "chats_dont_text_me"
        override def value: Boolean = true
      }
    }
  }

  after {
    verifyNoMoreInteractions(passportManager, draftsManager)
  }

  private def responseFor(draft: Offer): Future[DraftResponse] = Future.successful {
    DraftResponse
      .newBuilder()
      .setOfferId(draft.getId)
      .setOffer(draft)
      .build()
  }

  private def responseForOption(draft: Offer): Future[Option[DraftResponse]] = Future.successful {
    Option(
      DraftResponse
        .newBuilder()
        .setOfferId(draft.getId)
        .setOffer(draft)
        .build()
    )
  }

  private def saveResponseFor(offer: Offer): Future[OffersSaveSuccessResponse] = Future.successful {
    ManagerUtils.offerSaveSuccessResponse(offer, Nil)
  }

  test("create draft with uid") {
    val offer1 = OfferGen.next
    when(draftsManager.draftCreate(?, ?, ?, ?)(?)).thenReturn(responseFor(offer1))

    val user = ModelGenerators.PrivateUserRefGen.next
    val offer = ApiOfferModel.Offer.newBuilder().build()

    checkSuccessRequest(
      Post(s"/1.0/user/draft/cars", offer).withHeaders(RawHeader("x-uid", user.uid.toString)),
      expectedOfferId = offer1.id
    )
    verify(draftsManager).draftCreate(eq(Cars), eq(user), eq(offer), eq(false))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("send broker event") {
    val offer1 = OfferGen.next

    val user = ModelGenerators.PrivateUserRefGen.next
    when(draftsManager.lightFormRequest(?, ?, ?)(?)).thenReturn(Future.successful(ManagerUtils.SuccessResponse))

    checkSuccessRequest(
      Post(s"/1.0/user/draft/cars/${offer1.getId}/light-form-request")
        .withHeaders(RawHeader("x-uid", user.uid.toString))
    )
    verify(draftsManager).lightFormRequest(eq(Cars), eq(user), eq(OfferID.parse(offer1.getId)))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("update draft") {
    val offerId = OfferIDGen.next

    val user = ModelGenerators.PrivateUserRefGen.next
    val offer = ApiOfferModel.Offer
      .newBuilder()
      .setId(offerId.toPlain)
      .setCarInfo(CarsModel.CarInfo.newBuilder().setArmored(true))
      .setSeller(Seller.newBuilder().setChatsEnabled(true).build())
      .build()

    when(draftsManager.updateDraftWithPartnerOptions(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(responseFor(offer))

    checkSuccessRequest(
      Put(s"/1.0/user/draft/cars/$offerId", offer)
        .withHeaders(RawHeader("x-uid", user.uid.toString)),
      expectedOfferId = offerId
    )

    verify(draftsManager).updateDraftWithPartnerOptions(
      eq(Cars),
      eq(user),
      eq(offerId),
      eq(offer),
      eq(false),
      eq(true),
      eq(true)
    )(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("publish draft") {
    val offer = OfferGen.next
    val offerId = offer.id
    val offerId1 = OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next
    when(draftsManager.publishDraft(?, ?, ?, ?, ?, ?, ?, ?)(?))
      .thenReturn(saveResponseFor(offer).map(PublishResult(_, isNew = false)))
    // шлем с uid
    checkSuccessRequest(
      Post(s"/1.0/user/draft/cars/$offerId1/publish").withHeaders(RawHeader("x-uid", user.uid.toString)),
      expectedOfferId = offerId
    )
    verify(draftsManager).publishDraft(eq(Cars), eq(user), eq(offerId1), eq(None), ?, ?, ?, eq(None))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("publish draft as banned user") {
    val offer = OfferGen.next
    val user = ModelGenerators.PrivateUserRefGen.next
    when(draftsManager.publishDraft(?, ?, ?, ?, ?, ?, ?, ?)(?))
      .thenReturn(Future.failed(new BannedDomainException("CARS")))

    Post(s"/1.0/user/draft/cars/${offer.id}/publish").withHeaders(RawHeader("x-uid", user.uid.toString)) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader(TokenServiceImpl.swagger.asHeader) ~>
      route ~>
      check {
        status shouldBe StatusCodes.Forbidden
      }
    verify(draftsManager).publishDraft(eq(Cars), eq(user), eq(offer.id), eq(None), ?, ?, ?, eq(None))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("delete draft") {
    val offerId1 = OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next
    when(draftsManager.deleteDraft(?, ?, ?)(?)).thenReturnF(ManagerUtils.SuccessResponse)
    // шлем с uid
    checkSuccessRequest(
      Delete(s"/1.0/user/draft/cars/$offerId1")
        .withHeaders(RawHeader("x-uid", user.uid.toString))
    )
    verify(draftsManager).deleteDraft(eq(Cars), eq(user), eq(offerId1))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  test("publish draft without x-uid") {
    val offerId1 = OfferIDGen.next

    when(passportManager.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)

    // шлем без uid
    Post(s"/1.0/user/draft/cars/$offerId1/publish") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader(TokenServiceImpl.swagger.asHeader) ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.Unauthorized
          response should matchJson("""{
              |  "error": "NO_AUTH",
              |  "status": "ERROR",
              |  "detailed_error": "Need authentication"
              |}""".stripMargin)
          verify(passportManager).createAnonymousSession()(?)
          verify(passportManager).getSessionFromUserTicket()(?)
        }
      }
  }

  test("respond with 400 Bad Request on unknown category (create offer)") {
    val user: AutoruUser = ModelGenerators.PrivateUserRefGen.next
    val offer = ModelGenerators.offerGen(user).next

    unknownCategory(Post(s"/1.0/user/draft/cars4", offer), user)
  }

  test("respond with 400 Bad Request on unknown category (update offer)") {
    val user: AutoruUser = ModelGenerators.PrivateUserRefGen.next
    val offer = ModelGenerators.offerGen(user).next

    unknownCategory(Put(s"/1.0/user/draft/cars4/${offer.getId}", offer), user)
  }

  test(s"Read same offer (selector = ${CategorySelector.Cars})") {
    val selector = CategorySelector.Cars
    val offer = ModelGenerators.PrivateOfferGen.next

    val offerId = offer.id
    val user = offer.privateUserRef

    when(draftsManager.getSameOffer(?, ?, ?)(?)).thenReturn(responseForOption(offer))

    Get(s"/1.0/user/draft/$selector/$offerId/same") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
        }

        verify(draftsManager).getSameOffer(eq(user), eq(selector), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test(s"Read same offer (selector = ${CategorySelector.Cars}) when offer doesn't exists") {
    val offer = ModelGenerators.PrivateOfferGen.next
    val selector = CategorySelector.Cars

    val offerId = offer.id
    val user = offer.privateUserRef

    when(draftsManager.getSameOffer(?, ?, ?)(?))
      .thenReturnF(None)

    Get(s"/1.0/user/draft/$selector/$offerId/same") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.NoContent
          contentType shouldBe ContentTypes.NoContentType
        }

        verify(draftsManager).getSameOffer(eq(user), eq(selector), eq(offerId))(?)
        verify(passportManager).getClientId(eq(user))(?)

      }
  }

  test(s"Create draft from garage card") {
    val offer1 = OfferGen.next
    when(draftsManager.createDraftFromGarageCard(?, ?, ?, ?, ?)(?)).thenReturn(responseFor(offer1))

    val user = ModelGenerators.PrivateUserRefGen.next
    val offer = ApiOfferModel.Offer.newBuilder().build()

    checkSuccessRequest(
      Post(s"/1.0/user/draft/cars/garage/garage-id", offer).withHeaders(RawHeader("x-uid", user.uid.toString)),
      expectedOfferId = offer1.id
    )
    verify(draftsManager).createDraftFromGarageCard(eq(Cars), eq(user), eq("garage-id"), eq(true), eq(true))(?)
    verify(passportManager).getClientId(eq(user))(?)
  }

  private def checkSuccessRequest(req: HttpRequest): Unit = {
    req ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader(TokenServiceImpl.swagger.asHeader) ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
          val responseStatus = Protobuf.fromJson[SuccessResponse](response).getStatus
          responseStatus shouldBe ResponseStatus.SUCCESS
        }
      }
  }

  private def checkSuccessRequest(req: HttpRequest, expectedOfferId: OfferID): Unit = {
    req ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader(TokenServiceImpl.swagger.asHeader) ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
          val offerId2 = Protobuf.fromJson[OffersSaveSuccessResponse](response).getOfferId
          offerId2 shouldBe expectedOfferId.toPlain
        }
      }
  }

  private def unknownCategory(req: HttpRequest, user: AutoruUser): Unit = {
    req ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader(TokenServiceImpl.swagger.asHeader) ~>
      route ~>
      check {
        val response: String = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.BadRequest
          val fields = Json.parse(response).as[JsObject].value
          fields("error").as[String] shouldBe "BAD_REQUEST"
          fields("status").as[String] shouldBe "ERROR"
          fields("detailed_error").as[String] should endWith(
            "] Unknown category selector: [cars4]. Known values: cars, moto, trucks, all"
          )
          verifyNoMoreInteractions(vosClient)
          verify(passportManager).getClientId(eq(user))(?)
        }
      }
  }
}
