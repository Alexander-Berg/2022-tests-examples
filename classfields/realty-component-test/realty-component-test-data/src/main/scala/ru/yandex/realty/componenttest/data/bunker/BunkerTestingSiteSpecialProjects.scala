package ru.yandex.realty.componenttest.data.bunker

import ru.yandex.realty.resources.BunkerNode

object BunkerTestingSiteSpecialProjects {

  val Node: BunkerNode = {
    BunkerNode
      .newBuilder()
      .setPath("/realty-www/site-special-projects")
      .setContent(
        """[{"params": {"geoId": [1,10174],"startDate": "2019-12-01","endDate": "2021-12-31"},"data": {"developerId": 52308,"developerName": "ПИК","showTab": true,"hasLogoInTab": true,"tabUrl": "/pik/?from=main_menu","showPin": true,"hideAds": true,"showFilter": true,"filterText": "Только ЖК от ПИК","developerFullName": "ПИК","sideMenuText": "Квартиры от ПИК"}}]"""
      )
      .build()
  }

}
