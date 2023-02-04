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
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.model.AutoLentaSubscription
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

@DisplayName("POST lenta/subscription")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class PostSubscriptionTagsTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {
    val userId = "user:12"
    adaptor.deleteLentaSubscriptions(userId)

    val subscription =
      new AutoLentaSubscription().includeTags(List("NISSAN", "NISSAN#MICRA", "NISSAN#MICRA#7758163").asJava)

    val req = (apiClient: ApiClient) =>
      apiClient
        .lenta()
        .setSubscriptionLenta()
        .reqSpec(defaultSpec)
        .body(subscription)
        .userIdQuery(userId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    val subscription = new AutoLentaSubscription().includeTags(List("NISSAN").asJava)
    api.lenta().setSubscriptionLenta()
      .body(subscription)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee400WithoutBody(): Unit = {
    api.lenta().setSubscriptionLenta()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  def shouldSee400OnInvalidUserId(): Unit = {
    val userId = "invalid_user:123"
    val subscription = new AutoLentaSubscription().includeTags(List("NISSAN").asJava)
    api.lenta().setSubscriptionLenta()
      .reqSpec(defaultSpec)
      .body(subscription)
      .userIdQuery(userId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }
}
