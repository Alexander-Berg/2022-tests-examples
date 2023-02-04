package ru.auto.catalog.core.testkit

import ru.yandex.extdata.core.DataType
import ru.yandex.vertis.baker.util.extdata.ExtDataEngine

import java.io.InputStream

/** Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 09.01.17
  */
object TestDataEngine extends ExtDataEngine {

  // При обновлении данных необходимо сначала удалить старые каталоги из mds
  private val MdsData = Set(
    "CARS_CATALOG-65",
    "TRUCKS_CATALOG-11",
    "MOTO_CATALOG-8",
    "VERBA-102",
    "VERBA_TRUCKS-101",
    "VERBA_MOTO-101",
    "CARS_OFFERS_STAT-3",
    "REGIONS-6",
    "CARS_REVIEWS_DATA-1",
    "PRESET_GROUPS-1"
  )

  override def subscribe(dataTypes: DataType*)(action: => Unit): Unit = ()

  override def readData(dataType: DataType): InputStream = {
    val dataName = s"${dataType.name}-${dataType.format}"
    val fromResources = this.getClass.getResourceAsStream(s"data/$dataName")
    val stream =
      if (fromResources == null && MdsData.contains(dataName))
        TestMdsLoader.loadData(dataName)
      else
        fromResources
    if (stream == null)
      throw new NoSuchElementException(s"$dataType not found in test data")
    stream
  }
}
