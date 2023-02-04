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
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.{defaultSpec, withJsonBody}
import ru.auto.tests.publicapi.shark.IntegrationSberbankTest.StatusBankClaimId
import ru.auto.tests.publicapi.testdata.TestData

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

object IntegrationSberbankTest {

  @Parameterized.Parameters(name = "{index}: {0}")
  def getParameters: Array[Array[AnyRef]] =
    TestData.sharkSberbankStatusResponses()
      .zip(TestData.defaultCreditApplications).map { case (a, b) => Array(a, b) }

  private val StatusBankClaimId = "defea882-9aca-4ef1-aa1e-6b4951c36299"
}

@DisplayName("GET /shark/integration/sberbank")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class IntegrationSberbankTest(val statusResponse: String, val creditApplication: String) {
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
      .reqSpec(withJsonBody(getResourceAsString(statusResponse)))
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(SHARK)
  def handlingStatusUpdate(): Unit = {
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

    api.shark.sberbankIntegrationRoute()
      .reqSpec(withJsonBody(getResourceAsString(statusResponse)))
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200Ok()))
  }

  @Test
  @Owner(SHARK)
  def returnNotFoundWhenCreditApplicationNotFound(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    api.shark.sberbankIntegrationRoute()
      .reqSpec(withJsonBody(notFoundReqJson))
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeCode(404)))
  }

  private def updateBankClaimIdReqJson(bankClaimId: String): String = s"""{"bankClaimId":"$bankClaimId"}"""

  private val notFoundReqJson: String = """{
                                          |    "bankClaimId": "not-found-id",
                                          |    "status": "Approved",
                                          |    "uuid": "some_request_uuid"
                                          |}""".stripMargin
}
