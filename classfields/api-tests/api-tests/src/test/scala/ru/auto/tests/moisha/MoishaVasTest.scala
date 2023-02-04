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
import ru.auto.tests.moisha.models.Categories.{New, Used}
import ru.auto.tests.moisha.models.GeoIds.{RegMoscow, RegSPb, RegVoronezh}
import ru.auto.tests.moisha.models.Products._
import ru.auto.tests.moisha.models.Transports.{Cars, Commercial, Moto}
import ru.auto.tests.moisha.models.{Categories, MoishaContext, MoishaOffer, Products, Transports}
import ru.auto.tests.moisha.module.MoishaApiModule
import ru.auto.tests.moisha.ra.RequestSpecBuilders.defaultSpec

import java.util
import scala.annotation.meta.getter

@DisplayName("POST /service/{service}/price/ for vas")
@GuiceModules(Array(classOf[MoishaApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class MoishaVasTest(
    product: Products.Value,
    clientRegionId: Int,
    transport: Transports.Value,
    category: Categories.Value) {
  import MoishaVasTest._

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  def shouldHaveIdenticalVasPrices(): Unit = {
    val now = DateTime.now()
    val req = (apiClient: ApiClient) =>
      apiClient.price.priceRoute
        .reqSpec(defaultSpec())
        .servicePath(AUTORU)
        .body(
          new RequestView()
            .context(MoishaContext(clientRegionId, 1))
            .product(product.toString)
            .interval(
              new RepresentsDateTimeInterval()
                .from(now.withTimeAtStartOfDay().toString)
                .to(now.withTimeAtStartOfDay().plusDays(1).minusMillis(1).toString)
            )
            .offer(MoishaOffer(category.toString, creationTs, mark, price, transport.toString))
        )
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonObject](req.apply(prodApi))
    )
  }
}

object MoishaVasTest {
  val mark = "BMW"
  val price = 100000000
  val creationTs = "2020-12-13T23:59:59.999+03:00"

  val products = List(Badge, Special, Boost, Premium)
  val regions = List(RegMoscow, RegSPb, RegVoronezh)
  val transports = List(Cars, Moto, Commercial)
  val categories = List(New, Used)

  @Parameterized.Parameters(name = "index=[{index}] product:\"{0}\" rid:\"{1}\" transport:\"{2}\" category:\"{3}\"")
  def getParameters: util.Collection[Array[Object]] = {
    val data: Array[Array[Object]] = (for {
      product <- products
      region <- regions
      transport <- transports
      category <- categories
    } yield Array(product, region.asInstanceOf[Object], transport, category)).toArray

    util.Arrays.asList(data: _*)
  }
}
