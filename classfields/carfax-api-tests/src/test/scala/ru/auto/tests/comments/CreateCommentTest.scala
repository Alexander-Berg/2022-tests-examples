package ru.auto.tests.comments

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.assertj.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.{getRandomShortInt, getRandomString}
import ru.auto.tests.model.{AutoApiVinCommentsUser, AutoApiVinCommentsVinReportComment}
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("PUT /comments/{vin}")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreateCommentTest {

  private val VIN = "Z8T4C5FS9BM005269"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidVin(): Unit = {
    api.comments
      .addComment()
      .reqSpec(defaultSpec)
      .vinPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutBody(): Unit = {
    api.comments
      .addComment()
      .reqSpec(defaultSpec)
      .vinPath(VIN)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldCreateComment(): Unit = {
    val body = new AutoApiVinCommentsVinReportComment()
      .text(getRandomString)
      .user(new AutoApiVinCommentsUser().id(s"qa_user:$getRandomShortInt"))

    val response = api.comments
      .addComment()
      .reqSpec(defaultSpec)
      .vinPath(VIN)
      .body(body)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getComment).hasText(body.getText)
    assertThat(response.getComment.getUser).hasId(body.getUser.getId)
  }
}
