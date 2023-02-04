package ru.auto.tests.publicapi.search

import com.carlosbecker.guice.GuiceModules
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.CATALOG
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.yandex.qatools.allure.annotations.{Description, Parameter}

import scala.annotation.meta.{field, getter, setter}
import scala.jdk.CollectionConverters._
import ru.auto.test.salesman.user.model.AutoApiOffer.CategoryEnum
import ru.auto.test.salesman.user.model.AutoApiOffer.CategoryEnum._

@DisplayName("GET /search/{category}/related")
@RunWith(classOf[Parameterized])
@GuiceModules(Array(classOf[PublicApiModule]))
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class GetSearchRelatedCompareTest(
    @Parameter("Категория")
    val categoryPath: CategoryEnum,
    @Parameter("Параметры")
    val catalogFilters: Array[String]
) {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Owner(CATALOG)
  @Description("Сравниваем с продакшном ответы запросов на поиск автомобилей того же класса")
  def shouldHasNoDiffWithProduction(): Unit = {
    val request = (apiClient: ApiClient) => {
      val req = apiClient.search.relatedGET
        .reqSpec(defaultSpec)
        .categoryPath(categoryPath)
        .pageQuery("1")
        .pageSizeQuery("3")
        .groupByQuery("CONFIGURATION")

      catalogFilters
        .foreach(req.catalogFilterQuery(_))

      req
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])
    }

    MatcherAssert.assertThat(
      request(api),
      jsonEquals[JsonObject](request(prodApi))
        .whenIgnoringPaths("offers[*].relevance")
    )
  }
}

object GetSearchRelatedCompareTest {

  @(Parameterized.Parameters @getter)
  val parameters: java.util.Collection[Array[Object]] =
    Seq[Array[Object]](
      Array(CARS, Array("mark=KIA,model=OPTIMA,generation=21342050,configuration=21342121")),
      // @see AUTORUBACK-2689
      // @see AUTORUBACK-2683
      // Simplified example with mismatched generation and nameplate.
      Array(
        CARS,
        Array(
          "mark=BMW,model=X3,generation=20156797,nameplate_name=m40d"
        )
      ),
      // Full example with multiple generation and nameplate combinations, some of them mismatched - should return a few results.
      Array(
        CARS,
        Array(
          "mark=BMW,model=X3,generation=20156797,nameplate_name=35d",
          "mark=BMW,model=X3,generation=20156797,nameplate_name=m40d",
          "mark=BMW,model=X3,generation=21029610,nameplate_name=35d",
          "mark=BMW,model=X3,generation=21029610,nameplate_name=m40d"
        )
      ),
      Array(MOTO, Array("mark=HONDA,model=ATC_200X")),
      Array(TRUCKS, Array("mark=CITROEN,model=NEMO"))
    ).asJava
}
