package ru.yandex.realty.notifier.model.push

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import realty.palma.PushTemplateOuterClass.PushTemplate
import ru.yandex.realty.context.v2.palma.PushTemplatesProvider.prepareTemplates
import ru.yandex.realty.pushnoy.model.{MetrikaPushId, PalmaPushRenderer, PushActionType, PushTestUtils}
import ru.yandex.vertis.util.time.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class TenantRentPaymentPushSpec extends FlatSpec with Matchers {

  classOf[FirstRentPaymentPush].getName should "have all necessary fields where expected" in {
    PushTestUtils.checkV4Push(
      PushActionType.TenantRentPayment,
      MetrikaPushId.TenantRentFirstPayment,
      url = Some("deeplink"),
      customData = Map(
        "flat_id" -> "MYFLATID",
        "payment_id" -> "MYPAYMENTID",
        "recipient_id" -> "1515151"
      )
    )(
      PalmaPushRenderer.renderV4(
        FirstRentPaymentPush(
          "MYFLATID",
          "MYPAYMENTID",
          1515151L
        ),
        prepareTemplates(
          Seq(
            PushTemplate
              .newBuilder()
              .setPushId("FIRST_RENT_PAYMENT")
              .setIosMinVersion("0.0.0")
              .setAndroidMinVersion("0.0.0")
              .setMetrikaId("TENANT_RENT_FIRST_PAYMENT")
              .setPushInfoId("tenant_rent_first_payment")
              .setTitle("title")
              .setText("text")
              .setDeeplink("deeplink")
              .build()
          )
        ).head
      )
    )
  }

  classOf[RentPaymentSoonPush].getName should "have all necessary fields where expected" in {
    PushTestUtils.checkV4Push(
      PushActionType.TenantRentPayment,
      MetrikaPushId.TenantRentPaymentSoon,
      url = Some("deeplink"),
      customData = Map(
        "flat_id" -> "MYFLATID",
        "payment_id" -> "MYPAYMENTID",
        "recipient_id" -> "1515151"
      )
    )(
      PalmaPushRenderer.renderV4(
        RentPaymentSoonPush(
          "MYFLATID",
          "MYPAYMENTID",
          1515151L,
          DateTimeUtil.now()
        ),
        prepareTemplates(
          Seq(
            PushTemplate
              .newBuilder()
              .setPushId("RENT_PAYMENT_SOON")
              .setIosMinVersion("0.0.0")
              .setAndroidMinVersion("0.0.0")
              .setMetrikaId("TENANT_RENT_PAYMENT_SOON")
              .setPushInfoId("tenant_rent_payment_soon")
              .setTitle("title")
              .setText("text")
              .setDeeplink("deeplink")
              .build()
          )
        ).head
      )
    )
  }

  classOf[RentPaymentTodayPush].getName should "have all necessary fields where expected" in {
    PushTestUtils.checkV4Push(
      PushActionType.TenantRentPayment,
      MetrikaPushId.TenantRentPaymentToday,
      url = Some("deeplink"),
      customData = Map(
        "flat_id" -> "MYFLATID",
        "payment_id" -> "MYPAYMENTID",
        "recipient_id" -> "1515151"
      )
    )(
      PalmaPushRenderer.renderV4(
        RentPaymentTodayPush(
          "MYFLATID",
          "MYPAYMENTID",
          1515151L,
          DateTimeUtil.now(),
          DateTimeUtil.now()
        ),
        prepareTemplates(
          Seq(
            PushTemplate
              .newBuilder()
              .setPushId("RENT_PAYMENT_TODAY")
              .setIosMinVersion("0.0.0")
              .setAndroidMinVersion("0.0.0")
              .setMetrikaId("TENANT_RENT_PAYMENT_TODAY")
              .setPushInfoId("tenant_rent_payment_today")
              .setTitle("title")
              .setText("text")
              .setDeeplink("deeplink")
              .build()
          )
        ).head
      )
    )
  }

  classOf[RentPaymentOverduePush].getName should "have all necessary fields where expected" in {
    PushTestUtils.checkV4Push(
      PushActionType.TenantRentPayment,
      MetrikaPushId.TenantRentPaymentOverdue,
      url = Some("deeplink"),
      customData = Map(
        "flat_id" -> "MYFLATID",
        "payment_id" -> "MYPAYMENTID",
        "recipient_id" -> "1515151"
      )
    )(
      PalmaPushRenderer.renderV4(
        RentPaymentOverduePush(
          "MYFLATID",
          "MYPAYMENTID",
          1515151L,
          DateTimeUtil.now(),
          DateTimeUtil.now()
        ),
        prepareTemplates(
          Seq(
            PushTemplate
              .newBuilder()
              .setPushId("RENT_PAYMENT_OVERDUE")
              .setIosMinVersion("0.0.0")
              .setAndroidMinVersion("0.0.0")
              .setMetrikaId("TENANT_RENT_PAYMENT_OVERDUE")
              .setPushInfoId("tenant_rent_payment_overdue")
              .setTitle("title")
              .setText("text")
              .setDeeplink("deeplink")
              .build()
          )
        ).head
      )
    )
  }
}
