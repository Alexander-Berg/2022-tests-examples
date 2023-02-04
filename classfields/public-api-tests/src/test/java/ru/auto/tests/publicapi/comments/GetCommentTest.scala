package ru.auto.tests.publicapi.comments

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.{Issue, Owner}
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NOT_FOUND}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Ignore, Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /comments/{topicGroup}/{topicId}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetCommentTest {

  private val TOPIC_GROUP = "review"
  private val TOPIC_ID = "8498572144589540098"
  private val INVALID_TOPIC_ID = "-1"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.comments.getComments
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  @Ignore("AUTORUBACK-519")
  @Issue("AUTORUBACK-519")
  def shouldSee404WithInvalidTopicGroup(): Unit = {
    api.comments.getComments.reqSpec(defaultSpec)
      .topicGroupPath(getRandomString)
      .topicIdPath(TOPIC_ID)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidTopicID(): Unit = {
    api.comments.getComments.reqSpec(defaultSpec)
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(INVALID_TOPIC_ID)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldGetMigrationInfoHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) => apiClient.comments.getComments
      .reqSpec(defaultSpec)
      .topicGroupPath(TOPIC_GROUP)
      .topicIdPath(TOPIC_ID)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
