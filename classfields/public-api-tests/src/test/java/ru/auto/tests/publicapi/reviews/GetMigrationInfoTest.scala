package ru.auto.tests.publicapi.reviews

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.{Issue, Owner}
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN}
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
import ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter
import scala.util.Random

@DisplayName("GET /reviews/{subject}/migration/{oldId}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetMigrationInfoTest {

  private val OLD_REVIEW_ID = 1111

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.reviews.getMigrationInfo
      .subjectPath(AUTO)
      .oldIdPath(OLD_REVIEW_ID)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee400WithInvalidSubject(): Unit = {
    api.reviews.getMigrationInfo.reqSpec(defaultSpec)
      .subjectPath(getRandomString)
      .oldIdPath(OLD_REVIEW_ID)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Ignore("AUTORUBACK-283")
  @Issue("AUTORUBACK-283")
  def shouldSee400WithInvalidOldReviewId(): Unit = {
    api.reviews.getMigrationInfo.reqSpec(defaultSpec)
      .subjectPath(AUTO)
      .oldIdPath(Random.nextInt(10))
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldGetMigrationInfoHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) => apiClient.reviews.getMigrationInfo
      .reqSpec(defaultSpec)
      .oldIdPath(OLD_REVIEW_ID)
      .subjectPath(AUTO)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
