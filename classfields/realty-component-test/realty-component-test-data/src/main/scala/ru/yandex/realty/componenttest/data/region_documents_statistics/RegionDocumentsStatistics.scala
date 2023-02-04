package ru.yandex.realty.componenttest.data.region_documents_statistics

import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.loadProtoListFromJsonResource
import ru.yandex.realty.model.message.ExtDataSchema.RegionDocumentsStatisticsMessage

object RegionDocumentsStatistics {

  val List: Seq[RegionDocumentsStatisticsMessage] =
    loadProtoListFromJsonResource[RegionDocumentsStatisticsMessage]("region_documents_statistics/storage.json")

}
