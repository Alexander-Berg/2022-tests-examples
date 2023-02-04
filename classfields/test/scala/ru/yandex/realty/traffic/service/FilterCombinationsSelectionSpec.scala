package ru.yandex.realty.traffic.service

import org.junit.runner.RunWith
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.traffic.TestData
import ru.yandex.realty.traffic.logic.generator.FiltersCombinationStrategy
import ru.yandex.realty.urls.router.model.filter.FilterDeclaration
import zio.ZLayer
import zio.test._
import zio.test.junit._
import zio.test.Assertion._

@RunWith(classOf[ZTestJUnitRunner])
class FilterCombinationsSelectionSpec extends JUnitRunnableSpec {

  private def findFilter(name: String, isRegionFilter: Boolean = false): FilterDeclaration = {
    val result = TestData.filtersMap
      .getOrElse(
        FilterDeclaration.wrapName(name),
        throw new IllegalArgumentException(s"Broken spec! Filter with name `$name` not found!")
      )

    val actualIsRegionFilter = TestData.routerFilters.regionFilters.contains(result.name)
    if (isRegionFilter != actualIsRegionFilter) {
      throw new IllegalArgumentException(
        s"Broken spec! Filter with name `$name` expected isRegionFilter=$isRegionFilter, but found $actualIsRegionFilter!"
      )
    }

    result
  }

  private lazy val regionFilter1: FilterDeclaration = findFilter("loft", isRegionFilter = true)
  private lazy val regionFilter2: FilterDeclaration = findFilter("penthouse", isRegionFilter = true)
  private lazy val f1: FilterDeclaration = findFilter("panel")
  private lazy val f2: FilterDeclaration = findFilter("apartamenty")
  private lazy val f3: FilterDeclaration = findFilter("stalinskiy")

  private def combinationsAsStringsSeq(combs: Iterable[Seq[FilterDeclaration]]): Seq[String] =
    combs
      .map { x =>
        x.map(_.name.asInstanceOf[String]).sorted.mkString("&")
      }
      .toSeq
      .sorted

  private def specCase(caseName: String)(
    rgid: Long,
    filters: Seq[FilterDeclaration],
    strategy: FiltersCombinationStrategy,
    expected: Iterable[Seq[FilterDeclaration]]
  ) =
    testM(caseName) {
      FilterCombinationsSelection
        .selectCombinations(rgid, filters, strategy)
        .provideLayer {
          ZLayer.succeed(TestData.filtersProvider) >>> FilterCombinationsSelection.live
        }
        .map { res =>
          assert(combinationsAsStringsSeq(res))(hasSameElements(combinationsAsStringsSeq(expected)))
        }

    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("FilterCombinationsSelection")(
      specCase("should correctly return for AtMost2 for msk")(
        NodeRgid.MOSCOW,
        Seq(regionFilter1, regionFilter2, f1),
        FiltersCombinationStrategy.AtMostTwo,
        Iterable(
          Seq.empty,
          Seq(f1),
          Seq(regionFilter1),
          Seq(regionFilter2),
          Seq(f1, regionFilter1),
          Seq(f1, regionFilter2),
          Seq(regionFilter1, regionFilter2)
        )
      ),
      specCase("should correctly return for AtMost2 for kazan")(
        NodeRgid.KAZAN,
        Seq(regionFilter1, regionFilter2, f1, f2),
        FiltersCombinationStrategy.AtMostTwo,
        Iterable(
          Seq.empty,
          Seq(f1),
          Seq(f2),
          Seq(f1, f2)
        )
      ),
      specCase("should correctly return for AtMost1 for msk")(
        NodeRgid.MOSCOW,
        Seq(regionFilter1, regionFilter2, f1),
        FiltersCombinationStrategy.AtMostOne,
        Iterable(
          Seq.empty,
          Seq(f1),
          Seq(regionFilter1),
          Seq(regionFilter2)
        )
      ),
      specCase("should correctly return for AtMost1 for kazan")(
        NodeRgid.KAZAN,
        Seq(regionFilter1, regionFilter2, f1, f2),
        FiltersCombinationStrategy.AtMostOne,
        Iterable(
          Seq.empty,
          Seq(f1),
          Seq(f2)
        )
      ),
      specCase("should correctly return for must contains on simple filter when it exists")(
        NodeRgid.MOSCOW,
        Seq(f1, f2, f3),
        FiltersCombinationStrategy.MustIncludeAndAtMostTwo(f1.name),
        Iterable(
          Seq(f1),
          Seq(f1, f2),
          Seq(f1, f3)
        )
      ),
      specCase("should correctly return for must contains on simple filter when it does not exist")(
        NodeRgid.MOSCOW,
        Seq(f2, f3),
        FiltersCombinationStrategy.MustIncludeAndAtMostTwo(f1.name),
        Iterable()
      ),
      specCase("should return empty when result must include regionFilter but geo is not main")(
        NodeRgid.KAZAN,
        Seq(f1, f2, f3),
        FiltersCombinationStrategy.MustIncludeAndAtMostTwo(regionFilter1.name),
        Iterable()
      )
    )
}
