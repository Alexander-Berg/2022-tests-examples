package auto.carfax.carfax_money.logic.test

import auto.carfax.carfax_money.logic.{EstimateLogic, MoneyManager, ValidationManager}
import auto.carfax.carfax_money.model.Meta.Defaults
import auto.carfax.carfax_money.model.errors.CalculationPriceError.{
  IncorrectPackageCountError,
  NotAllowedPackageCountError,
  UnknownServiceError,
  UnknownUserError
}
import auto.carfax.carfax_money.storage.StorageService
import auto.carfax.money.money_model.{PriceCriteria, ServiceContext, ServiceResult, UserContext}
import auto.carfax.money.money_model.ServiceContext.PackageInfo
import common.zio.events_broker.Broker
import common.zio.events_broker.testkit.TestBroker
import common.zio.logging.Logging
import zio.ZLayer
import zio.test._
import zio.test.Assertion._

object MoneyManagerSpec extends DefaultRunnableSpec {

  implicit private val requestId: String = ""

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("MoneyManager") {
      val estimateLogicLive = (Logging.live ++ StorageService.live) >+> EstimateLogic.live
      val managerLive =
        (estimateLogicLive ++ ValidationManager.live ++ ZLayer.succeed[Broker.Service](
          new TestBroker.NoOpBroker
        )) >>> MoneyManager.live
      testM("should fail for incorrect report type") {
        val criteria = PriceCriteria()
        assertM(MoneyManager.calculate(criteria).run)(
          fails(equalTo(UnknownServiceError(criteria.getServiceContext)))
        )
          .provideLayer(managerLive)

      }

      testM("should fail for incorrect report count") {
        val criteria = PriceCriteria().withServiceContext(
          ServiceContext(
            serviceType = ServiceContext.ServiceType.OFFER_REPORT_SERVICE,
            packageInfo = Some(PackageInfo.of(3))
          )
        )
        assertM(MoneyManager.calculate(criteria).run)(
          fails(equalTo(NotAllowedPackageCountError(criteria)))
        )
          .provideLayer(managerLive)
      }

      testM("should fail for incorrect report packages for reseller 5") {

        val criteria = PriceCriteria()
          .withServiceContext(
            ServiceContext(
              serviceType = ServiceContext.ServiceType.OFFER_REPORT_SERVICE,
              packageInfo = Some(PackageInfo.of(5))
            )
          )
          .withUserContext(UserContext(uid = "user:123", isUserReseller = true))
        assertM(MoneyManager.calculate(criteria).run)(
          fails(equalTo(IncorrectPackageCountError(criteria)))
        )
          .provideLayer(managerLive)
      }

      testM("should fail for incorrect report packages for not reseller") {

        val criteria = PriceCriteria()
          .withServiceContext(
            ServiceContext(
              serviceType = ServiceContext.ServiceType.OFFER_REPORT_SERVICE,
              packageInfo = Some(PackageInfo.of(50))
            )
          )
          .withUserContext(UserContext(uid = "user:123", isUserReseller = false))
        assertM(MoneyManager.calculate(criteria).run)(
          fails(equalTo(IncorrectPackageCountError(criteria)))
        )
          .provideLayer(managerLive)
      }

      testM("should fail for incorrect user") {

        val criteria = PriceCriteria()
          .withServiceContext(
            ServiceContext(
              serviceType = ServiceContext.ServiceType.OFFER_REPORT_SERVICE,
              packageInfo = Some(PackageInfo.of(1))
            )
          )
          .withUserContext(UserContext(uid = "anon:123", isUserReseller = false, geoId = 1))
        assertM(MoneyManager.calculate(criteria).run)(
          fails(equalTo(UnknownUserError(criteria.getUserContext)))
        )
          .provideLayer(managerLive)
      }

      testM("should return correct price calculation for user") {

        val criteria = PriceCriteria()
          .withServiceContext(
            ServiceContext(
              serviceType = ServiceContext.ServiceType.VIN_REPORT_SERVICE,
              packageInfo = Some(PackageInfo.of(1))
            )
          )
          .withUserContext(UserContext(uid = "user:123", isUserReseller = false, geoId = 1))
        assertM(MoneyManager.calculate(criteria))(
          equalTo(
            ServiceResult.of(
              Defaults.DefaultUserVinHistoryOneReportPrice.price,
              Defaults.DefaultUserVinHistoryOneReportPrice.duration,
              Some(criteria)
            )
          )
        )
          .provideLayer(managerLive)
      }

      testM("should return correct price calculation for dealer and type offer report") {

        val criteria = PriceCriteria()
          .withServiceContext(
            ServiceContext(
              serviceType = ServiceContext.ServiceType.OFFER_REPORT_SERVICE,
              packageInfo = Some(PackageInfo.of(1))
            )
          )
          .withUserContext(UserContext(uid = "dealer:123", geoId = 1))
        assertM(MoneyManager.calculate(criteria))(
          equalTo(
            ServiceResult.of(Defaults.DefaultDealerPrice.price, Defaults.DefaultDealerPrice.duration, Some(criteria))
          )
        )
          .provideLayer(managerLive)
      }

      testM("should return correct price calculation for dealer and type vin report") {

        val criteria = PriceCriteria()
          .withServiceContext(
            ServiceContext(
              serviceType = ServiceContext.ServiceType.VIN_REPORT_SERVICE,
              packageInfo = Some(PackageInfo.of(1))
            )
          )
          .withUserContext(UserContext(uid = "dealer:123", geoId = 1))
        assertM(MoneyManager.calculate(criteria))(
          equalTo(
            ServiceResult.of(Defaults.DefaultDealerPrice.price, Defaults.DefaultDealerPrice.duration, Some(criteria))
          )
        )
          .provideLayer(managerLive)
      }
    }
  }
}
