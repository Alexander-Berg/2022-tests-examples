package common.clients.sms.testkit

import common.clients.sms.SmsSender.SmsSender
import common.clients.sms.model.{SmsMessage, SmsParams}
import zio.test.mock
import zio.test.mock.Mock
import zio.{Has, URLayer, ZLayer}

object SmsSenderTest extends Mock[SmsSender] {

  object SendSms extends Effect[(SmsMessage, SmsParams), Throwable, String]

  override val compose: URLayer[Has[mock.Proxy], SmsSender] =
    ZLayer.fromService(proxy => (message: SmsMessage, params: SmsParams) => proxy(SendSms, message, params))
}
