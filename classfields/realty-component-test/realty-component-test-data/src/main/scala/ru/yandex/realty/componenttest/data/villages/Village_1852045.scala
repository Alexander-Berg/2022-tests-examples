package ru.yandex.realty.componenttest.data.villages

import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.{
  extractIdFromClassName,
  loadProtoFromJsonResource
}
import ru.yandex.realty.model.message.{VillageCampaign, VillageDynamicInfo}
import ru.yandex.realty.proto.village.Village

object Village_1852045 {

  val Id: Long = extractIdFromClassName(getClass)

  val Proto: Village = loadProtoFromJsonResource[Village](s"villages/village_$Id.json")

  val Campaign: VillageCampaign =
    loadProtoFromJsonResource[VillageCampaign](s"villages/village_campaign_$Id.json")

  val DynamicInfo: VillageDynamicInfo =
    loadProtoFromJsonResource[VillageDynamicInfo](s"villages/village_dynamic_info_$Id.json")

  require(
    Proto.getId == Id,
    s"Loaded proto ID is not matched to expected: expectedId=$Id, protoId=${Proto.getId}"
  )

}
