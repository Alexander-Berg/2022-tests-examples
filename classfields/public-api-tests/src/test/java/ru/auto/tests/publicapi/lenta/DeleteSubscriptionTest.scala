package ru.auto.tests.publicapi.lenta

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_NOT_FOUND}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.model.AutoLentaSubscription
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

@DisplayName("DELETE lenta/delete-subscription")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteSubscriptionTest {

  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  def deleteNonExistingSubscription(): Unit = {
    val userId = "user:18"

    val tags = List("NISSAN", "NISSAN#MICRA", "NISSAN#MICRA#7758163")
    setSubscriptionTags(userId, tags)
    adaptor.deleteLentaSubscriptions(userId)
    adaptor.getLentaSubscriptions(userId, SC_NOT_FOUND)

    api.lenta().deleteSubscriptionLenta()
      .reqSpec(defaultSpec)
      .userIdQuery(userId)
      .execute(validatedWith(shouldBe200OkJSON))

    adaptor.getLentaSubscriptions(userId, SC_NOT_FOUND)
  }

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.lenta().deleteSubscriptionLenta()
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee400OnInvalidUserId(): Unit = {
    val userId = "invalid_user:123"
    api.lenta().deleteSubscriptionLenta()
      .reqSpec(defaultSpec)
      .userIdQuery(userId)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  private def setSubscriptionTags(userId: String, tagsToAdd: List[String]) = {
    val addedTagsSubscription =
      new AutoLentaSubscription().includeTags(tagsToAdd.asJava)
    adaptor.addLentaSubscription(userId, addedTagsSubscription)
  }
}
