package ru.yandex.realty.search.clauses.impl

import org.apache.lucene.search.{NumericRangeQuery, Query}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.search.SearchQuery
import ru.yandex.realty.util.{Range => SearchRange}

@RunWith(classOf[JUnitRunner])
class FloorsTotalClauseSpec extends SpecBase {
  val min = 5
  val max = 10

  "SearchClauseBuilder " should {
    " have min and max values if it defined in range" in {
      val floorsTotal: Option[SearchRange] = Some(SearchRange.create(min.floatValue(), max.floatValue()))
      val searchQuery = SearchQuery(floorsTotal = floorsTotal)
      val luceneQuery: Query = FloorsTotalClause.toLuceneQuery(searchQuery).get
      val source = luceneQuery.asInstanceOf[NumericRangeQuery[Integer]]
      source.getMin shouldBe min
      source.getMax shouldBe max
    }
    " have only min if max not defined " in {
      val floorsTotal: Option[SearchRange] = Some(SearchRange.create(min.floatValue(), null))
      val searchQuery = SearchQuery(floorsTotal = floorsTotal)
      val luceneQuery: Query = FloorsTotalClause.toLuceneQuery(searchQuery).get
      val source = luceneQuery.asInstanceOf[NumericRangeQuery[Integer]]
      source.getMax shouldBe null
      source.getMin shouldBe min
    }
    " return None if range is not defined " in {
      val emptyQuery = SearchQuery()
      val luceneQuery = FloorsTotalClause.toLuceneQuery(emptyQuery)
      luceneQuery shouldBe None
    }
  }

}
