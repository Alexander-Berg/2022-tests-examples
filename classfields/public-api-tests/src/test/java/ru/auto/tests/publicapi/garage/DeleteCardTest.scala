package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
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
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter

@DisplayName("DELETE /garage/user/card/{cardId}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteCardTest {

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

    val deleteCard = (api: ApiClient) => {
      val cardId = adaptor
        .createGarageCard(getResourceAsString("garage/create_card_request.json"), session.getId).getCard.getId
      adaptor.waitUntilRecallsCardCreatedForGarageCard(cardId, session.getId)
      val card = adaptor.getGarageCard(cardId, session.getId)
      val response = api
        .garage()
        .deleteCardGarage()
        .reqSpec(defaultSpec())
        .xSessionIdHeader(session.getId)
        .cardIdPath(card.getCard.getId)
        .execute(validatedWith(shouldBeSuccess()))
        .as(classOf[JsonObject])
      adaptor.waitUntilRecallsCardDeleted(card.getCard.getRecalls.getCard.getCardId, session.getId)
      response
    }

    val actual = deleteCard(api)
    val equalsExpected: Matcher[JsonObject] = jsonEquals(deleteCard(prodApi))

    MatcherAssert.assertThat(actual, equalsExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetNotFoundError(): Unit = {

    val account = am.create()
    val session = adaptor.login(account).getSession

    // Чтобы юзер появился в базе гаража
    val card = adaptor.createGarageCard(getResourceAsString("garage/create_card_request.json"), session.getId)

    val deleteCard = (api: ApiClient) =>
      api
        .garage()
        .deleteCardGarage()
        .reqSpec(defaultSpec())
        .xSessionIdHeader(session.getId)
        .cardIdPath(123)
        .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
        .as(classOf[JsonObject])

    val actual = deleteCard(api)
    val equalsExpected: Matcher[JsonObject] = jsonEquals(deleteCard(prodApi))

    adaptor.deleteGarageCardAndWaitRecallsDeleted(card.getCard.getId, session.getId)

    MatcherAssert.assertThat(actual, equalsExpected)
  }
}
