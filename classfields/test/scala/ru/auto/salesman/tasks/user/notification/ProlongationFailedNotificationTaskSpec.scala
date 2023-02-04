package ru.auto.salesman.tasks.user.notification

import org.joda.time.DateTime
import ru.auto.salesman.dao.user.BundleDao.Filter.ForBundleId
import ru.auto.salesman.dao.user.SubscriptionDao.Filter.ForSubscriptionId
import ru.auto.salesman.dao.user.{BundleDao, GoodsDao, SubscriptionDao}
import ru.auto.salesman.model.notification.ProlongationFailedNotification
import ru.auto.salesman.model.notification.ProlongationFailedNotification.ProlongationFailedNotificationStatus
import ru.auto.salesman.model.user._
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.impl.user.notify.{
  FailedProlongationNotificationPersistService,
  NotificationSender
}
import ru.auto.salesman.service.user.{BundleService, GoodsService, SubscriptionService}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserDaoGenerators

import java.time.Duration

class ProlongationFailedNotificationTaskSpec extends BaseSpec with UserDaoGenerators {

  val failedProlongationNotificationPersistService =
    mock[FailedProlongationNotificationPersistService]
  val processingInterval = Duration.ofHours(3)
  val notificationSender = mock[NotificationSender]
  val bundleService = mock[BundleService]
  val goodsService = mock[GoodsService]
  val subscriptionService = mock[SubscriptionService]

  val task = new ProlongationFailedNotificationTask(
    failedProlongationNotificationPersistService =
      failedProlongationNotificationPersistService,
    numberProcessedMessageAtTime = 10,
    processingInterval = processingInterval,
    notificationSender = notificationSender,
    bundleService = bundleService,
    goodsService = goodsService,
    subscriptionService = subscriptionService
  )

  "FailedProlongationNotificationTask" should {
    "success send push notification from database" in {
      val goods = goodsGen().next
      val notificationId = 1L
      mockReadNotifications(
        List(
          notificationGenerate(notificationId, productId = goods.id)
        )
      )

      mockGoodsService(GoodsDao.Filter.ForGoodsId(goods.id), goods)
      mockNotificationSender(goods)
      mockSaveProcessedNotification(notificationId)
      task
        .execute()
        .success

    }

    "success send notifications by goods subscribe and bundle" in {

      val goods = goodsGen().next
      val subscription = subscriptionGen().next
      val bundle = GoodsBundleGen.next
      val goodsNotificationId: Long = 1L
      val subscriptionNotificationId: Long = 11L
      val bundleNotificationId: Long = 18L

      mockReadNotifications(
        List(
          notificationGenerate(
            id = goodsNotificationId,
            productId = goods.id
          ),
          notificationGenerate(
            id = subscriptionNotificationId,
            productId = subscription.id,
            productType = ProductType.Subscription
          ),
          notificationGenerate(
            id = bundleNotificationId,
            productId = bundle.id,
            productType = ProductType.Bundle
          )
        )
      )

      mockGoodsService(GoodsDao.Filter.ForGoodsId(goods.id), goods)
      mockBundleService(ForBundleId(bundle.id), bundle)
      mockSubscriptionService(ForSubscriptionId(subscription.id), subscription)

      mockNotificationSender(bundle)
      mockNotificationSender(subscription)
      mockNotificationSender(goods)

      mockSaveProcessedNotification(id = goodsNotificationId)
      mockSaveProcessedNotification(id = subscriptionNotificationId)
      mockSaveProcessedNotification(id = bundleNotificationId)

      task
        .execute()
        .success

    }

    "success save processed notification when notification sender throw exception " in {
      val goods = goodsGen().next
      val notificationId = 1L
      val notification =
        notificationGenerate(notificationId, productId = goods.id)
      mockReadNotifications(
        List(
          notification
        )
      )

      mockGoodsService(GoodsDao.Filter.ForGoodsId(goods.id), goods)
      val exception = new Exception("test eception")
      mockThrowableInNotificationSender(goods, exception)
      mockSaveProcessedNotification(
        notificationId,
        Some(
          s"Error send prolongation notifications $notification error: $exception"
        )
      )
      task
        .execute()
        .success
    }

    "success save notification if paidProduct not found in database " in {
      val goods = goodsGen().next
      val notificationId = 1L
      val notification =
        notificationGenerate(notificationId, productId = goods.id)
      mockReadNotifications(
        List(
          notification
        )
      )

      (goodsService
        .get(_: GoodsDao.Filter))
        .expects(GoodsDao.Filter.ForGoodsId(goods.id))
        .returningZ(List())

      mockSaveProcessedNotification(
        notificationId,
        Some(
          s"Error send prolongation notifications $notification error: java.util.NoSuchElementException: None.get"
        )
      )
      task
        .execute()
        .success
    }

    "success processed 3 notification of which one throw exception " in {
      val goods = goodsGen().next
      val subscription = subscriptionGen().next
      val bundle = GoodsBundleGen.next
      val goodsNotificationId: Long = 1L
      val subscriptionNotificationId: Long = 11L
      val bundleNotificationId: Long = 18L
      val goodsNotification = notificationGenerate(
        id = goodsNotificationId,
        productId = goods.id
      )
      val subscriptionNotification = notificationGenerate(
        id = subscriptionNotificationId,
        productId = subscription.id,
        productType = ProductType.Subscription
      )

      val bundleNotification = notificationGenerate(
        id = bundleNotificationId,
        productId = bundle.id,
        productType = ProductType.Bundle
      )

      mockReadNotifications(
        List(
          goodsNotification,
          subscriptionNotification,
          bundleNotification
        )
      )

      mockGoodsService(GoodsDao.Filter.ForGoodsId(goods.id), goods)
      mockBundleService(ForBundleId(bundle.id), bundle)
      mockSubscriptionService(ForSubscriptionId(subscription.id), subscription)

      val exception = new Exception("test ex")

      mockNotificationSender(bundle)
      mockThrowableInNotificationSender(subscription, exception)
      mockNotificationSender(goods)

      mockSaveProcessedNotification(id = goodsNotificationId)
      mockSaveProcessedNotification(
        id = subscriptionNotificationId,
        Some(
          s"Error send prolongation notifications $subscriptionNotification error: $exception"
        )
      )
      mockSaveProcessedNotification(id = bundleNotificationId)

      task
        .execute()
        .success
    }

    "failed save status after send notification" in {
      val goods = goodsGen().next
      val subscription = subscriptionGen().next
      val bundle = GoodsBundleGen.next
      val goodsNotificationId: Long = 1L
      val subscriptionNotificationId: Long = 11L
      val bundleNotificationId: Long = 18L
      val goodsNotification = notificationGenerate(
        id = goodsNotificationId,
        productId = goods.id
      )
      val subscriptionNotification = notificationGenerate(
        id = subscriptionNotificationId,
        productId = subscription.id,
        productType = ProductType.Subscription
      )

      val bundleNotification = notificationGenerate(
        id = bundleNotificationId,
        productId = bundle.id,
        productType = ProductType.Bundle
      )

      mockReadNotifications(
        List(
          goodsNotification,
          subscriptionNotification,
          bundleNotification
        )
      )

      mockGoodsService(GoodsDao.Filter.ForGoodsId(goods.id), goods)

      mockNotificationSender(goods)
      val exception = new Exception("excaption in change status in databse")
      (failedProlongationNotificationPersistService
        .markStatusAsProcessed(_: Long, _: Option[String]))
        .expects(goodsNotificationId, None)
        .throwingZ(exception)

      (failedProlongationNotificationPersistService
        .markStatusAsProcessed(_: Long, _: Option[String]))
        .expects(
          goodsNotificationId,
          Some(
            s"Error send prolongation notifications $goodsNotification error: $exception"
          )
        )
        .throwingZ(exception)

      task
        .execute()
        .failed
        .get shouldBe exception
    }

  }

