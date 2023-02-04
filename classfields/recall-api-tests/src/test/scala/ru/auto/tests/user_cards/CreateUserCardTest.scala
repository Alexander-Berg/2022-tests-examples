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
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.module.RecallApiModule
import ru.auto.tests.recall.ApiClient
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("PUT /user-cards")
@GuiceModules(Array(classOf[RecallApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreateUserCardTest {

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
      .addUserCard()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldCreateUserCard(): Unit = {
    val userId = getRandomString

    api.userCards
      .addUserCard()
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .hasEmailQuery(true)
      .xUserIdHeader(userId)
      .execute(validatedWith(shouldBe200OkJSON))

    val userCards = adaptor.getUserCards(userId)
    assertThat(userCards.getCards.get(0)).hasVinCode(VIN)
  }
}
