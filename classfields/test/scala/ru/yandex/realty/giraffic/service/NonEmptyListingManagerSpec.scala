package ru.yandex.realty.giraffic.service

import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV
import org.junit.runner.RunWith
import ru.yandex.realty.giraffic.model.links.NonEmptyListingInfo
import ru.yandex.realty.giraffic.service.mock.TestData
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.urls.router.model.ViewType
import zio.test.Assertion._
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}
import zio.{Task, ZLayer}

@RunWith(classOf[ZTestJUnitRunner])
class NonEmptyListingManagerSpec extends JUnitRunnableSpec {

  import TestData._

  private def findNonEmptyListings(urls: KnownUrl*): Task[Seq[NonEmptyListingInfo]] = {
    val callLayer = (countsLive ++ routerLive ++ TestData.actionObserver >>> NonEmptyListingsManager.live) ++
      ZLayer.succeed(Traced.empty)

    NonEmptyListingsManager
      .findNonEmptyListings(urls.map(_.request), ViewType.Desktop)
      .provideLayer(callLayer)
  }

  private def nonEmptyListingInfoFrom(known: KnownUrl) =
    NonEmptyListingInfo(
      urlPath = known.translatedPath.get,
      count = refineV[Positive](known.offersCount).right.get,
      listingRequest = known.request
    )

  override def spec: ZSpec[Environment, Failure] =
    suite("NonEmptyListingManager")(
      testM("should provider listing when url translated and has offers") {
        for {
          res <- findNonEmptyListings(MoscowSellApartmentKnown, CanonicalWithoutOffersUrl)
        } yield assert(res)(hasSameElements(Seq(nonEmptyListingInfoFrom(MoscowSellApartmentKnown))))
      },
      testM("shouldn't duplicate urls in response") {
        for {
          res <- findNonEmptyListings(MoscowSellApartmentKnown, MoscowSellApartmentKnown, CanonicalWithoutOffersUrl)
        } yield assert(res)(hasSameElements(Seq(nonEmptyListingInfoFrom(MoscowSellApartmentKnown))))
      },
      testM("don't provide urls when no offers or no canonical") {
        for {
          res <- findNonEmptyListings(NoOffersNoCanonicalUrl, CanonicalWithoutOffersUrl, WithOffersAndNoCanonicalUrl)
        } yield assert(res)(isEmpty)
      }
    )
}
