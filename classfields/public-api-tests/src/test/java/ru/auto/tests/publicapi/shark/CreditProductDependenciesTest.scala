package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, validatedWith}
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._

@DisplayName("GET /shark/credit-product/dependencies/{domain}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreditProductDependenciesTest {

  @(Rule@getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  private def fromJavaList[E](list: java.util.List[E]) =
    Option(list).map(_.asScala).getOrElse(Seq.empty)

  @Test
  @Owner(SHARK)
  def shouldGetCreditProductDependencies(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val allProducts = adaptor.getAllCreditProducts(sessionId).getCreditProducts.asScala
    val creditProduct1 = allProducts.find(_.getId == TestProductId).get
    val creditProduct2 = allProducts.find(_.getId == TestProductId2).get

    val borrowerProfileReq1 = fromJavaList(creditProduct1.getBorrowerPersonProfileBlocks).find(_.getRequiredCondition != null)
    val borrowerProfileReq2 = fromJavaList(creditProduct2.getBorrowerPersonProfileBlocks).find(_.getRequiredCondition != null)
    val infoReq1 = fromJavaList(creditProduct1.getCreditApplicationInfoBlocks).find(_.getRequiredCondition != null)
    val infoReq2 = fromJavaList(creditProduct2.getCreditApplicationInfoBlocks).find(_.getRequiredCondition != null)

    val productsIdsQuery = List(TestProductId, TestProductId2).mkString(",")

    val resp = api.shark().creditProductDependencies()
      .creditProductIdsQuery(productsIdsQuery)
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON()))

    val borrowerProfileReqs = (borrowerProfileReq1 ++ borrowerProfileReq2).toArray
    val infoReqs = (infoReq1 ++ infoReq2).toArray

    Assertions.assertThat(resp.getCreditProductDependencies.getCreditApplicationInfoBlocks)
      .contains(infoReqs: _*)
    Assertions.assertThat(resp.getCreditProductDependencies.getBorrowerPersonProfileBlocks)
      .contains(borrowerProfileReqs:_ *)
  }

  @Test
  @Owner(SHARK)
  def shouldHasNoDiffWithProduction(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val req = (apiClient: ApiClient) => apiClient.shark.creditProductDependencies()
      .creditProductIdsQuery(TestProductId, TestProductId2)
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
