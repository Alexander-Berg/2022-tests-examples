package ru.auto.salesman.tasks.user.schedule

import org.joda.time.DateTime
import ru.auto.salesman.dao.user.BundleDao.Filter
import ru.auto.salesman.dao.user.ProductScheduleDao
import ru.auto.salesman.model
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles
import ru.auto.salesman.model.user.schedule.ScheduleSource
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, Epoch}
import ru.auto.salesman.service.EpochService
import ru.auto.salesman.service.user.BundleService
import ru.auto.salesman.tasks.Markers
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.EpochInPastGen
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.util.time.DateTimeUtil

class CreateRefreshScheduleForVipTaskSpec
    extends BaseSpec
    with ProductScheduleModelGenerators {

  private val epochService = mock[EpochService]
  private val productScheduleDao = mock[ProductScheduleDao]
  private val bundleService = mock[BundleService]

  val task = new CreateRefreshScheduleForVipTask(
    bundleService,
    productScheduleDao,
    epochService,
    new VipRefreshScheduleCalculator()
  )

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  implicit val rc: RequestContext = AutomatedContext("unit-test")

  "CreateRefreshScheduleForVipTask" should {
    "insert schedule should get and set Marker" in {
      forAll(EpochInPastGen) { epoch =>
        forAll(
          list(1, 10, goodsBundleActivatedGen(new DateTime(epoch + 100)))
        ) { goodsList =>
          val vipBundles = goodsList.map(
            _.copy(
              product = AutoruBundles.Vip,
              deadline = new DateTime(epoch + 100).plusDays(60)
            )
          )

          (bundleService.get _)
            .expects(
              Filter.VipActiveActivatedAfter(DateTimeUtil.fromMillis(epoch))
            )
            .returningZ(vipBundles)

          (epochService
            .getOptional(_: String))
            .expects(Markers.CreateRefreshScheduleForVipEpoch)
            .returningT(Option(epoch))
          (epochService
            .set(_: String, _: model.Epoch))
            .expects(
              Markers.CreateRefreshScheduleForVipEpoch,
              argThat { epoch: Epoch =>
                math.abs(DateTimeUtil.now().getMillis - epoch) < 3000
              }
            )
            .returningT(Unit)
          (productScheduleDao
            .replace(_: Iterable[ScheduleSource]))
            .expects(*)
            .returningT(Unit)
          task.execute().success
        }
      }
    }
    "insert schedule should get and set Marker even if insertIfAbsent called with empty" in {
      forAll(EpochInPastGen) { epoch =>
        (bundleService.get _)
          .expects(*)
          .returningZ(None)
        (epochService
          .getOptional(_: String))
          .expects(Markers.CreateRefreshScheduleForVipEpoch)
          .returningT(Option(epoch))
        val ps: Iterable[ScheduleSource] = Iterable()
        (productScheduleDao
          .replace(_: Iterable[ScheduleSource]))
          .expects(ps)
          .returningT(Unit)
        (epochService
          .set(_: String, _: model.Epoch))
          .expects(
            Markers.CreateRefreshScheduleForVipEpoch,
            argThat { epoch: Epoch =>
              math.abs(DateTimeUtil.now().getMillis - epoch) < 3000
            }
          )
          .returningT(Unit)
        task.execute().success
      }
    }
    "insert schedule shouldn't call epochService.set if Failure before it" in {
      forAll(EpochInPastGen) { epoch =>
        (epochService
          .getOptional(_: String))
          .expects(Markers.CreateRefreshScheduleForVipEpoch)
          .returningT(Option(epoch))

        (bundleService.get _)
          .expects(*)
          .throwingZ(new Exception("test"))

        task.execute().failed
      }
    }
  }
}
