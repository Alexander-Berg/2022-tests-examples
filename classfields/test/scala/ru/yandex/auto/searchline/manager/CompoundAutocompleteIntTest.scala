package ru.yandex.auto.searchline.manager

import org.scalatest.{FreeSpecLike, Matchers}
import org.springframework.test.AbstractDependencyInjectionSpringContextTests
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.SearchlineModel
import ru.yandex.auto.searchline.api.directive.DebugParams
import ru.yandex.auto.searchline.model.{SearchQuery, Suggest}

import scala.collection.JavaConverters._

/**
  * @author pnaydenov
  */
class CompoundAutocompleteIntTest extends AbstractDependencyInjectionSpringContextTests
  with FreeSpecLike with Matchers {
  private val ctx = loadContextLocations(Array("searchline-core-test.xml"))
  private val suggestManager = ctx.getBean("allCategorySuggestManager").asInstanceOf[CompoundSuggestManager]
  private val UNRECOGNIZED_FRAGMENT = SearchlineModel.SearchSuggestResponse.Token.Type.UNRECOGNIZED_FRAGMENT.getNumber

  private def testQuery(testName: String, category: Option[Category] = None)
                       (testFun: PartialFunction[List[Suggest], Unit]): Unit =
    registerTest(testName){
      val suggest = suggestManager.suggestFromQuery(SearchQuery(testName, testName.length, category,
        DebugParams.empty)).toList
      testFun.apply(suggest)
    }

  private def testQuerySequence(testName: String, category: Option[Category] = None)
                               (testFun: Function2[String, List[Suggest], Unit]): Unit = {
    require(testName.contains('|'))
    registerTest(testName){
      val idx = testName.indexOf('|')
      for (i <- ((idx + 1) to testName.length)) {
        val query = testName.take(idx) + testName.substring(idx + 1, i)
        val suggest = suggestManager.suggestFromQuery(SearchQuery(query, query.length, category,
          DebugParams.empty)).toList
        testFun.apply(query, suggest)
      }
    }
  }

  private implicit class RichSuggest(list: List[Suggest]) {
    def toMarkModels(): Seq[String] = list.flatMap(_.params.getMarkModelNameplateList.asScala).distinct
  }

  "FORD FOCUS SET" - {
    testQuerySequence("ф|о") { (query, suggest) =>
      val marks = suggest.toMarkModels
      marks should contain ("FORD")
      marks should contain ("VOLKSWAGEN")
      val fordOrVolkswagen =
        suggest.filter(s => s.params.getMarkModelNameplate(0) == "FORD" ||
          s.params.getMarkModelNameplate(0) == "VOLKSWAGEN")
      for (s <- fordOrVolkswagen) {
        s.autocomplete should not be empty
      }
      suggest.find(_.params.getMarkModelNameplate(0) == "FORD").get.autocomplete.get.query shouldEqual "форд"
      suggest.find(_.params.getMarkModelNameplate(0) == "VOLKSWAGEN").get.autocomplete.get.query shouldEqual "фольксваген"
    }

    // TODO: temporary test fix, trucks & moto data type does not contains cyrillicName yet
    testQuery("фор", category = Some(Category.CARS)) { case suggest =>
      val marks = suggest.toMarkModels
      marks should contain ("FORD")
      marks should not contain ("VOLKSWAGEN")
      for (s <- suggest) {
        s.autocomplete shouldNot be (empty)
      }
      suggest.find(_.params.getMarkModelNameplate(0) == "FORD").get.autocomplete.get.query shouldEqual "форд"
    }

    testQuery("форд") { case suggest =>
      val marks = suggest.toMarkModels
      marks should contain ("FORD")
      marks shouldNot contain ("VOLKSWAGEN")
      for (s <- suggest) {
        s.autocomplete shouldBe empty
      }
    }

    "test 'форд '" in {
      val query = "форд "
      val suggest = suggestManager.suggestFromQuery(SearchQuery(query, query.length, None, DebugParams.empty)).toList
      val marks = suggest.toMarkModels
      marks should contain ("FORD")
      marks shouldNot contain ("VOLKSWAGEN")
      for (s <- suggest) {
        s.autocomplete shouldBe empty
      }
    }

    testQuerySequence("форд ф|оку") { (query, suggest) =>
      val marks = suggest.toMarkModels
      marks.toSet should contain ("FORD#FOCUS")
      suggest.flatMap(_.autocomplete.map(_.query)) should contain ("форд фокус")
    }

    testQuery("форд фокус") { case suggest =>
      val marks = suggest.toMarkModels
      marks.toSet shouldEqual Set("FORD#FOCUS")
      for (s <- suggest) {
        s.autocomplete shouldBe empty
      }
    }

    testQuery("форе") { case suggest =>
      val marks = suggest.toMarkModels
      marks should (contain("SUBARU#FORESTER") and contain("SUZUKI#FORENZA"))
    }

    testQuerySequence("фок|ус") { (_, suggest) =>
      suggest.toMarkModels should contain("FORD#FOCUS")
    }
  }

  "Statistic in use" - {
    testQuery("ла") { case suggest =>
      val marks = suggest.toMarkModels
      marks should (contain("VAZ") and contain("RENAULT#LAGUNA"))
      val ladaIndex = marks.indexOf("VAZ")
      val renaultIndex = marks.indexOf("RENAULT#LAGUNA")
      assert(ladaIndex < renaultIndex, "Lada should be more popular than Renault Laguna")
    }
  }
}
