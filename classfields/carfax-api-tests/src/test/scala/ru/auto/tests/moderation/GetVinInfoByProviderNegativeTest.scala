package ru.auto.tests.moderation

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.module.CarfaxApiModule
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /moderation/{provider}")
@GuiceModules(Array(classOf[CarfaxApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetVinInfoByProviderNegativeTest {

  private val VIN = "XWFPE9DN1D0004086"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidProvider(): Unit = {
    api.moderation.autocode
      .reqSpec(defaultSpec)
      .vinQuery(VIN)
      .providerPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidVin(): Unit = {
    api.moderation.autocode
      .reqSpec(defaultSpec)
      .vinQuery(getRandomString)
      .providerPath("ANY")
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

}
