package ru.auto.tests.user_cards

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.assertj.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.Base
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Owners.CARFAX
import ru.auto.tests.garage.ApiClient
import ru.auto.tests.garage.model._
import ru.auto.tests.module.GarageApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /user/card")
@GuiceModules(Array(classOf[GarageApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreateCardTest extends Base {

  private val vin = "KMHDN41BP6U366989"
  private val secondVin = "XW7BF4FK10S105523"
  private val mark = "TOYOTA"
  private val model = "CAMRY"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(CARFAX)
  def shouldSee400WithEmptyBody(): Unit = {
    api.userCard
      .createCard()
      .reqSpec(defaultSpec)
      .xUserIdHeader(getRandomString)
      .body(new AutoApiVinGarageCreateCardRequest)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(CARFAX)
  def shouldCreateCardManually(): Unit = {
    val userId = genUser()

    val requestBody =
      genCardRequest(userId, new AutoApiCarInfo().mark(mark).model(model))
    val createResponse = adaptor.createCard(userId, requestBody)

    val response = adaptor.getUserCard(createResponse.getCard.getId)
    assertThat(createResponse.getCard.getVehicleInfo.getDocuments)
      .hasVin(response.getCard.getVehicleInfo.getDocuments.getVin)
    assertThat(createResponse.getCard)
      .hasUserId(userId)
      .hasStatus(AutoApiVinGarageCard.StatusEnum.ACTIVE)
    assertThat(response.getCard.getSourceInfo)
      .hasManuallyAdded(true)
    assertThat(response.getCard.getVehicleInfo.getCarInfo)
      .hasMark(mark)
      .hasModel(model)
  }

  @Test
  @Owner(CARFAX)
  def shouldCreateCardByVin(): Unit = {
    val userId = genUser()
    val builtCard = adaptor.buildCardFromVin(userId, vin, "CURRENT_CAR")

    val requestBody = new AutoApiVinGarageCreateCardRequest()
      .addedManually(false)
      .addedByIdentifier(AutoApiVinGarageCreateCardRequest.AddedByIdentifierEnum.VIN)
      .card(builtCard.getCard)

    val createResponse = adaptor.createCard(userId, requestBody)
    val response = adaptor.getUserCard(createResponse.getCard.getId)
    assertThat(response.getCard.getSourceInfo)
      .hasAddedByIdentifier(AutoApiVinGarageSourceInfo.AddedByIdentifierEnum.VIN)
    assertThat(createResponse.getCard)
      .hasUserId(userId)
      .hasStatus(AutoApiVinGarageCard.StatusEnum.ACTIVE)
  }

  @Test
  @Owner(CARFAX)
  def shouldThrowValidationComplectationError(): Unit = {
    val userId = genUser()

    val requestBody =
      genCardRequest(userId, new AutoApiCarInfo().mark(mark).model(model).complectationId(100))

    val result = api.userCard
      .createCard()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

    assertThat(result)
      .hasStatus(AutoApiVinGarageCreateCardResponse.StatusEnum.ERROR)
      .hasOnlyValidationResults(
        new AutoApiVinGarageValidationResult()
          .description("Невозможно задать такую комплектацию для данного автомобиля")
          .field(AutoApiVinGarageValidationResult.FieldEnum.COMPLECTATION_ID)
      )
      .hasError(AutoApiVinGarageCreateCardResponse.ErrorEnum.VALIDATION_ERROR)
  }

  @Test
  @Owner(CARFAX)
  def shouldThrowValidationTechParamError(): Unit = {
    val userId = genUser()

    val requestBody =
      genCardRequest(userId, new AutoApiCarInfo().mark(mark).model(model).techParamId(100))

    val result = api.userCard
      .createCard()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

    assertThat(result)
      .hasStatus(AutoApiVinGarageCreateCardResponse.StatusEnum.ERROR)
      .hasOnlyValidationResults(
        new AutoApiVinGarageValidationResult()
          .description("Невозможно задать такую модификацию для данного автомобиля")
          .field(AutoApiVinGarageValidationResult.FieldEnum.TECH_PARAM_ID)
      )
      .hasError(AutoApiVinGarageCreateCardResponse.ErrorEnum.VALIDATION_ERROR)
  }

  @Test
  @Owner(CARFAX)
  def shouldThrowValidationConfigurationError(): Unit = {
    val userId = genUser()

    val requestBody =
      genCardRequest(userId, new AutoApiCarInfo().mark(mark).model(model).configurationId(100))

    val result = api.userCard
      .createCard()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

    assertThat(result)
      .hasStatus(AutoApiVinGarageCreateCardResponse.StatusEnum.ERROR)
      .hasOnlyValidationResults(
        new AutoApiVinGarageValidationResult()
          .description("Невозможно задать такую конфигурацию для данного автомобиля")
          .field(AutoApiVinGarageValidationResult.FieldEnum.CONFIGURATION_ID)
      )
      .hasError(AutoApiVinGarageCreateCardResponse.ErrorEnum.VALIDATION_ERROR)
  }

  @Test
  @Owner(CARFAX)
  def shouldThrowValidationSuperGenError(): Unit = {
    val userId = genUser()

    val requestBody =
      genCardRequest(userId, new AutoApiCarInfo().mark(mark).model(model).superGenId(100))

    val result = api.userCard
      .createCard()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

    assertThat(result)
      .hasStatus(AutoApiVinGarageCreateCardResponse.StatusEnum.ERROR)
      .hasOnlyValidationResults(
        new AutoApiVinGarageValidationResult()
          .description("Невозможно задать такое поколение для данного автомобиля")
          .field(AutoApiVinGarageValidationResult.FieldEnum.SUPER_GEN_ID)
      )
      .hasError(AutoApiVinGarageCreateCardResponse.ErrorEnum.VALIDATION_ERROR)
  }

  @Test
  @Owner(CARFAX)
  def shouldThrowValidationModelError(): Unit = {
    val userId = genUser()

    val requestBody =
      genCardRequest(userId, new AutoApiCarInfo().mark(mark).model("R8"))

    val result = api.userCard
      .createCard()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

    assertThat(result)
      .hasStatus(AutoApiVinGarageCreateCardResponse.StatusEnum.ERROR)
      .hasOnlyValidationResults(
        new AutoApiVinGarageValidationResult()
          .description("Невозможно задать такую модель для данного автомобиля")
          .field(AutoApiVinGarageValidationResult.FieldEnum.MODEL)
      )
      .hasError(AutoApiVinGarageCreateCardResponse.ErrorEnum.VALIDATION_ERROR)
  }

  @Test
  @Owner(CARFAX)
  def shouldSee400WithInvalidUser(): Unit = {
    api.userCard
      .createCard()
      .reqSpec(defaultSpec)
      .body(genCardRequest(getRandomString, new AutoApiCarInfo().mark(mark).model(model)))
      .xUserIdHeader(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(CARFAX)
  def shouldSee400ForUnknownIdentifier(): Unit = {
    val userId = genUser()
    val documents = new AutoApiDocuments().vin(secondVin)
    val vehicleInfo = new AutoApiVinGarageVehicle().documents(documents)
    val card = new AutoApiVinGarageCard()
      .vehicleInfo(vehicleInfo)
      .userId(userId)
    val requestBody = new AutoApiVinGarageCreateCardRequest()
      .addedManually(false)
      .card(card)

    api.userCard
      .createCard()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

  }

  private def genCardRequest(userId: String, carInfo: AutoApiCarInfo) = {
    val documents = new AutoApiDocuments().vin(secondVin)
    val vehicleInfo = new AutoApiVinGarageVehicle().carInfo(carInfo).documents(documents)
    val card = new AutoApiVinGarageCard()
      .vehicleInfo(vehicleInfo)
      .userId(userId)
    val requestBody = new AutoApiVinGarageCreateCardRequest()
      .addedManually(true)
      .card(card)
    requestBody
  }
}
