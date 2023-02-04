package ru.yandex.realty.componenttest.data.bunker

import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import ru.yandex.realty.componenttest.data.campaigns.Campaigns
import ru.yandex.realty.componenttest.data.villages.VillageDynamicInfos
import ru.yandex.realty.resources.BunkerNode
import ru.yandex.realty.telepony.TestingPhoneNumber

import scala.collection.JavaConverters._

object BunkerTestingPhoneNumbers {

  val Node: BunkerNode = {
    val redirects =
      Campaigns.all.flatMap(_.getRedirectsList.asScala) ++
        VillageDynamicInfos.all
          .flatMap(_.getAuction.getSortedParticipantsList.asScala.headOption)
          .map(_.getSalesDepartment)
          .flatMap(_.getPhoneRedirectList.asScala)

    BunkerNode
      .newBuilder()
      .setPath("/realty/telepony/testing-phone-numbers")
      .setContent(
        Json.stringify(
          toJson(
            redirects.map(_.getSource).map(testingPhoneNumber)
          )
        )
      )
      .build()
  }

  private def testingPhoneNumber(phone: String): TestingPhoneNumber = {
    TestingPhoneNumber(phone, description = None)
  }

}
