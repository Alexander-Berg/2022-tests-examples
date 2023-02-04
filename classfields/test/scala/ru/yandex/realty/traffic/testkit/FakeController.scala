package ru.yandex.realty.traffic.testkit

import ru.yandex.extdata.core.{Controller, DataType, TaskId}
import ru.yandex.extdata.core.event.{Event, EventListener}
import ru.yandex.extdata.core.service.ExtDataService
import ru.yandex.vertis.mockito.MockitoSupport

object FakeController extends Controller with MockitoSupport {
  override def start(): Unit = ()
  override def close(): Unit = ()
  override def replicate(dataType: DataType): Unit = ()
  override def register(listener: EventListener): Unit = ()
  override def onEvent(e: Event): Unit = ()
  override def dispatch(id: TaskId, weight: Int, payload: () => Unit): Unit = ()
  override def extDataService: ExtDataService = mock[ExtDataService]
}
