package ru.yandex.auto.searcher.search

import com.yandex.yoctodb.query.QueryBuilder
import org.junit.{Ignore, Test}
import org.scalatest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests
import ru.yandex.auto.core.AutoLocale
import ru.yandex.auto.core.model.ShortCarAd
import ru.yandex.auto.core.model.enums.{CarAdField, CommonField, EntityField}
import ru.yandex.auto.core.region.Region
import ru.yandex.auto.core.search2.YoctoSearchTemplateImpl
import ru.yandex.auto.core.vendor.{Vendor, VendorManagerImpl}
import ru.yandex.auto.searcher.configuration.SearchConfiguration
import ru.yandex.auto.searcher.core.{CarSearchParams, SearchContext, YoctoSearchQueryCompiler}

@Ignore
@ContextConfiguration(locations = Array("/context/search_car_ads.xml"))
class SearchCarAdTest extends AbstractJUnit4SpringContextTests with Matchers {

  @Autowired var shortCarAdSearchTemplate: YoctoSearchTemplateImpl[ShortCarAd] = _
  @Autowired var queryCompiler: YoctoSearchQueryCompiler = _

  private val filters: java.util.List[EntityField] = java.util.Arrays.asList[EntityField](
    CommonField.MARK_FIELD,
    CommonField.MODEL_FIELD,
    CommonField.SUPER_GENERATION_ID_FIELD,
    CommonField.VENDOR_FIELD,
    CommonField.MARK_MODEL_FIELD,
    CommonField.TECH_PARAM_ID_FIELD,
    CommonField.COMPLECTATION_ID_FIELD,
    CommonField.COMPLECTATION_NAME_FIELD,
    CarAdField.OFFER_CONFIGURATION_ID_FIELD
  )

  private def buildSearchContext = {
    val sc = new SearchConfiguration()
    sc.setLocale(AutoLocale.RU)
    sc.setSearchContext(new SearchContext)
    sc.getSearchContext.setVendorManager(
      new VendorManagerImpl(
        new Vendor(null, 0, "", java.util.Collections.emptySet[Region], java.util.Collections.emptySet[Region])
      )
    )

    sc
  }

  @Test
  def findAll(): Unit = {
    val holder = shortCarAdSearchTemplate.acquireIndexSearcherHolder()
    val res = shortCarAdSearchTemplate.search(QueryBuilder.select, holder)

    res.size should be > 0
  }

  @Test
  def findByMark(): Unit = {
    val sc = buildSearchContext
    sc.getParams.addParam(CarSearchParams.CATALOG_FILTER, "mark=BMW")

    val query = queryCompiler.compileQuery(sc, filters)
    val holder = shortCarAdSearchTemplate.acquireIndexSearcherHolder()
    val res = shortCarAdSearchTemplate.search(query, holder)

    res.size should be > 0
  }

  @Test
  def findByMarkModel(): Unit = {
    val sc = buildSearchContext
    sc.getParams.addParam(CarSearchParams.CATALOG_FILTER, "mark=BMW,model=3ER")

    val query = queryCompiler.compileQuery(sc, filters)
    val holder = shortCarAdSearchTemplate.acquireIndexSearcherHolder()
    val res = shortCarAdSearchTemplate.search(query, holder)

    res.size should be > 0
  }

}
