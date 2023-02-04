package ru.yandex.realty.event.impl

import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.{any, same}
import org.mockito.Mockito.when
import org.scalatest.Ignore
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.event.{VertisEventSender, VertisEventSenderTest}
import ru.yandex.realty.event.impl.VertisEventSenderImplTest.mockBrokerClient
import ru.yandex.vertis.broker.client.simple.BrokerClient

import scala.concurrent.Future

/**
  * Created by Sergey Kozlov <slider5@yandex-team.ru> on 03.05.2018
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class VertisEventSenderImplTest extends VertisEventSenderTest {
  protected val eventSender: VertisEventSender = new VertisEventSenderImpl(mockBrokerClient())
}

object VertisEventSenderImplTest {

  def mockBrokerClient(): BrokerClient = {
    val ret = mock[BrokerClient]
    when(
      ret.send(any[String](), any[ru.yandex.vertis.events.Event]())(
        same(VertisEventSenderImpl.vertisEventProtoMarshaller)
      )
    ).thenReturn(Future.successful[Unit](()))
    ret
  }
}
