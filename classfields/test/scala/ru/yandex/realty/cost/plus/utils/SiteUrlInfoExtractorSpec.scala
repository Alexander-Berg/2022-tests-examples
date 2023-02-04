package ru.yandex.realty.cost.plus.utils

import eu.timepit.refined.auto._
import org.junit.runner.RunWith
import ru.yandex.realty.cost.plus.utils.SiteUrlInfoExtractor.SiteUrlInfo
import ru.yandex.realty.traffic.model.site.StatItem.StatItemValue
import ru.yandex.realty.traffic.model.site.{SiteByRoomInfo, StatItem}
import zio.random.Random
import zio.test._
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

@RunWith(classOf[ZTestJUnitRunner])
class SiteUrlInfoExtractorSpec extends JUnitRunnableSpec {

  private val primaryStatItemValue = StatItemValue(1000L, 100L, 10)
  private val secondaryStatItemValue = StatItemValue(500L, 50L, 5)

  private val primaryStat = StatItem.PrimaryStat(primaryStatItemValue)
  private val secondaryStat = StatItem.SecondaryStat(secondaryStatItemValue)
  private val allStat = StatItem.AllStat(primaryStatItemValue, secondaryStatItemValue)

  private val devUrl: String = "/dev/url/"
  private val secondaryUrl: String = "/secondary/url/"

  private def roomInfo(stat: StatItem) = SiteByRoomInfo(stat, devUrl, secondaryUrl)

  private val withPrimaryGen: Gen[Random, SiteByRoomInfo] = Gen.fromIterable(
    Iterable(roomInfo(primaryStat), roomInfo(allStat))
  )

  private val expectedPrimaryInfo = SiteUrlInfo(
    devUrl,
    primaryStatItemValue.offersCount.value,
    isPrimaryPath = true
  )

  private val expectedSecondaryInfo = SiteUrlInfo(
    secondaryUrl,
    secondaryStatItemValue.offersCount.value,
    isPrimaryPath = false
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("SiteUrlInfoExtractor")(
      testM("return developer url when site with offers from developer") {

        checkAll(withPrimaryGen) { info =>
          assertTrue(SiteUrlInfoExtractor.getUrlInfo(info, isPaid = true) == expectedPrimaryInfo) &&
          assertTrue(SiteUrlInfoExtractor.getUrlInfo(info, isPaid = false) == expectedPrimaryInfo)
        }
      },
      test("return developer when only secondary offers but site is paid") {
        assertTrue(
          SiteUrlInfoExtractor.getUrlInfo(roomInfo(secondaryStat), isPaid = true) == expectedPrimaryInfo.copy(
            offersCount = secondaryStatItemValue.offersCount.value
          )
        )
      },
      test("return secondary when only secondary offers but site is not paid") {
        assertTrue(
          SiteUrlInfoExtractor.getUrlInfo(roomInfo(secondaryStat), isPaid = false) == expectedSecondaryInfo
        )
      }
    )
}
