package vertis.spamalot

import monocle.macros.{GenLens, GenPrism}
import ru.yandex.vertis.spamalot.inner.OperationPayload.Payload
import ru.yandex.vertis.spamalot.inner.{OperationPayload, StoredNotification}
import vertis.ydb.queue.storage.QueueElement

trait SpamalotOptics {

  protected val payloadNotificationOptics = GenLens[OperationPayload](_.payload)
    .andThen(GenPrism[Payload, Payload.AddNotification])
    .andThen(GenLens[Payload.AddNotification](_.value.notification))

  protected val topicOptics = payloadNotificationOptics.andThen(GenLens[StoredNotification](_.topic))

  protected val notificationIdOptics = payloadNotificationOptics.andThen(GenLens[StoredNotification](_.id))

  protected val queueElementSendPushNotificationOptics = GenLens[QueueElement[OperationPayload]](_.payload.payload)
    .andThen(GenPrism[Payload, Payload.SendPush])
    .andThen(GenLens[Payload.SendPush](_.value.notification))
}
