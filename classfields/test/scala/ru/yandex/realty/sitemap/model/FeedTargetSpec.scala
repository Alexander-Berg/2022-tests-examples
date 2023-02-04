package ru.yandex.realty.sitemap.model

import zio.test._
import zio.test.junit.JUnitRunnableSpec

class FeedTargetSpec extends JUnitRunnableSpec {

  private val Exclude: Set[FeedTarget] = Set(
    FeedTarget.YandexRentStandApartSitemap
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("FeedTarget") {
      testM("every target should has 'sitemap_' prefix") {
        checkAll(Gen.fromIterable(FeedTarget.values.filterNot(Exclude.contains))) { target =>
          assertTrue(target.entryName.startsWith("sitemap_"))
        }
      }
    }
}
