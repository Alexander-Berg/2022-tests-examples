package ru.auto.tests.publicapi.story

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.assertj.core.api.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.VertisStoryStory
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.story.SearchStoryTest._

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters.CollectionHasAsScala

@DisplayName("GET /story/search")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class SearchStoryTest {

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
    api.story.searchStory()
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSearchStoryHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) => apiClient.story.searchStory()
      .reqSpec(defaultSpec)
      .executeAs(validatedWith(shouldBe200OkJSON))
    val actual = req.apply(api).getStories.asScala.map(cleanRandom)
    val expected = req.apply(prodApi).getStories.asScala.map(cleanRandom)
    assertThat(actual).isEqualTo(expected)
  }

  private def cleanRandom(value: VertisStoryStory): VertisStoryStory = {
    if (Option(value.getCardId).filterNot(_.isEmpty).isDefined) {
      value.setTitle(DefaultTitle)
    }
    value
  }
}

object SearchStoryTest {

  private val DefaultTitle: String = "some-title"
}
