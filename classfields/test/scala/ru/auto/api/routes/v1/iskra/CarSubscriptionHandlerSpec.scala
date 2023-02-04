package ru.auto.api.routes.v1.iskra

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Accept
import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.managers.iskra.IskraManager
import ru.auto.api.model.AutoruUser
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.MockedClients
import ru.auto.iskra.proto.ApiModel.SubscriptionOptionalPersonalData.{Home, HomeOwnStatus, Work}
import ru.auto.iskra.proto.ApiModel.{CarSubscription, CarSubscriptionCreateFields, CarSubscriptionUpdateFields, CatalogIdentity, Document, DocumentType, DocumentVerificationStatus, SubscriptionDuration, SubscriptionMainPersonalData, SubscriptionOptionalPersonalData, SubscriptionStatus}
import ru.auto.iskra.proto.SubscriptionService.{CreateSubscriptionResponse, GetSubscriptionResponse, UpdateSubscriptionResponse}

import scala.jdk.CollectionConverters._

class CarSubscriptionHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {

  import CarSubscriptionHandlerSpec._

  override lazy val iskraManager: IskraManager = mock[IskraManager]

  when(iskraManager.getCarSubscription(?)(?)).thenReturnF(GetResponse)
  when(iskraManager.createCarSubscription(?, ?)(?)).thenReturnF(CreateResponse)
  when(iskraManager.updateCarSubscription(?, ?)(?)).thenReturnF(UpdateResponse)

  "CarSubscriptionHandler" should {
    "get subscription successfully" in {
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentialsGen.next)
      Get("/1.0/car-subscription") ~>
        xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-uid", Uid) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(iskraManager).getCarSubscription(eq(User))(?)
          responseAs[GetSubscriptionResponse] shouldBe GetResponse
        }
    }

    "create subscription successfully" in {
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentialsGen.next)
      Post("/1.0/car-subscription", CreateRequest) ~>
        xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-uid", Uid) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(iskraManager).createCarSubscription(eq(User), eq(CreateRequest))(?)
          responseAs[CreateSubscriptionResponse] shouldBe CreateResponse
        }
    }

    "update subscription successfully" in {
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(UserEssentialsGen.next)
      Put("/1.0/car-subscription", UpdateRequest) ~>
        xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        addHeader("x-uid", Uid) ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          verify(iskraManager).updateCarSubscription(eq(User), eq(UpdateRequest))(?)
          responseAs[UpdateSubscriptionResponse] shouldBe UpdateResponse
        }
    }

    "get subscription fails for anonymous user" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      Get("/1.0/car-subscription") ~>
        xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        route ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }
    }

    "create subscription fails for anonymous user" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      Post("/1.0/car-subscription", CreateRequest) ~>
        xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        route ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }
    }

    "update subscription fails for anonymous user" in {
      when(passportClient.createAnonymousSession()(?)).thenReturnF(SessionResultGen.next)
      Put("/1.0/car-subscription", UpdateRequest) ~>
        xAuthorizationHeader ~>
        addHeader(Accept(`application/json`)) ~>
        route ~>
        check {
          status shouldBe StatusCodes.Unauthorized
        }
    }
  }
}

object CarSubscriptionHandlerSpec {

  private val Uid: String = "12345678"
  private val User: AutoruUser = AutoruUser(Uid.toLong)

  private val CatalogIdentityInstance: CatalogIdentity =
    CatalogIdentity
      .newBuilder()
      .setMarkCode("Mark code")
      .setModelCode("Model code")
      .setGenerationId("Generation id")
      .setConfigurationId("Configuration id")
      .setTechParamId("Tech param id")
      .setComplectationId("Complectation id")
      .build()

  private val MainPersonalData: SubscriptionMainPersonalData =
    SubscriptionMainPersonalData
      .newBuilder()
      .setName("Name")
      .setEmail("some@yandex.ru")
      .setPhone("+79001234567")
      .build()

  private val OptionalPersonalData: SubscriptionOptionalPersonalData =
    SubscriptionOptionalPersonalData
      .newBuilder()
      .setWork {
        Work
          .newBuilder()
          .setAddress("Address")
          .setPlace("Company")
          .setExperienceInMonths(36)
      }
      .setHome {
        Home
          .newBuilder()
          .setAddress("Address")
          .setOwnStatus(HomeOwnStatus.MORTGAGE)
      }
      .setMonthlyIncomeInRubles(100000)
      .setRelativesPhone("+79001234567")
      .build()

  private val DocumentsList: List[Document] = List(
    Document
      .newBuilder()
      .setType(DocumentType.PASSPORT_PERSONAL_DATA)
      .setStatus(DocumentVerificationStatus.DVS_VERIFIED)
      .build()
  )

  private val CarSubscriptionInstance: CarSubscription =
    CarSubscription
      .newBuilder()
      .setCarBrand("BMW")
      .setCatalogIdentity(CatalogIdentityInstance)
      .setDuration(SubscriptionDuration.SD_12_MONTHS)
      .setStatus(SubscriptionStatus.SBS_ON_REVIEW)
      .setMainPersonalData(MainPersonalData)
      .setOptionalPersonalData(OptionalPersonalData)
      .build()

  private val CreateRequest: CarSubscriptionCreateFields =
    CarSubscriptionCreateFields
      .newBuilder()
      .setCarBrand("BMW")
      .setCatalogIdentity(CatalogIdentityInstance)
      .setDuration(SubscriptionDuration.SD_12_MONTHS)
      .setMainPersonalData(MainPersonalData)
      .build()

  private val UpdateRequest: CarSubscriptionUpdateFields =
    CarSubscriptionUpdateFields
      .newBuilder()
      .setCarBrand("BMW")
      .setCatalogIdentity(CatalogIdentityInstance)
      .setDuration(SubscriptionDuration.SD_12_MONTHS)
      .setMainPersonalData(MainPersonalData)
      .setOptionalPersonalData(OptionalPersonalData)
      .build()

  private val GetResponse: GetSubscriptionResponse =
    GetSubscriptionResponse
      .newBuilder()
      .setSubscription(CarSubscriptionInstance)
      .addAllDocuments(DocumentsList.asJava)
      .build()

  private val CreateResponse: CreateSubscriptionResponse =
    CreateSubscriptionResponse
      .newBuilder()
      .setSubscription(CarSubscriptionInstance)
      .addAllDocuments(DocumentsList.asJava)
      .build()

  private val UpdateResponse: UpdateSubscriptionResponse =
    UpdateSubscriptionResponse
      .newBuilder()
      .setSubscription(CarSubscriptionInstance)
      .addAllDocuments(DocumentsList.asJava)
      .build()
}
