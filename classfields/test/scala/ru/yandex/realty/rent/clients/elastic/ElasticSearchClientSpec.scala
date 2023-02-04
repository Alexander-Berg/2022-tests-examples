package ru.yandex.realty.rent.clients.elastic

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.SearchHit
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import ru.yandex.realty.tracing.Traced

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class ElasticSearchClientSpec extends ElasticSearchSpecBase with Matchers with BeforeAndAfter {
  private val IndexName = "testindex"

  before {
    val createIndexResponse = elasticSearchClient.createIndex {
      createIndex(IndexName)
    }(Traced.empty).futureValue
    createIndexResponse.acknowledged should be(true)
  }

  after {
    val deleteIndexResponse = elasticSearchClient.deleteIndex {
      deleteIndex(IndexName)
    }(Traced.empty).futureValue
    deleteIndexResponse.acknowledged should be(true)
  }

  "ElasticSearchClient" should {
    "perform search on empty index" in {
      val result = elasticSearchClient.search {
        search(IndexName)
          .query(matchAllQuery())
          .limit(1)
      }(Traced.empty).futureValue
      result.totalHits should be(0L)
    }

    "search document by indexed field" in {
      val indexResult = elasticSearchClient.index {
        indexInto(IndexName)
          .id("20211109")
          .fields(
            "year" -> 2021,
            "month" -> 11,
            "day" -> 9,
            "foo" -> "bar"
          )
          .copy(refresh = Some(RefreshPolicy.Immediate))
      }(Traced.empty).futureValue
      indexResult.index should be(IndexName)
      indexResult.id should be("20211109")

      val barResults = elasticSearchClient.search {
        search(IndexName)
          .query(termQuery("foo", "bar"))
          .fetchSource(true)
          .limit(10)
      }(Traced.empty).futureValue
      barResults.totalHits should be(1L)
      val hit = barResults.hits.hits(0)
      hit.id should be("20211109")
      hit match {
        case h: SearchHit =>
          h.sourceFieldOpt("foo") should be(Some("bar"))
          h.sourceFieldOpt("day") should be(Some(9))
      }

      val bazResults = elasticSearchClient.search {
        search(IndexName)
          .query(termQuery("foo", "baz"))
          .fetchSource(true)
          .limit(10)
      }(Traced.empty).futureValue
      bazResults.totalHits should be(0L)
    }
  }
}
