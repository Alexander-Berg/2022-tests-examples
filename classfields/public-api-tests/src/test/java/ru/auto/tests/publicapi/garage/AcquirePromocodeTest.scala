package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.CARFAX
import ru.auto.tests.publicapi.model.AutoApiVinGarageGarageAcquirePromocodeResponse
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.{defaultSpec, withJsonBody}

import scala.annotation.meta.getter

@DisplayName("POST /garage/user/promos/acquire_promocode")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class AcquirePromocodeTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val am: AccountManager = null

  @Inject private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(CARFAX)
  def shouldResponseWithAccessDeniedWithoutSession(): Unit = {
    val requestJson = "{\"pool_id\": \"koleso_30_service\"}"

    val response = api
      .garage()
      .acquirePromocode()
      .reqSpec(defaultSpec())
      .reqSpec(withJsonBody(requestJson))
      .executeAs(validatedWith(shouldBeCode(403)))

    assertThat(response.getError.toString)
      .isEqualTo(AutoApiVinGarageGarageAcquirePromocodeResponse.ErrorEnum.UNSUPPORTED_USER_TYPE.toString)
  }

  @Test
  @Owner(CARFAX)
  def shouldSeeEqualsPromocodeIfUserAcquiredTwice(): Unit = {
    val requestJson = "{\"pool_id\": \"koleso_30_service\"}"

    val account = am.create()
    val sessionId = adaptor.login(account).getSession.getId

    val response1 = api
      .garage()
      .acquirePromocode()
      .xSessionIdHeader(sessionId)
      .reqSpec(defaultSpec())
      .reqSpec(withJsonBody(requestJson))
      .executeAs(validatedWith(shouldBe200OkJSON))

    val response2 = api
      .garage()
      .acquirePromocode()
      .xSessionIdHeader(sessionId)
      .reqSpec(defaultSpec())
      .reqSpec(withJsonBody(requestJson))
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response1.getPromocode).isNotEmpty
    assertThat(response1).isEqualTo(response2)
  }

  @Test
  @Owner(CARFAX)
  def shouldSetPromocodesAreOverForUnknownPoolId(): Unit = {
    val requestJson = "{\"pool_id\": \"unknown_pool_id\"}"

    val account = am.create()
    val sessionId = adaptor.login(account).getSession.getId

    val response = api
      .garage()
      .acquirePromocode()
      .xSessionIdHeader(sessionId)
      .reqSpec(defaultSpec())
      .reqSpec(withJsonBody(requestJson))
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getPromocode).isNull()
    assertThat(response.getPromocodesAreOver).isTrue
  }

}
