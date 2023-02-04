package ru.auto.cabinet.tasks.impl.pdf

trait PagesSpec {

  import TestData._
  import ru.auto.cabinet.tasks.impl.pdf.PairSides._

  private[pdf] def asAnnotatedPairs(items: List[ChronologyItem]) =
    items
      .groupBy(_.key.key)
      .map { case (pageKey, is) =>
        val rows = is.groupBy(i => i.dt.dayOfMonth().get)
        pageKey -> Range
          .inclusive(1, TestReportDate.dayOfMonth().getMaximumValue)
          .map { row =>
            row -> rows
              .get(row)
              .map { vs =>
                val sums = vs.groupBy(_.key.asInstanceOf[PairKey].side).map {
                  case (subk, col) =>
                    subk -> col.map(_.value).sum
                }
                List(sums.getOrElse(LEFT, 0.0), sums.getOrElse(RIGHT, 0.0))
              }
              .getOrElse(List(0.0, 0.0))
          }
          .toList
          .sortBy(_._1)
          .map { case (k, v) => k.toString :: v }
      }

  private[pdf] def asAnnotatedSingles(items: List[ChronologyItem]) =
    items
      .groupBy(_.key.key)
      .map { case (pageKey, is) =>
        val rows = is.groupBy(i => i.dt.dayOfMonth().get)
        pageKey -> Range
          .inclusive(1, TestReportDate.dayOfMonth().getMaximumValue)
          .map { row =>
            row -> rows.get(row).map(_.map(_.value).sum)
          }
          .toList
          .sortBy(_._1)
          .map { case (k, v) => List[Any](k.toString, v.getOrElse(0d)) }
      }

  private[pdf] def asPairs(items: List[SimpleItem]) =
    items
      .groupBy(_.key.key)
      .map { case (pageKey, is) =>
        val rows = is.groupBy(_.key.asInstanceOf[SimplePairKey].rowNum)
        pageKey -> Range
          .inclusive(1, rows.keys.size)
          .map { row =>
            row -> rows
              .get(row)
              .map { vs =>
                val sums = vs.groupBy(_.key.asInstanceOf[PairKey].side).map {
                  case (subk, col) =>
                    subk -> col.map(_.value).sum / col.size
                }
                List(sums.getOrElse(LEFT, 0.0), sums.getOrElse(RIGHT, 0.0))
              }
              .getOrElse(List(0.0, 0.0))
          }
          .toList
          .sortBy(_._1)
          .map(_._2)
      }

  private[pdf] def asTuples(items: List[SimpleItem]) =
    items
      .groupBy(_.key.key)
      .map { case (pageKey, is) =>
        pageKey -> is
          .groupBy(_.key.asInstanceOf[MultiItemKey].itemNum)
          .map { case (row, vs) =>
            row -> vs.map(_.value).sum
          }
          .toList
          .sortBy(_._1)
          .map(_._2)
      }

}
