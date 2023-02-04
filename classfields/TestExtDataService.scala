package ru.yandex.auto.common.util

import ru.yandex.extdata.core.service.ExtDataService
import ru.yandex.extdata.core.spec.DataSpec
import ru.yandex.extdata.core.storage.Meta
import ru.yandex.extdata.core._

import scala.util.{Failure, Try}

class TestExtDataService(givenInstance: Try[Instance]) extends ExtDataService {
  private def notImplemented = Failure(new RuntimeException("Not implemented in dummy EDS!"))

  def write(dataType: DataType, instanceId: Option[InstanceId], is: Data): Try[WriteResult] =
    throw notImplemented.exception

  def write(dataType: DataType, dataResource: Instance): Try[WriteResult] = throw notImplemented.exception

  def get: Try[Seq[Meta]] = throw notImplemented.exception

  def get(dataTypeName: String): Try[Seq[Meta]] = throw notImplemented.exception

  def get(dataType: DataType): Try[Seq[Meta]] = throw notImplemented.exception

  def get(dataType: DataType, version: Version): Try[Instance] = givenInstance

  def getIfModifiedMeta(dataType: DataType, version: Option[Version]): Try[Option[Meta]] =
    throw notImplemented.exception

  def getIfModified(dataType: DataType, version: Option[Version]): Try[Option[Instance]] =
    throw notImplemented.exception

  def delete(dataType: DataType, version: Version): Try[Unit] = throw notImplemented.exception

  def specs: Seq[DataSpec] = Seq.empty

  def getLast(dataType: DataType): Try[Instance] = givenInstance

  def register(dataSpec: DataSpec): Unit = {}

  def start(): Unit = {}

  def close(): Unit = {}

}

import ru.yandex.extdata.core.Producer
import ru.yandex.extdata.core.event.ContainerEventListener
import ru.yandex.extdata.core.service.Dispatcher

class TestController(givenInstance: Try[Instance]) extends Controller {
  case object NotImplemented extends RuntimeException("Not implemented in dummy EDS!")

  override def start(): Unit = {}

  override def close(): Unit = {}

  override def fetch(dataType: DataType): Unit = {}

  override def produce(dataType: DataType, producer: Producer): Unit = {}

  override def replicate(dataType: DataType): Unit = {}

  override def listenerContainer: ContainerEventListener = throw NotImplemented

  override def dispatcher: Dispatcher = throw NotImplemented

  override def extDataService: ExtDataService = new TestExtDataService(givenInstance)
}
