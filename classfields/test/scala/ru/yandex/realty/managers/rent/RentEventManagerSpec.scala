package ru.yandex.realty.managers.rent

import org.scalamock.scalatest.MockFactory
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.event.RealtyEventModelGen
import ru.yandex.realty.events.{Event, RentFrontEventBatch}
import ru.yandex.realty.request.{Request, RequestImpl}
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller
import ru.yandex.vertis.broker.client.simple.BrokerClient

class RentEventManagerSpec extends AsyncSpecBase with RealtyEventModelGen with MockFactory {
  private val brokerClient = mock[BrokerClient]
  private val eventManager = new DefaultRentEventManager(brokerClient)
  implicit private val request: Request = new RequestImpl

  "EventManager" should {

    "successfully log empty batch" in {

      (brokerClient
        .send[Event](_: Option[String], _: Event, _: Option[String])(_: ProtoMarshaller[Event]))
        .expects(*, *, *, *)
        .never()

      val eventBatch = RentFrontEventBatch.getDefaultInstance
      eventManager.logFrontEventBatch(eventBatch, Seq.empty).futureValue
    }

  }
}
