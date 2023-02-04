package ru.yandex.realty.managers.fastlink

import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.graph.RegionGraph

import scala.concurrent.Await
import scala.concurrent.duration._

class SimilarQuerySearcherImplTest extends WordSpec with Matchers with MockFactory {

  import SimilarQuerySearcherImplTest._

  "SimilarQuerySearcherImpl" should {

    val regionGraph = mock[RegionGraph]
    val searcher = new SimilarQuerySearcherImpl(regionGraph)

    "floorExceptFirst rule applying as a part of similar query search process" in {
      val simpleQuery = "/kupit/kvartira".toQuery
      val res = searcher.findSimilarQuery(simpleQuery)
      res should contain("/kupit/kvartira?floorExceptFirst=YES".toQuery)
    }
  }
}

object SimilarQuerySearcherImplTest {

  implicit class QueryParser(value: String) {

    def toQuery: Query = {
      val eventualStringToStrings = TestHumanReadableUrlFormat.toParamMap(value)
      val paramMap = Await.result(eventualStringToStrings, 10 seconds)
      QueryFormatImpl.toQuery(paramMap)
    }
  }

}
