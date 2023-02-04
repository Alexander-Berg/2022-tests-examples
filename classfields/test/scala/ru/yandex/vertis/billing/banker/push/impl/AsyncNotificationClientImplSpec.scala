package ru.yandex.vertis.billing.banker.push.impl

import com.google.protobuf.Message
import org.joda.time.format.ISODateTimeFormat
import org.scalacheck.Gen
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.model.EventsModel
import ru.yandex.vertis.billing.banker.actor.ActorSpecBase
import ru.yandex.vertis.billing.banker.model.PushNotification.NotifyStatuses
import ru.yandex.vertis.billing.banker.model.gens.{PayloadJsonGen, PayloadTextGen, Producer, PushNotificationGen}
import ru.yandex.vertis.billing.banker.model.{Payload, PushNotification}
import ru.yandex.vertis.billing.banker.proto.EventsProtoFormats.NotificationProtoWriter
import ru.yandex.vertis.billing.banker.push.AsyncNotificationClient.{NewVersion, OldVersion}
import ru.yandex.vertis.billing.banker.push.impl.AsyncNotificationClientImplSpec.{v1json, v2json, v2proto}
import ru.yandex.vertis.billing.banker.util.AkkaHttpTestUtils
import ru.yandex.vertis.billing.banker.util.DateTimeUtils.IsoDateTimeFormatter
import spray.json.{JsArray, JsObject, JsString, JsValue}

import scala.concurrent.duration.FiniteDuration

/**
  * Specs on [[AsyncNotificationClientImpl]]
  *
  * @author alex-kovalenko
  */
class AsyncNotificationClientImplSpec
  extends ActorSpecBase("AsyncNotificationClientImplSpec")
  with AkkaHttpTestUtils
  with AsyncSpecBase {

  implicit val timeout: FiniteDuration = FiniteDuration(3, "seconds")

  val responder = new MockHttpResponder()

  override def beforeEach(): Unit = {
    responder.reset()
    super.beforeEach()
  }

  def prepareNotifications(count: Int, pg: Gen[Payload]): Seq[PushNotification] =
    PushNotificationGen
      .next(count)
      .zip(pg.next(count))
      .map { case (n, p) =>
        n.copy(
          payload = p,
          time = n.time.withMillis(0)
        ) // drop millis because Joda writes them always, but proto skip if zero
      }
      .toSeq

  "AsyncNotificationClient" when {
    "configured for v1" should {
      val client = new AsyncNotificationClientImpl("fake", responder, OldVersion)
      "serialize with text payload" in {
        val notifications = prepareNotifications(3, PayloadTextGen)
        responder.expectJson(v1json(notifications))
        client.notify(notifications).futureValue
      }
      "serialize with json payload" in {
        val notifications = prepareNotifications(3, PayloadJsonGen)
        responder.expectJson(v1json(notifications))
        client.notify(notifications).futureValue
      }
    }

    "configured for v2 with json" should {
      val client = new AsyncNotificationClientImpl("fake", responder, NewVersion(NewVersion.ContentTypes.Json))
      "serialize with text payload" in {
        val notifications = prepareNotifications(1, PayloadTextGen)
        responder.expectJson(v2json(notifications))
        client.notify(notifications).futureValue
      }
      "serialize with json payload" in {
        val notifications = prepareNotifications(1, PayloadJsonGen)
        responder.expectJson(v2json(notifications))
        client.notify(notifications).futureValue
      }
    }

    "configured for v2 with protobuf" should {
      val client = new AsyncNotificationClientImpl("fake", responder, NewVersion(NewVersion.ContentTypes.Protobuf))
      "serialize with text payload" in {
        val notifications = prepareNotifications(3, PayloadTextGen)
        responder.expectedManyProto(v2proto(notifications))
        client.notify(notifications).futureValue
      }
      "serialize with json payload" in {
        val notifications = prepareNotifications(3, PayloadJsonGen)
        responder.expectedManyProto(v2proto(notifications))
        client.notify(notifications).futureValue
      }
    }
  }
}

object AsyncNotificationClientImplSpec {

  def v1json(ns: Iterable[PushNotification]): JsValue = {
    JsArray {
      ns.map { n =>
        val p = n.payload match {
          case Payload.Text(text) => JsString(text)
          case Payload.Json(json) => json
          case other => throw new IllegalArgumentException(s"Unexpected $other")
        }
        JsObject(
          "id" -> JsString(n.id),
          "time" -> JsString(IsoDateTimeFormatter.print(n.time)),
          "action" -> JsString(n.status.toString),
          "payload" -> p
        )
      }.toVector
    }
  }

  private val UtcFormatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC()

  def v2json(ns: Iterable[PushNotification]): JsValue = {
    JsArray {
      ns.map { n =>
        val p = n.payload match {
          case Payload.Text(text) => JsObject("plain" -> JsString(text))
          case Payload.Json(json) => JsObject("struct" -> json)
          case other => throw new IllegalArgumentException(s"Unexpected $other")
        }
        val optAction = n.status match {
          case NotifyStatuses.Activate =>
            None
          case NotifyStatuses.Deactivate =>
            Some("action" -> JsString(EventsModel.PaymentNotification.Action.DEACTIVATE.toString))
        }
        val fields = Seq(
          "id" -> JsString(n.id),
          "timestamp" -> JsString(UtcFormatter.print(n.time)),
          "payload" -> p
        ) ++ Seq(optAction).flatten
        JsObject(fields: _*)
      }.toVector
    }
  }

  def v2proto(ns: Iterable[PushNotification]): Iterable[Message] =
    ns.map(NotificationProtoWriter.write)

}