  private def mockReadNotifications(
      response: List[ProlongationFailedNotification]
  ): Unit =
    (failedProlongationNotificationPersistService
      .getUnProcessed(
        _: Int,
        _: Duration
      ))
      .expects(
        10,
        processingInterval
      )
      .returningZ(
        response
      )

  private def notificationGenerate(
      id: Long,
      productId: String,
      productType: ProductType = ProductType.Goods,
      status: ProlongationFailedNotificationStatus =
        ProlongationFailedNotificationStatus.UnProcessed,
      details: Option[String] = None,
      epoch: DateTime = DateTime.now(),
      lastUpdate: DateTime = DateTime.now()
  ): ProlongationFailedNotification =
    ProlongationFailedNotification(
      id = id,
      productType = productType,
      productId = productId,
      status = status,
      details = details,
      epoch = epoch,
      lastUpdate = lastUpdate
    )

  private def mockGoodsService(query: GoodsDao.Filter, response: Goods): Unit =
    (goodsService
      .get(_: GoodsDao.Filter))
      .expects(query)
      .returningZ(List(response))

  private def mockSubscriptionService(
      query: SubscriptionDao.Filter,
      response: Subscription
  ): Unit =
    (subscriptionService
      .get(_: SubscriptionDao.Filter))
      .expects(query)
      .returningZ(List(response))

  private def mockBundleService(
      query: BundleDao.Filter,
      response: Bundle
  ): Unit =
    (bundleService
      .get(_: BundleDao.Filter))
      .expects(query)
      .returningZ(List(response))

  private def mockNotificationSender(request: PaidProduct): Unit =
    (notificationSender
      .notifyProlongationFailed(_: PaidProduct))
      .expects(request)
      .returningZ(Unit)

  private def mockThrowableInNotificationSender(
      request: PaidProduct,
      exception: Throwable
  ): Unit =
    (notificationSender
      .notifyProlongationFailed(_: PaidProduct))
      .expects(request)
      .throwingZ(exception)

  private def mockSaveProcessedNotification(
      id: Long,
      details: Option[String] = None
  ): Unit =
    (failedProlongationNotificationPersistService
      .markStatusAsProcessed(_: Long, _: Option[String]))
      .expects(id, details)
      .returningZ(1)

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
