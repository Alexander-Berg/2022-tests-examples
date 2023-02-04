package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.GuiceModules
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, validatedWith}
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.TestData

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._


object CreditProductListTest {

  @Parameterized.Parameters(name = "{0}")
  def getParameters: Array[Array[AnyRef]] =
    Array(TestData.defaultCreditApplications())
}

@DisplayName("POST /shark/credit-product/list/{domain}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class CreditProductListTest(creditApplication: String) {

  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(SHARK)
  def shouldGetAllCreditProducts(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val resp = api.shark().creditProductList().reqSpec(defaultSpec)
      .body(new VertisSharkApiCreditProductsRequest().all(new Object()))
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON()))
    Assertions.assertThat(resp.getCreditProducts).isNotEmpty
  }

  @Test
  @Owner(SHARK)
  def shouldGetCreditProductsByGeo(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val resp = api.shark().creditProductList().reqSpec(defaultSpec)
      .body(new VertisSharkApiCreditProductsRequest().byGeo(
        new VertisSharkApiCreditProductsRequestByGeo()
          .geobaseIds(List(Long.box(TestProductGeoId)).asJava)
      ))
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON()))

    Assertions.assertThat(resp.getCreditProducts).isNotEmpty
    Assertions.assertThat(resp.getCreditProducts.asScala.map(_.getId).asJava).contains(TestProductId)
  }

  @Test
  @Owner(SHARK)
  def shouldGetCreditProductsByCreditApplication(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val creditApplicationId = adaptor.createCreditApplication(sessionId, creditApplication).getCreditApplication.getId

    api.shark.addProducts()
      .reqSpec(defaultSpec)
      .creditApplicationIdPath(creditApplicationId)
      .creditProductIdsQuery(TestProductId)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val resp = api.shark().creditProductList().reqSpec(defaultSpec)
      .body(new VertisSharkApiCreditProductsRequest().byCreditApplication(
        new VertisSharkApiCreditProductsRequestByCreditApplication()
          .creditApplicationId(creditApplicationId)
          .alsoNotSuitable(true)
      ))
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON()))

    Assertions.assertThat(resp.getCreditProducts).isNotEmpty
    Assertions.assertThat(resp.getCreditProducts.asScala.map(_.getId).asJava).contains(TestProductId)
  }
}
