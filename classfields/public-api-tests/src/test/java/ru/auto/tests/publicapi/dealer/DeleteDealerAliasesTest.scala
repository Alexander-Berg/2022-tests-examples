package ru.auto.tests.publicapi.dealer

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_NOT_FOUND}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{After, Before, Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import ru.auto.tests.publicapi.consts.Owners.AVGRIBANOV
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{NOT_FOUND, NO_AUTH}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.model.{AutoApiErrorResponse, AutoDealerAliasesV2PublicInput}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.xUserCMExpertAliases

import scala.annotation.meta.getter

@DisplayName("POST /dealer/aliases")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteDealerAliasesTest {

  private val origin: String = "msk0003"
  private val externalId: String = "0002"
  private val aliaseCalltraking = AutoDealerAliasesV2PublicInput.AliasTagEnum.CALLTRACKING
  private val requestBody = new AutoDealerAliasesV2PublicInput()
    .aliasTag(aliaseCalltraking)
    .origin(origin)
    .externalId(externalId)

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Before
  def addAlias(): Unit = {
    adaptor.addDealerAlias(requestBody)
  }

  @Test
  @Owner(AVGRIBANOV)
  def shouldSeeSuccess(): Unit = {
    val response = api.dealer.deleteAlias().reqSpec(xUserCMExpertAliases)
      .body(requestBody)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response).hasMessage("Alias deleted, 1 entries affected")
  }

  @Test
  @Owner(AVGRIBANOV)
  def shouldSeeErrorWithoutTokenCMExpert(): Unit = {
    val response = api.dealer.deleteAlias()
      .body(requestBody)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(NO_AUTH)
      .hasDetailedError("Unknown application. Please provide valid token in X-Authorization header")
  }

  @Test
  @Owner(AVGRIBANOV)
  def shouldSeeErrorWithtoutOrigin(): Unit = {
    val requestBodyWithtoutOrigin = new AutoDealerAliasesV2PublicInput()
      .aliasTag(AutoDealerAliasesV2PublicInput.AliasTagEnum.CALLTRACKING)
      .externalId(externalId)

    val response = api.dealer.deleteAlias().reqSpec(xUserCMExpertAliases)
      .body(requestBodyWithtoutOrigin)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(NOT_FOUND)
      .hasDetailedError("NOT_FOUND: Origin  cannot be resolved into dealerId")
  }

  @After
  def deleteAlias(): Unit = {
    adaptor.deleteDealerAlias(requestBody)
  }

}