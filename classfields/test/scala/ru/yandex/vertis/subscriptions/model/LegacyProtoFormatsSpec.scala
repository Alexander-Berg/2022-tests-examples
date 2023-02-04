package ru.yandex.vertis.subscriptions.model

import java.util.concurrent.TimeUnit

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.model.delivery.DeliveryGenerators
import ru.yandex.vertis.subscriptions.model.owner.OwnerGenerators

import scala.concurrent.duration.FiniteDuration

/**
  * Runnable spec on [[LegacyProtoFormats]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class LegacyProtoFormatsSpec extends ProtoFormatSpecBase {

  testFormat(LegacyProtoFormats.LegacyDurationProtoFormat, for {
    length <- Gen.posNum[Int]
    unit <- Gen.oneOf(TimeUnit.values())
  } yield FiniteDuration(length, unit))

  testFormat(LegacyProtoFormats.LegacyPushProtoFormat, DeliveryGenerators.push)

  testFormat(LegacyProtoFormats.LegacyEmailProtoFormat, DeliveryGenerators.email)

  testFormat(LegacyProtoFormats.LegacyDeliveryProtoFormat, DeliveryGenerators.deliveries)

  testFormat(LegacyProtoFormats.LegacyUserProtoFormat, OwnerGenerators.owner)

  testFormat(LegacyProtoFormats.LegacySubscriptionProtoFormat, ModelGenerators.subscription)
}
