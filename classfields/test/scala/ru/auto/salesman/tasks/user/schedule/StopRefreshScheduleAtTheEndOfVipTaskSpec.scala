package ru.auto.salesman.tasks.user.schedule

import org.joda.time.DateTime
import ru.auto.salesman.dao.user.ProductScheduleDao
import ru.auto.salesman.model
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, Epoch}
import ru.auto.salesman.service.EpochService
import ru.auto.salesman.service.user.BundleService
import ru.auto.salesman.tasks.Markers
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.EpochInPastGen
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.util.time.DateTimeUtil

class StopRefreshScheduleAtTheEndOfVipTaskSpec
    extends BaseSpec
    with ProductScheduleModelGenerators {
  val epochService: EpochService = mock[EpochService]
  val productScheduleDao = mock[ProductScheduleDao]
  val bundleService = mock[BundleService]

  val task = new StopRefreshScheduleAtTheEndOfVipTask(
    bundleService,
    productScheduleDao,
    epochService
  )

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  implicit val rc: RequestContext = AutomatedContext("unit-test")

  "StopRefreshScheduleAtTheEndOfVipTask" should {
    "should get and set Marker" in {
      forAll(EpochInPastGen) { epoch =>
        forAll(
          list(1, 10, goodsBundleActivatedGen(new DateTime(epoch + 100)))
        ) { goodsList =>
          (epochService
            .getOptional(_: String))
            .expects(Markers.StopRefreshScheduleAtTheEndOfVipEpoch)
            .returningT(Option(epoch))

          (bundleService.get _)
            .expects(*)
            .returningZ(goodsList)

          (epochService
            .set(_: String, _: model.Epoch))
            .expects(
              Markers.StopRefreshScheduleAtTheEndOfVipEpoch,
              argThat { epoch: Epoch =>
                math.abs(DateTimeUtil.now().getMillis - epoch) < 3000
              }
            )
            .returningT(Unit)

          (productScheduleDao
            .update(
              _: ProductScheduleDao.Patch,
              _: ProductScheduleDao.ScheduleUpdateFilter
            ))
            .expects(*, *)
            .anyNumberOfTimes()
            .returningT(Unit)

          task.execute().success
        }
      }
    }
    "shouldn't set Marker" in {
      forAll(EpochInPastGen) { epoch =>
        forAll(
          list(1, 10, goodsBundleActivatedGen(new DateTime(epoch + 100)))
        ) { goodsList =>
          (epochService
            .getOptional(_: String))
            .expects(Markers.StopRefreshScheduleAtTheEndOfVipEpoch)
            .returningT(Option(epoch))

          (bundleService.get _)
            .expects(*)
            .returningZ(goodsList)

          (productScheduleDao
            .update(
              _: ProductScheduleDao.Patch,
              _: ProductScheduleDao.ScheduleUpdateFilter
            ))
            .expects(*, *)
            .anyNumberOfTimes()
            .throwingT(new Exception("test"))

          task.execute().failure
        }
      }
    }
  }
}
