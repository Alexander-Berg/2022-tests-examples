package ru.auto.tests.publicapi.chat

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.passport.account.Account
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.RAIV
import ru.auto.tests.publicapi.model.AutoApiChatChatEvent
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /chat/event")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetChatEventTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  private val account: Account = null

  @Test
  @Owner(RAIV)
  def shouldSee403WhenNoAuth(): Unit = {
    api.chat
      .getEvent()
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(RAIV)
  def shouldNotFailWithAuth(): Unit = {

    val loginResponse = adaptor.login(account)
    val session = loginResponse.getSession
    val sessionId = session.getId

    val response = api.chat
      .getEvent()
      .xSessionIdHeader(sessionId)
      .reqSpec(defaultSpec())
      .execute(validatedWith(shouldBe200OkJSON()))
      .as(classOf[AutoApiChatChatEvent])

    assertThat(response).isNotNull

  }

}
