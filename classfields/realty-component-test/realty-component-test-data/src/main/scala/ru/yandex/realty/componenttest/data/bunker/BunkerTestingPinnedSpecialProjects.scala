package ru.yandex.realty.componenttest.data.bunker

import ru.yandex.realty.resources.BunkerNode

object BunkerTestingPinnedSpecialProjects {

  val Node: BunkerNode = {
    BunkerNode
      .newBuilder()
      .setPath("/realty-www/site_special_projects_second_package")
      .setContent(
        """[ {"params": {"geoId": [1, 10174], "startDate": "2020-12-01", "endDate": "3021-12-31"},
          |"data": {"developerId": 102320, "developerName": "Группа «Самолет»",
          |"pinnedSiteIds": [57547, 280521]}}]""".stripMargin
      )
      .build()
  }

}
