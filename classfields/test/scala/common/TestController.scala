package common

import ru.yandex.extdata.core.event.ContainerEventListener
import ru.yandex.extdata.core.service.{Dispatcher, ExtDataService}
import ru.yandex.extdata.core.{Controller, DataType, Instance, Producer}

import scala.util.Try

class TestController(givenInstance: Try[Instance]) extends Controller {
  case object NotImplemented extends RuntimeException("Not implemented in test!")

  override def start(): Unit = {}

  override def close(): Unit = {}

  override def fetch(dataType: DataType): Unit = {}

  override def produce(dataType: DataType, producer: Producer): Unit = {}

  override def replicate(dataType: DataType): Unit = {}

  override def listenerContainer: ContainerEventListener = throw NotImplemented

  override def dispatcher: Dispatcher = throw NotImplemented

  override def extDataService: ExtDataService = new TestExtDataService(givenInstance)
}
