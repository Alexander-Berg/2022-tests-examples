package ru.auto.tests.moisha

import com.carlosbecker.guice.GuiceModules
import com.google.gson.JsonObject
import com.google.inject.Inject

import io.qameta.allure.junit4.DisplayName
import org.hamcrest.MatcherAssert
import org.joda.time.DateTime
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.moisha.ResponseSpecBuilders.validatedWith
import ru.auto.tests.moisha.anno.Prod
import ru.auto.tests.moisha.constants.MoishaConstants._
import ru.auto.tests.moisha.model.{RepresentsDateTimeInterval, RequestView}
import ru.auto.tests.moisha.models.GeoIds.{RegMoscow, RegSPb, RegVoronezh}
import ru.auto.tests.moisha.models.QuotaProducts.{PlacementCarsNew, PlacementMoto}
import ru.auto.tests.moisha.models.{MoishaQuotaContext, QuotaProducts}
import ru.auto.tests.moisha.module.MoishaApiModule
import ru.auto.tests.moisha.ra.RequestSpecBuilders.defaultSpec

import java.util
import scala.annotation.meta.getter

@DisplayName("POST /service/{service}/price/ for quota")
@GuiceModules(Array(classOf[MoishaApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class MoishaQuotaTest(product: QuotaProducts.Value, clientRegionId: Int) {
  import MoishaQuotaTest._

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  def shouldHaveIdenticalQuotaPrices(): Unit = {
    val now = DateTime.now()
    val req = (apiClient: ApiClient) =>
      apiClient.price.priceRoute
        .reqSpec(defaultSpec())
        .servicePath(QUOTA)
        .body(
          new RequestView()
            .context(MoishaQuotaContext(amount, clientRegionId))
            .product(product.toString)
            .interval(
              new RepresentsDateTimeInterval()
                .from(now.withTimeAtStartOfDay().toString)
                .to(now.withTimeAtStartOfDay().plusDays(1).minusMillis(1).toString)
            )
        )
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonObject](req.apply(prodApi))
    )
  }
}

object MoishaQuotaTest {
  val amount = 2147483647

  val regions = List(RegMoscow, RegSPb, RegVoronezh)
  val products = List(PlacementCarsNew, PlacementMoto)

  @Parameterized.Parameters(name = "index=[{index}] product:\"{0}\" rid:\"{1}\"")
  def getParameters: util.Collection[Array[Object]] = {
    val data: Array[Array[Object]] = (for {
      region <- regions
      product <- products
    } yield Array(product, region.asInstanceOf[Object])).toArray

    util.Arrays.asList(data: _*)
  }
}
