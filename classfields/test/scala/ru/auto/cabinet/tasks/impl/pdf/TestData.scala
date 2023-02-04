package ru.auto.cabinet.tasks.impl.pdf

import org.joda.time.LocalDate
import ru.auto.cabinet.tasks.impl.pdf.PairSides._

object TestData {
  val TestReportDate = LocalDate.parse("2017-06-30")

  val DealerOfferDurationsItems = List(
    SimpleItem(
      SimplePairKey("period.charts.days", LEFT, 4),
      10.347417840375586,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", LEFT, 2),
      6.638888888888889,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", LEFT, 3),
      8.12482853223594,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", LEFT, 1),
      7.363636363636363,
      AggregationTypes.Avg),
    SimpleItem(MultiItemKey("period.numSales", 4), 13.0, AggregationTypes.Sum),
    SimpleItem(MultiItemKey("period.numSales", 2), 9.0, AggregationTypes.Sum),
    SimpleItem(MultiItemKey("period.numSales", 3), 20.0, AggregationTypes.Sum),
    SimpleItem(MultiItemKey("period.numSales", 1), 1.0, AggregationTypes.Sum),
    SimpleItem(
      SimplePairKey("period.charts.days", LEFT, 4),
      10.12,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", LEFT, 2),
      6.525925925925926,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", LEFT, 3),
      7.705218617771509,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", LEFT, 1),
      7.6,
      AggregationTypes.Avg),
    SimpleItem(MultiItemKey("period.numSales", 4), 12.0, AggregationTypes.Sum),
    SimpleItem(MultiItemKey("period.numSales", 2), 10.0, AggregationTypes.Sum),
    SimpleItem(MultiItemKey("period.numSales", 3), 35.0, AggregationTypes.Sum),
    SimpleItem(MultiItemKey("period.numSales", 1), 0.0, AggregationTypes.Sum),
    SimpleItem(MultiItemKey("period.numSales", 4), 7.0, AggregationTypes.Sum),
    SimpleItem(MultiItemKey("period.numSales", 2), 9.0, AggregationTypes.Sum),
    SimpleItem(MultiItemKey("period.numSales", 3), 43.0, AggregationTypes.Sum),
    SimpleItem(MultiItemKey("period.numSales", 1), 0.0, AggregationTypes.Sum)
  )

  val DealerOfferCountItems = List(
    ChronologyItem(
      AnnotatedPairKey("sale.cars_used.charts.sales", LEFT, 21),
      LocalDate.parse("2017-06-21"),
      3066.0),
    ChronologyItem(
      AnnotatedPairKey("sale.cars_used.charts.sales", RIGHT, 21),
      LocalDate.parse("2017-06-21"),
      43.0),
    ChronologyItem(
      AnnotatedPairKey("sale.cars_used.charts.sales", LEFT, 20),
      LocalDate.parse("2017-06-20"),
      3088.0),
    ChronologyItem(
      AnnotatedPairKey("sale.cars_used.charts.sales", RIGHT, 20),
      LocalDate.parse("2017-06-20"),
      57.0),
    ChronologyItem(
      AnnotatedPairKey("sale.cars_used.charts.sales", LEFT, 19),
      LocalDate.parse("2017-06-19"),
      3093.0),
    ChronologyItem(
      AnnotatedPairKey("sale.cars_used.charts.sales", RIGHT, 19),
      LocalDate.parse("2017-06-19"),
      59.0)
  )

  val TotalDealerOfferDurationsItems = List(
    SimpleItem(
      SimplePairKey("period.charts.days", RIGHT, 4),
      5.5655608214849925,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", RIGHT, 2),
      8.013461538461538,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", RIGHT, 3),
      4.5677105579932435,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", RIGHT, 1),
      7.951404185022026,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", RIGHT, 4),
      5.397142904492799,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", RIGHT, 2),
      7.8913470573161355,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", RIGHT, 3),
      4.392858212157806,
      AggregationTypes.Avg),
    SimpleItem(
      SimplePairKey("period.charts.days", RIGHT, 1),
      7.800264162019372,
      AggregationTypes.Avg)
  )

