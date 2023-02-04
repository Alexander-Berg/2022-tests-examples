package ru.auto.tests.comments

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.adaptor.CarfaxApiAdaptor
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.{getRandomShortInt, getRandomString}
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /comments/{vin}/{block_id}/{user_id}")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetCommentByUserTest {

  private val VIN = "Z8T4C5FS9BM005269"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val adaptor: CarfaxApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidParams(): Unit = {
    api.comments.getComment
      .reqSpec(defaultSpec)
      .vinPath(getRandomString)
      .blockIdPath(getRandomString)
      .userIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldNotSeeDiffWithProductionPaidReport(): Unit = {
    val blockId = getRandomString
    val userId = s"user:$getRandomShortInt"
    adaptor.createComment(VIN, blockId, userId)

    val request = (apiClient: ApiClient) =>
      apiClient.comments.getComment
        .reqSpec(defaultSpec)
        .vinPath(VIN)
        .blockIdPath(blockId)
        .userIdPath(userId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api), jsonEquals[JsonObject](request(prodApi)))
  }

}
