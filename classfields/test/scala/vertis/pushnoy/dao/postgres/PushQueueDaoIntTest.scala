package vertis.pushnoy.dao.postgres

import java.time.{LocalDateTime, ZoneId}

import ru.yandex.pushnoy.push_request_model.{PushMessage, PushRequest, XivaInfo}
import scalikejdbc.DB
import vertis.pushnoy.PushnoySpecBase
import vertis.pushnoy.model.ClientType.Auto
import vertis.pushnoy.model.DeliveryParams
import vertis.pushnoy.util.TestUtils

/** Created by Karpenko Maksim (knkmx@yandex-team.ru) on 16/04/2018.
  */
class PushQueueDaoIntTest extends PushnoySpecBase with PostgreTest with TestUtils {

  val pushDao = new PgPushQueueDao

  "PushQueueDao" should {
    "save push" in {
      DB.localTx { implicit session =>
        val clientType = Auto
        val xivaInfo = XivaInfo("test-push")
        val androidMessage = PushMessage("test", "test")
        val iosMessage = PushMessage("test", "test")

        val deliveryParams = DeliveryParams(None, Some(LocalDateTime.now().withNano(0)), None)
        val request = PushRequest(
          devices = Seq("g5a2aa14553ibeh4vmb2m99cfmmnergv.4b51ffdf39657a704c6742325cc8f599"),
          xiva = Some(xivaInfo),
          androidMessage = Some(androidMessage),
          iosMessage = Some(iosMessage)
        )

        pushDao.savePushRequest(request, deliveryParams, clientType)

        val pushes = pushDao.getNewScheduledPushes(10)
        val scheduledPush = pushes.head._2
        scheduledPush.pushRequest shouldBe request.clearDevices
        scheduledPush.clientType shouldBe clientType
        scheduledPush.deliveryParams shouldBe deliveryParams
      }
    }

    "have local date time at timezone" ignore {
      DB.localTx { _ =>
        val moscowTime = LocalDateTime.now(ZoneId.of("Europe/Moscow")).withNano(0)
        val novosibTime = LocalDateTime.now(ZoneId.of("Asia/Novosibirsk")).withNano(0)
        val localTime = LocalDateTime.now().withNano(0)

        localTime shouldBe moscowTime
        moscowTime should not be novosibTime
      }
    }

    "not fail clearQueueFromOldPushes " in {
      DB.localTx { _ =>
        {
          pushDao.clearQueueFromOldPushes(5)
          succeed
        }
      }
    }
  }
}
