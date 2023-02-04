package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.hamcrest.{Matcher, MatcherAssert}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.util.Utils.getResourceAsString
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.CARFAX
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.{defaultSpec, withJsonBody}
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter

@DisplayName("PUT /garage/user/card/{cardId}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class UpdateCardTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val am: AccountManager = null

  @Inject private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val createJson = getResourceAsString("garage/create_card_request.json")
    val updateJson = getResourceAsString("garage/update_card_request.json")

    val updateCard = (api: ApiClient, cardId: String) =>
      api
        .garage()
        .updateCardGarage()
        .xSessionIdHeader(session.getId)
        .cardIdPath(cardId)
        .reqSpec(defaultSpec())
        .reqSpec(withJsonBody(updateJson))
        .execute(validatedWith(shouldBeSuccess()))
        .as(classOf[JsonObject])


    val cardId = adaptor.createGarageCard(createJson, session.getId).getCard.getId
    val actual = updateCard(api, cardId)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(cardId, session.getId)

    val prodCardId = adaptor.createGarageCard(createJson, session.getId).getCard.getId
    val equalsExpected: Matcher[JsonObject] =
      jsonEquals(updateCard(prodApi, prodCardId)).whenIgnoringPaths(GarageTestUtils.ignoredPaths: _*)
    adaptor.deleteGarageCardAndWaitRecallsDeleted(prodCardId, session.getId)

    MatcherAssert.assertThat(actual, equalsExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetNotFoundError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val bodyJson = getResourceAsString("garage/update_card_request.json")

    // Чтобы юзер появился в базе гаража
    val card = adaptor.createGarageCard(getResourceAsString("garage/create_card_request.json"), session.getId)

    val updateCard: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .updateCardGarage()
        .xSessionIdHeader(session.getId)
        .cardIdPath(123)
        .reqSpec(defaultSpec())
        .reqSpec(withJsonBody(bodyJson))
        .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
        .as(classOf[JsonObject])

    val actual: JsonObject = updateCard(api)
    val matchExpected: Matcher[JsonObject] = jsonEquals(updateCard(prodApi))

    adaptor.deleteGarageCardAndWaitRecallsDeleted(card.getCard.getId, session.getId)

    MatcherAssert.assertThat(actual, matchExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetValidationError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession
    val validCardJson = getResourceAsString("garage/create_card_request.json")
    val invalidCardJson = getResourceAsString("garage/invalid_create_card_request.json")

    val card = adaptor.createGarageCard(validCardJson, session.getId)

    val updateCard = (api: ApiClient) =>
      api
        .garage()
        .updateCardGarage()
        .xSessionIdHeader(session.getId)
        .cardIdPath(card.getCard.getId)
        .reqSpec(defaultSpec())
        .reqSpec(withJsonBody(invalidCardJson))
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
        .as(classOf[JsonObject])

    val actual = updateCard(api)
    val equalsExpected: Matcher[JsonObject] = jsonEquals(updateCard(prodApi))

    adaptor.deleteGarageCardAndWaitRecallsDeleted(card.getCard.getId, session.getId)

    MatcherAssert.assertThat(actual, equalsExpected)
  }
}
