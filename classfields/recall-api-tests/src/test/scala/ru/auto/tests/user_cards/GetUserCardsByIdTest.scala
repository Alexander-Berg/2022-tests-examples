package ru.auto.tests.user_cards

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.hamcrest.MatcherAssert
import org.hamcrest.core.IsEqual
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.module.RecallApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.recall.ApiClient

import scala.annotation.meta.getter

object GetUserCardsByIdTest {

  @Parameterized.Parameters(name = "{index}: {0}")
  def getParameters: Array[Array[Any]] = {
    Map(
      "existing" -> (894, "user:22281346", true),
      "wrongUserId" -> (894, "another-user-id", false),
      "nonExistingCardId" -> (49802760, "any-userd-id", false),
      "existingButRemovedForUser" -> (854, "user:22281346", false)
    ).values.map(_.productIterator.toArray).toArray
  }

}

@DisplayName("GET /user-cards/{cardId}")
@GuiceModules(Array(classOf[RecallApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class GetUserCardsByIdTest(cardId: Long, userId: String, isCardFound: Boolean) {

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
  def shouldNotSeeDiffWithProduction(): Unit = {
    val responseValidator =
      if (isCardFound) shouldBe200OkJSON
      else shouldBeCode(404)

    val request = (apiClient: ApiClient) =>
      apiClient.userCards.getCardById
        .reqSpec(defaultSpec)
        .cardIdPath(cardId)
        .xUserIdHeader(userId)
        .execute(validatedWith(responseValidator))
        .asString()

    MatcherAssert.assertThat(request(api), new IsEqual(request(api)))
  }
}
