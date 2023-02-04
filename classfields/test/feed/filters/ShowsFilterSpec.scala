package ru.yandex.vertis.general.wizard.scheduler.feed.filters

import ru.yandex.vertis.general.wizard.Generators.stockOfferWithCtrGen
import ru.yandex.vertis.general.wizard.model.StockOfferWithCtr
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, _}
import zio.test.environment.TestEnvironment

object ShowsFilterSpec extends DefaultRunnableSpec {
  private val nonNegativeIntGen = Gen.int(0, Integer.MAX_VALUE)

  private def propOfferNotFilteredWhenParamIsNotPresent(stockOfferWithCtr: StockOfferWithCtr) =
    for {
      actual <- ShowsFilter(None)(stockOfferWithCtr)
    } yield assert(actual)(equalTo(true))

  private def propTooManyShowsOfferFiltered(maxNumShows: Int, stockOfferWithCtr: StockOfferWithCtr) =
    for {
      actual <- ShowsFilter(Some(maxNumShows))(stockOfferWithCtr)
      expected = stockOfferWithCtr.shows.forall(_ <= maxNumShows)
    } yield assert(actual)(equalTo(expected))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ShowsFilter")(
      testM("When there is no maybeMaxNumShows no filtering should be performed") {
        checkM(stockOfferWithCtrGen)(propOfferNotFilteredWhenParamIsNotPresent)
      },
      testM(
        """When there is a positive maybeMaxNumShows, only offers which don't have shows or have less shows than
          |maybeMaxNumShows are left""".stripMargin
      ) {
        checkM(nonNegativeIntGen, stockOfferWithCtrGen)(propTooManyShowsOfferFiltered)
      }
    )
}
