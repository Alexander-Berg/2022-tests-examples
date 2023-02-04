package ru.auto.tests.publicapi.feedback

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.{getRandomEmail, getRandomString}
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiFeedbackEmailFeedbackRequest
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeEmptyJson

import scala.annotation.meta.getter

@DisplayName("POST /feedback/send")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
class SendFeedbackTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.feedback.sendEmail().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutBody(): Unit = {
    api.feedback.sendEmail().reqSpec(defaultSpec).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSuccessSendFeedback(): Unit = {
    val feedback = new AutoApiFeedbackEmailFeedbackRequest().email(getRandomEmail).message(getRandomString)

    api.feedback.sendEmail().reqSpec(defaultSpec)
      .body(feedback)
      .execute(validatedWith(shouldBeEmptyJson))
  }

}
