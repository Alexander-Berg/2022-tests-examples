package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import io.restassured.response.Response
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_OK}
import org.assertj.core.api.Java6Assertions.assertThat
import org.hamcrest.{Matcher, MatcherAssert}
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.CARFAX
import ru.auto.tests.publicapi.model.AutoApiVinGarageLanding
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter;

@DisplayName("GET /garage/landing")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetLandingTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Owner(CARFAX)
  def shouldSee403WhenNoAuth(): Unit = {
    api.garage().getLanding.execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProductionAndHasExpectedFields(): Unit = {
    val landing: ApiClient => Response = (api: ApiClient) =>
      api
        .garage()
        .getLanding
        .reqSpec(defaultSpec())
        .execute(validatedWith(shouldBeCode(SC_OK)))

    val actual: Response = landing(api)
    val expected: Response = landing(prodApi)

    val actualAsModel = actual.as(classOf[AutoApiVinGarageLanding])
    assertThat(actualAsModel.getPartnerPromos.get(0).getTitle.length).isBetween(1, 50)
    assertThat(actualAsModel.getPartnerPromos.get(0).getDescription.length).isBetween(1, 150)
    assertThat(actualAsModel.getPartnerPromos.get(0).getImage.getSizes.size()).isGreaterThanOrEqualTo(4)

    val matchExpected: Matcher[JsonObject] = jsonEquals(expected.as(classOf[JsonObject]))
    MatcherAssert.assertThat(actual.as(classOf[JsonObject]), matchExpected)
  }
}
