package ru.auto.salesman.tasks.user.schedule

import org.joda.time.{DateTime, DateTimeZone, LocalTime}
import ru.auto.api.billing.schedules.ScheduleModel.ScheduleRequest
import ru.auto.api.billing.schedules.ScheduleModel.ScheduleType.ONCE_AT_TIME
import ru.auto.salesman._
import ru.auto.salesman.client.PromocoderClient
import ru.auto.salesman.dao.impl.jdbc.user.{
  JdbcBundleDao,
  JdbcGoodsDao,
  JdbcProductScheduleDao,
  JdbcUserCashbackDao
}
import ru.auto.salesman.dao.user.BundleDao
import ru.auto.salesman.dao.user.ProductScheduleDao.ScheduleFilter._
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.ProductContext.BundleContext
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.Vip
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Boost
import ru.auto.salesman.model.user.schedule.AllowMultipleRescheduleUpsert.SameOrTrue
import ru.auto.salesman.model.user.schedule.{
  IsVisible,
  ProductSchedule,
  ScheduleParameters,
  ScheduleSource
}
import ru.auto.salesman.model.user.{Price, ProductPrice, Prolongable}
import ru.auto.salesman.model.{AutoruUser, ProductDuration, ProductStatuses}
import ru.auto.salesman.service.EpochService
import ru.auto.salesman.service.impl.UserPromocodesServiceImpl
import ru.auto.salesman.service.impl.user.goods.BoostScheduler
import ru.auto.salesman.service.impl.user.{
  BundleServiceImpl,
  GoodsServiceImpl,
  PromocoderServiceImpl
}
import ru.auto.salesman.service.schedules.AsyncScheduleCrudService
import ru.auto.salesman.service.user.UserFeatureService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate
import ru.yandex.common.monitoring.CompoundHealthCheckRegistry
import zio.{Ref, UIO, ZIO}

