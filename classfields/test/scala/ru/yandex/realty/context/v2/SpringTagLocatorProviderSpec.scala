package ru.yandex.realty.context.v2

import java.util.zip.GZIPInputStream

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.extdata.core.event.{ContainerEventListener, Event, EventListener}
import ru.yandex.extdata.core.service.ExtDataService
import ru.yandex.extdata.core.{DataType, Producer, ServerController, TaskId}

import scala.util.{Failure, Success, Try}

/**
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class SpringTagLocatorProviderSpec extends FlatSpec with MockFactory with Matchers {

  private val fakeController = new FakeController

  "SpringTagLocator" should "read data" in {
    val inputStream = getClass.getResource("/tags-1-4.data").openStream()
    val preparedStream = Try(new GZIPInputStream(inputStream)) match {
      case Success(gzis) => Success(gzis)
      case Failure(e) =>
        Try(inputStream.close())
        Failure(e)
    }

    val tagLocator = new SpringTagLocatorProvider(fakeController)
    val result = tagLocator.parse(preparedStream.get)

    inputStream.close()

    val tags = result.get
    tags.reverseIndex.values.size should be(7)
    tags.extractors.size should be(7)
  }

}

class FakeController extends ServerController with MockFactory {

  override def start(): Unit = {}

  override def close(): Unit = {}

  override def replicate(dataType: DataType): Unit = {}

  override def extDataService: ExtDataService = mock[ExtDataService]

  override def register(listener: EventListener): Unit = {}

  override def onEvent(e: Event): Unit = {}

  override def dispatch(id: TaskId, weight: Int, payload: () => Unit): Unit = {}

  override def fetch(dataType: DataType): Unit = {}

  override def produce(dataType: DataType, producer: Producer): Unit = {}

}

class FakeContainerEventListener extends ContainerEventListener {
  override def register(listener: EventListener): Unit = {}

  override def onEvent(e: Event): Unit = {}
}
