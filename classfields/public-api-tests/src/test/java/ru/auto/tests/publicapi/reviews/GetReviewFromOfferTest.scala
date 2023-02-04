package ru.auto.tests.publicapi.reviews

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS
import ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /reviews/{subject}/{category}/offer/{offerId}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetReviewFromOfferTest {

  private val IGNORING_PATHS = Array(
    "review.id",
    "review.content[*].created_time",
    "review.content[*].content_value[*]" // Because the images uploaded to mds every time
  )

  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.reviews.createReviewFromOffer()
      .subjectPath(AUTO)
      .categoryPath(CARS)
      .offerIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee400WithInvalidSubject(): Unit = {
    api.reviews.createReviewFromOffer().reqSpec(defaultSpec)
      .subjectPath(getRandomString)
      .categoryPath(CARS)
      .offerIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldGetReviewFromOfferHasNoDiffWithProduction(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val offer = adaptor.createOffer(account.getLogin, sessionId, CARS)

    val req = (apiClient: ApiClient) => apiClient.reviews.createReviewFromOffer()
      .reqSpec(defaultSpec)
      .subjectPath(AUTO)
      .categoryPath(CARS)
      .offerIdPath(offer.getOfferId)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonObject](req.apply(prodApi)).whenIgnoringPaths(IGNORING_PATHS: _*))
  }
}
