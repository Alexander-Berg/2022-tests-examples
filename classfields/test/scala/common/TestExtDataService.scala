package common

import ru.yandex.extdata.core.service.ExtDataService
import ru.yandex.extdata.core.spec.DataSpec
import ru.yandex.extdata.core.storage.Meta
import ru.yandex.extdata.core._

import scala.util.{Failure, Try}

class TestExtDataService(givenInstance: Try[Instance]) extends ExtDataService {
  private val notImplemented = Failure(new RuntimeException("Not implemented in test!"))

  def write(dataType: DataType, instanceId: Option[InstanceId], is: Data): Try[WriteResult] = notImplemented

  def write(dataType: DataType, dataResource: Instance): Try[WriteResult] = notImplemented

  def get: Try[Seq[Meta]] = notImplemented

  def get(dataTypeName: String): Try[Seq[Meta]] = notImplemented

  def get(dataType: DataType): Try[Seq[Meta]] = notImplemented

  def get(dataType: DataType, version: Version): Try[Instance] = givenInstance

  def getIfModifiedMeta(dataType: DataType, version: Option[Version]): Try[Option[Meta]] = notImplemented

  def getIfModified(dataType: DataType, version: Option[Version]): Try[Option[Instance]] = notImplemented

  def delete(dataType: DataType, version: Version): Try[Unit] = notImplemented

  def specs: Seq[DataSpec] = Seq.empty

  def getLast(dataType: DataType): Try[Instance] = givenInstance

  def register(dataSpec: DataSpec): Unit = {}

  def start(): Unit = {}

  def close(): Unit = {}

}
