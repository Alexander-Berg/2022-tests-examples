package ru.auto.tests.publicapi.lenta

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NO_CONTENT}
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.model.{AutoLentaSubscription, AutoLentaSubscriptionResponse}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import java.util
import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

@DisplayName("DELETE lenta/delete-subscription-tags")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteSubscriptionTagsTest {

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
  def deleteSubscriptionTags(): Unit = {
    val userId = "user:15"
    adaptor.deleteLentaSubscriptions(userId)

    val nissanTags = List("NISSAN", "NISSAN#MICRA", "NISSAN#MICRA#7758163")
    val fordTags = List("FORD", "FORD#FOCUS", "FORD#FOCUS#7306596")
    val initialTags = nissanTags ++ fordTags
    addSubscriptionTags(userId, initialTags)

    val updatedSubscriptionResponse: AutoLentaSubscriptionResponse = deleteSubscriptionTags(userId, nissanTags)

    Assertions.assertThat(updatedSubscriptionResponse.getSubscription.getIncludeTags).hasSize(3)
    Assertions.assertThat(updatedSubscriptionResponse.getSubscription.getIncludeTags).containsOnlyElementsOf(fordTags.asJava)
  }

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {
    val userId = "user:16"
    adaptor.deleteLentaSubscriptions(userId)
    val tags = List("NISSAN", "NISSAN#MICRA", "NISSAN#MICRA#7758163")

    addSubscriptionTags(userId, tags)

    val tagsToDeleteSubscription = new AutoLentaSubscription().includeTags(tags.asJava)

    val req = (apiClient: ApiClient) =>
      apiClient
        .lenta()
        .deleteSubscriptionTagsLenta()
        .reqSpec(defaultSpec)
        .body(tagsToDeleteSubscription)
        .userIdQuery(userId)
        .execute(validatedWith(shouldBeCode(SC_NO_CONTENT)))

    Assertions.assertThat(req.apply(api).getStatusCode).isEqualTo(req.apply(prodApi).getStatusCode)
  }

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    val subscription = new AutoLentaSubscription().includeTags(util.Arrays.asList("NISSAN"))
    api.lenta().deleteSubscriptionTagsLenta()
      .body(subscription)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee400WithoutBody(): Unit = {
    api.lenta().deleteSubscriptionTagsLenta()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  def shouldSee400OnInvalidUserId(): Unit = {
    val userId = "invalid_user:123"
    val subscription = new AutoLentaSubscription().includeTags(util.Arrays.asList("NISSAN"))
    api.lenta().deleteSubscriptionTagsLenta()
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

  private def deleteSubscriptionTags(userId: String, tagsToDelete: List[String]) = {
    val deletedTagsSubscription =
      new AutoLentaSubscription().includeTags(tagsToDelete.asJava)
    adaptor.deleteLentaSubscriptionTags(userId, deletedTagsSubscription)
  }
}
