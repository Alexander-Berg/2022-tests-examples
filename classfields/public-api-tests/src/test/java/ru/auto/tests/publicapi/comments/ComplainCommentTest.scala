package ru.auto.tests.publicapi.comments

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import io.qameta.allure.{Issue, Owner}
import io.restassured.mapper.ObjectMapperType.GSON
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NOT_FOUND, SC_UNAUTHORIZED}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Ignore, Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_PARAMS_DETAILS, BAD_REQUEST, SESSION_NOT_FOUND}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /comments/{topicGroup}/{topicId}/{commentId}/complain")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class ComplainCommentTest {

  private val TOPIC_GROUP = "review"
  private val TOPIC_ID = "8498572144589540098"
  private val COMMENT_ID = 5596740
  private val INVALID_COMMENT_ID = 0

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldAddNewComment(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    api.comments.complainComment().reqSpec(defaultSpec)
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .commentIdPath(COMMENT_ID)
      .messageQuery(getRandomString)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))
  }


  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.comments.complainComment()
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .commentIdPath(COMMENT_ID)
      .messageQuery(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  @Ignore("AUTORUBACK-519")
  @Issue("AUTORUBACK-519")
  def shouldSee404WithInvalidTopicGroup(): Unit = {
    api.comments.complainComment().reqSpec(defaultSpec)
      .topicGroupPath(getRandomString)
      .topicIdPath(TOPIC_ID)
      .commentIdPath(INVALID_COMMENT_ID)
      .messageQuery(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee401WhenNotAuth(): Unit = {
    val response = api.comments.complainComment().reqSpec(defaultSpec)
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .commentIdPath(COMMENT_ID)
      .messageQuery(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse], GSON)

    assertThat(response).hasStatus(ERROR).hasError(SESSION_NOT_FOUND)
      .hasDetailedError(SESSION_NOT_FOUND.getValue)
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidCommentId(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val response = api.comments.complainComment().reqSpec(defaultSpec)
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .commentIdPath(INVALID_COMMENT_ID)
      .messageQuery(getRandomString)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse], GSON)

    assertThat(response).hasStatus(ERROR).hasError(BAD_PARAMS_DETAILS)
      .hasDetailedError("Bad parameters given to internal method")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutMessage(): Unit = {
    val response = api.comments.complainComment().reqSpec(defaultSpec)
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .commentIdPath(COMMENT_ID)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse], GSON)

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError).contains("Request is missing required query parameter 'message'")
  }

}
