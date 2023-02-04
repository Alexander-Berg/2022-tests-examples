package ru.yandex.realty.searcher.response.builders.inexact

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.search.inexact.{BuiltYearInexact, FloorInexact, InexactMatching}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.util.Range
import ru.yandex.vertis.protobuf.ProtoInstanceProvider._
import ProtoHelper._
import scala.collection.JavaConverters._
import org.scalatest.prop.TableDrivenPropertyChecks._

@RunWith(classOf[JUnitRunner])
class FloorInexactEnricherSpec extends WordSpec with Matchers {

  val sq = new SearchQuery()
  sq.setFloor(Range.create(6f, 8f))

  val absentValue =
    """{
      |  "absent": {}
      |}""".stripMargin
      .toProto[FloorInexact]

  def expectedInexact(value: Int, diff: Int, trend: String): FloorInexact =
    s"""{
       |  "inexact": {
       |    "value": $value,
       |    "diff":  $diff,
       |    "trend": "$trend"
       |  }
       |}""".stripMargin
      .toProto[FloorInexact]

  "FloorInexactEnricher" should {

    "checkAndEnrich should set floorInexact according floor value" in {

      val testData =
        Table(
          ("floors", "inexatc", "callNum"),
          (List(7), FloorInexact.getDefaultInstance, 2),
          (List.empty, absentValue, 2),
          (List(9), expectedInexact(9, 1, "MORE"), 3),
          (List(4), expectedInexact(4, 2, "LESS"), 3)
        )

      forAll(testData) { (floors: List[Int], inexact: FloorInexact, callNum: Int) =>
        new OfferBuilderContextFixture {
          val enricher = FloorInexactEnricher.provideIfApply(sq).get
          val defaultInstance = FloorInexact.getDefaultInstance
          private val offerFloors = floors.map(i => java.lang.Integer.valueOf(i))
          (dummyOfferBuilderContext.apartmentInfo.getFloors _).expects().returning(offerFloors.asJava).repeat(callNum)
          val builder = enricher.checkAndEnrich(InexactMatching.newBuilder, dummyOfferBuilderContext)

          builder.build.getFloor shouldBe inexact
        }
      }

    }

    "checkAndEnrich should set absence when apartmentInfo is null" in new OfferBuilderContextFixture {
      val enricher = FloorInexactEnricher.provideIfApply(sq).get
      private val nullApartmentInfoContext: OfferBuilderContext = dummyOfferBuilderContext.copy(apartmentInfo = null)
      val builder = enricher.checkAndEnrich(InexactMatching.newBuilder, nullApartmentInfoContext)
      builder.build.getFloor shouldBe absentValue
    }

    "provideIfApply" in {

      val res = FloorInexactEnricher.provideIfApply(sq)
      res should not be empty

    }

  }
}
