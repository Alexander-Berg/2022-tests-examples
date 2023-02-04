package ru.yandex.vertis.parsing.auto.components.bunkerconfig

import org.scalatest.FunSuite
import play.api.libs.json.Json
import ru.yandex.vertis.parsing.common.Site

class BunkerConfigTest extends FunSuite {
  test("distribution by regions") {
    pending
    val source = scala.io.Source.fromFile("/home/aborunov/code/parsing/tmp.json")
    val config = {
      try {
        val str = source.getLines().mkString("\n")
        BunkerConfig.fromInner(Json.parse(str).as[BunkerInnerConfig])
      } finally {
        source.close
      }
    }
    val regions = config.cars_call_centers.flatMap(_.percents.map(_.region)).distinct.sorted
    regions.foreach { region =>
      val not100ForRegion = Seq(Site.Avito, Site.Drom, Site.Amru)
        .map { site =>
          val all = config.cars_call_centers.map(_.percentsForSiteAndRegion(site, region)).sum
          (site, 100 - all)
        }
        .filter(_._2 != 0)
        .toMap
      if (not100ForRegion.nonEmpty) {
        println(s"""{
          |  "region": "$region",
          |  "avito": ${not100ForRegion.getOrElse(Site.Avito, 0)},
          |  "drom": ${not100ForRegion.getOrElse(Site.Drom, 0)},
          |  "others": 100
          |},""".stripMargin)
      }
    }
    assert(true)
  }
}
