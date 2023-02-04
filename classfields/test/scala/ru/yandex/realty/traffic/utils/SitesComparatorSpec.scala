package ru.yandex.realty.traffic.utils

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.model.sites.{Site, SiteStatistics}
import ru.yandex.realty.sites.SiteStatisticsStorage
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class SitesComparatorSpec extends WordSpec with Matchers {

  private case class SiteInfo(
    hasTrusted: Boolean,
    hasPlans: Boolean,
    hasUntrusted: Boolean,
    relAdded: Option[Double]
  ) {

    def stat: SiteStatistics = {
      val res = new SiteStatistics

      res.setHasTrustedOffers(hasTrusted)
      res.setOffersWithPlans(if (hasPlans) 1 else 0)
      res.setHasUntrustedOffers(hasUntrusted)
      res
    }
  }

  private case class TestCase(v1: SiteInfo, v2: SiteInfo, expected: Int)

  //noinspection NameBooleanParameters
  private val cases: Seq[TestCase] =
    Seq(
      TestCase(SiteInfo(true, false, true, None), SiteInfo(false, false, false, None), -1),
      TestCase(SiteInfo(false, false, false, None), SiteInfo(false, false, false, None), 0),
      TestCase(SiteInfo(false, true, true, None), SiteInfo(false, false, false, Some(0.1)), 0),
      TestCase(SiteInfo(false, false, true, None), SiteInfo(false, false, true, Some(0.1)), 1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(false, false, true, Some(0.1)), -1),
      TestCase(SiteInfo(false, true, true, Some(0.1)), SiteInfo(false, false, true, Some(0.1)), 0),
      TestCase(SiteInfo(false, true, true, None), SiteInfo(false, true, false, None), -1),
      TestCase(SiteInfo(true, false, true, None), SiteInfo(true, false, true, None), 0),
      TestCase(SiteInfo(false, true, false, None), SiteInfo(false, false, false, None), 0),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(true, true, true, Some(0.1)), 1),
      TestCase(SiteInfo(true, false, false, None), SiteInfo(false, false, false, Some(0.1)), 0),
      TestCase(SiteInfo(false, true, false, Some(0.1)), SiteInfo(false, true, false, Some(0.1)), 0),
      TestCase(SiteInfo(false, true, false, Some(0.1)), SiteInfo(false, false, false, Some(0.1)), 0),
      TestCase(SiteInfo(false, true, true, None), SiteInfo(false, false, false, None), -1),
      TestCase(SiteInfo(false, true, false, None), SiteInfo(false, true, false, None), 0),
      TestCase(SiteInfo(false, false, true, None), SiteInfo(false, false, false, Some(0.1)), 0),
      TestCase(SiteInfo(true, true, true, Some(0.1)), SiteInfo(true, true, true, Some(0.1)), 0),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(false, false, true, None), -1),
      TestCase(SiteInfo(true, true, true, Some(0.1)), SiteInfo(true, true, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(false, true, false, None), -1),
      TestCase(SiteInfo(false, true, false, None), SiteInfo(false, true, false, Some(0.1)), 1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(false, true, true, None), -1),
      TestCase(SiteInfo(true, false, false, Some(0.1)), SiteInfo(true, false, false, Some(0.1)), 0),
      TestCase(SiteInfo(true, false, true, None), SiteInfo(false, false, true, Some(0.1)), 0),
      TestCase(SiteInfo(true, false, false, None), SiteInfo(false, false, false, None), -1),
      TestCase(SiteInfo(true, true, false, Some(0.1)), SiteInfo(false, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, false, true, None), SiteInfo(false, false, true, None), -1),
      TestCase(SiteInfo(false, true, true, None), SiteInfo(false, true, true, None), 0),
      TestCase(SiteInfo(true, false, true, None), SiteInfo(true, false, false, None), -1),
      TestCase(SiteInfo(false, false, false, Some(0.1)), SiteInfo(false, false, false, Some(0.1)), 0),
      TestCase(SiteInfo(true, true, false, None), SiteInfo(false, true, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, false, None), SiteInfo(false, true, false, None), -1),
      TestCase(SiteInfo(false, false, true, Some(0.1)), SiteInfo(false, false, true, Some(0.1)), 0),
      TestCase(SiteInfo(false, true, true, None), SiteInfo(false, false, true, Some(0.1)), 1),
      TestCase(SiteInfo(true, true, true, Some(0.1)), SiteInfo(false, false, true, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, true, Some(0.1)), SiteInfo(true, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, false, None), SiteInfo(true, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(false, false, true, Some(0.1)), SiteInfo(false, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(false, true, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(false, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(true, false, true, None), -1),
      TestCase(SiteInfo(true, false, false, None), SiteInfo(true, false, false, Some(0.1)), 1),
      TestCase(SiteInfo(true, false, true, None), SiteInfo(true, false, true, Some(0.1)), 1),
      TestCase(SiteInfo(true, false, false, None), SiteInfo(true, false, false, None), 0),
      TestCase(SiteInfo(true, true, true, Some(0.1)), SiteInfo(false, true, true, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, true, Some(0.1)), SiteInfo(false, true, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, false, false, Some(0.1)), SiteInfo(false, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, false, None), SiteInfo(true, true, false, None), 0),
      TestCase(SiteInfo(true, true, false, None), SiteInfo(true, false, false, None), -1),
      TestCase(SiteInfo(false, true, true, Some(0.1)), SiteInfo(false, true, true, Some(0.1)), 0),
      TestCase(SiteInfo(true, true, false, None), SiteInfo(false, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(true, true, false, Some(0.1)), 0),
      TestCase(SiteInfo(true, true, true, Some(0.1)), SiteInfo(false, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, false, None), SiteInfo(true, true, false, Some(0.1)), 1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(true, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, true, Some(0.1)), SiteInfo(true, false, true, Some(0.1)), -1),
      TestCase(SiteInfo(false, false, false, None), SiteInfo(false, false, false, Some(0.1)), 1),
      TestCase(SiteInfo(true, true, false, Some(0.1)), SiteInfo(true, true, false, Some(0.1)), 0),
      TestCase(SiteInfo(true, false, true, Some(0.1)), SiteInfo(false, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, false, true, None), SiteInfo(true, false, false, Some(0.1)), 0),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(true, false, false, None), -1),
      TestCase(SiteInfo(true, false, true, Some(0.1)), SiteInfo(true, false, true, Some(0.1)), 0),
      TestCase(SiteInfo(true, true, false, Some(0.1)), SiteInfo(true, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(false, true, true, None), SiteInfo(false, true, true, Some(0.1)), 1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(false, true, true, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, false, None), SiteInfo(false, false, false, None), -1),
      TestCase(SiteInfo(false, true, true, Some(0.1)), SiteInfo(false, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(false, true, false, None), SiteInfo(false, false, false, Some(0.1)), 1),
      TestCase(SiteInfo(true, false, true, None), SiteInfo(false, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(true, false, true, Some(0.1)), -1),
      TestCase(SiteInfo(false, true, true, Some(0.1)), SiteInfo(false, true, false, Some(0.1)), -1),
      TestCase(SiteInfo(true, false, true, Some(0.1)), SiteInfo(true, false, false, Some(0.1)), -1),
      TestCase(SiteInfo(false, true, true, None), SiteInfo(false, false, true, None), 0),
      TestCase(SiteInfo(true, false, true, Some(0.1)), SiteInfo(false, false, true, Some(0.1)), -1),
      TestCase(SiteInfo(true, true, false, Some(0.1)), SiteInfo(false, true, false, Some(0.1)), -1),
      TestCase(SiteInfo(false, false, true, None), SiteInfo(false, false, true, None), 0),
      TestCase(SiteInfo(false, true, true, None), SiteInfo(false, true, false, Some(0.1)), 0),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(false, false, false, None), -1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(true, true, false, None), -1),
      TestCase(SiteInfo(true, true, true, None), SiteInfo(true, true, true, None), 0),
      TestCase(SiteInfo(false, false, true, None), SiteInfo(false, false, false, None), -1)
    )

  private def checkCase(testCase: TestCase) = {
    val s1 = new Site(1)
    testCase.v1.relAdded.foreach(x => s1.setRelevanceAddend(x.toFloat))

    val s2 = new Site(2)
    testCase.v2.relAdded.foreach(x => s2.setRelevanceAddend(x.toFloat))

    val stats = new SiteStatisticsStorage(
      Map(
        Long.box(s1.getId) -> testCase.v1.stat,
        Long.box(s2.getId) -> testCase.v2.stat
      ).asJava
    )

    withClue(s"left: ${testCase.v1}, right: ${testCase.v2}") {
      SitesComparator
        .comparator(stats)
        .compare(s1, s2) shouldBe testCase.expected
    }
  }

  "SitesComparator" should {
    "correctly compare" in {
      cases.foreach(checkCase)
    }
  }
}
