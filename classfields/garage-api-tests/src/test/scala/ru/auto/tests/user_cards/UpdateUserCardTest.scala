package ru.auto.tests.user_cards

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.assertj.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.Base
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.{getRandomShortInt, getRandomShortLong, getRandomString}
import ru.auto.tests.constants.Owners.CARFAX
import ru.auto.tests.garage.ApiClient
import ru.auto.tests.garage.model._
import ru.auto.tests.module.GarageApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import java.time.OffsetDateTime
import scala.annotation.meta.getter

@DisplayName("PUT /user/card/{card-id}")
@GuiceModules(Array(classOf[GarageApiModule]))
@RunWith(classOf[GuiceTestRunner])
class UpdateUserCardTest extends Base {

  private val userId = "qa_user:64617860"
  private val vin = "KMHDN41BP6U366989"
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
      .updateCard()
      .reqSpec(defaultSpec)
      .cardIdPath(getRandomShortLong)
      .xUserIdHeader(userId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(CARFAX)
  def shouldSee404ForUnknownCard(): Unit = {
    api.userCard
      .updateCard()
      .reqSpec(defaultSpec)
      .cardIdPath(-1)
      .xUserIdHeader(userId)
      .body(new AutoApiVinGarageUpdateCardRequest().card(genCard))
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(CARFAX)
  def shouldUpdateCreatedCard(): Unit = {
    val userId = genUser()
    val builtCard = adaptor.buildCardFromVin(userId, vin, "CURRENT_CAR")

    val requestBody = new AutoApiVinGarageCreateCardRequest()
      .addedManually(false)
      .addedByIdentifier(AutoApiVinGarageCreateCardRequest.AddedByIdentifierEnum.VIN)
      .card(builtCard.getCard)

    val createResponse = adaptor.createCard(userId, requestBody)
    val card = createResponse.getCard
    val randomMileage = getRandomShortInt
    card.getVehicleInfo.state(
      new AutoApiVinGarageVehicleState()
        .mileage(new AutoApiVinGarageVehicleMileage().value(randomMileage).date(OffsetDateTime.now()))
    )
    val updateRequest = new AutoApiVinGarageUpdateCardRequest().card(card)

    val response = api.userCard
      .updateCard()
      .reqSpec(defaultSpec)
      .cardIdPath(createResponse.getCard.getId)
      .body(updateRequest)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getCard)
      .hasStatus(AutoApiVinGarageCard.StatusEnum.ACTIVE)
    assertThat(response.getCard.getVehicleInfo.getState.getMileage)
      .hasValue(randomMileage)

  }

  @Test
  @Owner(CARFAX)
  def shouldNotUpdateDeletedCard(): Unit = {
    val userId = genUser()
    val builtCard = adaptor.buildCardFromVin(userId, vin, "CURRENT_CAR")

    val requestBody = new AutoApiVinGarageCreateCardRequest()
      .addedManually(false)
      .addedByIdentifier(AutoApiVinGarageCreateCardRequest.AddedByIdentifierEnum.VIN)
      .card(builtCard.getCard)

    val createResponse = adaptor.createCard(userId, requestBody)
    val card = createResponse.getCard
    val updateRequest = new AutoApiVinGarageUpdateCardRequest().card(card)

    adaptor.deleteCard(card.getId.toLong, userId)

    val response = api.userCard
      .updateCard()
      .reqSpec(defaultSpec)
      .cardIdPath(card.getId)
      .body(updateRequest)
      .xUserIdHeader(userId)
      .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

    assertThat(response)
      .hasStatus(AutoApiVinGarageUpdateCardResponse.StatusEnum.ERROR)
      .hasError(AutoApiVinGarageUpdateCardResponse.ErrorEnum.BAD_REQUEST)

  }

  @Test
  @Owner(CARFAX)
  def shouldSee404ForInvalidCardId(): Unit = {
    api.userCard
      .updateCard()
      .reqSpec(defaultSpec)
      .cardIdPath(getRandomString)
      .xUserIdHeader(userId)
      .body(new AutoApiVinGarageUpdateCardRequest().card(genCard))
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(CARFAX)
  def shouldSee400ForInvalidUserId(): Unit = {
    api.userCard
      .updateCard()
      .reqSpec(defaultSpec)
      .cardIdPath(1)
      .xUserIdHeader(getRandomString)
      .body(new AutoApiVinGarageUpdateCardRequest().card(genCard))
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  private def genCard = {
    val documents = new AutoApiDocuments().vin(vin)
    val vehicleInfo = new AutoApiVinGarageVehicle()
      .carInfo(new AutoApiCarInfo().mark(mark).model(model))
      .documents(documents)
    new AutoApiVinGarageCard()
      .vehicleInfo(vehicleInfo)
      .userId(userId)
  }

}
