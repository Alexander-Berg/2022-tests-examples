package ru.auto.tests.publicapi.lenta

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
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

import java.util
import scala.annotation.meta.getter

@DisplayName("GET lenta/subscription")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetSubscriptionTest {

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
  def getSubscriptionWhenSettingsNotSet(): Unit = {
    api
      .lenta()
      .getSubscriptionLenta
      .reqSpec(defaultSpec)
      .userIdQuery("user:1234567890")
      .executeAs(validatedWith(shouldBeCode(404)))
  }

  @Test
  def getSubscriptionWithWrongUserId(): Unit = {
    api
      .lenta()
      .getSubscriptionLenta
      .reqSpec(defaultSpec)
      .userIdQuery("user:wrong")
      .executeAs(validatedWith(shouldBeCode(400)))
  }

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {
    val userId = "user:10"
    adaptor.deleteLentaSubscriptions(userId)

    val tags = util.Arrays.asList("NISSAN", "NISSAN#MICRA", "NISSAN#MICRA#7758163")
    setSubscriptionTags(userId, tags)

    val req = (apiClient: ApiClient) =>
      apiClient
        .lenta()
        .getSubscriptionLenta()
        .reqSpec(defaultSpec)
        .userIdQuery(userId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }

  private def setSubscriptionTags(userId: String, tagsToAdd: util.List[String]) = {
    val addedTagsSubscription =
      new AutoLentaSubscription().includeTags(tagsToAdd)
    adaptor.addLentaSubscriptionTags(userId, addedTagsSubscription)
  }
}
