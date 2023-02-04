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
import ru.auto.tests.adaptor.RecallApiAdaptor
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{
  shouldBe200Ok,
  shouldBe200OkJSON,
  shouldBeCode,
  validatedWith
}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.module.RecallApiModule
import ru.auto.tests.recall.ApiClient
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("PUT /user-cards/{card_id}/subscription")
@GuiceModules(Array(classOf[RecallApiModule]))
@RunWith(classOf[GuiceTestRunner])
class SubscribeUserCardTest {

  private val VIN = "YV1LFA2BCH1124389"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: RecallApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutParams(): Unit = {
    api.userCards
      .subscribe()
      .cardIdPath(getRandomString)
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSubscribeToUserCard(): Unit = {
    val userId = getRandomString
    val userCardId = adaptor.createUserCard(VIN, userId).getCard.getCardId

    api.userCards
      .subscribe()
      .reqSpec(defaultSpec)
      .cardIdPath(userCardId)
      .xUserIdHeader(userId)
      .execute(validatedWith(shouldBe200Ok))

    val userCards = adaptor.getUserCards(userId)
    assertThat(userCards.getCards.get(0)).hasIsSubscribed(true)
  }
}
