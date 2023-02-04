package ru.auto.tests.publicapi.lenta

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.model.{AutoLentaContentReadState, AutoLentaReadRequest}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import java.util
import scala.annotation.meta.getter

@DisplayName("POST lenta/mark-read")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class PostMarkReadTest {

  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {

    val userId = "user:17"

    val previewedState = new AutoLentaContentReadState()
      .contentId("magazine_123")
      .wasSeenPreview(true)
    val readState = new AutoLentaContentReadState()
      .contentId("review_456")
      .wasSeenPreview(true)
      .wasRead(true)
    val notifiedState = new AutoLentaContentReadState()
      .contentId("magazine_789")
      .wasSentNotification(true)
      .wasRead(true)

    val readRequest = new AutoLentaReadRequest().contentReadState(
      util.Arrays.asList(
        previewedState,
        readState,
        notifiedState
      )
    )

    val req = (apiClient: ApiClient) =>
      apiClient
        .lenta()
        .markReadLenta()
        .body(readRequest)
        .reqSpec(defaultSpec)
        .userIdQuery(userId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    val readState = new AutoLentaContentReadState()
      .contentId("review_456")
      .wasSeenPreview(true)
      .wasRead(true)
    val readRequest = new AutoLentaReadRequest().contentReadState(util.Arrays.asList(readState))

    api.lenta().markReadLenta()
      .body(readRequest)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee400WithoutBody(): Unit = {
    api.lenta().markReadLenta()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  def shouldSee400OnInvalidUserId(): Unit = {
    val userId = "invalid_user:123"
    val readState = new AutoLentaContentReadState()
      .contentId("review_456")
      .wasSeenPreview(true)
      .wasRead(true)
    val readRequest = new AutoLentaReadRequest().contentReadState(util.Arrays.asList(readState))

    api.lenta().markReadLenta()
      .reqSpec(defaultSpec)
      .body(readRequest)
      .userIdQuery(userId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }
}
