package ru.auto.tests.publicapi.comments

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import io.qameta.allure.{Issue, Owner}
import io.restassured.mapper.ObjectMapperType.GSON
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NOT_FOUND}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Ignore, Rule, Test}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_PARAMS_DETAILS, BAD_REQUEST}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.model.{AutoApiAddCommentRequest, AutoApiErrorResponse}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /comments/{topicGroup}/{topicId}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreateCommentNegativeTest {

  private val TOPIC_GROUP = "review"
  private val TOPIC_ID = "8498572144589540098"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.comments.addComment()
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .body(new AutoApiAddCommentRequest().message(getRandomString))
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  @Ignore("AUTORUBACK-519")
  @Issue("AUTORUBACK-519")
  def shouldSee404WithInvalidTopicGroup(): Unit = {
    api.comments.addComment().reqSpec(defaultSpec)
      .topicGroupPath(getRandomString)
      .topicIdPath(TOPIC_ID)
      .body(new AutoApiAddCommentRequest().message(getRandomString))
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WhenNotAuth(): Unit = {
    val response = api.comments.addComment().reqSpec(defaultSpec)
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .body(new AutoApiAddCommentRequest().message(getRandomString))
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse], GSON)

    assertThat(response).hasStatus(ERROR).hasError(BAD_PARAMS_DETAILS)
      .hasDetailedError("Вы не авторизованы! Пожалуйста, авторизуйтесь.")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutBody(): Unit = {
    val response = api.comments.addComment().reqSpec(defaultSpec)
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse], GSON)

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError)
      .contains("The request content was malformed:\nExpect message object but got: null")
  }
}
