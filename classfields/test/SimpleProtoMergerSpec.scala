package ru.yandex.vertis.billing.emon.consumer

import billing.common_model.Project
import billing.emon.model.EventTypeNamespace.EventType
import billing.emon.model.{Event, EventId, EventPayload, Payer}
import common.scalapb.ScalaProtobuf.instantToTimestamp
import ru.yandex.realty.developer.chat.event.event.RealtyDevchatBillingEvent
import ru.yandex.vertis.billing.emon.consumer.SimpleProtoMerger.MessageContext
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object SimpleProtoMergerSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("SimpleProtoMerger")(
      testM("merge fields based on timestamps") {

        val eventId1 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event1 = Event(
          Some(eventId1),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(123, None)), 456L))),
          Some(EventPayload.defaultInstance)
        )
        val event1Context = MessageContext(
          event1,
          fd =>
            Map(
              Event.EVENT_ID_FIELD_NUMBER -> 3,
              Event.TIMESTAMP_FIELD_NUMBER -> 2,
              Event.PAYER_FIELD_NUMBER -> 2,
              Event.PAYLOAD_FIELD_NUMBER -> 1
            )(fd.getNumber)
        )

        val eventId2 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "2")
        val event2 = Event(
          Some(eventId2),
          Some(instantToTimestamp(Instant.ofEpochMilli(2))),
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(234, None)), 567L))),
          Some(EventPayload.defaultInstance.withRealtyDevchat(RealtyDevchatBillingEvent.defaultInstance))
        )
        val event2Context = MessageContext(
          event2,
          fd =>
            Map(
              Event.EVENT_ID_FIELD_NUMBER -> 3,
              Event.TIMESTAMP_FIELD_NUMBER -> 1,
              Event.PAYER_FIELD_NUMBER -> 1,
              Event.PAYLOAD_FIELD_NUMBER -> 2
            )(fd.getNumber)
        )

        for {
          mergeResult <- ZIO.effect(
            SimpleProtoMerger.merge(event2Context.message.companion.javaDescriptor, event1Context, event2Context)
          )
        } yield {
          assert(mergeResult.fieldToTimestamp.map(kv => kv._1.getNumber -> kv._2))(
            equalTo(
              Map(
                Event.EVENT_ID_FIELD_NUMBER -> 3L,
                Event.TIMESTAMP_FIELD_NUMBER -> 2L,
                Event.PAYER_FIELD_NUMBER -> 2L,
                Event.PAYLOAD_FIELD_NUMBER -> 2L
              )
            )
          ) && assert(mergeResult.message)(
            equalTo(
              Event(
                event2.eventId,
                event1.timestamp,
                event1.payer,
                event2.payload
              )
            )
          )
        }
      },
      testM("add new fields to old") {

        val eventId1 = EventId(Project.REALTY, EventType.REALTY_DEVCHAT, "1")
        val event1 = Event(
          Some(eventId1),
          Some(instantToTimestamp(Instant.ofEpochMilli(1))),
          None,
          None
        )
        val event1Context = MessageContext(
          event1,
          fd =>
            Map(
              Event.EVENT_ID_FIELD_NUMBER -> 3,
              Event.TIMESTAMP_FIELD_NUMBER -> 2
            )(fd.getNumber)
        )

        val event2 = Event(
          None,
          None,
          Some(Payer().withBalance(Payer.Balance(Some(Payer.Balance.CustomerId(234, None)), 567L))),
          Some(EventPayload.defaultInstance.withRealtyDevchat(RealtyDevchatBillingEvent.defaultInstance))
        )
        val event2Context = MessageContext(
          event2,
          fd =>
            Map(
              Event.PAYER_FIELD_NUMBER -> 1,
              Event.PAYLOAD_FIELD_NUMBER -> 2
            )(fd.getNumber)
        )

        for {
          mergeResult <- ZIO.effect(
            SimpleProtoMerger.merge(event2Context.message.companion.javaDescriptor, event1Context, event2Context)
          )
        } yield {
          assert(mergeResult.fieldToTimestamp.map(kv => kv._1.getNumber -> kv._2))(
            equalTo(
              Map(
                Event.EVENT_ID_FIELD_NUMBER -> 3L,
                Event.TIMESTAMP_FIELD_NUMBER -> 2L,
                Event.PAYER_FIELD_NUMBER -> 1L,
                Event.PAYLOAD_FIELD_NUMBER -> 2L
              )
            )
          ) && assert(mergeResult.message)(
            equalTo(
              Event(
                event1.eventId,
                event1.timestamp,
                event2.payer,
                event2.payload
              )
            )
          )
        }
      }
    ) @@ sequential
  }
}
