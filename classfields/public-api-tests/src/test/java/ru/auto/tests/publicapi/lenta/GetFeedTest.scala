package ru.auto.tests.publicapi.lenta

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
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
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import ru.auto.tests.publicapi.model.{AutoLentaContentReadState, AutoLentaReadRequest, AutoLentaSubscription}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import java.util
import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

@DisplayName("GET lenta/feed")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetFeedTest {

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
  def getFeedWithoutSubscription(): Unit = {
    val userId = "user:1"
    adaptor.deleteLentaSubscriptions(userId)

    val feedResponse = api
      .lenta()
      .getFeedLenta
      .reqSpec(defaultSpec)
      .userIdQuery(userId)
      .contentAmountQuery(10)
      .executeAs(validatedWith(shouldBe200OkJSON))

    Assertions.assertThat(feedResponse.getPayloads).isNotEmpty
  }

  @Test
  def getFeedAfterSubscription(): Unit = {
    val userId = "user:2"
    adaptor.deleteLentaSubscriptions(userId)
    val subscription =
      new AutoLentaSubscription().includeTags(util.Arrays.asList("BYD", "BYD#F3", "BYD#F3#20414063"))

    adaptor.addLentaSubscription(userId, subscription)

    val feedResponse = api
      .lenta()
      .getFeedLenta
      .reqSpec(defaultSpec)
      .userIdQuery(userId)
      .contentAmountQuery(10)
      .executeAs(validatedWith(shouldBe200OkJSON))

    Assertions.assertThat(feedResponse.getPayloads).isNotEmpty
    val firstPayload = feedResponse.getPayloads.asScala.head
    Assertions.assertThat(firstPayload.getTags.getGenId).contains("BYD#F3#20414063")
  }

  @Test
  def getFeedWithMarkedReadContent(): Unit = {
    val userId = "user:3"
    adaptor.deleteLentaSubscriptions(userId)

    val feedResponse1 = adaptor.getFeed(userId, 10)

    Assertions.assertThat(feedResponse1.getPayloads).isNotEmpty
    val payloadToPreview = feedResponse1.getPayloads.asScala.head
    val payloadToRead = feedResponse1.getPayloads.asScala.tail.head

    val previewedState = new AutoLentaContentReadState().contentId(payloadToPreview.getId).wasSeenPreview(true)
    val readState = new AutoLentaContentReadState().contentId(payloadToRead.getId).wasSeenPreview(true).wasRead(true)

    val readRequest = new AutoLentaReadRequest().contentReadState(
      util.Arrays.asList(
        previewedState,
        readState
      )
    )

    adaptor.markRead(userId, readRequest)

    val feedResponse2 = api
      .lenta()
      .getFeedLenta
      .reqSpec(defaultSpec)
      .userIdQuery(userId)
      .contentAmountQuery(10)
      .contentIdQuery(payloadToPreview.getId)
      .executeAs(validatedWith(shouldBeCode(200)))

    Assertions.assertThat(feedResponse2.getPayloads).isNotEmpty
    val firstPayload = feedResponse2.getPayloads.asScala.head
    Assertions.assertThat(firstPayload.getId).doesNotMatch(payloadToPreview.getId)
    assertThat(firstPayload).hasWasSeenPreview(null)
    Assertions.assertThat(feedResponse2.getPayloads.asScala.map(_.getId).asJava).doesNotContain(payloadToRead.getId)
  }

  @Test
  def getFeedWithWrongUserId(): Unit = {
    api
      .lenta()
      .getFeedLenta
      .reqSpec(defaultSpec)
      .userIdQuery("user:wrong")
      .contentAmountQuery(10)
      .executeAs(validatedWith(shouldBeCode(400)))
  }

  @Test
  def getFeedWithoutContentId(): Unit = {
    api
      .lenta()
      .getFeedLenta
      .reqSpec(defaultSpec)
      .userIdQuery("user:4")
      .executeAs(validatedWith(shouldBeCode(400)))
  }

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {
    val userId = "user:5"
    adaptor.deleteLentaSubscriptions(userId)

    val req = (apiClient: ApiClient) =>
      apiClient
        .lenta()
        .getFeedLenta
        .reqSpec(defaultSpec)
        .userIdQuery(userId)
        .contentAmountQuery(10)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
