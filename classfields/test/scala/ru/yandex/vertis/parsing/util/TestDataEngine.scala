package ru.yandex.vertis.parsing.util

import java.io.InputStream

import ru.yandex.extdata.core.DataType
import ru.yandex.vertis.parsing.util.extdata.ExtDataEngine

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 09.01.17
  */
object TestDataEngine extends ExtDataEngine {

  // При обновлении данных необходимо сначала удалить старые каталоги из mds
  private val MdsData: Set[String] = Set()

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
