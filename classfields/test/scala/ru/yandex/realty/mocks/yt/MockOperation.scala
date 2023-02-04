package ru.yandex.realty.mocks.yt
import org.joda.time.Duration
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.operations.OperationStatus
import ru.yandex.inside.yt.kosher.ytree.YTreeNode

/**
  * @author azakharov
  */
object MockOperation extends ru.yandex.inside.yt.kosher.operations.Operation {
  override def getId: GUID = ???

  override def getStatus: OperationStatus = ???

  override def getResult: YTreeNode = ???

  override def await(): Unit = ???

  override def await(timeout: Duration): Unit = ???

  override def abort(): Unit = ???

  override def awaitAndThrowIfNotSuccess(): Unit = {}
}