class CreateBoostScheduleForVipTaskIntSpec
    extends BaseSpec
    with SalesmanUserJdbcSpecTemplate {

  private val goodsDao = new JdbcGoodsDao(database)
  private val bundleDao = new JdbcBundleDao(database)
  private val userCashbackDao = new JdbcUserCashbackDao(transactor, transactor)
  private val scheduleDao = new JdbcProductScheduleDao(database)
  private val epochService = stub[EpochService]
  private val promocoder = stub[PromocoderClient]
  private val promocoderService = new PromocoderServiceImpl(promocoder)

  private val vipBoostUsingCustomPriceEnabledValue = Ref.make(false).unsafeRun()

  private val featureService = new UserFeatureService {
    def cashbackFullPaymentRestrictionEnabled: Boolean = true
    def vinDecoderSttpClientEnabled: Boolean = ???
    def bestServicePriceEnabled: Boolean = ???
    def increasePlacementByVipReleaseDateFeature: DateTime = ???
    def prolongationFailedPushNotificationEnabled: Boolean = ???
    def prolongationFailedSmsNotificationEnabled: Boolean = ???

    def prolongationFailedEmailNotificationEnabled: Boolean = ???

    def enableAsyncSendingProlongationFailedNotification: Boolean = ???

    def requestPriceFromVosMasterEnabled: Boolean = ???

    def userQuotaModerationEventsEnabled: Boolean = ???

    def statisticLogBrokerEnabled: Boolean = ???

    def statisticLogBrokerSyncLogEnabled: Boolean = ???

    def useTrustGate: Boolean = ???

    def callCarfaxForReportPrice: Boolean = ???

    def usePriceFromCarfaxForReportPrice: Boolean = ???

    def useNewRecurrentPaymentWay: Boolean = ???

    def useTrustForScheduledPayments: Boolean = ???

    override val vipBoostUsingCustomPriceEnabled: UIO[Boolean] =
      vipBoostUsingCustomPriceEnabledValue.get
  }

  private val userPromocodesService =
    new UserPromocodesServiceImpl(promocoderService, userCashbackDao)

  private val boostScheduler = new BoostScheduler(scheduleDao)

  private val goodsService =
    new GoodsServiceImpl(
      goodsDao,
      bundleDao,
      promocoder,
      userPromocodesService,
      boostScheduler,
      featureService
    )

  private val bundleService =
    new BundleServiceImpl(bundleDao, goodsService, promocoder, featureService)

  private val task =
    new CreateBoostScheduleForVipTask(
      bundleService,
      scheduleDao,
      epochService,
      featureService
    )

  private val scheduleService =
    new AsyncScheduleCrudService(scheduleDao, new CompoundHealthCheckRegistry)

  private val createBoostScheduleForVip = ZIO.fromBlockingTry(task.execute())

  private val offerId = AutoruOfferId("1101595454-fc557038")
  private val user = AutoruUser(13106730)

  "CreateBoostScheduleForVipTask" should {

    "create schedule with allowMultipleReschedule = false when user already has ordinary boost schedule" in {
      for {
        _ <- createOrdinaryBoostSchedule(offerId, user)
        _ <- createVip(offerId, user)
        _ = stubEpochService()
        _ <- createBoostScheduleForVip
        result <- getActiveSchedule(offerId)
      } yield result.loneElement.allowMultipleReschedule shouldBe false
    }.success

    "create schedule so that it survives user update" in {
      for {
        _ <- createVip(offerId, user)
        _ = stubEpochService()
        _ <- createBoostScheduleForVip
        scheduleCreatedByVip <- getActiveSchedule(offerId).map(_.loneElement)
        _ <- receiveScheduleUpdateFromUser(scheduleCreatedByVip, time = "11:00")
        result <- getActiveSchedule(offerId)
      } yield result.loneElement.allowMultipleReschedule shouldBe false
    }.success

    "create schedule with empty custom price when vip-boost-using-custom-price-enabled feature is turned off" in {
      for {
        _ <- createOrdinaryBoostSchedule(offerId, user)
        _ <- createVip(offerId, user)
        _ = stubEpochService()
        _ <- createBoostScheduleForVip
        result <- getActiveSchedule(offerId)
      } yield result.loneElement.customPrice shouldBe None
    }.success

    "create schedule with custom price = 0 when vip-boost-using-custom-price-enabled feature is turned on" in {
      for {
        _ <- createOrdinaryBoostSchedule(offerId, user)
        _ <- createVip(offerId, user)
        _ = stubEpochService()
        _ <- vipBoostUsingCustomPriceEnabledValue.set(true)
        _ <- createBoostScheduleForVip
        result <- getActiveSchedule(offerId)
      } yield result.loneElement.customPrice.value shouldBe 0
    }.success
  }

  private def stubEpochService(): Unit = {
    (epochService.getOptional _).when(*).returningT(None)
    (epochService.set _).when(*, *).returningT(())
  }

  private def createOrdinaryBoostSchedule(
      offerId: AutoruOfferId,
      user: AutoruUser
  ) = ZIO.fromBlockingTry {
    scheduleDao.insertIfAbsent(
      ScheduleSource(
        offerId,
        user,
        Boost,
        ScheduleParameters.OnceAtTime(
          weekdays = Set(1),
          LocalTime.parse("10:00"),
          DateTimeZone.forID("Europe/Moscow")
        ),
        IsVisible(true),
        expireDate = None,
        customPrice = None,
        allowMultipleReschedule = SameOrTrue,
        prevScheduleId = None
      )
    )
  }

  private def receiveScheduleUpdateFromUser(
      schedule: ProductSchedule,
      time: String
  ) =
    ZIO.fromFuture { _ =>
      scheduleService.putSchedules(
        schedule.user,
        schedule.product,
        List(schedule.offerId),
        ScheduleRequest
          .newBuilder()
          .setScheduleType(ONCE_AT_TIME)
          .setTime(time)
          .build()
      )
    }

  private def createVip(offerId: AutoruOfferId, user: AutoruUser) = {
    val activated = DateTime.parse("2020-12-30T12:00:00+03:00")
    bundleDao.insertIfNotExists(
      BundleDao.Request(
        Some(offerId),
        user.toString,
        Vip,
        amount = 100,
        ProductStatuses.Active,
        transactionId = "test",
        baseBundleId = None,
        activated,
        deadline = activated.plusDays(60),
        BundleContext(
          ProductPrice(
            Vip,
            ProductDuration.days(60),
            paymentReason = None,
            Price(
              basePrice = 100,
              effectivePrice = 100,
              prolongPrice = None,
              modifier = None,
              policyId = None
            ),
            prolongationAllowed = false,
            prolongationForced = false,
            prolongationForcedNotTogglable = false,
            productPriceInfo = None,
            analytics = None,
            scheduleFreeBoost = false
          )
        ),
        Prolongable(false)
      )
    )
  }

  private def getActiveSchedule(offerId: AutoruOfferId) = ZIO.fromBlockingTry {
    scheduleDao.get(ForOfferId(offerId), IsDeleted(false))
  }
}
