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
import ru.auto.tests.moisha.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.moisha.anno.Prod
import ru.auto.tests.moisha.constants.MoishaConstants._
import ru.auto.tests.moisha.model.{RepresentsDateTimeInterval, RequestView}
import ru.auto.tests.moisha.models.Categories.{New, Used}
import ru.auto.tests.moisha.models.Categories
import ru.auto.tests.moisha.models.GeoIds.{RegMoscow, RegSPb, RegVoronezh}
import ru.auto.tests.moisha.models.Products.Placement
import ru.auto.tests.moisha.models.Transports.{Cars, Commercial}
import ru.auto.tests.moisha.models.{MoishaContext, MoishaOffer, Transports}
import ru.auto.tests.moisha.module.MoishaApiModule
import ru.auto.tests.moisha.ra.RequestSpecBuilders.defaultSpec

import java.util
import scala.annotation.meta.getter

@DisplayName("POST /service/{service}/price/ for placement")
@GuiceModules(Array(classOf[MoishaApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class MoishaPlacementTest(
    clientRegionId: Int,
    offerPlacementDay: Int,
    transport: Transports.Value,
    category: Categories.Value) {
  import MoishaPlacementTest._

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  def shouldHaveIdenticalPlacementPrices(): Unit = {
    val now = DateTime.now()
    val req = (apiClient: ApiClient) =>
      apiClient.price.priceRoute
        .reqSpec(defaultSpec())
        .servicePath(AUTORU)
        .body(
          new RequestView()
            .context(MoishaContext(clientRegionId, offerPlacementDay))
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

object MoishaPlacementTest {
  val product = Placement
  val mark = "BMW"
  val price = 100000000
  val creationTs = "2020-12-13T23:59:59.999+03:00"

  val regions = List(RegMoscow, RegSPb, RegVoronezh)
  val days = List(1, 2)
  val transports = List(Cars, Commercial)
  val categories = List(New, Used)

  @Parameterized.Parameters(name = "index=[{index}] rid:\"{0}\" day:\"{1}\" transport:\"{2}\" category:\"{3}\"")
  def getParameters: util.Collection[Array[Object]] = {
    val data: Array[Array[Object]] = (for {
      region <- regions
      day <- days
      transport <- transports
      category <- categories
    } yield Array(region.asInstanceOf[Object], day.asInstanceOf[Object], transport, category)).toArray

    util.Arrays.asList(data: _*)
  }
}
