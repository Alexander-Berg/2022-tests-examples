package ru.yandex.auto.garage.api.handlers.user.notifications

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import auto.carfax.common.clients.spamalot.notification.SpamalotNotificationsClient
import auto.carfax.common.utils.tracing.Traced
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.spamalot.model.{Notification, Payload, ReceivedNotification}
import com.google.protobuf.timestamp.Timestamp
import ru.auto.api.vin.garage.garage_api_model.{Notification => GarageNotification}
import ru.yandex.auto.vin.decoder.model.UserRef
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.spamalot.model.Notification.Content
import ru.yandex.vertis.spamalot.model.Payload.Action.PlatformUrl
import ru.yandex.vertis.spamalot.service.ListResponse

import scala.concurrent.duration._
import java.time.Instant
import scala.concurrent.Future

class GarageNotificationManagerTest extends AnyWordSpecLike with Matchers with ScalatestRouteTest with MockitoSupport {

  implicit val t = Traced.empty

  private val client = mock[SpamalotNotificationsClient]
  private val manager = new GarageNotificationManager(client)

  private def buildNotification(id: String, name: String, created: Long): ReceivedNotification = {
    ReceivedNotification(
      id = id,
      notification = Notification(
        name = Some(name),
        content = Content.Payload(
          Payload(
            title = s"title $id",
            body = s"body $id",
            action = Some(Payload.Action(url = Some(PlatformUrl(s"web $id", s"android $id", s"ios $id"))))
          )
        )
      ),
      createTs = Timestamp.apply(Instant.ofEpochMilli(created))
    )
  }

  "getLastNotification" should {
    "return most fresh renewal notification" in {
      val user = UserRef.user(123L)

      val outDatedInsuranceRenewal =
        buildNotification("1", "garage_insurance_renewal", System.currentTimeMillis() - 40.days.toMillis)
      val freshInsuranceRenewal =
        buildNotification("2", "garage_insurance_renewal", System.currentTimeMillis() - 10.days.toMillis)
      val mostFreshInsuranceRenewal =
        buildNotification("3", "garage_insurance_renewal", System.currentTimeMillis() - 1.days.toMillis)
      val unknownNotification = buildNotification("4", "unknown_name", System.currentTimeMillis())

      val notifications =
        List(outDatedInsuranceRenewal, freshInsuranceRenewal, mostFreshInsuranceRenewal, unknownNotification)

      when(client.getList(?)(?)).thenReturn(Future.successful(ListResponse(notifications)))

      val result = manager.getLastNotification(user, "ios").await

      result shouldBe Some(GarageNotification(id = "3", title = "title 3", body = "body 3", url = "ios 3"))
    }
    "return None if there are no suitable notifications" in {
      val user = UserRef.user(123L)

      val outDatedInsuranceRenewal =
        buildNotification("1", "garage_insurance_renewal", System.currentTimeMillis() - 40.days.toMillis)
      val unknownNotification = buildNotification("4", "unknown_name", System.currentTimeMillis())

      val notifications = List(outDatedInsuranceRenewal, unknownNotification)

      when(client.getList(?)(?)).thenReturn(Future.successful(ListResponse(notifications)))

      val result = manager.getLastNotification(user, "ios").await

      result shouldBe None
    }
  }

}
