package ru.auto.tests.comments

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import ru.auto.tests.model.AutoApiInternalMdsPhotoInfo
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /comments/mds_photo")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetPhotoTest {

  private val PHOTO_INFO_BODY = new AutoApiInternalMdsPhotoInfo().namespace("autoru-all").groupId(123).name("abc")

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
  def shouldSee400WithoutBody(): Unit = {
    api.comments.getMdsPhoto
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldNotSeeDiffWithProductionPaidReport(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient.comments.getMdsPhoto
        .reqSpec(defaultSpec)
        .body(PHOTO_INFO_BODY)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api), jsonEquals[JsonObject](request(prodApi)))
  }

}
