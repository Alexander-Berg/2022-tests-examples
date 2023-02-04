package ru.yandex.vos2.autoru.utils

import java.io.InputStream

import ru.yandex.extdata.core.DataType
import ru.yandex.vos2.extdata.ExtDataEngine

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 09.01.17
  */
object TestDataEngine extends ExtDataEngine {

  // При обновлении данных необходимо сначала удалить старые каталоги из mds
  private val MdsData = Set(
    "CARS_CATALOG-65",
    "TRUCKS_CATALOG-11",
    "MOTO_CATALOG-8",
    "REGIONS-6",
    "DEALERS-8",
    "RAW_BUNKER-1",
    "CARS_MODIFICATION_CODES-1",
    "VERBA-102",
    "RAW_MODERATION_BUNKER-1"
  )

  override def subscribe(dataTypes: DataType*)(action: => Unit): Unit = ()

  override def readData(dataType: DataType): InputStream = {
    val dataName = s"${dataType.name}-${dataType.format}"
    val stream = if (MdsData.contains(dataName)) {
      TestMdsLoader.loadData(dataName)
    } else {
      this.getClass.getResourceAsStream(s"/$dataName")
    }
    if (stream == null) {
      throw new NoSuchElementException(s"$dataType not found in test data")
    }
    stream
  }
}
