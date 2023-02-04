package ru.auto.salesman.service.impl.user.notify.impl

import ru.auto.salesman.model.user.{PaidProduct, ProductType}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.impl.user.notify.{
  FailedProlongationNotificationPersistService,
  NotificationSender
}
import ru.auto.salesman.service.user.UserFeatureService
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}

class NotificationServiceImplSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators
    with IntegrationPropertyCheckConfig {

  val featureService: UserFeatureService = mock[UserFeatureService]

  val failedProlongationNotificationPersistService: FailedProlongationNotificationPersistService =
    mock[FailedProlongationNotificationPersistService]
  val notificationSender: NotificationSender = mock[NotificationSender]

  val notificationService = new NotificationServiceImpl(
    featureService = featureService,
    failedProlongationNotificationPersistService =
      failedProlongationNotificationPersistService,
    notificationSender = notificationSender
  )

  "NotificationServiceImpl" should {
    "save notification in database if feature enableAsyncSendingProlongationFailedNotification turn on" in {
      val goods = GoodsGen.next
      mockFeature(true)
      (failedProlongationNotificationPersistService
        .insert(_: ProductType, _: String))
        .expects(goods.product.productType, goods.id)
        .returningZ(())

      notificationService
        .notifyProlongationFailed(goods)
        .success

    }

    "send notification if feature enableAsyncSendingProlongationFailedNotification turn off" in {
      val subscription = subscriptionGen().next
      mockFeature(false)
      (notificationSender
        .notifyProlongationFailed(_: PaidProduct))
        .expects(subscription)
        .returningZ(())

      notificationService
        .notifyProlongationFailed(subscription)
        .success
    }

    "error heandling for save to database if feature enableAsyncSendingProlongationFailedNotification turn on" in {
      val goods = GoodsGen.next
      mockFeature(true)

      (failedProlongationNotificationPersistService
        .insert(_: ProductType, _: String))
        .expects(goods.product.productType, goods.id)
        .throwingZ(new Exception("ttttt"))

      notificationService
        .notifyProlongationFailed(goods)
        .success
    }

    "error handling for send notification if feature enableAsyncSendingProlongationFailedNotification turn off" in {
      val subscription = GoodsBundleGen.next
      mockFeature(false)
      (notificationSender
        .notifyProlongationFailed(_: PaidProduct))
        .expects(subscription)
        .throwingZ(new Exception("testttt"))

      notificationService
        .notifyProlongationFailed(subscription)
        .success
    }

  }

  private def mockFeature(returning: Boolean): Unit =
    (featureService.enableAsyncSendingProlongationFailedNotification _)
      .expects()
      .returning(returning)

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
