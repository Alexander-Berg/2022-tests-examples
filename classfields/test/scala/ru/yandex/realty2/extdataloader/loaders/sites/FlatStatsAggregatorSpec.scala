package ru.yandex.realty2.extdataloader.loaders.sites

import org.scalatest.OneInstancePerTest
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.Rooms
import ru.yandex.realty.model.sites.SaleStatus.NOT_ON_SALE
import ru.yandex.realty.model.sites.SimpleSiteStatisticsResult.EMPTY_RANGE
import ru.yandex.realty.model.sites.range.RangeImpl
import ru.yandex.realty.model.sites.{AgregateFlatInfo, SaleStatus, SimpleSiteStatisticsResult}

class FlatStatsAggregatorSpec extends SpecBase with OneInstancePerTest {

  private val EMPTY_SITE_STATISTICS =
    new SimpleSiteStatisticsResult(EMPTY_RANGE, EMPTY_RANGE, EMPTY_RANGE, NOT_ON_SALE)

  private val testCases: Seq[TestCase] = Seq(
    TestCase(
      description = "empty list",
      flats = Seq.empty,
      expectedResult = EMPTY_SITE_STATISTICS
    ),
    TestCase(
      description = "empty `from` ranges from one flat",
      flats = Seq(
        new AgregateFlatInfo(
          Rooms._1,
          SaleStatus.ON_SALE,
          null,
          1.0f,
          null,
          10.0f
        )
      ),
      expectedResult = new SimpleSiteStatisticsResult(
        RangeImpl.create(null, 1.0f),
        RangeImpl.create(null, 10.0f),
        RangeImpl.create(10.0f, 10.0f),
        SaleStatus.ON_SALE
      )
    ),
    TestCase(
      description = "empty `from` ranges from several flats",
      flats = Seq(
        new AgregateFlatInfo(
          Rooms._1,
          SaleStatus.ON_SALE,
          null,
          1.0f,
          null,
          10.0f
        ),
        new AgregateFlatInfo(
          Rooms._1,
          SaleStatus.ON_SALE,
          null,
          2.0f,
          null,
          15.0f
        )
      ),
      expectedResult = new SimpleSiteStatisticsResult(
        RangeImpl.create(null, 2.0f),
        RangeImpl.create(null, 15.0f),
        RangeImpl.create(7.5f, 10.0f),
        SaleStatus.ON_SALE
      )
    ),
    TestCase(
      description = "empty `to` range from one flat",
      flats = Seq(
        new AgregateFlatInfo(
          Rooms._1,
          SaleStatus.ON_SALE,
          1.0f,
          null,
          10.0f,
          null
        )
      ),
      expectedResult = new SimpleSiteStatisticsResult(
        RangeImpl.create(1.0f, null),
        RangeImpl.create(10.0f, null),
        RangeImpl.create(10.0f, 10.0f),
        SaleStatus.ON_SALE
      )
    ),
    TestCase(
      description = "empty `to` ranges from several flats",
      flats = Seq(
        new AgregateFlatInfo(
          Rooms._1,
          SaleStatus.ON_SALE,
          1.0f,
          null,
          10.0f,
          null
        ),
        new AgregateFlatInfo(
          Rooms._1,
          SaleStatus.ON_SALE,
          2.0f,
          null,
          15.0f,
          null
        )
      ),
      expectedResult = new SimpleSiteStatisticsResult(
        RangeImpl.create(1.0f, null),
        RangeImpl.create(10.0f, null),
        RangeImpl.create(7.5f, 10.0f),
        SaleStatus.ON_SALE
      )
    ),
    TestCase(
      description = "compute `from` and `to` of ranges",
      flats = Seq(
        new AgregateFlatInfo(
          Rooms._1,
          SaleStatus.ON_SALE,
          null,
          5.0f,
          10.0f,
          null
        ),
        new AgregateFlatInfo(
          Rooms._1,
          SaleStatus.ON_SALE,
          2.0f,
          null,
          null,
          20.0f
        )
      ),
      expectedResult = new SimpleSiteStatisticsResult(
        RangeImpl.create(2.0f, 5.0f),
        RangeImpl.create(10.0f, 20.0f),
        RangeImpl.create(2.0f, 10.0f),
        SaleStatus.ON_SALE
      )
    )
  )

  "FlatStatsAggregator" should {
    testCases.foreach {
      case TestCase(description, flats, expectedResult) =>
        description in {
          val aggregator = new FlatStatsAggregator
          flats.foreach(aggregator.updateWithFlat)
          aggregator.toSimpleStats shouldEqual expectedResult
        }
    }
  }

  case class TestCase(description: String, flats: Seq[AgregateFlatInfo], expectedResult: SimpleSiteStatisticsResult)

}
