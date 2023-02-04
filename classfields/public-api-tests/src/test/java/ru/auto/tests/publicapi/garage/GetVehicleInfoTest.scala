package ru.auto.tests.publicapi.garage

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.hamcrest.{Matcher, MatcherAssert}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.carfax.RawReportUtils
import ru.auto.tests.publicapi.consts.Owners.CARFAX
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess

import scala.annotation.meta.getter

@DisplayName("PUT /garage/user/vehicle_info/{identifier}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetVehicleInfoTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val testApi: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val am: AccountManager = null

  @Inject private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(CARFAX)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession

    val getVehicleInfo: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .getVehicleInfo
        .xSessionIdHeader(session.getId)
        .identifierPath("X9FLXXEEBLES67719")
        .reqSpec(defaultSpec())
        .execute(validatedWith(shouldBeSuccess()))
        .as(classOf[JsonObject])

    val actual = getVehicleInfo(testApi)
    val equalsExpected: Matcher[JsonObject] = jsonEquals(getVehicleInfo(prodApi)).whenIgnoringPaths(RawReportUtils.IGNORED_PATHS:_*)

    MatcherAssert.assertThat(actual, equalsExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetNotFoundError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession

    val getVehicleInfo: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .getVehicleInfo
        .xSessionIdHeader(session.getId)
        .identifierPath("Z0NZWE00054341404")
        .reqSpec(defaultSpec())
        .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
        .as(classOf[JsonObject])

    val actual = getVehicleInfo(testApi)
    val equalsExpected: Matcher[JsonObject] = jsonEquals(getVehicleInfo(prodApi))

    MatcherAssert.assertThat(actual, equalsExpected)
  }

  @Test
  @Owner(CARFAX)
  def shouldGetBadRequestError(): Unit = {
    val account = am.create()
    val session = adaptor.login(account).getSession

    val getVehicleInfo: ApiClient => JsonObject = (api: ApiClient) =>
      api
        .garage()
        .getVehicleInfo
        .xSessionIdHeader(session.getId)
        .identifierPath("X0FLXXEEBLES67719O")
        .reqSpec(defaultSpec())
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
        .as(classOf[JsonObject])

    val actual = getVehicleInfo(testApi)
    val equalsExpected: Matcher[JsonObject] = jsonEquals(getVehicleInfo(prodApi))

    MatcherAssert.assertThat(actual, equalsExpected)
  }
}
