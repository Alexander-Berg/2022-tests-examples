package auto.carfax.carfax_money.logic.test

import zio.test._
import auto.carfax.carfax_money.logic.EstimateLogic
import auto.carfax.carfax_money.logic.testkit.PriceCriteriaGen
import auto.carfax.carfax_money.model.Meta.Defaults
import auto.carfax.carfax_money.storage.StorageService
import auto.common.model.user.AutoruUser
import common.zio.logging.Logging

object EstimateLogicSpec extends DefaultRunnableSpec {

  implicit private val requestId: String = ""

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("EstimateLogic") {
      val managerLive = (StorageService.live ++ Logging.live) >>> EstimateLogic.live

      testM("should return correct price for dealers vin history report") {
        checkNM(100)(PriceCriteriaGen.dealersCriteriaGen) { criteria =>
          (for {
            result <- EstimateLogic.estimate(criteria, AutoruUser.UserRef.fromString(criteria.getUserContext.uid))
          } yield assertTrue(result == Defaults.DefaultDealerPrice))
            .provideLayer(managerLive)
        }
      }

      testM("should return correct price for dealers offer history report") {
        checkNM(100)(PriceCriteriaGen.dealersCriteriaGen) { criteria =>
          (for {
            result <- EstimateLogic.estimate(criteria, AutoruUser.UserRef.fromString(criteria.getUserContext.uid))
          } yield assertTrue(result == Defaults.DefaultDealerPrice))
            .provideLayer(managerLive)
        }
      }

      testM("should return correct price for users vin history report") {
        checkNM(10000)(PriceCriteriaGen.userVinCriteriaGen) { criteria =>
          (for {
            result <- EstimateLogic.estimate(criteria, AutoruUser.UserRef.fromString(criteria.getUserContext.uid))
          } yield assertTrue(criteria.getServiceContext.getPackageInfo.servicesCount match {
            case 1 => Defaults.DefaultUserVinHistoryOneReportPrice == result
            case 5 => Defaults.DefaultUserFiveReportsPrice == result
            case 10 => Defaults.DefaultUserTenReportsPrice == result
            case 50 => Defaults.DefaultUserFiftyReportsPrice == result
            case _ => false
          }))
            .provideLayer(managerLive)
        }
      }

      testM("should return correct price for users offer history report") {
        checkNM(10000)(PriceCriteriaGen.userOfferCriteriaGen) { criteria =>
          (for {
            result <- EstimateLogic.estimate(criteria, AutoruUser.UserRef.fromString(criteria.getUserContext.uid))
          } yield assertTrue(criteria.getServiceContext.getPackageInfo.servicesCount match {
            case 1 => result.duration == 365 && result.price >= 6900 && result.price <= 39700
            case 5 => Defaults.DefaultUserFiveReportsPrice == result
            case 10 => Defaults.DefaultUserTenReportsPrice == result
            case 50 => Defaults.DefaultUserFiftyReportsPrice == result
            case _ => false
          }))
            .provideLayer(managerLive)
        }
      }

      testM("should return correct price for anon user offer-history-report") {
        checkNM(10000)(PriceCriteriaGen.userOfferCriteriaGen) { criteria =>
          (for {
            result <- EstimateLogic.estimate(criteria, None)
          } yield assertTrue(criteria.getServiceContext.getPackageInfo.servicesCount match {
            case 1 => result.duration == 365 && result.price >= 6900 && result.price <= 39700
            case 5 => Defaults.DefaultUserFiveReportsPrice == result
            case 10 => Defaults.DefaultUserTenReportsPrice == result
            case 50 => Defaults.DefaultUserFiftyReportsPrice == result
            case _ => false
          }))
            .provideLayer(managerLive)
        }
      }
    }
}