  val ViewWithTeleponyCallsItems = List(
    ChronologyItem(
      AnnotatedSingleKey("call.charts.views", 21),
      LocalDate.parse("2017-06-21"),
      1694.0),
    ChronologyItem(
      AnnotatedSingleKey("call.charts.views", 21),
      LocalDate.parse("2017-06-21"),
      40.0),
    ChronologyItem(
      AnnotatedSingleKey("call.charts.views", 20),
      LocalDate.parse("2017-06-20"),
      2.0),
    ChronologyItem(
      AnnotatedSingleKey("call.charts.views", 20),
      LocalDate.parse("2017-06-20"),
      3.0),
    ChronologyItem(
      AnnotatedSingleKey("call.charts.views", 20),
      LocalDate.parse("2017-06-20"),
      1.0),
    ChronologyItem(
      AnnotatedSingleKey("call.charts.views", 20),
      LocalDate.parse("2017-06-20"),
      1.0),
    ChronologyItem(
      AnnotatedSingleKey("call.charts.views", 20),
      LocalDate.parse("2017-06-20"),
      2.0),
    ChronologyItem(
      AnnotatedSingleKey("call.charts.views", 19),
      LocalDate.parse("2017-06-19"),
      5.0),
    ChronologyItem(
      AnnotatedSingleKey("call.charts.views", 19),
      LocalDate.parse("2017-06-19"),
      1.0),
    ChronologyItem(
      AnnotatedSingleKey("call.charts.views", 19),
      LocalDate.parse("2017-06-19"),
      2.0),
    ChronologyItem(
      AnnotatedPairKey("call.charts.calls", LEFT, 21),
      LocalDate.parse("2017-06-21"),
      171.0),
    ChronologyItem(
      AnnotatedPairKey("call.charts.calls", LEFT, 20),
      LocalDate.parse("2017-06-20"),
      180.0),
    ChronologyItem(
      AnnotatedPairKey("call.charts.calls", RIGHT, 20),
      LocalDate.parse("2017-06-20"),
      2.0),
    ChronologyItem(
      AnnotatedPairKey("call.charts.calls", LEFT, 19),
      LocalDate.parse("2017-06-19"),
      159.0),
    ChronologyItem(
      AnnotatedPairKey("call.charts.calls", RIGHT, 19),
      LocalDate.parse("2017-06-19"),
      5.0)
  )

  val ActivationAmountItems = List(
    ChronologyItem(
      AnnotatedPairKey("total.charts.cost", LEFT, 21),
      LocalDate.parse("2017-06-21"),
      100.0),
    ChronologyItem(
      AnnotatedPairKey("total.charts.cost", LEFT, 21),
      LocalDate.parse("2017-06-21"),
      50.0),
    ChronologyItem(
      AnnotatedPairKey("total.charts.cost", RIGHT, 21),
      LocalDate.parse("2017-06-21"),
      20.0),
    ChronologyItem(
      AnnotatedPairKey("total.charts.cost", LEFT, 20),
      LocalDate.parse("2017-06-20"),
      1.4269e5),
    ChronologyItem(
      AnnotatedPairKey("total.charts.cost", RIGHT, 20),
      LocalDate.parse("2017-06-20"),
      50.0),
    ChronologyItem(
      AnnotatedPairKey("total.charts.cost", LEFT, 19),
      LocalDate.parse("2017-06-19"),
      1.4434e5),
    ChronologyItem(
      AnnotatedPairKey("total.charts.cost", RIGHT, 19),
      LocalDate.parse("2017-06-19"),
      30),
    ChronologyItem(
      AnnotatedPairKey("total.charts.cost", RIGHT, 19),
      LocalDate.parse("2017-06-19"),
      50)
  )

  val RevokedOfferItems = List(
    ChronologyItem(
      MultiItemKey("period.steps", 3),
      LocalDate.parse("2017-06-21"),
      32.0,
      AggregationTypes.MaxDate),
    ChronologyItem(
      MultiItemKey("period.steps", 3),
      LocalDate.parse("2017-06-20"),
      20.0,
      AggregationTypes.MaxDate),
    ChronologyItem(
      MultiItemKey("period.steps", 3),
      LocalDate.parse("2017-06-19"),
      20.0,
      AggregationTypes.MaxDate)
  )

  val DealerOffersQualityItems = List(
    ChronologyItem(
      MultiItemKey("sale.cars_used.charts.quality", 0),
      LocalDate.parse("2017-06-21"),
      10),
    ChronologyItem(
      MultiItemKey("sale.cars_used.charts.quality", 1),
      LocalDate.parse("2017-06-21"),
      30),
    ChronologyItem(
      MultiItemKey("sale.cars_used.charts.quality", 2),
      LocalDate.parse("2017-06-21"),
      50)
  )
}
