package ru.yandex.auto.searcher.search

import org.junit.{Ignore, Test}
import org.scalatest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests
import ru.yandex.auto.core.AutoLocale
import ru.yandex.auto.core.region.Region
import ru.yandex.auto.core.vendor.{Vendor, VendorManagerImpl}
import ru.yandex.auto.searcher.configuration.SearchConfiguration
import ru.yandex.auto.searcher.core.{CarSearchParams, CatalogFilter, SearchContext}

import java.util.Collections
import scala.collection.JavaConverters._

@Ignore
@ContextConfiguration(locations = Array("/context/search_catalog_cards.xml"))
class NewYoctoCatalogCardSearcherTest extends AbstractJUnit4SpringContextTests with Matchers {

  @Autowired var catalogSearcher: NewYoctoCatalogCardSearcher = _

  private def buildSearchContext = {
    val sc = new SearchConfiguration()
    sc.setLocale(AutoLocale.RU)
    sc.setSearchContext(new SearchContext)
    sc.getSearchContext.setVendorManager(
      new VendorManagerImpl(new Vendor(null, 0, "", Collections.emptySet[Region], Collections.emptySet[Region]))
    )

    sc
  }

  @Test def findCardsByCatalogFilter(): Unit = {
    val sc = buildSearchContext
    val (mark, model) = ("BMW", "3ER")
    sc.getParams
      .addParam(
        CarSearchParams.CATALOG_FILTER,
        s"mark=$mark,model=$model"
      )

    val cards = catalogSearcher.findCards(sc)

    val excludeMark = cards.asScala.filter(_.getMark.getCode != mark)
    val excludeModel = cards.asScala.filter(_.getModel.getCode != model)

    cards.size should be > 0
    Seq(excludeMark, excludeModel)
      .foreach(_.size shouldEqual 0)
  }

  @Test def findByCatalogFilterWithIds(): Unit = {
    val sc = buildSearchContext
    val (mark, model, techId, configId, complId) = ("BMW", "3ER", 2305578L, 2305571L, 2411796L)
    sc.getParams
      .addParam(
        CarSearchParams.CATALOG_FILTER,
        s"mark=$mark,model=$model,tech_param=$techId,complectation=$complId,configuration=$configId"
      )

    val cards = catalogSearcher.findCards(sc)

    val excludeMark = cards.asScala.filter(_.getMark.getCode != mark)
    val excludeModel = cards.asScala.filter(_.getModel.getCode != model)
    val excludeTech = cards.asScala.filter(_.getTechParameter.getId != techId)
    val excludeConfig = cards.asScala.filter(_.getConfiguration.getId != configId)
    val excludeCompl = cards.asScala.filter(_.getComplectation.getId != complId)

    cards.size should be > 0
    Seq(excludeMark, excludeModel, excludeTech, excludeConfig, excludeCompl)
      .foreach(_.size shouldEqual 0)
  }

  @Test def findByNegatedCatalogFilter(): Unit = {
    val sc = buildSearchContext
    val (mark, model, techId, configId, complId) = ("BMW", "3ER", 2305578L, 2305571L, 2411796L)
    sc.getParams
      .addParam(
        CarSearchParams.CATALOG_FILTER,
        s"mark=$mark,model=$model"
      )
    sc.getParams.addParam(
      CarSearchParams.CATALOG_FILTER,
      s"tech_param=$techId,${CatalogFilter.NEGATION}=true"
    )

    val cards = catalogSearcher.findCards(sc)

    val excludeMark = cards.asScala.filter(_.getMark.getCode != mark)
    val excludeModel = cards.asScala.filter(_.getModel.getCode != model)
    val withTech = cards.asScala.filter(_.getTechParameter.getId == techId)

    cards.size should be > 0
    Seq(excludeMark, excludeModel, withTech)
      .foreach(_.size shouldEqual 0)
  }

  @Test def findCardsByPrice(): Unit = {
    val sc = buildSearchContext
    val (from, to) = (1000, 1000000)
    sc.setPriceFromInRubles(from)
    sc.setPriceToInRubles(to)

    val cards = catalogSearcher.findCards(sc)

    val exclude = cards.asScala.filter { card =>
      val price = card.getPriceInRublesForRussia
      price < from || to < price
    }

    cards.size should be > 0
    exclude.size shouldEqual 0
  }

  @Test def findCardsHasPrice(): Unit = {
    val sc = buildSearchContext
    sc.setHasPrice(true)

    val cards = catalogSearcher.findCards(sc)

    val exclude = cards.asScala.filter(_.getHasPrice != true)

    cards.size should be > 0
    exclude.size shouldEqual 0
  }

  @Test def filterByConfiguration(): Unit = {
    val sc = buildSearchContext
    sc.getParams.addParam(CarSearchParams.CATALOG_FILTER, "mark=BMW")
    sc.setOnlyCatalogSearch(true)
    sc.setConfigurationIds(Seq[java.lang.Long](21398651L).asJava)
    sc.setExcludeConfigurationIds(Seq[java.lang.Long](21398651L).asJava)

    val cards = catalogSearcher.findCards(sc)

    cards.size shouldEqual 0
  }

  @Test def filterByConfiguration2(): Unit = {
    val sc = buildSearchContext
    sc.getParams.addParam(CarSearchParams.CATALOG_FILTER, "mark=BMW,configuration=21398651")
    sc.setOnlyCatalogSearch(true)

    val byIds = catalogSearcher.findCards(sc)

    sc.getParams.setParam(CarSearchParams.CATALOG_FILTER, "mark=BMW")
    sc.setExcludeConfigurationIds(Seq[java.lang.Long](21398651L).asJava)
    val excludeIds = catalogSearcher.findCards(sc)

    byIds.size should be > 0
    excludeIds.size should be > 0
    byIds.asScala.intersect(excludeIds.asScala).size shouldEqual 0
  }

  @Test def findByEngineType(): Unit = {
    val sc = buildSearchContext
    val etc = "GASOLINE"
    sc.setEngineTypes(Collections.singletonList(etc))

    val byConfig = catalogSearcher.findCards(sc)

    val exclude = byConfig.asScala.filter(_.getTechParameter.getEngineTypeCode != etc)
    exclude.size shouldBe 0
    byConfig.size should be > 0
  }

}
