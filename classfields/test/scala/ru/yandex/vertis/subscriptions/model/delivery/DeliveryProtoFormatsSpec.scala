package ru.yandex.vertis.subscriptions.model.delivery

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.model.ProtoFormatSpecBase

/**
  * Specs on [[DeliveryProtoFormats]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class DeliveryProtoFormatsSpec extends ProtoFormatSpecBase {

  testFormat(DeliveryProtoFormats.PushProtoFormat, DeliveryGenerators.push)

  testFormat(DeliveryProtoFormats.EmailProtoFormat, DeliveryGenerators.email)

  testFormat(DeliveryProtoFormats.DeliveriesProtoFormat, DeliveryGenerators.deliveries)
}
