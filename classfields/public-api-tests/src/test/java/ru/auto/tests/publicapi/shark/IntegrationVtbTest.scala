package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.commons.util.Utils.getResourceAsString
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.{PublicApiAdaptor, SharkAdaptor}
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.{defaultSpec, withXmlBody}
import ru.auto.tests.publicapi.shark.IntegrationVtbTest.{MaBankClaimId, StatusBankClaimId}
import ru.auto.tests.publicapi.testdata.TestData

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

object IntegrationVtbTest {

  @Parameterized.Parameters(name = "{index}: {0}")
  def getParameters: Array[Array[AnyRef]] =
    TestData.sharkVtbMaResponses()
      .zip(TestData.sharkVtbStatusResponses)
      .zip(TestData.defaultCreditApplications).map { case ((a, b), c) => Array(a, b, c) }

  private val MaBankClaimId = "a15dgswmz8g"
  private val StatusBankClaimId = "a14svy5unxd"
}

@DisplayName("GET /shark/integration/vtb")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class IntegrationVtbTest(val maResponse: String, val statusResponse: String, val creditApplication: String) {
  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  private val sharkApi: SharkAdaptor = null

  @Test
  @Owner(SHARK)
  def shouldSee403WhenNoAuth(): Unit = {
    api.shark.integrationRoute()
      .reqSpec(withXmlBody(getResourceAsString(maResponse)))
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(SHARK)
  def handlingMaResponse(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val s = adaptor.createCreditApplication(sessionId, creditApplication)
    val creditApplicationId = s.getCreditApplication.getId

    api.shark.addProducts()
      .reqSpec(defaultSpec)
      .creditApplicationIdPath(creditApplicationId)
      .creditProductIdsQuery(TestProductId)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200Ok()))

    val creditApplicationAfterChange = adaptor.getCreditApplication(sessionId, creditApplicationId)
    val claim = creditApplicationAfterChange.getCreditApplication.getClaims.asScala.find(_.getCreditProductId == TestProductId).get

    sharkApi.updateClaim(creditApplicationId, claim.getId, updateBankClaimIdReqJson(MaBankClaimId))

    api.shark.integrationRoute()
      .reqSpec(withXmlBody(getResourceAsString(maResponse)))
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200Ok()))
  }

  @Test
  @Owner(SHARK)
  def handlingStatusResponse(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val s = adaptor.createCreditApplication(sessionId, creditApplication)
    val creditApplicationId = s.getCreditApplication.getId

    api.shark.addProducts()
      .reqSpec(defaultSpec)
      .creditApplicationIdPath(creditApplicationId)
      .creditProductIdsQuery(TestProductId)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200Ok()))

    val creditApplicationAfterChange = adaptor.getCreditApplication(sessionId, creditApplicationId)
    val claim = creditApplicationAfterChange.getCreditApplication.getClaims.asScala.find(_.getCreditProductId == TestProductId).get

    sharkApi.updateClaim(creditApplicationId, claim.getId, updateBankClaimIdReqJson(StatusBankClaimId))

    api.shark.integrationRoute()
      .reqSpec(withXmlBody(getResourceAsString(statusResponse)))
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200Ok()))
  }

  private def updateBankClaimIdReqJson(bankClaimId: String) = s"""{"bankClaimId":"$bankClaimId"}"""
}
