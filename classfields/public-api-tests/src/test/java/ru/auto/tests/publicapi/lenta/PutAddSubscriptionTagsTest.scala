package ru.auto.tests.publicapi.lenta

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN}
import org.assertj.core.api.Assertions
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
import ru.auto.tests.publicapi.model.{AutoLentaSubscription, AutoLentaSubscriptionResponse}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

@DisplayName("PUT lenta/add-subscription-tags")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class PutAddSubscriptionTagsTest {

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
  def addSubscriptionTags(): Unit = {
    val userId = "user:13"
    adaptor.deleteLentaSubscriptions(userId)

    val initialTags = List("NISSAN", "NISSAN#MICRA", "NISSAN#MICRA#7758163")
    val createdSubscriptionResponse: AutoLentaSubscriptionResponse = addSubscriptionTags(userId, initialTags)

    Assertions.assertThat(createdSubscriptionResponse.getSubscription.getIncludeTags).hasSize(3)
    Assertions.assertThat(createdSubscriptionResponse.getSubscription.getIncludeTags).containsAll(initialTags.asJava)

    val tagsToAdd = List("FORD", "FORD#FOCUS", "FORD#FOCUS#7306596")
    val updatedSubscriptionResponse: AutoLentaSubscriptionResponse = addSubscriptionTags(userId, tagsToAdd)

    Assertions.assertThat(updatedSubscriptionResponse.getSubscription.getIncludeTags).hasSize(6)
    Assertions.assertThat(updatedSubscriptionResponse.getSubscription.getIncludeTags).containsAll(initialTags.asJava)
    Assertions.assertThat(updatedSubscriptionResponse.getSubscription.getIncludeTags).containsAll(tagsToAdd.asJava)
  }

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {
    val userId = "user:14"
    adaptor.deleteLentaSubscriptions(userId)

    val tags = List("NISSAN", "NISSAN#MICRA", "NISSAN#MICRA#7758163").asJava
    val subscription = new AutoLentaSubscription().includeTags(tags)

    val req = (apiClient: ApiClient) =>
      apiClient
        .lenta()
        .addSubscriptionTagsLenta()
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
    api.lenta().addSubscriptionTagsLenta()
      .body(subscription)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee400WithoutBody(): Unit = {
    api.lenta().addSubscriptionTagsLenta()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  def shouldSee400OnInvalidUserId(): Unit = {
    val userId = "invalid_user:123"
    val subscription = new AutoLentaSubscription().includeTags(List("NISSAN").asJava)
    api.lenta().addSubscriptionTagsLenta()
      .reqSpec(defaultSpec)
      .body(subscription)
      .userIdQuery(userId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  private def addSubscriptionTags(userId: String, tagsToAdd: List[String]) = {
    val addedTagsSubscription =
      new AutoLentaSubscription().includeTags(tagsToAdd.asJava)
    adaptor.addLentaSubscriptionTags(userId, addedTagsSubscription)
  }
}
