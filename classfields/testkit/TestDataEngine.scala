package ru.auto.api.testkit

import ru.auto.api.extdata.{ExtData, ExtDataEngine}
import ru.yandex.extdata.core.DataType

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 09.01.17
  */
object TestDataEngine extends ExtDataEngine {

  private val MdsData = Set(
    "RAW_BUNKER-1",
    "API_TOKENS_MODEL-1",
    "REGIONS-6"
  )

  override def subscribe(dataTypes: DataType*)(action: => Unit): Unit = ()

  override def readData(dataType: DataType): ExtData = {
    val dataName = s"${dataType.name}-${dataType.format}"
    val dataPath = s"/test-data/$dataName"
    val stream = if (MdsData.contains(dataName)) {
      TestMdsLoader.loadData(dataName)
    } else {
      this.getClass.getResourceAsStream(s"$dataPath")
    }
    if (stream == null) {
      throw new NoSuchElementException(s"$dataType not found in test data")
    } else ExtData(stream, dataType.format)
  }
}
