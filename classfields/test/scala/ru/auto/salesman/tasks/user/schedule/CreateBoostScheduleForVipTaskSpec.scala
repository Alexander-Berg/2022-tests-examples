package ru.auto.salesman.tasks.user.schedule

import org.joda.time.DateTime
import ru.auto.salesman.dao.user.ProductScheduleDao
import ru.auto.salesman.model
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles
import ru.auto.salesman.model.user.schedule.ScheduleSource
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.EpochService
import ru.auto.salesman.service.user.{BundleService, UserFeatureService}
import ru.auto.salesman.tasks.Markers
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.EpochInPastGen
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import zio.UIO

class CreateBoostScheduleForVipTaskSpec
    extends BaseSpec
    with ProductScheduleModelGenerators {

  private val epochService: EpochService = mock[EpochService]
  private val productScheduleDao = mock[ProductScheduleDao]
  private val bundleService = mock[BundleService]

  private val featureService = new UserFeatureService {
    def cashbackFullPaymentRestrictionEnabled: Boolean = ???
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

    override val vipBoostUsingCustomPriceEnabled: UIO[Boolean] = UIO(false)
  }

  val task = new CreateBoostScheduleForVipTask(
    bundleService,
    productScheduleDao,
    epochService,
    featureService
  )

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  implicit val rc: RequestContext = AutomatedContext("unit-test")

  "CreateBoostScheduleForVipTask" should {
    "insert schedule should get and set Marker" in {
      forAll(EpochInPastGen) { epoch =>
        forAll(
          list(1, 10, goodsBundleActivatedGen(new DateTime(epoch + 100)))
        ) { goodsList =>
          val n = goodsList.size / 2
          goodsList.patch(
            n,
            goodsList.take(n).map(_.copy(product = AutoruBundles.Vip)),
            1
          )

          (bundleService.get _)
            .expects(*)
            .returningZ(goodsList)

          (epochService
            .getOptional(_: String))
            .expects(Markers.CreateBoostScheduleForVipEpoch)
            .returningT(Option(epoch))
          (epochService
            .set(_: String, _: model.Epoch))
            .expects(Markers.CreateBoostScheduleForVipEpoch, *)
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
          .expects(Markers.CreateBoostScheduleForVipEpoch)
          .returningT(Option(epoch))
        val ps: Iterable[ScheduleSource] = Iterable()
        (productScheduleDao
          .replace(_: Iterable[ScheduleSource]))
          .expects(ps)
          .returningT(Unit)
        (epochService
          .set(_: String, _: model.Epoch))
          .expects(Markers.CreateBoostScheduleForVipEpoch, *)
          .returningT(Unit)
        task.execute().success
      }
    }
    "insert schedule shouldn't call epochService.set if Failure before it" in {
      forAll(EpochInPastGen) { epoch =>
        (epochService
          .getOptional(_: String))
          .expects(Markers.CreateBoostScheduleForVipEpoch)
          .returningT(Option(epoch))

        (bundleService.get _)
          .expects(*)
          .throwingZ(new Exception("test"))

        task.execute().failed
      }
    }
  }
}
