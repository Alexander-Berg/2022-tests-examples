package ru.yandex.auto.searcher.http.action.subscriptions

import org.junit.{Ignore, Test}
import org.scalatest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests
import ru.yandex.auto.Fixtures
import ru.yandex.auto.core.util.ProfileDataStorage
import ru.yandex.auto.searcher.configuration.SearchConfigurationBuilder
import ru.yandex.auto.searcher.core.{
  CarSearchParams,
  MotoYoctoSearchQueryCompiler,
  TrucksYoctoSearchQueryCompiler,
  YoctoSearchQueryCompiler
}
import ru.yandex.auto.searcher.request.ServRequestBuilder
import ru.yandex.vertis.subscriptions.DSL.{not => negate, _}
import ru.yandex.vertis.subscriptions.Model

import scala.collection.convert.decorateAll._

@ContextConfiguration(locations = Array("/auto-searcher-test-unit.xml"))
@Ignore("fix in https://st.yandex-team.ru/AUTO-11309")
class SubscriptionQueryTest extends AbstractJUnit4SpringContextTests with Matchers with Fixtures {

  @Autowired var servRequestBuilder: ServRequestBuilder = _
  @Autowired var searchConfigurationBuilder: SearchConfigurationBuilder = _
  @Autowired var yoctoSearchQueryCompiler: YoctoSearchQueryCompiler = _
  @Autowired var trucksYoctoSearchQueryCompiler: TrucksYoctoSearchQueryCompiler = _
  @Autowired var motoYoctoSearchQueryCompiler: MotoYoctoSearchQueryCompiler = _

  /**
    *     Notice that fields may have different mapping from query string
    *     `catalog_equipment` is a tag-like field (combination of AND|OR is allowed)
    *     opposite to `dealer_org_type` which is an enum type (strict OR)
    *
    *     @see https://st.yandex-team.ru/VSSUBS-751
    */
  @Test def testMixedFieldsClauses(): Unit = {
    val model = getModelQuery(
      "catalog_equipment=bluetooth&catalog_equipment=climate-control-1,climate-control-2,multizone-climate-control&dealer_org_type=1&dealer_org_type=2&dealer_org_type=4"
    )

    model.getAnd.getQueriesList.asScala.toList should contain allElementsOf List(
      and(
        term(point("search_c_options", "bluetooth")),
        or(
          term(point("search_c_options", "climate-control-1")),
          term(point("search_c_options", "climate-control-2")),
          term(point("search_c_options", "multizone-climate-control"))
        )
      ),
      or(
        term(point("org_type", "1")),
        term(point("org_type", "2")),
        term(point("org_type", "4"))
      )
    )
  }

  @Test def testAndClauses(): Unit = {
    val model = getModelQuery(
      "catalog_equipment=bluetooth,climate-control-1,climate-control-2"
    )

    model.getAnd.getQueriesList.asScala.toList should contain allElementsOf List(
      or(
        term(point("search_c_options", "bluetooth")),
        term(point("search_c_options", "climate-control-1")),
        term(point("search_c_options", "climate-control-2"))
      )
    )
  }

  @Test def testMixedAndOrClauses(): Unit = {
    val model = getModelQuery(
      "catalog_equipment=bluetooth&catalog_equipment=climate-control-1,climate-control-2"
    )

    model.getAnd.getQueriesList.asScala.toList should contain allElementsOf List(
      and(
        term(point("search_c_options", "bluetooth")),
        or(
          term(point("search_c_options", "climate-control-1")),
          term(point("search_c_options", "climate-control-2"))
        )
      )
    )
  }

  @Test def testOrClauses(): Unit = {
    val model = getModelQuery(
      "dealer_org_type=1&dealer_org_type=2&dealer_org_type=4"
    )

    model.getAnd.getQueriesList.asScala.toList should contain allElementsOf List(
      or(
        term(point("org_type", "1")),
        term(point("org_type", "2")),
        term(point("org_type", "4"))
      )
    )
  }

  @Test def testNegativeForCatalogFilter(): Unit = {
    val model = getModelQuery(
      """not_catalog_filter=mark=AUDI,model=A1&
        |not_catalog_filter=mark=AUDI,model=A2&
        |not_catalog_filter=mark=AUDI,model=A3""".stripMargin
    )

    model.getAnd.getQueriesList.asScala.toList should contain allElementsOf List(
      and(
        negate(term(point("mark_model_code", "AUDI#A1"))),
        negate(term(point("mark_model_code", "AUDI#A2"))),
        negate(term(point("mark_model_code", "AUDI#A3")))
      )
    )
  }

  @Test def testBothForCatalogFilter(): Unit = {
    val model = getModelQuery(
      """catalog_filter=mark=AUDI,model=A1&
        |catalog_filter=mark=AUDI,model=A2&
        |not_catalog_filter=mark=AUDI,model=A3&
        |not_catalog_filter=mark=AUDI,model=A4""".stripMargin
    )

    model.getAnd.getQueriesList.asScala.toList should contain allElementsOf List(
      and(
        or(
          term(point("mark_model_code", "AUDI#A1")),
          term(point("mark_model_code", "AUDI#A2"))
        ),
        and(
          negate(term(point("mark_model_code", "AUDI#A3"))),
          negate(term(point("mark_model_code", "AUDI#A4")))
        )
      )
    )
  }

  @Test def testCatalogFilterBothForSize1(): Unit = {
    val model = getModelQuery(
      """catalog_filter=mark=AUDI,model=A1&
        |not_catalog_filter=mark=AUDI,model=A2""".stripMargin
    )

    model.getAnd.getQueriesList.asScala.toList should contain allElementsOf List(
      and(
        term(point("mark_model_code", "AUDI#A1")),
        negate(term(point("mark_model_code", "AUDI#A2")))
      )
    )
  }

  @Test def testCatalogFilterOnePositive(): Unit = {
    val model = getModelQuery("catalog_filter=mark=AUDI,model=A1&")

    model.getAnd.getQueriesList.asScala.toList should contain allElementsOf List(
      term(point("mark_model_code", "AUDI#A1"))
    )
  }

  @Test def testCatalogFilterOneNegative(): Unit = {
    val model = getModelQuery("not_catalog_filter=mark=AUDI,model=A1&")

    model.getAnd.getQueriesList.asScala.toList should contain allElementsOf List(
      negate(term(point("mark_model_code", "AUDI#A1")))
    )
  }

  private def carSearchParams(request: String): CarSearchParams = {
    servRequestBuilder.buildRequest(requestFromQuery(request))
  }

  private def getModelQuery(params: String): Model.Query = {
    new SubscriptionQueryAction(
      yoctoSearchQueryCompiler,
      trucksYoctoSearchQueryCompiler,
      motoYoctoSearchQueryCompiler,
      searchConfigurationBuilder
    ).processInternal(carSearchParams(params), new ProfileDataStorage, emptyRequest)
  }

}
