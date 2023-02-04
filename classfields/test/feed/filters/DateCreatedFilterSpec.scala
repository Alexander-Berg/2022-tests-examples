package ru.yandex.vertis.general.wizard.scheduler.feed.filters

import ru.yandex.vertis.general.wizard.Generators.stockOfferWithCtrGen
import ru.yandex.vertis.general.wizard.model.{StockOffer, StockOfferWithCtr}
import zio.clock.Clock
import zio.{Task, ZIO}
import zio.test.environment.TestEnvironment
import zio.test._
import zio.test.Assertion._
import zio.test.magnolia._

import java.time.Duration
import scala.concurrent.duration.DurationInt
import scala.math.Ordered.orderingToOrdered

object DateCreatedFilterSpec extends DefaultRunnableSpec {

  private def propFilteredOffersCreatedLessThan2DaysAgo(offer: StockOfferWithCtr) = {
    val createdAtDelta = Duration.ofMillis(2.days.toMillis)

    for {
      currentInstant <- ZIO.serviceWith[Clock.Service](_.instant)
      filterResult <- DateCreatedFilter(currentInstant)(offer)
      latestTimestamp = currentInstant.minus(createdAtDelta)
      expectedResult = offer.stockOffer.createdAt < latestTimestamp
    } yield assert(filterResult)(equalTo(expectedResult))
  }

  override def spec: ZSpec[TestEnvironment, Any] = suite("DateCreatedFilter")(
    testM("Filtered offers are older than 2 days") {
      checkM(stockOfferWithCtrGen)(propFilteredOffersCreatedLessThan2DaysAgo)
    }
  )
}
