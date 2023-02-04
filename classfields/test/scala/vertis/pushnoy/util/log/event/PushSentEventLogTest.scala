package vertis.pushnoy.util.log.event

import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.pushnoy.EventLogModel.PushSentEvent
import ru.yandex.pushnoy.EventLogModel.PushSentEvent.SendMethod
import vertis.pushnoy.gen.ModelGenerators._
import vertis.pushnoy.util.GeneratorUtils._

class PushSentEventLogTest extends AsyncFunSuite {

  test("PushMessage serialization") {
    val user = UserGen.next
    val deviceInfo = DeviceInfoGen.next
    val pushMessage = PushMessageGen.next
    val eventLog = PushSentEventLog(
      user = Some(user),
      deviceInfo = Some(deviceInfo),
      pushMessage = Some(pushMessage),
      deliveryName = Some("testDeliveryName")
    )

    assert(
      eventLog.message.build() == PushSentEvent
        .newBuilder()
        .setSendMethod(SendMethod.DIRECT)
        .setUserId(user.id)
        .setApplication(user.clientType)
        .setApplicationVersion(deviceInfo.appVersion.get)
        .setDeviceBrand(deviceInfo.brand)
        .setDeviceModel(deviceInfo.model)
        .setDeviceOs(deviceInfo.clientOS.toString)
        .setTitle("title")
        .setText("body")
        .setDeeplink("url")
        .setPushName("pushName")
        .setXivaEvent(pushMessage.event)
        .setDeliveryName("testDeliveryName")
        .build()
    )
  }
}
